/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.mutation.internal;

import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

/**
 * Standard JdbcValueDescriptor implementation
 *
 * @author Steve Ebersole
 */
public class JdbcValueDescriptorImpl implements JdbcValueDescriptor {
	private final String columnName;
	private final ParameterUsage usage;
	private final JdbcMapping jdbcMapping;
	private final int jdbcPosition;

	public JdbcValueDescriptorImpl(JdbcParameterBinder jdbcParameterBinder, int jdbcPosition) {
		this( (ColumnValueParameter) jdbcParameterBinder, jdbcPosition );
	}

	public JdbcValueDescriptorImpl(ColumnValueParameter columnValueParameter, int jdbcPosition) {
		this.columnName = columnValueParameter.getColumnReference().getColumnExpression();
		this.usage = columnValueParameter.getUsage();
		this.jdbcMapping = columnValueParameter.getJdbcMapping();
		this.jdbcPosition = jdbcPosition;
	}

	@Override
	public String getColumnName() {
		return columnName;
	}

	@Override
	public ParameterUsage getUsage() {
		return usage;
	}

	@Override
	public int getJdbcPosition() {
		return jdbcPosition;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}
}
