/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.cyclebreak;

import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;

/// Concrete factory that builds:
///
/// ```sql
///   UPDATE <table> SET fk1=?, fk2=? WHERE keyParts...
/// ```
///
/// @author Steve Ebersole
public final class FkFixupUpdateFactory {
//	private final PreparedStatementGroupExecutorHook executorHook;
//
//	public FkFixupUpdateFactory(EntityPersister entityPersister, PreparedStatementGroupExecutorHook executorHook) {
//		this.entityPersister = entityPersister;
//		this.executorHook = executorHook;
//	}

	public record UpdateTemplate(
			MutationOperationGroup group,
			MutationOperation operation,
			int shapeHash,
			PreparedStatementGroupExecutorHook executorHook) {
	}

	public interface PreparedStatementGroupExecutorHook {
		void execute(MutationExecutor exec, SharedSessionContractImplementor session);
	}

	public static final PreparedStatementGroupExecutorHook DEFAULT_UPDATE_EXEC =
			(exec, session) -> {
		throw new UnsupportedOperationException( "Not supported yet." );
//				final PreparedStatementGroup psg = exec.getPreparedStatementGroup();
//				for ( PreparedStatementDetails psd : psg.getPreparedStatementDetails()) {
//					final int rowCount = psd.executeUpdate(); // adjust if needed
//					psd.getExpectation().verifyOutcome(rowCount, psd.getStatement(), -1, session);
//				}
			};

//	public UpdateTemplate buildFkFixupUpdateGroup(
//			String tableName,
//			Set<String> keyColumnsToFix,
//			SharedSessionContractImplementor session) {
//		final EntityTableMapping tableMapping = findMutableTableMapping(entityPersister, tableName);
//
//		final MutationGroupBuilder groupBuilder = new MutationGroupBuilder( MutationKind.UPDATE, entityPersister);
//		final TableUpdateBuilder<?> updateBuilder = createTableUpdateBuilder(tableMapping);
//
//		groupBuilder.addTableDetailsBuilder(updateBuilder);
//
//		// SET fk = ?
//		final LinkedHashSet<String> restrictedFkColumns = new LinkedHashSet<>();
//		tableMapping.getKeyMapping().forEachSelectable( (i, selectableMapping) -> {
//			if (selectableMapping == null || selectableMapping.isFormula()) {
//				return;
//			}
//			if ( keyColumnsToFix.contains( selectableMapping.getSelectableName() ) ) {
//				updateBuilder.addValueColumn( selectableMapping );
//				restrictedFkColumns.add(norm(selectableMapping.getSelectionExpression()));
//			}
//		} );
//
//		// WHERE key restrictions (skips nullable/formula by defaults you shared)
//		final List<String> keyColumns = arrayList(tableMapping.getKeyMapping().getColumnCount());
//		tableMapping.getKeyMapping().forEachSelectable(  (i, selectableMapping) -> {
//			updateBuilder.addKeyRestriction(selectableMapping);
//			keyColumns.add(norm(selectableMapping.getSelectionExpression()));
//		} );
//
//		final var builtMutationGroup = groupBuilder.buildMutationGroup();
//		final MutationOperationGroup opGroup = createOperationGroup(null, builtMutationGroup);
//
//		final MutationOperation operation = opGroup.getOperation(0);
//		final int shapeHash = Objects.hash(norm(tableName), restrictedFkColumns, keyColumns);
//
//		return new UpdateTemplate( opGroup, operation, shapeHash, executorHook );
//	}
//
//	// ---- delegate-aware builder creation (your method) ----
//
//	private TableUpdateBuilder<?> createTableUpdateBuilder(EntityTableMapping tableMapping) {
//		final var delegate =
//				tableMapping.isIdentifierTable()
//						? persister.getUpdateDelegate()
//						: null;
//		return delegate != null
//				? delegate.createTableMutationBuilder(tableMapping.getInsertExpectation(), persister.getFactory())
//				: newTableUpdateBuilder(tableMapping);
//	}
//
//	private TableMutationBuilder<?> newTableUpdateBuilder(EntityTableMapping tableMapping) {
//		throw new UnsupportedOperationException("WIRE ME: newTableUpdateBuilder(EntityTableMapping) from your coordinator");
//	}
//
//	private MutationOperationGroup createOperationGroup(Object mutationTarget, Object builtMutationGroup) {
//		throw new UnsupportedOperationException("WIRE ME: createOperationGroup(target, mutationGroup) from your coordinator");
//	}
//
//	// ---- mapping discovery / selectable lookup ----
//
//	private static EntityTableMapping findMutableTableMapping(EntityPersister persister, String tableName) {
//		final String wanted = norm(tableName);
//		final EntityTableMapping[] found = new EntityTableMapping[1];
//		persister.forEachMutableTable(t -> {
//			final EntityTableMapping etm = (EntityTableMapping) t;
//			if (found[0] == null && norm(etm.getTableName()).equals(wanted)) found[0] = etm;
//		});
//		if (found[0] == null) {
//			throw new IllegalArgumentException("No mutable table mapping for [" + tableName + "] in [" + persister.getEntityName() + "]");
//		}
//		return found[0];
//	}
//
//	private static String norm(String s) {
//		if (s == null) return "";
//		String x = s.trim();
//		int dot = x.lastIndexOf('.');
//		if (dot >= 0) x = x.substring(dot + 1);
//		return x.toLowerCase( Locale.ROOT).replace("\"", "").replace("`", "");
//	}

}
