/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.values.internal;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.RestrictedTableMutation;
import org.hibernate.sql.model.ast.builder.AbstractTableUpdateBuilder;
import org.hibernate.sql.model.internal.TableUpdateStandard;

/**
 * @author Marco Belladelli
 */
public class TableUpdateReturningBuilder<O extends MutationOperation> extends AbstractTableUpdateBuilder<O> {
	final List<ColumnReference> generatedColumns;

	public TableUpdateReturningBuilder(
			EntityPersister mutationTarget,
			MutatingTableReference tableReference,
			List<ColumnReference> generatedColumns,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, tableReference, sessionFactory );
		this.generatedColumns = generatedColumns;
	}

	@Override
	protected EntityPersister getMutationTarget() {
		return (EntityPersister) super.getMutationTarget();
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public RestrictedTableMutation<O> buildMutation() {
		return (RestrictedTableMutation<O>) new TableUpdateStandard(
				getMutatingTable(),
				getMutationTarget(),
				getSqlComment(),
				combine( getValueBindings(), getKeyBindings(), getLobValueBindings() ),
				getKeyRestrictionBindings(),
				getOptimisticLockBindings(),
				null,
				null,
				generatedColumns
		);
	}
}
