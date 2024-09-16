/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@JiraKey( value = "HHH-12292")
@RequiresDialect(H2Dialect.class)
@SkipForDialect(dialectClass = H2Dialect.class, majorVersion = 2, reason = "Array support was changed to now be typed")
@Jpa(
		annotatedClasses = QueryParametersValidationArrayTest.Event.class
)
public class QueryParametersValidationArrayTest {
	@Test
	public void setParameterWithWrongTypeShouldNotThrowIllegalArgumentException(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager ->
					entityManager.createNativeQuery(
							"select id " +
									"from Event " +
									"where readings = :readings" )
							.unwrap( NativeQuery.class )
							.setParameter( "readings", new String[]{null, "a"}, StringArrayType.INSTANCE )
							.getResultList()
		);
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Column(columnDefinition = "ARRAY")
		private String[] readings;
	}

	public static class StringArrayType
			extends AbstractSingleColumnStandardBasicType<String[]> {

		public static final StringArrayType INSTANCE = new StringArrayType ();

		public StringArrayType() {
			super( StringArrayJdbcType.INSTANCE, StringArrayTypeDescriptor.INSTANCE);
		}

		public String getName() {
			return "string-array";
		}

		@Override
		protected boolean registerUnderJavaType() {
			return true;
		}
	}

	public static class StringArrayJdbcType implements JdbcType {

		public static final StringArrayJdbcType INSTANCE = new StringArrayJdbcType();

		@Override
		public int getJdbcTypeCode() {
			return Types.ARRAY;
		}

		@Override
		public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
					StringArrayTypeDescriptor arrayTypeDescriptor = (StringArrayTypeDescriptor) javaType;
					st.setArray(index, st.getConnection().createArrayOf(
							arrayTypeDescriptor.getSqlArrayType(),
							arrayTypeDescriptor.unwrap((String[]) value, Object[].class, options)
					));
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) {
					throw new UnsupportedOperationException("Binding by name is not supported!");
				}
			};
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
			return new BasicExtractor<>( javaType, this) {
				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return javaType.wrap( rs.getArray( paramIndex ), options);
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					return javaType.wrap( statement.getArray( index), options);
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					return javaType.wrap( statement.getArray( name), options);
				}
			};
		}

	}

	public static class StringArrayTypeDescriptor
			extends AbstractClassJavaType<String[]> {

		public static final StringArrayTypeDescriptor INSTANCE = new StringArrayTypeDescriptor();

		public StringArrayTypeDescriptor() {
			super(String[].class);
		}

		public boolean areEqual(String[] one, String[] another) {
			if ( one == another ) {
				return true;
			}
			return !( one == null || another == null ) && Arrays.equals( one, another );
		}

		public String toString(String[] value) {
			return Arrays.deepToString( value);
		}

		@Override
		public String[] fromString(CharSequence string) {
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <X> X unwrap(String[] value, Class<X> type, WrapperOptions options) {
			return (X) value;
		}

		@Override
		public <X> String[] wrap(X value, WrapperOptions options) {
			if (value instanceof Array ) {
				Array array = (Array) value;
				try {
					return (String[]) array.getArray();
				} catch (SQLException e) {
					throw new IllegalArgumentException(e);
				}
			}
			return (String[]) value;
		}

		public String getSqlArrayType() {
			return "varchar";
		}
	}
}
