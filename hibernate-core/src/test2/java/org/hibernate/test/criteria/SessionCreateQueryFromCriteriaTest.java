/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.dialect.AbstractHANADialect;
import org.junit.After;
import org.junit.Test;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10950")
public class SessionCreateQueryFromCriteriaTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {TestEntity.class};
	}

	@After
	public void tearDown() {
		try (Session s = openSession()) {
			session.getTransaction().begin();
			try {
				s.createQuery( "delete from TestEntity" ).executeUpdate();
				s.getTransaction().commit();
			}
			catch (Exception e) {
				if ( s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
				throw e;
			}
		}
	}

	@Test
	public void testUniqueResult() {
		final String entityName = "expected";
		try (Session session = openSession()) {
			final CriteriaQuery<TestEntity> query = createTestEntityCriteriaQuery( entityName, session );
			final Optional<TestEntity> result = session.createQuery( query ).uniqueResultOptional();
			assertThat( result.isPresent(), is( false ) );
		}
	}

	private CriteriaQuery<TestEntity> createTestEntityCriteriaQuery(
			String entityName,
			Session session) {
		final CriteriaBuilder builder = session.getCriteriaBuilder();
		final CriteriaQuery<TestEntity> query =
				builder.createQuery( TestEntity.class );
		final Root<TestEntity> fromTestEntity = query.from( TestEntity.class );
		query.select( fromTestEntity );
		query.where( builder.equal(
				fromTestEntity.get( "name" ),
				entityName
		) );
		return query;
	}

	@Test
	public void testStreamMethod() {
		final String entityName = "expected";
		insertTestEntity( entityName );
		try (Session session = openSession()) {
			final CriteriaQuery<TestEntity> query = createTestEntityCriteriaQuery(
					entityName,
					session
			);
			final Stream<TestEntity> stream = session.createQuery( query ).stream();
			assertThat( stream.count(), is( 1L ) );
		}
	}

	@Test
	@SkipForDialect(value = AbstractHANADialect.class, comment = "HANA only supports forward-only cursors")
	public void testScrollMethod() {
		final String entityName = "expected";
		insertTestEntity( entityName );
		try (Session session = openSession()) {
			final CriteriaQuery<TestEntity> query = createTestEntityCriteriaQuery(
					entityName,
					session
			);
			try (final ScrollableResults scroll = session.createQuery( query ).scroll()) {
				assertThat( scroll.first(), is( true ) );
			}

		}
	}

	private void insertTestEntity(String name) {
		final TestEntity entity = new TestEntity();
		entity.setName( name );
		try (Session s = openSession()) {
			session.getTransaction().begin();
			try {
				s.save( entity );
				s.getTransaction().commit();
			}
			catch (Exception e) {
				if ( s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
				throw e;
			}
		}
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		@Id
		@GeneratedValue
		private long id;

		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
