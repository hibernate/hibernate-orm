/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast.builder;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.CollectionTableDescriptor;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.mutation.ast.TableDelete;
import org.hibernate.action.queue.mutation.ast.TableDeleteCustomSql;
import org.hibernate.action.queue.mutation.ast.TableDeleteStandard;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.mutation.CollectionGraphMutationTarget;
import org.hibernate.sql.model.TableMapping.MutationDetails;

/// Standard builder for graph-based DELETE mutations.
///
/// Parallel to {@link org.hibernate.sql.model.ast.builder.TableDeleteBuilderStandard}
/// but uses {@link EntityTableDescriptor} instead of
/// {@link org.hibernate.sql.model.ast.MutatingTableReference}.
///
/// @author Steve Ebersole
@Incubating
public class GraphTableDeleteBuilderStandard
		extends AbstractGraphTableDeleteBuilder {

	private final String whereFragment;
	private final MutationDetails customSql;

	public GraphTableDeleteBuilderStandard(
			CollectionGraphMutationTarget mutationTarget,
			CollectionTableDescriptor tableDescriptor,
			String whereFragment,
			MutationDetails customSql,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, tableDescriptor, sessionFactory );
		this.whereFragment = whereFragment;
		this.customSql = customSql;
	}

	public GraphTableDeleteBuilderStandard(
			GraphMutationTarget<?> mutationTarget,
			TableDescriptor tableDescriptor,
			SessionFactoryImplementor sessionFactory) {
		this( mutationTarget, tableDescriptor, null, sessionFactory );
	}

	public GraphTableDeleteBuilderStandard(
			GraphMutationTarget<?> mutationTarget,
			TableDescriptor tableDescriptor,
			String whereFragment,
			SessionFactoryImplementor sessionFactory) {
		super(mutationTarget, tableDescriptor, sessionFactory);
		this.whereFragment = whereFragment;
		this.customSql = extractCustomDeleteDetails( tableDescriptor );
	}

	private static MutationDetails extractCustomDeleteDetails(TableDescriptor tableDescriptor) {
		var deleteDetails = tableDescriptor.deleteDetails();
		return deleteDetails != null && deleteDetails.getCustomSql() != null
				? deleteDetails
				: null;
	}

	@Override
	public TableDelete buildMutation() {
		if (customSql != null && customSql.getCustomSql() != null) {
			return new TableDeleteCustomSql(
					getTableDescriptor(),
					getMutationTarget(),
					customSql,
					getSqlComment(),
					keyRestrictionBindings,
					optimisticLockBindings,
					getParameters()
			);
		}

		return new TableDeleteStandard(
				getTableDescriptor(),
				getMutationTarget(),
				getSqlComment(),
				keyRestrictionBindings,
				optimisticLockBindings,
				whereFragment,
				getParameters()
		);
	}
}
