/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal.model.builder;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.persister.collection.mutation.CollectionTableMapping;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.LogicalTableUpdate;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.TableUpdateCustomSql;
import org.hibernate.sql.ast.spi.model.TableUpdateStandard;

import java.util.Collections;
import java.util.List;

/**
 * Custom table update builder for one-to-many collections that handles row deletes
 *
 * @author Marco Belladelli
 */
public class CollectionRowDeleteByUpdateSetNullBuilder<O extends MutationOperation> extends TableUpdateBuilderStandard<O> {
	public CollectionRowDeleteByUpdateSetNullBuilder(
			CollectionMutationTarget mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory,
			String whereFragment) {
		super( mutationTarget, tableReference, sessionFactory, whereFragment );
		assert tableReference.getTableMapping() instanceof CollectionTableMapping;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public LogicalTableUpdate<O> buildMutation() {
		final CollectionTableMapping tableMapping = (CollectionTableMapping) getMutatingTable().getTableMapping();
		final List<ColumnValueBinding> valueBindings = combine(
				getValueBindings(),
				getKeyBindings(),
				getLobValueBindings()
		);
		if ( tableMapping.getDeleteRowDetails().getCustomSql() != null ) {
			return (LogicalTableUpdate<O>) new TableUpdateCustomSql(
					getMutatingTable(),
					getMutationTarget(),
					getSqlComment(),
					valueBindings,
					getKeyRestrictionBindings(),
					getOptimisticLockBindings()
			) {
				@Override
				public String getCustomSql() {
					return tableMapping.getDeleteRowDetails().getCustomSql();
				}

				@Override
				public boolean isCallable() {
					return tableMapping.getDeleteRowDetails().isCallable();
				}

				@Override
				public Expectation getExpectation() {
					return tableMapping.getDeleteRowDetails().getExpectation();
				}
			};
		}
		return (LogicalTableUpdate<O>) new TableUpdateStandard(
				getMutatingTable(),
				getMutationTarget(),
				getSqlComment(),
				valueBindings,
				getKeyRestrictionBindings(),
				getOptimisticLockBindings(),
				getWhereFragment(),
				null,
				Collections.emptyList()
		) {
			@Override
			public Expectation getExpectation() {
				return tableMapping.getDeleteRowDetails().getExpectation();
			}
		};
	}
}
