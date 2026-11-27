package org.hibernate.persister.entity.mutation;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.hibernate.audit.ModificationType;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.model.TableInsert;
import org.hibernate.sql.ast.spi.model.builder.TableInsertBuilderStandard;

/// Assigns the snapshot columns of one audit table. For ADD and MOD the live row
/// supplies the stored generated values. For DEL it has already been removed.
final class AuditTableMutation {
	private final EntityPersister persister;
	private final EntityTableMapping source;
	private final Set<String> computed;
	private final Map<String, StoredColumn> storedColumns = new LinkedHashMap<>();
	private final Set<AttributeMapping> storedAttributes = new LinkedHashSet<>();

	private record StoredColumn(AttributeMapping attribute, SelectableMapping column, String deletionWriteExpression) {}

	AuditTableMutation(EntityPersister persister, EntityTableMapping source, Set<String> computed) {
		this.persister = persister;
		this.source = source;
		this.computed = computed;
		for ( int index : source.getAttributeIndexes() ) {
			final var attribute = persister.getAttributeMapping( index );
			final var generator = attribute.getGenerator();
			// A computed audit column evaluates against the other columns in its audit row.
			// Its SQL expression may depend on any of the physical columns of this table.
			if ( !persister.isPropertyAuditedExcluded( index ) && !attribute.isPluralAttributeMapping()
					&& ( !computed.isEmpty() || generator != null && generator.generatesSometimes() ) ) {
				attribute.forEachNonFormula( (i, column) -> {
					final String name = column.getSelectionExpression();
					if ( belongsToSource( column ) && !computed.contains( name ) ) {
						storedColumns.putIfAbsent( name, new StoredColumn( attribute, column, deletionWriteExpression( column ) ) );
					}
				} );
			}
		}
		source.getKeyMapping().forEachKeyColumn( (i, key) -> storedColumns.remove( key.getColumnName() ) );
		storedColumns.values().forEach( value -> storedAttributes.add( value.attribute() ) );
	}

	private String deletionWriteExpression(SelectableMapping column) {
		final String write = column.getWriteExpression();
		if ( !"?".equals( write ) ) {
			// With no read transformation the snapshot already contains the stored value.
			// Applying a write-only @ColumnTransformer again would change it.
			final var reference = new ColumnReference( (String) null, column );
			if ( column.getSelectionExpression().equals( reference.getExpressionText() ) ) {
				return column.getJdbcMapping().getJdbcType().wrapWriteExpression(
						"?", column.toSize(), persister.getFactory().getJdbcServices().getDialect() );
			}
		}
		return write;
	}

	Set<AttributeMapping> storedAttributes() {
		return storedAttributes;
	}

	private boolean belongsToSource(SelectableMapping column) {
		return source.getTableName().equals( persister.physicalTableNameForMutation( column ) );
	}

	private boolean bindFromState(AttributeMapping attribute, SelectableMapping column, boolean included, ModificationType type) {
		if ( column.isFormula() || !belongsToSource( column ) || computed.contains( column.getSelectionExpression() ) ) {
			return false;
		}
		final var storedColumn = storedColumns.get( column.getSelectionExpression() );
		return storedColumn == null
				? included && column.isInsertable()
				: type == ModificationType.DEL && storedColumn.attribute() == attribute;
	}

	private boolean includeAttribute(AttributeMapping attribute, boolean included, ModificationType type) {
		return !attribute.isPluralAttributeMapping() && ( included
				|| type == ModificationType.DEL && storedAttributes.contains( attribute ) );
	}

	void addStateAssignments(TableInsertBuilderStandard builder, boolean[] inclusions, ModificationType type) {
		for ( int index : source.getAttributeIndexes() ) {
			final var attribute = persister.getAttributeMapping( index );
			if ( includeAttribute( attribute, inclusions[index], type ) ) {
				attribute.forEachNonFormula( (i, column) -> {
					if ( bindFromState( attribute, column, inclusions[index], type ) ) {
						final var stored = storedColumns.get( column.getSelectionExpression() );
						builder.addColumnAssignment( column, stored == null
								? column.getWriteExpression() : stored.deletionWriteExpression() );
					}
				} );
			}
		}
	}

	TableInsert buildInsert(TableInsertBuilderStandard builder, ModificationType type, List<SelectableMapping> indexColumns) {
		final boolean copyStored = type != ModificationType.DEL && !storedColumns.isEmpty();
		// An optional live row may be absent. A scalar subquery preserves its audit row in that case.
		final boolean insertSelect = copyStored && !source.isOptional();
		if ( copyStored ) {
			for ( var value : storedColumns.values() ) {
				copyColumn( builder, value.column(), insertSelect );
			}
		}
		for ( var column : indexColumns ) {
			copyColumn( builder, column, insertSelect );
		}
		if ( insertSelect ) {
			source.getKeyMapping().forEachKeyColumn( (i, key) -> builder.addColumnAssignment( key, key.getColumnName() ) );
			return builder.buildInsertSelect( source );
		}
		source.getKeyMapping().forEachKeyColumn( builder::addColumnAssignment );
		return builder.buildMutation( copyStored || !indexColumns.isEmpty() );
	}

	private void copyColumn(TableInsertBuilderStandard builder, SelectableMapping column, boolean insertSelect) {
		if ( insertSelect ) {
			builder.addColumnAssignment( column, column.getSelectionExpression() );
		}
		else {
			AbstractMutationCoordinator.addColumnFromLiveTable( builder, source, column, ParameterUsage.SET,
					persister.getFactory().getJdbcServices().getDialect() );
		}
	}

	void bindValues(Object[] values, boolean[] inclusions, ModificationType type,
			BiConsumer<Object, String> bind, SharedSessionContractImplementor session) {
		for ( int index : source.getAttributeIndexes() ) {
			final var attribute = persister.getAttributeMapping( index );
			if ( includeAttribute( attribute, inclusions[index], type ) ) {
				attribute.decompose( values[index], 0, bind, null, (i, binder, unused, value, column) -> {
					if ( bindFromState( attribute, column, inclusions[index], type ) ) {
						binder.accept( value, column.getSelectionExpression() );
					}
				}, session );
			}
		}
	}
}
