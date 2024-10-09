/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
		@SuppressWarnings("unchecked")
		final BasicPluralJavaType<X> pluralJavaType = (BasicPluralJavaType<X>) javaTypeDescriptor;
		final ValueBinder<X> elementBinder = getElementJdbcType().getBinder( pluralJavaType.getElementJavaType() );
		return new BasicBinder<>( javaTypeDescriptor, this ) {

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
			public Object getBindValue(X value, WrapperOptions options) throws SQLException {
				return ( (PostgreSQLArrayJdbcType) getJdbcType() ).getArray( this, elementBinder, value, options );
			}

			private java.sql.Array getArray(X value, WrapperOptions options) throws SQLException {
				final PostgreSQLArrayJdbcType arrayJdbcType = (PostgreSQLArrayJdbcType) getJdbcType();
				final Object[] objects;

				final JdbcType elementJdbcType = arrayJdbcType.getElementJdbcType();
				if ( elementJdbcType instanceof AggregateJdbcType ) {
					// The PostgreSQL JDBC driver does not support arrays of structs, which contain byte[]
					final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) elementJdbcType;
					final Object[] domainObjects = getJavaType().unwrap(
							value,
							Object[].class,
							options
					);
					objects = new Object[domainObjects.length];
					for ( int i = 0; i < domainObjects.length; i++ ) {
						if ( domainObjects[i] != null ) {
							objects[i] = aggregateJdbcType.createJdbcValue( domainObjects[i], options );
						}
					}
				}
				else {
					objects = arrayJdbcType.getArray( this, elementBinder, value, options );
				}

				final SharedSessionContractImplementor session = options.getSession();
				final String typeName = arrayJdbcType.getElementTypeName( getJavaType(), session );
				return session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()
						.createArrayOf( typeName, objects );
			}
		};
	}

	@Override
	public String toString() {
		return "PostgreSQLArrayTypeDescriptor(" + getElementJdbcType().toString() + ")";
	}
}
