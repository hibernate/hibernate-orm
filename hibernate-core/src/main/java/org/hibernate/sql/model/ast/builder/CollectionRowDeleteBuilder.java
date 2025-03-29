/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.collection.mutation.CollectionTableMapping;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableDelete;
import org.hibernate.sql.model.internal.TableDeleteCustomSql;
import org.hibernate.sql.model.internal.TableDeleteStandard;

/**
 * Custom table delete builder for many-to-many collection join tables that handles row deletes
 *
 * @author Marco Belladelli
 */
public class CollectionRowDeleteBuilder extends TableDeleteBuilderStandard {
	public CollectionRowDeleteBuilder(
			MutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory,
			String whereFragment) {
		super( mutationTarget, tableReference, sessionFactory, whereFragment );
		assert tableReference.getTableMapping() instanceof CollectionTableMapping;
	}

	@Override
	public TableDelete buildMutation() {
		final CollectionTableMapping tableMapping = (CollectionTableMapping) getMutatingTable().getTableMapping();
		if ( tableMapping.getDeleteRowDetails().getCustomSql() != null ) {
			return new TableDeleteCustomSql(
					getMutatingTable(),
					getMutationTarget(),
					getSqlComment(),
					getKeyRestrictionBindings(),
					getOptimisticLockBindings(),
					getParameters()
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
		return new TableDeleteStandard(
				getMutatingTable(),
				getMutationTarget(),
				getSqlComment(),
				getKeyRestrictionBindings(),
				getOptimisticLockBindings(),
				getParameters(),
				getWhereFragment()
		) {
			@Override
			public Expectation getExpectation() {
				return tableMapping.getDeleteRowDetails().getExpectation();
			}
		};
	}
}
