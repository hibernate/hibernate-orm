package org.hibernate.persister.entity.mutation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.audit.AuditException;
import org.hibernate.audit.ModificationType;
import org.hibernate.engine.jdbc.batch.internal.BasicBatchKey;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AuditMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueParameter;
import org.hibernate.sql.ast.spi.model.ColumnWriteFragment;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.TableMutation;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.MutationType;

/// Writes the audit rows of an entity to its audit log tables.
///
/// An audit row is a copy of a current row of the entity, with the changeset
/// which wrote it and the kind of modification. Every table of the entity has
/// an audit log table, and each changeset which modifies the entity writes one
/// row to each of them.
///
/// The rows are written when the transaction completes, once the current rows
/// have their final state for the changeset. The current rows of an entity no
/// longer exist when its removal is recorded, and they held nothing which its
/// previous audit rows do not, so the rows which record a removal are copies
/// of those.
///
/// @implSpec Used by both [graph][org.hibernate.action.queue.spi.QueueType#GRAPH] and
/// [legacy][org.hibernate.action.queue.spi.QueueType#LEGACY] action-queue paths,
/// and by stateless sessions.
///
/// @author Steve Ebersole
/// @author Gavin King
/// @since 8.0
@Internal
public class EntityAuditSupport extends AbstractAuxiliaryRowWriter {

	/// An audit log table, with the name of the table whose rows it records.
	private record AuditTable(String currentTableName, EntityTableMapping mapping) {
	}

	private final AuditMapping auditMapping;
	private final List<AuditTable> auditTables = new ArrayList<>();
	private final boolean[] auditedPropertyMask;
	/// The function which yields the changeset identifier, if it is a timestamp
	/// taken from the database, and so not bound as a parameter.
	@Nullable
	private final String currentTimestampFunctionName;
	private final BasicBatchKey copyBatchKey;
	private final BasicBatchKey removalBatchKey;
	/// Copies the current rows.
	private final MutationOperationGroup copyGroup;
	/// Copies the previous audit rows, to record a removal.
	private final MutationOperationGroup removalGroup;
	/// Ends the previous audit rows, for the validity audit strategy.
	@Nullable
	private final MutationOperationGroup endGroup;

	public EntityAuditSupport(
			@Nonnull EntityPersister entityPersister,
			@Nonnull SessionFactoryImplementor factory) {
		super( entityPersister, factory );
		final var auditMapping = entityPersister.getAuditMapping();
		if ( auditMapping == null ) {
			throw new MappingException( "No audit mapping available for " + entityPersister.getEntityName() );
		}
		this.auditMapping = auditMapping;
		for ( var currentTable : entityPersister.getTableMappings() ) {
			if ( !currentTable.isInverse() ) {
				final String currentTableName = currentTable.getTableName();
				auditTables.add( new AuditTable( currentTableName,
						createAuxiliaryTableMapping( currentTable, auditMapping.resolveTableName( currentTableName ) ) ) );
			}
		}
		auditedPropertyMask = new boolean[entityPersister.getPropertySpan()];
		for ( int i = 0; i < auditedPropertyMask.length; i++ ) {
			auditedPropertyMask[i] = !entityPersister.isPropertyAuditedExcluded( i );
		}
		final var dialect = factory.getJdbcServices().getDialect();
		currentTimestampFunctionName = factory.getChangesetCoordinator().useServerTimestamp( dialect )
				? dialect.getCurrentTemporalSupport().currentTimestamp()
				: null;
		final String entityName = entityPersister.getEntityName();
		copyBatchKey = new BasicBatchKey( entityName + "#AUDIT_COPY" );
		removalBatchKey = new BasicBatchKey( entityName + "#AUDIT_REMOVAL" );
		copyGroup = buildCopyGroup( false );
		removalGroup = buildCopyGroup( true );
		endGroup = buildEndGroup();
	}

	/// Whether an update which modified the given attributes must be recorded.
	/// An update which modified only attributes excluded from auditing is not.
	public boolean isAuditedUpdate(@Nullable int[] dirtyAttributeIndexes, boolean hasDirtyCollection) {
		if ( dirtyAttributeIndexes == null || dirtyAttributeIndexes.length == 0 || hasDirtyCollection ) {
			return true;
		}
		for ( int dirtyIndex : dirtyAttributeIndexes ) {
			if ( auditedPropertyMask[dirtyIndex] ) {
				return true;
			}
		}
		return false;
	}

	/// Writes the audit rows which record the net modification of an entity by
	/// the current changeset, when the transaction completes.
	public void writeModification(
			@Nonnull Object id,
			@Nonnull ModificationType modificationType,
			@Nonnull SharedSessionContractImplementor session) {
		final Object changesetId = currentTimestampFunctionName == null ? session.getCurrentChangesetIdentifier() : null;
		endPreviousRows( id, modificationType, changesetId, session );
		final boolean removal = modificationType == ModificationType.DEL;
		execute( removal ? removalGroup : copyGroup, removal ? removalBatchKey : copyBatchKey, session, bindings -> {
			for ( var auditTable : auditTables ) {
				final String tableName = auditTable.mapping.getTableName();
				bindChangesetId( changesetId, bindings, tableName,
						auditMapping.getChangesetIdMapping( auditTable.currentTableName ) );
				final var modificationTypeMapping = auditMapping.getModificationTypeMapping( auditTable.currentTableName );
				if ( modificationTypeMapping != null ) {
					bindings.bindValue( modificationType, tableName,
							modificationTypeMapping.getSelectionExpression(), ParameterUsage.SET );
				}
				bindKey( id, bindings, auditTable.mapping, session );
			}
		} );
	}

	/// With the validity audit strategy, marks the previous audit rows of the
	/// entity as invalidated by the current changeset. This must precede the
	/// copy of the new audit rows, which are themselves not yet invalidated.
	private void endPreviousRows(
			Object id, ModificationType modificationType, @Nullable Object changesetId,
			SharedSessionContractImplementor session) {
		if ( endGroup != null ) {
			final String entityName = entityPersister().getEntityName();
			// executed at once, so that the outcome of each statement is checked
			execute( endGroup, null, session,
					bindings -> {
						for ( var auditTable : auditTables ) {
							final var invalidatingMapping =
									auditMapping.getInvalidatingChangesetIdMapping( auditTable.currentTableName );
							if ( invalidatingMapping != null ) {
								bindChangesetId( changesetId, bindings, auditTable.mapping.getTableName(),
										invalidatingMapping );
								bindKey( id, bindings, auditTable.mapping, session );
							}
						}
					},
					(statementDetails, affectedRowCount, batchPosition) ->
							verifyTransactionEndOutcome( affectedRowCount, modificationType, entityName, id )
			);
		}
	}

	private void bindChangesetId(
			@Nullable Object changesetId, JdbcValueBindings bindings, String tableName, SelectableMapping mapping) {
		if ( currentTimestampFunctionName == null ) {
			bindings.bindValue( changesetId, tableName, mapping.getSelectionExpression(), ParameterUsage.SET );
		}
	}

	public static boolean verifyTransactionEndOutcome(
			int affectedRowCount,
			@Nonnull ModificationType modificationType,
			@Nonnull String entityName,
			@Nonnull Object id) {
		if ( affectedRowCount > 1
				|| affectedRowCount == 0 && modificationType != ModificationType.ADD ) {
			throw new AuditException(
					"Cannot update previous revision for entity "
							+ entityName + " and id " + id
							+ " (" + affectedRowCount + " rows modified)."
			);
		}
		return true;
	}

	/// @param previous whether the previous audit rows are copied, instead of the current rows
	private MutationOperationGroup buildCopyGroup(boolean previous) {
		final List<TableMutation<?>> mutations = new ArrayList<>( auditTables.size() );
		for ( var auditTable : auditTables ) {
			final String currentTableName = auditTable.currentTableName;
			final var copiedColumns = auditMapping.getCopiedColumns( currentTableName );
			final var changesetIdMapping = auditMapping.getChangesetIdMapping( currentTableName );
			final Function<MutatingTableReference, List<ColumnValueBinding>> assignments = target -> {
				final List<ColumnValueBinding> bindings = new ArrayList<>( 2 );
				bindings.add( changesetIdBinding( target, changesetIdMapping ) );
				final var modificationTypeMapping = auditMapping.getModificationTypeMapping( currentTableName );
				if ( modificationTypeMapping != null ) {
					bindings.add( parameterBinding( target, modificationTypeMapping ) );
				}
				return bindings;
			};
			mutations.add( previous
					// the previous audit row is the one written by the latest changeset
					? createCopyOfLastRow( auditTable.mapping, copiedColumns,
							changesetIdMapping.getSelectionExpression(), assignments )
					: createCopy( auditTable.mapping, currentTableMapping( currentTableName ),
							copiedColumns, assignments ) );
		}
		return createOperationGroup( MutationType.INSERT, mutations );
	}

	private EntityTableMapping currentTableMapping(String tableName) {
		for ( var currentTable : entityPersister().getTableMappings() ) {
			if ( currentTable.getTableName().equals( tableName ) ) {
				return currentTable;
			}
		}
		throw new MappingException( "Unable to resolve table mapping for " + tableName );
	}

	private ColumnValueBinding changesetIdBinding(MutatingTableReference target, SelectableMapping mapping) {
		return currentTimestampFunctionName == null
				? parameterBinding( target, mapping )
				: new ColumnValueBinding( new ColumnReference( target, mapping ),
						new ColumnWriteFragment( currentTimestampFunctionName, List.of(), mapping ) );
	}

	private static ColumnValueBinding parameterBinding(MutatingTableReference target, SelectableMapping mapping) {
		final var column = new ColumnReference( target, mapping );
		return new ColumnValueBinding( column,
				new ColumnWriteFragment( "?", new ColumnValueParameter( column ), mapping ) );
	}

	@Nullable
	private MutationOperationGroup buildEndGroup() {
		final List<TableMutation<?>> mutations = new ArrayList<>();
		for ( var auditTable : auditTables ) {
			final var invalidatingMapping = auditMapping.getInvalidatingChangesetIdMapping( auditTable.currentTableName );
			if ( invalidatingMapping != null ) {
				final var builder =
						new TableUpdateBuilderStandard<MutationOperation>( entityPersister(), auditTable.mapping, factory() );
				builder.addColumnAssignment( invalidatingMapping,
						currentTimestampFunctionName == null ? "?" : currentTimestampFunctionName );
				auditTable.mapping.getKeyMapping().forEachKeyColumn( (position, key) -> builder.addKeyRestrictionBinding( key ) );
				// only the audit row which is not yet invalidated
				builder.addNonKeyRestriction( new ColumnValueBinding(
						new ColumnReference( builder.getMutatingTable(), invalidatingMapping ),
						new ColumnWriteFragment( null, List.of(), invalidatingMapping )
				) );
				mutations.add( builder.buildMutation() );
			}
		}
		return mutations.isEmpty() ? null : createOperationGroup( MutationType.UPDATE, mutations );
	}
}
