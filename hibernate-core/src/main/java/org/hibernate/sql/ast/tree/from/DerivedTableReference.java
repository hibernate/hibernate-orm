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

/**
 * @author Christian Beikov
 */
public abstract class DerivedTableReference extends AbstractTableReference {

	private final List<String> columnNames;

	public DerivedTableReference(
			String identificationVariable,
			List<String> columnNames,
			SessionFactoryImplementor sessionFactory) {
		super( identificationVariable, false );
		this.columnNames = columnNames;
	}

	@Override
	public String getTableId() {
		return null;
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
