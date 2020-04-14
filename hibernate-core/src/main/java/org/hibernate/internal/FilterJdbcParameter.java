/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;

/**
 * @author Nathan Xu
 */
public class FilterJdbcParameter {
	private final JdbcParameter parameter;
	private final JdbcMapping jdbcMapping;
	private final Object jdbcParameterValue;

	public FilterJdbcParameter(JdbcMapping jdbcMapping, Object jdbcParameterValue) {
		this.parameter = new JdbcParameterImpl( jdbcMapping );
		this.jdbcMapping = jdbcMapping;
		this.jdbcParameterValue = jdbcParameterValue;
	}

	public JdbcParameter getParameter() {
		return parameter;
	}

	public JdbcParameterBinder getBinder() {
		return parameter.getParameterBinder();
	}

	public JdbcParameterBinding getBinding() {
		return new JdbcParameterBinding() {
			@Override
			public JdbcMapping getBindType() {
				return jdbcMapping;
			}

			@Override
			public Object getBindValue() {
				return jdbcParameterValue;
			}
		};
	}
}
