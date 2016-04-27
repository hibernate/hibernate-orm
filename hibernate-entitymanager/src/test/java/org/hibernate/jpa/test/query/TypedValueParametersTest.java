/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TypedQuery;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.type.CustomType;
import org.hibernate.usertype.UserType;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class TypedValueParametersTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Document.class };
	}

	private int docId;

	@Before
	public void init() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Document a = new Document();
		a.getTags().add("important");
		a.getTags().add("business");
		em.persist(a);
		docId = a.getId();

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testNative() {
		test(new Binder() {

			public void bind(Query q) {
				org.hibernate.Query hibernateQuery = q.unwrap(org.hibernate.Query.class);
				hibernateQuery.setParameter("tags", Arrays.asList("important","business"), new CustomType(TagUserType.INSTANCE));
			}
		});
	}

	@Test
	public void testJpa() {
		test(new Binder() {

			public void bind(Query q) {
				q.setParameter("tags", new TypedParameterValue( new CustomType( TagUserType.INSTANCE), Arrays.asList("important","business")));
			}
		});

	}

	private void test(Binder b) {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		TypedQuery<Long> q = em.createQuery( "select count(*) from Document d where d.tags = :tags", Long.class );
		b.bind( q );

		Long count = q.getSingleResult();

		em.getTransaction().commit();
		em.close();

		assertEquals( 1, count.intValue() );
	}

	private interface Binder {
		void bind(Query q);
	}

	@Entity( name = "Document" )
	@Table( name = "Document" )
	@TypeDef(name = "tagList", typeClass = TagUserType.class)
	public static class Document {

		@Id
		private int id;

		@Type(type = "tagList")
		@Column(name = "tags")
		private List<String> tags = new ArrayList<String>();

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

	public static class TagUserType implements UserType {

		public static final UserType INSTANCE = new TagUserType();

		private final int SQLTYPE = java.sql.Types.VARCHAR;

		@Override
		public void nullSafeSet(PreparedStatement statement, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
			if (value == null) {
				statement.setNull(index, SQLTYPE);
			} else {
				@SuppressWarnings("unchecked")
				List<String> list = (List<String>) value;
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
		public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
			String string = rs.getString(names[0]);

			if (rs.wasNull()) {
				return null;
			}

			List<String> list = new ArrayList<String>();
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

		public int[] sqlTypes() {
			return new int[]{SQLTYPE};
		}

		public Class returnedClass() {
			return List.class;
		}

		@Override
		public Object assemble(final Serializable cached, final Object owner) throws HibernateException {
			return cached;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object deepCopy(final Object o) throws HibernateException {
			return o == null ? null : new ArrayList<String>((List<String>) o);
		}

		@Override
		public Serializable disassemble(final Object o) throws HibernateException {
			return (Serializable) o;
		}

		@Override
		public boolean equals(final Object x, final Object y) throws HibernateException {
			return x == null ? y == null : x.equals(y);
		}

		@Override
		public int hashCode(final Object o) throws HibernateException {
			return o == null ? 0 : o.hashCode();
		}

		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public Object replace(final Object original, final Object target, final Object owner) throws HibernateException {
			return original;
		}

	}
}
