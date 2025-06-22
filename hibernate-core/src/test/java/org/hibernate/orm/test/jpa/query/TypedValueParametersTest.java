/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Type;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.TypedParameterValue;
import org.hibernate.type.CustomType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		TypedValueParametersTest.Document.class
})
public class TypedValueParametersTest {

	@BeforeEach
	public void init(EntityManagerFactoryScope scope) throws Exception {
		scope.inTransaction(
				entityManager -> {
					Document a = new Document();
					a.getTags().add("important");
					a.getTags().add("business");
					entityManager.persist(a);
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testNative(EntityManagerFactoryScope scope) {
		test(scope,
			q -> {
				final CustomType<List<String>> customType = new CustomType<>(
						TagUserType.INSTANCE,
						scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class ).getTypeConfiguration()
				);

				org.hibernate.query.Query hibernateQuery = q.unwrap( org.hibernate.query.Query.class );
				hibernateQuery.setParameter( "tags", Arrays.asList( "important", "business" ), customType );
			}
		);
	}

	@Test
	public void testJpa(EntityManagerFactoryScope scope) {
		test(scope,
			q -> {
				final CustomType<List<String>> customType = new CustomType<>(
						TagUserType.INSTANCE,
						scope.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class ).getTypeConfiguration()
				);
				q.setParameter("tags", TypedParameterValue.of(customType, Arrays.asList("important","business")));
			}
		);
	}

	private void test(EntityManagerFactoryScope scope, Binder b) {
		scope.inTransaction(
				entityManager -> {
					TypedQuery<Long> q = entityManager.createQuery( "select count(*) from Document d where d.tags = :tags", Long.class );
					b.bind( q );

					Long count = q.getSingleResult();
					assertEquals( 1, count.intValue() );
				}
		);
	}

	private interface Binder {
		void bind(Query q);
	}

	@Entity( name = "Document" )
	@Table( name = "Document" )
	public static class Document {

		@Id
		private int id;

		@Type( TagUserType.class )
		@Column(name = "tags")
		private List<String> tags = new ArrayList<>();

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public List<String> getTags() {
			return tags;
		}

		public void setTags(List<String> tags) {
			this.tags = tags;
		}
	}

	public static class TagUserType implements UserType<List<String>> {
		public static final TagUserType INSTANCE = new TagUserType();

		@Override
		public void nullSafeSet(PreparedStatement statement, List<String> list, int index, WrapperOptions options) throws HibernateException, SQLException {
			if ( list == null ) {
				statement.setNull( index, SqlTypes.VARCHAR );
			}
			else {
				StringBuilder sb = new StringBuilder();

				for (int i = 0; i < list.size(); i++) {
					if (i != 0) {
						sb.append('|');
					}
					sb.append(list.get(i));
				}

				statement.setString(index, sb.toString());
			}
		}

		@Override
		public List<String> nullSafeGet(ResultSet rs, int position, WrapperOptions options)
				throws SQLException {
			String string = rs.getString( position );

			if (rs.wasNull()) {
				return null;
			}

			List<String> list = new ArrayList<>();
			int lastIndex = 0, index;

			while ((index = string.indexOf('|', lastIndex)) != -1) {
				list.add(string.substring(lastIndex, index));
				lastIndex = index + 1;
			}

			if (lastIndex != string.length()) {
				list.add(string.substring(lastIndex));
			}

			return list;
		}


		@Override
		public int getSqlType() {
			return Types.VARCHAR;
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Class<List<String>> returnedClass() {
			return (Class) List.class;
		}

		@Override
		public List<String> assemble(final Serializable cached, final Object owner) throws HibernateException {
			return (List<String>) cached;
		}

		@Override
		@SuppressWarnings("unchecked")
		public List<String> deepCopy(final List<String> o) throws HibernateException {
			return o == null ? null : new ArrayList<>( o );
		}

		@Override
		public Serializable disassemble(final List<String> o) throws HibernateException {
			return (Serializable) o;
		}

		@Override
		public boolean equals(final List<String> x, final List<String> y) throws HibernateException {
			return x == null ? y == null : x.equals(y);
		}

		@Override
		public int hashCode(final List<String> o) throws HibernateException {
			return o == null ? 0 : o.hashCode();
		}

		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public List<String> replace(final List<String> original, final List<String> target, final Object owner) throws HibernateException {
			return original;
		}

	}
}
