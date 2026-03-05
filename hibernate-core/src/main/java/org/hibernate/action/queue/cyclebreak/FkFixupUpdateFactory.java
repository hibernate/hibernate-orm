/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.cyclebreak;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.builder.MutationGroupBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.model.jdbc.JdbcUpdateMutation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.hibernate.action.queue.Helper.normalizeColumnName;
import static org.hibernate.action.queue.Helper.normalizeTableName;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;


/// Concrete factory that builds:
///
/// ```sql
///   UPDATE <table> SET fk1=?, fk2=? WHERE keyParts...
/// ```
///
/// @author Steve Ebersole
public final class FkFixupUpdateFactory {
	public record UpdateTemplate(
			MutationOperationGroup group,
			MutationOperation operation,
			int shapeHash) {
	}

	public UpdateTemplate buildFkFixupUpdateGroup(
			EntityPersister entityPersister,
			String tableName,
			Set<String> keyColumnsToFix,
			SharedSessionContractImplementor session) {
		final String normalizeTableName = normalizeTableName( tableName );
		var tableMapping = findMutableTableMapping(entityPersister, normalizeTableName);

		var groupBuilder = new MutationGroupBuilder( MutationType.UPDATE, entityPersister );
		final TableUpdateBuilder<JdbcUpdateMutation> updateBuilder = new TableUpdateBuilderStandard<>(
				entityPersister,
				tableMapping,
				session.getFactory()
		);
		groupBuilder.addTableDetailsBuilder(updateBuilder);

		// SET fk = ?
		final LinkedHashSet<String> restrictedFkColumns = new LinkedHashSet<>();
		tableMapping.getKeyMapping().forEachSelectable( (i, selectableMapping) -> {
			if (selectableMapping == null || selectableMapping.isFormula()) {
				return;
			}
			final String columnName = normalizeColumnName( selectableMapping.getSelectableName() );
			if ( keyColumnsToFix.contains( columnName ) ) {
				updateBuilder.addValueColumn( columnName, selectableMapping );
				restrictedFkColumns.add(columnName);
			}
		} );

		// WHERE pk = ?
		final List<String> keyColumns = arrayList( tableMapping.getKeyMapping().getColumnCount() );
		tableMapping.getKeyMapping().forEachSelectable( (i, selectableMapping) -> {
			if ( !selectableMapping.isFormula() ) {
				final String columnName = normalizeColumnName( selectableMapping.getSelectableName() );
				updateBuilder.addKeyRestriction(selectableMapping);
				keyColumns.add(columnName);
			}
		} );

		final JdbcUpdateMutation jdbcUpdate = updateBuilder
				.buildMutation()
				.createMutationOperation( null, session.getFactory() );
		final MutationOperationGroup opGroup = new FixupOperationGroup( entityPersister, jdbcUpdate );

		final int shapeHash = Objects.hash(normalizeTableName, restrictedFkColumns, keyColumns);
		return new UpdateTemplate( opGroup, jdbcUpdate, shapeHash );
	}

	private static EntityTableMapping findMutableTableMapping(EntityPersister persister, String wanted) {
		for ( int i = 0; i < persister.getTableMappings().length; i++ ) {
			if ( wanted.equals( normalizeTableName( persister.getTableMappings()[i].getTableName() ) ) ) {
				return persister.getTableMappings()[i];
			}
		}
		throw new IllegalArgumentException("No table mapping for [" + wanted + "] in [" + persister.getEntityName() + "]");
	}


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
//
//	private static String norm(String s) {
//		if (s == null) return "";
//		String x = s.trim();
//		int dot = x.lastIndexOf('.');
//		if (dot >= 0) x = x.substring(dot + 1);
//		return x.toLowerCase( Locale.ROOT).replace("\"", "").replace("`", "");
//	}

}
