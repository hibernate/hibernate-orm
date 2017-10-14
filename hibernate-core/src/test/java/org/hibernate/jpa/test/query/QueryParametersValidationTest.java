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
import java.sql.Types;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Type;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.usertype.UserType;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
public class QueryParametersValidationTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {TestEntity.class};
	}

	@TestForIssue(jiraKey = "HHH-11397")
	@Test(expected = IllegalArgumentException.class)
	public void setParameterWithWrongTypeShouldThrowIllegalArgumentException() {
		final EntityManager entityManager = entityManagerFactory().createEntityManager();
		try {
			entityManager.createQuery( "select e from TestEntity e where e.id = :id" ).setParameter( "id", 1 );
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11579")
	public void setParameterWithWrongTypeShouldNotThrowIllegalArgumentExceptionWhenValidationIsDisabled() {
		final SessionFactory sessionFactory = entityManagerFactory().unwrap( SessionFactory.class );
		final Session session = sessionFactory.withOptions().setQueryParameterValidation( false ).openSession();
		try {
			session.createQuery( "select e from TestEntity e where e.id = :id" ).setParameter( "id", 1 );
		}
		finally {
			session.close();
			sessionFactory.close();
		}
	}

	@Test
	public void setParameterWithCorrectTypeShouldNotThrowIllegalArgumentException() {
		final EntityManager entityManager = entityManagerFactory().createEntityManager();
		try {
			entityManager.createQuery( "select e from TestEntity e where e.id = :id" ).setParameter( "id", 1L );
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11971")
	public void setPrimitiveParameterShouldNotThrowExceptions() {
		final EntityManager entityManager = entityManagerFactory().createEntityManager();
		try {
			entityManager.createQuery( "select e from TestEntity e where e.active = :active" ).setParameter(
					"active",
					true
			);
			entityManager.createQuery( "select e from TestEntity e where e.active = :active" ).setParameter(
					"active",
					Boolean.TRUE
			);
		}
		finally {
			entityManager.close();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	@TestForIssue( jiraKey = "HHH-11971")
	public void setWrongPrimitiveParameterShouldThrowIllegalArgumentException() {
		final EntityManager entityManager = entityManagerFactory().createEntityManager();
		try {
			entityManager.createQuery( "select e from TestEntity e where e.active = :active" ).setParameter( "active", 'c' );
		}
		finally {
			entityManager.close();
		}
	}

	@Entity(name = "TestEntity")
	public class TestEntity {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@Type(type = "org.hibernate.jpa.test.query.QueryParametersValidationTest$BooleanUserType")
		private boolean active;
	}

	public static class BooleanUserType implements UserType {

		@Override
		public int[] sqlTypes() {
			return new int[] { Types.CHAR };
		}

		@Override
		public Class returnedClass() {
			return boolean.class;
		}

		@Override
		public boolean equals(Object x, Object y) throws HibernateException {
			return Objects.equals( x, y);
		}

		@Override
		public int hashCode(Object x) throws HibernateException {
			return Objects.hashCode(x);
		}

		@Override
		public Object nullSafeGet(
				ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
				throws HibernateException, SQLException {
			return "Y".equals(rs.getString(names[0]));
		}

		@Override
		public void nullSafeSet(
				PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
				throws HibernateException, SQLException {
			st.setString(index, ((Boolean) value).booleanValue() ? "Y" : "N");
		}

		@Override
		public Object deepCopy(Object value) throws HibernateException {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(Object value) throws HibernateException {
			return null;
		}

		@Override
		public Object assemble(Serializable cached, Object owner) throws HibernateException {
			return null;
		}

		@Override
		public Object replace(Object original, Object target, Object owner) throws HibernateException {
			return null;
		}
	}
}
