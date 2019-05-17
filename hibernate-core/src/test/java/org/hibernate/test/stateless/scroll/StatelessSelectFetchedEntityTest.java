/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.stateless.scroll;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-13405" )
public class StatelessSelectFetchedEntityTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-13405" )
	public void testScrollInStatelessSession() {
		inTransaction(
				session -> {
					AEntity aEntity = new AEntity( 1 );
					BEntity bEntity = new BEntity( 2 );
					CEntity cEntity = new CEntity( 3 );
					aEntity.bEntity = bEntity;
					bEntity.cEntity = cEntity;
					session.persist( aEntity );
				}
		);

		inStatelessTransaction(
				session -> {
					final String qry = "select a from AEntity a order by a.id";
					final ScrollableResults results = session.createQuery( qry ).scroll();
					assertTrue( results.next() );
					final AEntity aEntity = (AEntity) results.get( 0 );
					assertEquals( 1, aEntity.id );
					assertTrue( Hibernate.isInitialized( aEntity.bEntity ) );
					assertTrue( Hibernate.isInitialized( aEntity.bEntity.cEntity ) );
					assertFalse( results.next() );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13405" )
	public void testDistinctScrollInStatelessSession() {
		inTransaction(
				session -> {
					AEntity a = new AEntity( 1 );
					BEntity b = new BEntity( 2 );
					CEntity c = new CEntity( 3 );
					a.bEntity = b;
					b.cEntity = c;
					session.persist( a );
				}
		);

		final String hql = "Select distinct a from AEntity a order by a.id";

		inStatelessTransaction(
				session -> {
					final ScrollableResults rs = session.createQuery( hql ).scroll( ScrollMode.SCROLL_INSENSITIVE );
					while ( rs.next() ) {
						AEntity a = (AEntity) rs.get(0);
						log.info("result entity: " + a.id );
					}
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13405" )
	public void testDistinctListInStatelessSession() {
		inTransaction(
				session -> {
					AEntity a = new AEntity( 1 );
					BEntity b = new BEntity( 2 );
					CEntity c = new CEntity( 3 );
					a.bEntity = b;
					b.cEntity = c;
					session.persist( a );
				}
		);

		final String hql = "Select distinct a from AEntity a order by a.id";

		inStatelessTransaction(
				session -> {
					List result = session.createQuery( hql ).list();
					for (Object obj : result) {
						AEntity a = (AEntity) obj;
						log.info("result entity: " + a.id );
					}
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13405" )
	public void testGetInStatelessSession() {
		inTransaction(
				session -> {
					AEntity a = new AEntity( 1 );
					BEntity b = new BEntity( 2 );
					CEntity c = new CEntity( 3 );
					a.bEntity = b;
					b.cEntity = c;
					session.persist( a );
				}
		);

		inStatelessTransaction(
				session -> {
					AEntity a = (AEntity) session.get( AEntity.class, 1 );
				}
		);
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( AEntity.class );
		sources.addAnnotatedClass( BEntity.class );
		sources.addAnnotatedClass( CEntity.class );
	}

	@After
	public void cleanUpTestData() {
		inTransaction(
				session -> {
					for ( AEntity aEntity : session.createQuery( "from AEntity", AEntity.class ).list() ) {
						session.delete( aEntity );
					}
				}
		);
	}

	@Entity(name="AEntity")
	public static class AEntity {

		@Id
		private int id;

		@ManyToOne(fetch= FetchType.EAGER, cascade = CascadeType.ALL)
		@Fetch(FetchMode.SELECT)
		private BEntity bEntity;

		public AEntity() {
		}

		public AEntity(int id) {
			this.id = id;
		}

	}

	@Entity(name="BEntity")
	public static class BEntity {
		@Id
		private int id;

		@ManyToOne(fetch= FetchType.EAGER, cascade = CascadeType.ALL)
		@Fetch(FetchMode.SELECT)
		protected CEntity cEntity = null;

		public BEntity() {
		}

		public BEntity(int id) {
			this.id = id;
		}
	}

	@Entity(name="CEntity")
	public static class CEntity {

		@Id
		private int id;

		@ManyToOne(fetch= FetchType.EAGER, cascade = CascadeType.ALL)
		@Fetch(FetchMode.SELECT)
		private AEntity aEntity = null;

		public CEntity(int id) {
			this();
			this.id = id;
		}

		protected CEntity() {
			// this form used by Hibernate
		}
	}
}
