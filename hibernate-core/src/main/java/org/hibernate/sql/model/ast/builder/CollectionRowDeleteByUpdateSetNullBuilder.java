/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.model.ast.builder;

import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.collection.mutation.CollectionTableMapping;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.internal.TableUpdateCustomSql;
import org.hibernate.sql.model.internal.TableUpdateStandard;

/**
 * Custom table update builder for one-to-many collections that handles row deletes
 *
 * @author Marco Belladelli
 */
public class CollectionRowDeleteByUpdateSetNullBuilder<O extends MutationOperation> extends TableUpdateBuilderStandard<O> {
	public CollectionRowDeleteByUpdateSetNullBuilder(
			MutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory,
			String whereFragment) {
		super( mutationTarget, tableReference, sessionFactory, whereFragment );
		assert tableReference.getTableMapping() instanceof CollectionTableMapping;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public RestrictedTableMutation<O> buildMutation() {
		final CollectionTableMapping tableMapping = (CollectionTableMapping) getMutatingTable().getTableMapping();
		final List<ColumnValueBinding> valueBindings = combine(
				getValueBindings(),
				getKeyBindings(),
				getLobValueBindings()
		);
		if ( tableMapping.getDeleteRowDetails().getCustomSql() != null ) {
			return (RestrictedTableMutation<O>) new TableUpdateCustomSql(
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
		return (RestrictedTableMutation<O>) new TableUpdateStandard(
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
