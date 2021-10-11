/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
import org.hibernate.type.descriptor.java.AbstractClassJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-12292")
@RequiresDialect(H2Dialect.class)
@Jpa(
		annotatedClasses = QueryParametersValidationArrayTest.Event.class
)
public class QueryParametersValidationArrayTest {
	@Test
	public void setParameterWithWrongTypeShouldNotThrowIllegalArgumentException(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				(entityManager) -> {
					entityManager.createNativeQuery(
							"select id " +
									"from Event " +
									"where readings = :readings" )
							.unwrap( NativeQuery.class )
							.setParameter( "readings", new String[]{null, "a"}, StringArrayType.INSTANCE )
							.getResultList();
				}
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
			super( StringArrayJdbcTypeDescriptor.INSTANCE, StringArrayTypeDescriptor.INSTANCE);
		}

		public String getName() {
			return "string-array";
		}

		@Override
		protected boolean registerUnderJavaType() {
			return true;
		}
	}

	public static class StringArrayJdbcTypeDescriptor implements JdbcTypeDescriptor {

		public static final StringArrayJdbcTypeDescriptor INSTANCE = new StringArrayJdbcTypeDescriptor();

		@Override
		public int getJdbcTypeCode() {
			return Types.ARRAY;
		}

		@Override
		public <X> ValueBinder<X> getBinder(JavaType<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
					StringArrayTypeDescriptor arrayTypeDescriptor = (StringArrayTypeDescriptor) javaTypeDescriptor;
					st.setArray(index, st.getConnection().createArrayOf(
							arrayTypeDescriptor.getSqlArrayType(),
							arrayTypeDescriptor.unwrap((String[]) value, Object[].class, options)
					));
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					throw new UnsupportedOperationException("Binding by name is not supported!");
				}
			};
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaTypeDescriptor) {
			return new BasicExtractor<X>( javaTypeDescriptor, this) {
				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap( rs.getArray( paramIndex ), options);
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap(statement.getArray(index), options);
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap(statement.getArray(name), options);
				}
			};
		}

	}

	public static class StringArrayTypeDescriptor
			extends AbstractClassJavaTypeDescriptor<String[]> {

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

		@SuppressWarnings({"unchecked"})
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
