package org.hibernate.persister.entity.mutation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.model.TableInsert;
import org.hibernate.sql.ast.spi.model.builder.TableInsertBuilderStandard;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilder;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.spi.mutation.TableMapping;

import static org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer.UNFETCHED_PROPERTY;
import static org.hibernate.engine.jdbc.mutation.ParameterUsage.RESTRICT;
import static org.hibernate.internal.util.collections.ArrayHelper.toIntArray;
import static org.hibernate.persister.entity.mutation.AbstractMutationCoordinator.addColumnFromLiveTable;

/// Builds the history insert and the assignments for an update of an open revision.
/// Generated properties take their stored values from the live row, including when a
/// detached instance has no value for an insert-only property.
@Internal
public final class HistoryTableMutation {
	private final EntityPersister persister;
	private final EntityTableMapping source;
	private final TemporalMapping temporal;
	private final TableMapping history;
	private final Map<String, SelectableMapping> generatedColumns = new LinkedHashMap<>();

	public HistoryTableMutation(EntityPersister persister, TableMapping history) {
		this.persister = persister;
		this.history = history;
		source = persister.getIdentifierTableMapping();
		temporal = persister.getTemporalMapping();
		for ( int index : source.getAttributeIndexes() ) {
			final var attribute = persister.getAttributeMapping( index );
			if ( isGenerated( attribute ) ) {
				attribute.forEachNonFormula( (i, column) -> {
					if ( source.getTableName().equals( persister.physicalTableNameForMutation( column ) )
							&& !temporal.isComputedHistoryColumn( column.getSelectionExpression() ) ) {
						generatedColumns.putIfAbsent( column.getSelectionExpression(), column );
					}
				} );
			}
		}
		// A read-only projection of an identifier column shares the identifier assignment.
		source.getKeyMapping().forEachKeyColumn( (i, key) -> generatedColumns.remove( key.getColumnName() ) );
	}

	public static boolean isGenerated(AttributeMapping attribute) {
		final var generator = attribute.getGenerator();
		return generator != null && generator.generatesSometimes();
	}

	private boolean readsLiveTable() {
		return !generatedColumns.isEmpty();
	}

	/// A writable attribute may share its column with a read-only generated attribute.
	private boolean bindFromState(SelectableMapping column) {
		return !column.isFormula() && !generatedColumns.containsKey( column.getSelectionExpression() )
				&& !temporal.isComputedHistoryColumn( column.getSelectionExpression() )
				&& source.getTableName().equals( persister.physicalTableNameForMutation( column ) );
	}

	public TableInsert createInsert(boolean[] inclusions) {
		final var builder = new TableInsertBuilderStandard( persister, history, persister.getFactory() );
		for ( int index : source.getAttributeIndexes() ) {
			final var attribute = persister.getAttributeMapping( index );
			if ( inclusions[index] && !attribute.isPluralAttributeMapping() && !isGenerated( attribute ) ) {
				attribute.forEachInsertable( (i, column) -> {
					if ( bindFromState( column ) ) {
						builder.addColumnAssignment( column );
					}
				} );
			}
		}
		for ( var column : generatedColumns.values() ) {
			builder.addColumnAssignment( column, column.getSelectionExpression() );
		}
		final var target = builder.getMutatingTable();
		final var start = new ColumnReference( target, temporal.getStartingColumnMapping() );
		final var end = new ColumnReference( target, temporal.getEndingColumnMapping() );
		builder.addColumnAssignment( temporal.createStartingValueBinding( start ) );
		builder.addColumnAssignment( temporal.createNullEndingValueBinding( end ) );
		if ( readsLiveTable() ) {
			source.getKeyMapping().forEachKeyColumn( (i, key) -> builder.addColumnAssignment( key, key.getColumnName() ) );
			return builder.buildInsertSelect( source );
		}
		source.getKeyMapping().forEachKeyColumn( builder::addColumnAssignment );
		return builder.buildMutation();
	}

	public void bindInsert(Object id, Object[] state, boolean[] inclusions,
			BiConsumer<Object, String> bindValue, SharedSessionContractImplementor session) {
		source.getKeyMapping().breakDownKeyJdbcValues( id,
				(value, column) -> bindValue.accept( value, column.getColumnName() ), session );
		for ( int index : source.getAttributeIndexes() ) {
			final var attribute = persister.getAttributeMapping( index );
			if ( inclusions[index] && !attribute.isPluralAttributeMapping() && !isGenerated( attribute ) ) {
				bindAttribute( attribute, state[index], true, bindValue, session );
			}
		}
		if ( TemporalMutationHelper.isUsingParameters( session ) ) {
			bindValue.accept( session.getCurrentChangesetIdentifier(), temporal.getStartingColumnMapping().getSelectionExpression() );
		}
	}

	public record UpdateAssignments(int[] bindableIndexes, boolean readsLiveTable) {}

	/// Returns null if the revision has no columns to update.
	public @Nullable UpdateAssignments addUpdateAssignments(
			TableUpdateBuilder<?> builder, Object[] state, @Nullable int[] dirtyIndexes, boolean currentRevision) {
		final var updateability = persister.getPropertyUpdateability();
		final boolean[] dirty = new boolean[updateability.length];
		if ( dirtyIndexes != null ) {
			for ( int index : dirtyIndexes ) {
				dirty[index] = true;
			}
		}
		if ( currentRevision && persister.isVersioned() ) {
			// A subsequent flush may advance the version without listing it among the dirty attributes.
			dirty[persister.getVersionPropertyIndex()] = true;
		}
		final var bindable = new ArrayList<Integer>();
		final Set<String> copied = readsLiveTable() ? new LinkedHashSet<>() : Set.of();
		for ( int index : source.getAttributeIndexes() ) {
			final var attribute = persister.getAttributeMapping( index );
			final var generator = attribute.getGenerator();
			final boolean generatedOnUpdate = generator != null && generator.generatesOnUpdate();
			if ( attribute.isPluralAttributeMapping() || !currentRevision && !persister.isPropertyTemporalExcluded( index )
					&& !generatedOnUpdate ) {
				continue;
			}
			final boolean writeValue = updateability[index] && ( dirtyIndexes == null || dirty[index] );
			if ( isGenerated( attribute ) ) {
				if ( generatedOnUpdate || writeValue ) {
					attribute.forEachNonFormula( (i, column) -> {
						if ( generatedColumns.containsKey( column.getSelectionExpression() ) ) {
							copied.add( column.getSelectionExpression() );
						}
					} );
				}
			}
			else if ( writeValue && state[index] != UNFETCHED_PROPERTY ) {
				attribute.forEachUpdatable( (i, column) -> {
					if ( bindFromState( column ) ) {
						builder.addColumnAssignment( column );
					}
					else if ( generatedColumns.containsKey( column.getSelectionExpression() ) ) {
						copied.add( column.getSelectionExpression() );
					}
				} );
				bindable.add( index );
			}
		}
		for ( var column : copied ) {
			addColumnFromLiveTable( builder, source, generatedColumns.get( column ), RESTRICT,
					persister.getFactory().getJdbcServices().getDialect() );
		}
		return builder.hasAssignmentBindings() ? new UpdateAssignments( toIntArray( bindable ), !copied.isEmpty() ) : null;
	}

	public void bindUpdate(Object[] state, int[] indexes, BiConsumer<Object, String> bindValue,
			SharedSessionContractImplementor session) {
		for ( int index : indexes ) {
			bindAttribute( persister.getAttributeMapping( index ), state[index], false, bindValue, session );
		}
	}

	private void bindAttribute(AttributeMapping attribute, Object value, boolean insert,
			BiConsumer<Object, String> bindValue, SharedSessionContractImplementor session) {
		attribute.decompose( value, 0, bindValue, null, (i, binder, unused, jdbcValue, column) -> {
			if ( ( insert ? column.isInsertable() : column.isUpdateable() ) && bindFromState( column ) ) {
				binder.accept( jdbcValue, column.getSelectionExpression() );
			}
		}, session );
	}
}
