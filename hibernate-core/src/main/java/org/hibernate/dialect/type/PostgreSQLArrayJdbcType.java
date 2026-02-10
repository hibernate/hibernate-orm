/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Descriptor for {@link Types#ARRAY ARRAY} handling.
 */
public class PostgreSQLArrayJdbcType extends ArrayJdbcType {

	public PostgreSQLArrayJdbcType(JdbcType elementJdbcType) {
		super( elementJdbcType );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return new Binder<>( javaTypeDescriptor,
				(BasicPluralJavaType<?>) javaTypeDescriptor );
	}

	private class Binder<X,E> extends BasicBinder<X> {
		private final BasicPluralJavaType<E> pluralJavaType;

		private Binder(JavaType<X> javaType, BasicPluralJavaType<E> pluralJavaType) {
			super( javaType, PostgreSQLArrayJdbcType.this );
			this.pluralJavaType = pluralJavaType;
		}

		@Override
		protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
			st.setArray( index, getArray( value, options ) );
		}

		@Override
		protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
				throws SQLException {
			final java.sql.Array arr = getArray( value, options );
			try {
				st.setObject( name, arr, java.sql.Types.ARRAY );
			}
			catch (SQLException ex) {
				throw new HibernateException( "JDBC driver does not support named parameters for setArray. Use positional.", ex );
			}
		}

		@Override
		public Object[] getBindValue(X value, WrapperOptions options) throws SQLException {
			final var elementBinder = getElementJdbcType().getBinder( pluralJavaType.getElementJavaType() );
			return convertToArray( this, elementBinder, pluralJavaType, value, options );
		}

		private java.sql.Array getArray(X value, WrapperOptions options) throws SQLException {
			final var session = options.getSession();
			return session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()
					.createArrayOf( getElementTypeName( getJavaType(), session ),
							elements( value, options, PostgreSQLArrayJdbcType.this ) );
		}

		private Object[] elements(X value, WrapperOptions options, PostgreSQLArrayJdbcType arrayJdbcType)
				throws SQLException {
			final var elementJdbcType = arrayJdbcType.getElementJdbcType();
			if ( elementJdbcType instanceof AggregateJdbcType aggregateJdbcType ) {
				// The PostgreSQL JDBC driver does not support arrays of structs, which contain byte[]
				final var domainObjects = getJavaType().unwrap( value, Object[].class, options );
				final var objects = new Object[domainObjects.length];
				for ( int i = 0; i < domainObjects.length; i++ ) {
					if ( domainObjects[i] != null ) {
						objects[i] = aggregateJdbcType.createJdbcValue( domainObjects[i], options );
					}
				}
				return objects;
			}
			else {
				return getBindValue( value, options );
			}
		}
	}

	@Override
	public String toString() {
		return "PostgreSQLArrayTypeDescriptor(" + getElementJdbcType().toString() + ")";
	}
}
