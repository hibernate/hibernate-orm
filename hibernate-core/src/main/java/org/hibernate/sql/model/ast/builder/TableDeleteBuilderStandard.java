/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.ast.builder;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.ColumnValueParameter;
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

	private final List<ColumnValueParameter> parameters = new ArrayList<>();

	private String sqlComment;

	public TableDeleteBuilderStandard(
			MutationTarget<?> mutationTarget,
			TableMapping table,
			SessionFactoryImplementor sessionFactory) {
		this( mutationTarget, new MutatingTableReference( table ), sessionFactory );
	}

	public TableDeleteBuilderStandard(
			MutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super( MutationType.DELETE, mutationTarget, tableReference, sessionFactory );

		this.isCustomSql = tableReference.getTableMapping().getDeleteDetails().getCustomSql() != null;
		this.sqlComment = "delete for " + mutationTarget.getRolePath();
	}

	public String getSqlComment() {
		return sqlComment;
	}

	public void setSqlComment(String sqlComment) {
		this.sqlComment = sqlComment;
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
				getParameters()
		);
	}

	protected List<ColumnValueParameter> getParameters() {
		return parameters;
	}

	@Override
	protected void handleParameterCreation(ColumnValueParameter parameter) {
		parameters.add( parameter );
	}
}
