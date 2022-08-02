/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal;

import java.util.Objects;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.type.descriptor.java.JavaType;

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
		final BasicValueConverter valueConverter = jdbcMapping.getValueConverter();
		if ( valueConverter == null ) {
			return new JdbcParameterBindingImpl( jdbcMapping, jdbcParameterValue );
		}
		return new JdbcParameterBindingImpl( jdbcMapping, valueConverter.toRelationalValue( jdbcParameterValue ) );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		FilterJdbcParameter that = (FilterJdbcParameter) o;
		return Objects.equals( parameter, that.parameter ) &&
				Objects.equals( jdbcMapping, that.jdbcMapping ) &&
				( (JavaType<Object>) jdbcMapping.getMappedJavaType() ).areEqual(
						jdbcParameterValue,
						that.jdbcParameterValue
				);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
				parameter,
				jdbcMapping,
				( (JavaType<Object>) jdbcMapping.getMappedJavaType() ).extractHashCode( jdbcParameterValue )
		);
	}
}
