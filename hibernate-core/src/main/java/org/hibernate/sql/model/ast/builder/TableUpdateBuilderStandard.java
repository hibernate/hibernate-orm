/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.ast.builder;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.internal.TableUpdateCustomSql;
import org.hibernate.sql.model.internal.TableUpdateNoSet;
import org.hibernate.sql.model.internal.TableUpdateStandard;
import org.hibernate.sql.model.internal.TableUpsert;

/**
 * @author Steve Ebersole
 */
public class TableUpdateBuilderStandard<O extends MutationOperation> extends AbstractTableUpdateBuilder<O> {
	public TableUpdateBuilderStandard(
			MutationTarget mutationTarget,
			TableMapping tableMapping,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, tableMapping, sessionFactory );
	}

	public TableUpdateBuilderStandard(
			MutationTarget mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, tableReference, sessionFactory );
	}

	@SuppressWarnings("unchecked")
	@Override
	public RestrictedTableMutation<O> buildMutation() {
		final List<ColumnValueBinding> valueBindings = combine( getValueBindings(), getKeyBindings(), getLobValueBindings() );
		if ( valueBindings.isEmpty() ) {
			return (RestrictedTableMutation<O>) new TableUpdateNoSet( getMutatingTable(), getMutationTarget() );
		}

		if ( getMutatingTable().getTableMapping().getUpdateDetails().getCustomSql() != null ) {
			return (RestrictedTableMutation<O>) new TableUpdateCustomSql(
					getMutatingTable(),
					getMutationTarget(),
					getParameters(),
					valueBindings,
					getKeyRestrictionBindings(),
					getOptimisticLockBindings()
			);
		}

		if ( getMutatingTable().getTableMapping().isOptional() ) {
			return (RestrictedTableMutation<O>) new TableUpsert(
					getMutatingTable(),
					getMutationTarget(),
					valueBindings,
					getKeyRestrictionBindings(),
					getOptimisticLockBindings(),
					getParameters()
			);
		}

		return (RestrictedTableMutation<O>) new TableUpdateStandard(
				getMutatingTable(),
				getMutationTarget(),
				getParameters(),
				valueBindings,
				getKeyRestrictionBindings(),
				getOptimisticLockBindings()
		);
	}
}
