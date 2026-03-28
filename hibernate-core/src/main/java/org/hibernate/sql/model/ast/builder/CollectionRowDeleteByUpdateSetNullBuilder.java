/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.collection.mutation.CollectionMutationTarget;
import org.hibernate.persister.collection.mutation.CollectionTableMapping;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.LogicalTableUpdate;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.internal.TableUpdateCustomSql;
import org.hibernate.sql.model.internal.TableUpdateStandard;

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
