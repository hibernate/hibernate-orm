/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Descriptor for {@link Types#ARRAY ARRAY} handling.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLArrayJdbcType.
 */
public class GaussDBArrayJdbcType extends ArrayJdbcType {

	public GaussDBArrayJdbcType(JdbcType elementJdbcType) {
		super( elementJdbcType );
	}

	@Override
	protected String getElementTypeName(JavaType<?> javaType, SharedSessionContractImplementor session) {
		// GaussDB M mode stores DATE columns as the non-standard `datea` type, so a date array
		// column is `datea[]`. The base implementation resolves the element type name through the
		// DDL type registry, which yields "date" for DATE — `createArrayOf("date", ...)` then
		// produces a `date[]` array that GaussDB M mode rejects ("column ... is of type datea[] but
		// expression is of type date[]"). Return "datea" for date elements so the array is built
		// as `datea[]`, matching the column type.
		// A mode (openGauss PG kernel) stores DATE as the standard `date` (reported as timestamp by
		// the JDBC driver), and createArrayOf("date") works — so fall through to the base
		// implementation, which yields "date". Without this guard A mode passed "datea" to
		// createArrayOf and failed with "Unable to find server array type for provided name datea".
		// The array element is the standard LocalDateJdbcType (jdbc code DATE), not
		// GaussDBLocalDateJdbcType (which is OTHER/1111 and only used to read `datea` columns
		// directly), so check the jdbc type code rather than the concrete class.
		if ( getElementJdbcType().getJdbcTypeCode() == Types.DATE ) {
			final var dialect = session.getJdbcServices().getDialect();
			if ( dialect instanceof GaussDBDialect g && g.isMMode() ) {
				return "datea";
			}
		}
		return super.getElementTypeName( javaType, session );
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return new Binder<>( javaTypeDescriptor,
				(BasicPluralJavaType<?>) javaTypeDescriptor );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaTypeDescriptor) {
		if ( getElementJdbcType().getJdbcTypeCode() == Types.DATE ) {
			// gsjdbc4 returns PGobject for datea[] array elements (via java.sql.Array.getArray()),
			// which LocalDateJavaType.wrap can't handle. Read each element through
			// getResultSet().getDate(2) — reusing the same datea→java.sql.Date conversion that
			// GaussDBLocalDateJdbcType uses for scalar datea columns — then let ArrayJavaType wrap
			// the java.sql.Date[] via the standard LocalDateJavaType.wrap(java.sql.Date) path.
			return new BasicExtractor<>( javaTypeDescriptor, this ) {
				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return getJavaType().wrap( toDateArray( rs.getArray( paramIndex ) ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					return getJavaType().wrap( toDateArray( statement.getArray( index ) ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					return getJavaType().wrap( toDateArray( statement.getArray( name ) ), options );
				}
			};
		}
		return super.getExtractor( javaTypeDescriptor );
	}

	private static java.sql.Date[] toDateArray(java.sql.Array array) throws SQLException {
		if ( array == null ) {
			return null;
		}
		try ( ResultSet rs = array.getResultSet() ) {
			final List<java.sql.Date> dates = new ArrayList<>();
			while ( rs.next() ) {
				dates.add( rs.getDate( 2 ) );
			}
			return dates.toArray( new java.sql.Date[0] );
		}
	}

	private class Binder<X,E> extends BasicBinder<X> {
		private final BasicPluralJavaType<E> pluralJavaType;

		private Binder(JavaType<X> javaType, BasicPluralJavaType<E> pluralJavaType) {
			super( javaType, GaussDBArrayJdbcType.this );
			this.pluralJavaType = pluralJavaType;
		}

		@Override
		protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
				throws SQLException {
			st.setArray( index, getArray( value, options ) );
		}

		@Override
		protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
				throws SQLException {
			final java.sql.Array arr = getArray( value, options );
			try {
				st.setObject( name, arr, Types.ARRAY );
			}
			catch (SQLException ex) {
				throw new HibernateException(
						"JDBC driver does not support named parameters for setArray. Use positional.", ex );
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
							elements( value, options, GaussDBArrayJdbcType.this ) );
		}

		private Object[] elements(X value, WrapperOptions options, GaussDBArrayJdbcType arrayJdbcType)
				throws SQLException {
			final var elementJdbcType = arrayJdbcType.getElementJdbcType();
			if ( elementJdbcType instanceof AggregateJdbcType aggregateJdbcType ) {
				// The GaussDB JDBC driver does not support arrays of structs, which contain byte[]
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
		return "GaussDBArrayTypeDescriptor(" + getElementJdbcType().toString() + ")";
	}
}
