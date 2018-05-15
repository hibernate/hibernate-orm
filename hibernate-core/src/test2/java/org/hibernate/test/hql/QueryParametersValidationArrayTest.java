/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-12292")
@RequiresDialect(H2Dialect.class)
public class QueryParametersValidationArrayTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Event.class};
	}

	@Test
	public void setParameterWithWrongTypeShouldNotThrowIllegalArgumentException() {
		doInJPA(this::entityManagerFactory, entityManager -> {
			entityManager.createNativeQuery(
				"select id " +
				"from Event " +
				"where readings = :readings" )
			.unwrap( NativeQuery.class )
			.setParameter( "readings", new String[]{null, "a"}, StringArrayType.INSTANCE )
			.getResultList();
		});
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Column(columnDefinition = "ARRAY(1)")
		private String[] readings;
	}

	public static class StringArrayType
			extends AbstractSingleColumnStandardBasicType<String[]> {

		public static final StringArrayType INSTANCE = new StringArrayType ();

		public StringArrayType() {
			super( StringArraySqlTypeDescriptor.INSTANCE, StringArrayTypeDescriptor.INSTANCE);
		}

		public String getName() {
			return "string-array";
		}

		@Override
		protected boolean registerUnderJavaType() {
			return true;
		}
	}

	public static class StringArraySqlTypeDescriptor implements SqlTypeDescriptor {

		public static final StringArraySqlTypeDescriptor INSTANCE = new StringArraySqlTypeDescriptor();

		@Override
		public int getSqlType() {
			return Types.ARRAY;
		}

		@Override
		public boolean canBeRemapped() {
			return true;
		}

		@Override
		public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
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
		public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicExtractor<X>( javaTypeDescriptor, this) {
				@Override
				protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
					return javaTypeDescriptor.wrap(rs.getArray(name), options);
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
			extends AbstractTypeDescriptor<String[]> {

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
		public String[] fromString(String string) {
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
