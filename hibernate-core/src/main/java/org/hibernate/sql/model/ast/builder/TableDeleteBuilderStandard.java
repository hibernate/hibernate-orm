/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableDelete;
import org.hibernate.sql.model.internal.TableDeleteCustomSql;
import org.hibernate.sql.model.internal.TableDeleteStandard;
import org.hibernate.sql.model.jdbc.JdbcDeleteMutation;

/**
 * Standard TableDeleteBuilder implementation used when Hibernate
 * generates the delete statement
 *
 * @author Steve Ebersole
 */
public class TableDeleteBuilderStandard
		extends AbstractRestrictedTableMutationBuilder<JdbcDeleteMutation, TableDelete>
		implements TableDeleteBuilder {
	private final boolean isCustomSql;

	private String sqlComment;

	private final String whereFragment;

	public TableDeleteBuilderStandard(
			MutationTarget<?> mutationTarget,
			TableMapping table,
			SessionFactoryImplementor sessionFactory) {
		this( mutationTarget, new MutatingTableReference( table ), sessionFactory, null );
	}

	public TableDeleteBuilderStandard(
			MutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		this( mutationTarget, tableReference, sessionFactory, null );
	}

	public TableDeleteBuilderStandard(
			MutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory,
			String whereFragment) {
		super( MutationType.DELETE, mutationTarget, tableReference, sessionFactory );

		this.isCustomSql = tableReference.getTableMapping().getDeleteDetails().getCustomSql() != null;
		this.sqlComment = "delete for " + mutationTarget.getRolePath();
		this.whereFragment = whereFragment;
	}

	public String getSqlComment() {
		return sqlComment;
	}

	public void setSqlComment(String sqlComment) {
		this.sqlComment = sqlComment;
	}

	public String getWhereFragment() {
		return whereFragment;
	}

	@Override
	public void setWhere(String fragment) {
		if ( isCustomSql && fragment != null ) {
			throw new HibernateException(
					"Invalid attempt to apply where-restriction on top of custom sql-delete mapping : " +
							getMutationTarget().getNavigableRole().getFullPath()
			);
		}
	}

	@Override
	public void addWhereFragment(String fragment) {
		if ( isCustomSql && fragment != null ) {
			throw new HibernateException(
					"Invalid attempt to apply where-filter on top of custom sql-delete mapping : " +
							getMutationTarget().getNavigableRole().getFullPath()
			);
		}
	}

	@Override
	public TableDelete buildMutation() {
		if ( isCustomSql ) {
			return new TableDeleteCustomSql(
					getMutatingTable(),
					getMutationTarget(),
					sqlComment,
					getKeyRestrictionBindings(),
					getOptimisticLockBindings(),
					getParameters()
			);
		}

		return new TableDeleteStandard(
				getMutatingTable(),
				getMutationTarget(),
				sqlComment,
				getKeyRestrictionBindings(),
				getOptimisticLockBindings(),
				getParameters(),
				whereFragment
		);
	}
}
