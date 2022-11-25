/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.ast.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableInsert;
import org.hibernate.sql.model.internal.TableInsertCustomSql;
import org.hibernate.sql.model.internal.TableInsertStandard;

/**
 * Standard TableInsertBuilder
 *
 * @author Steve Ebersole
 */
public class TableInsertBuilderStandard extends AbstractTableInsertBuilder {
	private final boolean returningGeneratedKeys;
	private final boolean isCustomSql;
	private List<ColumnReference> returningColumnsList;

	public TableInsertBuilderStandard(
			MutationTarget mutationTarget,
			TableMapping table,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, table, sessionFactory );

		final Dialect dialect = getJdbcServices().getDialect();
		this.returningGeneratedKeys = dialect.getDefaultUseGetGeneratedKeys();

		this.isCustomSql = table.getInsertDetails().getCustomSql() != null;
	}

	public TableInsertBuilderStandard(
			MutationTarget mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, tableReference, sessionFactory );

		final Dialect dialect = getJdbcServices().getDialect();
		this.returningGeneratedKeys = dialect.getDefaultUseGetGeneratedKeys();

		this.isCustomSql = tableReference.getTableMapping().getInsertDetails().getCustomSql() != null;
	}

	public boolean isReturningGeneratedKeys() {
		return returningGeneratedKeys;
	}

	public List<ColumnReference> getReturningColumns() {
		return returningColumnsList == null ? Collections.emptyList() : returningColumnsList;
	}

	public ColumnReference addReturningColumn(SelectableMapping selectableMapping) {
		final ColumnReference columnReference = new ColumnReference( (String) null, selectableMapping, null );
		if ( returningColumnsList == null ) {
			returningColumnsList = new ArrayList<>();
		}
		returningColumnsList.add( columnReference );
		return columnReference;
	}

	@Override
	public TableInsert buildMutation() {
		if ( isCustomSql ) {
			return new TableInsertCustomSql(
					getMutatingTable(),
					getMutationTarget(),
					combine( getValueBindingList(), getKeyBindingList(), getLobValueBindingList() ),
					getParameters()
			);
		}

		return new TableInsertStandard(
				getMutatingTable(),
				getMutationTarget(),
				combine( getValueBindingList(), getKeyBindingList(), getLobValueBindingList() ),
				returningGeneratedKeys,
				returningColumnsList,
				getParameters()
		);
	}
}
