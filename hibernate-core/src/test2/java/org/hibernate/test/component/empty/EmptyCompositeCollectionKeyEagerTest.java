/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.component.empty;

import java.io.Serializable;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class EmptyCompositeCollectionKeyEagerTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.getProperties().put( Environment.CREATE_EMPTY_COMPOSITES_ENABLED, "true" );
		configuration.getProperties().put( Environment.USE_SECOND_LEVEL_CACHE, "false" );
	}

	@Test
	public void testGetEntityWithEmptyCollection() {
		AnEntity.PK id = doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = new AnEntity( new AnEntity.PK( "first", "last" ));
					session.persist( anEntity );
					return anEntity.id;
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = session.find( AnEntity.class, id );
					assertTrue( Hibernate.isInitialized( anEntity.names ) );
					assertTrue( anEntity.names.isEmpty() );
				}
		);
	}

	@Test
	public void testQueryEntityWithEmptyCollection() {
		AnEntity.PK id = doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = new AnEntity( new AnEntity.PK( "first", "last" ) );
					session.persist( anEntity );
					return anEntity.id;
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = session.createQuery(
							"from AnEntity where id = :id",
							AnEntity.class
					).setParameter( "id", id ).uniqueResult();
					assertTrue( Hibernate.isInitialized( anEntity.names ) );
					assertTrue( anEntity.names.isEmpty() );
				}
		);
	}

	@Test
	public void testQueryEntityJoinFetchEmptyCollection() {
		AnEntity.PK id = doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = new AnEntity( new AnEntity.PK( "first", "last" ) );
					session.persist( anEntity );
					return anEntity.id;
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = session.createQuery(
							"from AnEntity e join fetch e.names where e.id = :id ",
							AnEntity.class
					).setParameter( "id", id ).uniqueResult();
					assertNull( anEntity );
				}
		);
	}

	@Test
	public void testQueryEntityLeftJoinFetchEmptyCollection() {
		AnEntity.PK id = doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = new AnEntity( new AnEntity.PK( "first", "last" ) );
					session.persist( anEntity );
					return anEntity.id;
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					final AnEntity anEntity = session.createQuery(
							"from AnEntity e left join fetch e.names where e.id = :id",
							AnEntity.class
					).setParameter( "id", id ).uniqueResult();
					assertTrue( Hibernate.isInitialized( anEntity.names ) );
					assertTrue( anEntity.names.isEmpty() );
				}
		);
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Entity(name  = "AnEntity")
	public static class AnEntity {
		@EmbeddedId
		private PK id;

		@ElementCollection(fetch = FetchType.EAGER)
		private Set<String> names;

		public AnEntity() {
		}

		public AnEntity(PK id) {
			this.id = id;
		}

		@Embeddable
		public static class PK implements Serializable {
			private String firstName;
			private String lastName;

			public PK() {
			}

			public PK(String firstName, String lastName) {
				this.firstName = firstName;
				this.lastName = lastName;
			}
		}
	}
}
