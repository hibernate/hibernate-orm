package org.hibernate.persister.entity.mutation;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueParameterList;
import org.hibernate.sql.ast.spi.model.ColumnWriteFragment;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.persister.entity.mutation.TemporalMutationHelper.createRevisionEndBuilder;
import static org.hibernate.persister.entity.mutation.TemporalMutationHelper.isUsingParameters;
import static org.hibernate.sql.model.internal.MutationOperationGroupFactory.noOperations;

/// Writes the revisions of an entity to its
/// [history table][org.hibernate.temporal.TemporalTableStrategy#HISTORY_TABLE].
///
/// A revision is a copy of the row of the table holding current data, with the
/// changeset at which it became current. The three operations are executed by the
/// [work queue][org.hibernate.temporal.spi.HistoryWorkQueue] once the rows of
/// the table for current data have reached their final state for the changeset.
/// Each tolerates a missing row.
///
/// @author Gavin King
/// @since 8.0
@Internal
public final class HistoryRevisionWriter extends AbstractAuxiliaryRowWriter {
	private final TemporalMapping temporalMapping;
	private final EntityTableMapping currentTableMapping;
	private final EntityTableMapping historyTableMapping;
	private final BasicBatchKey endBatchKey;
	private final BasicBatchKey copyBatchKey;
	private final MutationOperationGroup endGroup;
	private final MutationOperationGroup copyGroup;
	/// The attributes whose columns the database may rewrite on any update of the current row.
	private final BitSet generatedOnUpdate = new BitSet();
	/// The columns of the current row which are copied to a revision.
	private final Set<String> copiedColumns;
	/// The columns which may be refreshed in an open revision.
	private final Set<String> refreshableColumns;
	private final Map<Refreshed, MutationOperationGroup> refreshGroups = new ConcurrentHashMap<>();

	/// What is copied to an open revision: the columns of attributes, and columns of no attribute.
	private record Refreshed(BitSet attributes, Set<String> columns) {
	}

	public HistoryRevisionWriter(@Nonnull EntityPersister entityPersister, @Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		temporalMapping = castNonNull( entityPersister.getTemporalMapping() );
		currentTableMapping = entityPersister.getIdentifierTableMapping();
		historyTableMapping = createAuxiliaryTableMapping( currentTableMapping, temporalMapping.getTableName() );
		final String entityName = entityPersister.getEntityName();
		endBatchKey = new BasicBatchKey( entityName + "#HISTORY_END" );
		copyBatchKey = new BasicBatchKey( entityName + "#HISTORY_COPY" );

		copiedColumns = Set.copyOf( temporalMapping.getCopiedColumns( currentTableMapping.getTableName() ) );
		refreshableColumns = new HashSet<>( copiedColumns );
		currentTableMapping.getKeyMapping().forEachKeyColumn( (position, key) -> refreshableColumns.remove( key.getColumnName() ) );
		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping != null ) {
			// The version belongs to the primary key of the history table,
			// and some databases do not allow an update of a key column.
			refreshableColumns.remove( versionMapping.getSelectionExpression() );
		}
		for ( int index : currentTableMapping.getAttributeIndexes() ) {
			final var generator = entityPersister.getAttributeMapping( index ).getGenerator();
			if ( generator != null && generator.generatesOnUpdate() ) {
				generatedOnUpdate.set( index );
			}
		}

		endGroup = buildEndGroup( null );
		copyGroup = buildCopyGroup();
	}

	/// Whether a revision holds a copy of any of the given columns of the current row.
	public boolean copiesAny(@Nonnull List<String> columnNames) {
		for ( String columnName : columnNames ) {
			if ( copiedColumns.contains( columnName ) ) {
				return true;
			}
		}
		return false;
	}

	/// Ends the open revision, if there is one.
	///
	/// @param partitionKeys the partition key of the open revision, as far as it is known
	public void endRevision(
			@Nonnull Object id,
			@Nonnull List<PartitionKeyValue> partitionKeys,
			@Nonnull SharedSessionContractImplementor session) {
		// a null or unknown partition key cannot be bound to the static statement
		final boolean dynamic = PartitionKeyValue.needsDynamicRestriction( entityPersister(), partitionKeys );
		final var group = dynamic ? buildEndGroup( partitionKeys ) : endGroup;
		execute( group, dynamic ? null : endBatchKey, session, bindings -> {
			bindKey( id, bindings, historyTableMapping, session );
			bindPartitionKeys( partitionKeys, bindings, group );
			if ( isUsingParameters( session ) ) {
				bindings.bindValue(
						session.getCurrentChangesetIdentifier(),
						historyTableMapping.getTableName(),
						temporalMapping.getEndingColumnMapping().getSelectionExpression(),
						ParameterUsage.SET
				);
			}
		} );
	}

	/// Opens a new revision holding a copy of the current row, if there is one.
	public void copyRevision(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
		execute( copyGroup, copyBatchKey, session, bindings -> {
			if ( isUsingParameters( session ) ) {
				bindings.bindValue(
						session.getCurrentChangesetIdentifier(),
						historyTableMapping.getTableName(),
						temporalMapping.getStartingColumnMapping().getSelectionExpression(),
						ParameterUsage.SET
				);
			}
			bindKey( id, bindings, historyTableMapping, session );
		} );
	}

	/// Copies the columns of the given attributes, and the given columns, from the
	/// current row to the open revision, along with every column the database may
	/// have generated when that row was updated. Used when a change does not warrant
	/// a new revision.
	///
	/// @param columns columns of the current row which belong to no attribute
	/// @param partitionKeys the partition key of the open revision, as far as it is known
	public void refreshRevision(
			@Nonnull Object id,
			@Nonnull BitSet attributes,
			@Nonnull Collection<SelectableMapping> columns,
			@Nonnull List<PartitionKeyValue> partitionKeys,
			@Nonnull SharedSessionContractImplementor session) {
		final var copied = (BitSet) attributes.clone();
		copied.or( generatedOnUpdate );
		final MutationOperationGroup group;
		if ( PartitionKeyValue.needsDynamicRestriction( entityPersister(), partitionKeys ) ) {
			group = buildRefreshGroup( copied, columns, partitionKeys );
		}
		else {
			final Set<String> columnNames = new HashSet<>();
			for ( var column : columns ) {
				columnNames.add( column.getSelectionExpression() );
			}
			group = refreshGroups.computeIfAbsent( new Refreshed( copied, columnNames ),
					refreshed -> buildRefreshGroup( copied, columns, null ) );
		}
		if ( group.getNumberOfOperations() > 0 ) {
			execute( group, null, session, bindings -> {
				bindKey( id, bindings, historyTableMapping, session );
				bindPartitionKeys( partitionKeys, bindings, group );
			} );
		}
	}

	private void bindPartitionKeys(List<PartitionKeyValue> partitionKeys, JdbcValueBindings bindings, MutationOperationGroup group) {
		// the history table has the partition key columns of the table for current data
		PartitionKeyValue.bindRestrictions( partitionKeys, currentTableMapping.getTableName(), bindings, group.getSingleOperation() );
	}

	/// @param partitionKeys the partition key of the revision, or `null` for
	///                      a parameter for each of its columns
	private MutationOperationGroup buildEndGroup(@Nullable List<PartitionKeyValue> partitionKeys) {
		final var builder = createRevisionEndBuilder( entityPersister(), historyTableMapping, MutationType.UPDATE );
		applyKeyRestriction( null, entityPersister(), builder, historyTableMapping );
		applyPartitionKeyRestriction( builder, currentTableMapping.getTableName(), partitionKeys, TableMapping::getUpdateDetails );
		return createOperationGroup( MutationType.UPDATE, List.of( builder.buildMutation() ) );
	}

	private MutationOperationGroup buildCopyGroup() {
		// the ending column is left null, marking the revision as open
		return createOperationGroup( MutationType.INSERT, List.of( createCopy(
				historyTableMapping,
				currentTableMapping,
				temporalMapping.getCopiedColumns( currentTableMapping.getTableName() ),
				target -> List.of( temporalMapping.createStartingValueBinding(
						new ColumnReference( target, temporalMapping.getStartingColumnMapping() ) ) )
		) ) );
	}

	/// @param partitionKeys the partition key of the revision, or `null` for
	///                      a parameter for each of its columns
	private MutationOperationGroup buildRefreshGroup(
			BitSet attributes, Collection<SelectableMapping> otherColumns,
			@Nullable List<PartitionKeyValue> partitionKeys) {
		final var builder =
				new TableUpdateBuilderStandard<MutationOperation>( entityPersister(), historyTableMapping, factory() );
		final Set<String> columns = new HashSet<>();
		for ( int index = attributes.nextSetBit( 0 ); index >= 0; index = attributes.nextSetBit( index + 1 ) ) {
			final var attribute = entityPersister().getAttributeMapping( index );
			if ( !attribute.isPluralAttributeMapping() ) {
				attribute.forEachSelectable( (position, column) -> refreshColumn( column, columns, builder ) );
			}
		}
		for ( var column : otherColumns ) {
			refreshColumn( column, columns, builder );
		}
		if ( columns.isEmpty() ) {
			return noOperations( MutationType.UPDATE, entityPersister() );
		}
		applyKeyRestriction( null, entityPersister(), builder, historyTableMapping );
		builder.addNullRestriction( temporalMapping.getEndingColumnMapping() );
		applyPartitionKeyRestriction( builder, currentTableMapping.getTableName(), partitionKeys, TableMapping::getUpdateDetails );
		return createOperationGroup( MutationType.UPDATE, List.of( builder.buildMutation() ) );
	}

	private void refreshColumn(
			SelectableMapping column, Set<String> columns, TableUpdateBuilderStandard<MutationOperation> builder) {
		if ( !column.isFormula()
				&& refreshableColumns.contains( column.getSelectionExpression() )
				&& columns.add( column.getSelectionExpression() ) ) {
			builder.addValueColumn( currentValueBinding( builder.getMutatingTable(), column ) );
		}
	}

	/// Assigns a column of the history table the value held by the current row.
	/// The subquery shares the key bindings of the restriction on the revision.
	private ColumnValueBinding currentValueBinding(MutatingTableReference target, SelectableMapping column) {
		final var keys = currentTableMapping.getKeyMapping();
		final var parameters = new ColumnValueParameterList( target, ParameterUsage.RESTRICT, keys.getColumnCount() );
		final var select = new SimpleSelect( dialect() )
				.setTableName( currentTableMapping.getTableName() )
				.addColumn( column.getSelectionExpression() );
		keys.forEachKeyColumn( (position, key) -> {
			select.addRestriction( key.getColumnName() );
			parameters.addColumValueParameter( key );
		} );
		return new ColumnValueBinding( new ColumnReference( target, column ),
				new ColumnWriteFragment( "(" + select.toStatementString() + ")", parameters, column ) );
	}
}
