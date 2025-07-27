/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;


import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.internal.TableUpdateCustomSql;
import org.hibernate.sql.model.internal.TableUpdateNoSet;
import org.hibernate.sql.model.internal.TableUpdateStandard;

import static java.util.Collections.emptyList;

/**
 * Standard TableUpdateBuilder implementation
 *
 * @author Steve Ebersole
 */
public class TableUpdateBuilderStandard<O extends MutationOperation>
		extends AbstractTableUpdateBuilder<O> {
	private final String whereFragment;

	public TableUpdateBuilderStandard(
			MutationTarget<?> mutationTarget,
			TableMapping tableMapping,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, tableMapping, sessionFactory );
		this.whereFragment = null;
	}

	public TableUpdateBuilderStandard(
			MutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		this( mutationTarget, tableReference, sessionFactory, null );
	}

	public TableUpdateBuilderStandard(
			MutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory,
			String whereFragment) {
		super( mutationTarget, tableReference, sessionFactory );
		this.whereFragment = whereFragment;
	}

	public String getWhereFragment() {
		return whereFragment;
	}

	//TODO: The unchecked typecasts here are horrible
	@SuppressWarnings("unchecked")
	@Override
	public RestrictedTableMutation<O> buildMutation() {
		final var valueBindings = combine( getValueBindings(), getKeyBindings(), getLobValueBindings() );
		if ( valueBindings.isEmpty() ) {
			return (RestrictedTableMutation<O>)
					new TableUpdateNoSet( getMutatingTable(), getMutationTarget() );
		}

		if ( getMutatingTable().getTableMapping().getUpdateDetails().getCustomSql() != null ) {
			return (RestrictedTableMutation<O>)
					new TableUpdateCustomSql(
							getMutatingTable(),
							getMutationTarget(),
							getSqlComment(),
							valueBindings,
							getKeyRestrictionBindings(),
							getOptimisticLockBindings()
					);
		}

		if ( getMutatingTable().getTableMapping().isOptional() ) {
			return (RestrictedTableMutation<O>)
					new OptionalTableUpdate(
							getMutatingTable(),
							getMutationTarget(),
							valueBindings,
							getKeyRestrictionBindings(),
							getOptimisticLockBindings()
					);
		}

		return (RestrictedTableMutation<O>)
				new TableUpdateStandard(
						getMutatingTable(),
						getMutationTarget(),
						getSqlComment(),
						valueBindings,
						getKeyRestrictionBindings(),
						getOptimisticLockBindings(),
						whereFragment,
						null,
						emptyList()
				);
	}
}
