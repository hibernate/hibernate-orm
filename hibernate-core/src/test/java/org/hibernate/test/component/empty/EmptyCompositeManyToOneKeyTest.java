/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.component.empty;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class EmptyCompositeManyToOneKeyTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class,
				OtherEntity.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.getProperties().put( Environment.CREATE_EMPTY_COMPOSITES_ENABLED, "true" );
		configuration.getProperties().put( Environment.USE_SECOND_LEVEL_CACHE, "false" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11922" )
	public void testGetEntityWithNullManyToOne() {
		int id = doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = new AnEntity();
					session.persist( anEntity );
					return anEntity.id;
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = session.find( AnEntity.class, id );
					assertNotNull( anEntity );
					assertNull( anEntity.otherEntity );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11922" )
	public void testQueryEntityWithNullManyToOne() {
		int id = doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = new AnEntity();
					session.persist( anEntity );
					return anEntity.id;
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = session.createQuery(
							"from AnEntity where id = " + id,
							AnEntity.class
					).uniqueResult();
					assertNull( anEntity.otherEntity );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11922" )
	public void testQueryEntityJoinFetchNullManyToOne() {
		int id = doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = new AnEntity();
					session.persist( anEntity );
					return anEntity.id;
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = session.createQuery(
							"from AnEntity e join fetch e.otherEntity where e.id = " + id,
							AnEntity.class
					).uniqueResult();
					assertNull( anEntity );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11922" )
	public void testQueryEntityLeftJoinFetchNullManyToOne() {
		int id = doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = new AnEntity();
					session.persist( anEntity );
					return anEntity.id;
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = session.createQuery(
							"from AnEntity e left join fetch e.otherEntity where e.id = " + id,
							AnEntity.class
					).uniqueResult();
					assertNull( anEntity.otherEntity );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11922" )
	public void testQueryEntityAndNullManyToOne() {
		int id = doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = new AnEntity();
					session.persist( anEntity );
					return anEntity.id;
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					final Object[] result = session.createQuery(
							"select e, e.otherEntity from AnEntity e left join e.otherEntity where e.id = " + id,
							Object[].class
					).uniqueResult();
					assertEquals( 2, result.length );
					assertTrue( AnEntity.class.isInstance( result[0] ) );
					assertNull( result[1] );
				}
		);
	}


	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Entity(name  = "AnEntity")
	public static class AnEntity {
		@Id
		private int id;

		@ManyToOne
		private OtherEntity otherEntity;
	}

	@Entity(name  = "OtherEntity")
	public static class OtherEntity implements Serializable {
		@Id
		private String firstName;

		@Id
		private String lastName;

		private String description;


		@Override
		public String toString() {
			return "OtherEntity{" +
					"firstName='" + firstName + '\'' +
					", lastName='" + lastName + '\'' +
					", description='" + description + '\'' +
					'}';
		}
	}
}
