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

/**
 * Factory for building UPDATE operations specifically for unique constraint swap cycles.
 * Unlike FK fixup UPDATEs, these work with unique constraint columns that may not be foreign keys.
 *
 * @author Steve Ebersole
 */
public final class UniqueSwapUpdateFactory {
	public record UpdateTemplate(
			String tableName,
			MutationOperationGroup group,
			MutationOperation operation,
			int shapeHash) {
	}

	public UpdateTemplate buildUniqueSwapUpdateGroup(
			EntityPersister entityPersister,
			String tableName,
			Set<String> columnsToFix,
			SharedSessionContractImplementor session) {
		final String normalizedTableName = normalizeTableName( tableName );
		var tableMapping = findMutableTableMapping(entityPersister, normalizedTableName);

		var groupBuilder = new MutationGroupBuilder( MutationType.UPDATE, entityPersister );
		final TableUpdateBuilder<JdbcUpdateMutation> updateBuilder = new TableUpdateBuilderStandard<>(
				entityPersister,
				tableMapping,
				session.getFactory()
		);
		groupBuilder.addTableDetailsBuilder(updateBuilder);

		// SET unique_column = ?
		final LinkedHashSet<String> restrictedColumns = new LinkedHashSet<>();
		entityPersister.forEachAttributeMapping( attributeMapping -> {
			attributeMapping.forEachSelectable( (i, selectableMapping) -> {
				if (selectableMapping == null || selectableMapping.isFormula()) {
					return;
				}
				final String columnName = normalizeColumnName( selectableMapping.getSelectableName() );
				if ( columnsToFix.contains( columnName ) ) {
					updateBuilder.addValueColumn( selectableMapping );
					restrictedColumns.add(columnName);
				}
			} );
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

		final int shapeHash = Objects.hash(normalizedTableName, restrictedColumns, keyColumns);
		return new UpdateTemplate( normalizedTableName, opGroup, jdbcUpdate, shapeHash );
	}

	private static EntityTableMapping findMutableTableMapping(EntityPersister persister, String wanted) {
		for ( int i = 0; i < persister.getTableMappings().length; i++ ) {
			if ( wanted.equals( normalizeTableName( persister.getTableMappings()[i].getTableName() ) ) ) {
				return persister.getTableMappings()[i];
			}
		}
		throw new IllegalArgumentException("No table mapping for [" + wanted + "] in [" + persister.getEntityName() + "]");
	}
}
