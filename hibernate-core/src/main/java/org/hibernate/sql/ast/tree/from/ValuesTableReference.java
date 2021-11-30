/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.insert.Values;

/**
 * @author Christian Beikov
 */
public class ValuesTableReference extends TableReference {

	private final List<Values> valuesList;
	private final List<String> columnNames;

	public ValuesTableReference(
			List<Values> valuesList,
			String identificationVariable,
			List<String> columnNames,
			SessionFactoryImplementor sessionFactory) {
		super( null, identificationVariable, false, sessionFactory );
		this.valuesList = valuesList;
		this.columnNames = columnNames;
	}

	public List<Values> getValuesList() {
		return valuesList;
	}

	public List<String> getColumnNames() {
		return columnNames;
	}

	@Override
	public TableReference resolveTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization) {
		throw new IllegalStateException( "Could not resolve binding for table `" + tableExpression + "`" );
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization,
			boolean resolve) {
		return null;
	}
}
