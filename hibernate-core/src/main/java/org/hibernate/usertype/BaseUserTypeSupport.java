/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.usertype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.BiConsumer;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class BaseUserTypeSupport<T> implements UserType<T> {
	private BasicJavaType<T> javaType;
	private JdbcTypeDescriptor jdbcType;

	private boolean resolved;

	// cached array wrapping our single type-code
	private int[] sqlTypes;
	// cached JDBC extractor and binder
	private ValueExtractor<T> jdbcValueExtractor;
	private ValueBinder<T> jdbcValueBinder;

	protected abstract void resolve(BiConsumer<BasicJavaType<T>,JdbcTypeDescriptor> resolutionConsumer);

	private void ensureResolved() {
		if ( resolved ) {
			return;
		}

		resolve( (javaType,jdbcType) -> {
			this.javaType = javaType;
			this.jdbcType = jdbcType;

			sqlTypes = new int[] { jdbcType.getJdbcTypeCode() };

			jdbcValueExtractor = jdbcType.getExtractor( javaType );
			jdbcValueBinder = jdbcType.getBinder( javaType );

			resolved = true;
		});
	}

	protected JdbcTypeDescriptor jdbcType() {
		ensureResolved();
		return jdbcType;
	}

	protected BasicJavaType<T> javaType() {
		ensureResolved();
		return javaType;
	}

	@Override
	public int[] sqlTypes() {
		ensureResolved();
		return sqlTypes;
	}

	@Override
	public Class<T> returnedClass() {
		return javaType().getJavaTypeClass();
	}

	@Override
	public boolean equals(Object x, Object y) throws HibernateException {
		//noinspection unchecked
		return javaType().areEqual( (T) x, (T) y );
	}

	@Override
	public int hashCode(Object x) throws HibernateException {
		//noinspection unchecked
		return javaType().extractHashCode( (T) x );
	}

	@Override
	public T nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
		ensureResolved();
		return jdbcValueExtractor.extract( rs, position, session );
	}

	@Override
	public void nullSafeSet(PreparedStatement st, T value, int index, SharedSessionContractImplementor session) throws SQLException {
		ensureResolved();
		jdbcValueBinder.bind( st, value, index, session );
	}

	@Override
	public Object deepCopy(Object value) throws HibernateException {
		//noinspection unchecked
		return javaType().getMutabilityPlan().deepCopy( (T) value );
	}

	@Override
	public boolean isMutable() {
		return javaType().getMutabilityPlan().isMutable();
	}

	@Override
	public Serializable disassemble(Object value) throws HibernateException {
		//noinspection unchecked
		return javaType().getMutabilityPlan().disassemble( (T) value, null );
	}

	@Override
	public Object assemble(Serializable cached, Object owner) throws HibernateException {
		return javaType().getMutabilityPlan().assemble( cached, null );
	}

	@Override
	public Object replace(Object original, Object target, Object owner) throws HibernateException {
		return deepCopy( original );
	}
}
