/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.stateless.scroll;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-13405" )
public class StatelessJoinFetchScrollCollectionTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-13405" )
	public void testScrollInStatelessSession() {
		inTransaction(
				session -> {
					int idCounter = 1;
					for ( int i = 1; i <= 3; i++ ) {
						AEntity aEntity = new AEntity( idCounter++ );
						for ( int j = 1; j <= i; j++ ) {
							BEntity bEntity = new BEntity( idCounter++ );
							aEntity.bEntities.add( bEntity );
							bEntity.aEntity = aEntity;
						}
						session.persist( aEntity );
					}
				}
		);

		inStatelessTransaction(
				session -> {
					final String qry = "select a from AEntity a join fetch a.bEntities order by a.id";
					final ScrollableResults results = session.createQuery( qry ).scroll();
					int idCounter = 1;
					for ( int i = 1; i <= 3; i++ ) {
						assertTrue( results.next() );
						final AEntity aEntity = (AEntity) results.get( 0 );
						assertEquals( idCounter++, aEntity.id );
						assertTrue( Hibernate.isInitialized( aEntity.bEntities ) );
						assertEquals( i, aEntity.bEntities.size() );
						final Set<Integer> expectedIds = new HashSet<>();
						for ( int j = 1; j <= i; j++ ) {
							expectedIds.add( idCounter++ );
						}
						for ( BEntity bEntity : aEntity.bEntities ) {
							assertTrue( expectedIds.contains( bEntity.id ) );
							assertSame( aEntity, bEntity.aEntity );
						}
					}
					assertFalse( results.next() );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13405" )
	public void testDistinctListInStatelessSession() {
		inTransaction(
				session -> {
					AEntity a = new AEntity( 1 );
					BEntity b1 = new BEntity( 2 );
					BEntity b2 = new BEntity( 3 );
					CEntity c = new CEntity( 4 );

					b1.aEntity = a;
					b2.aEntity = a;
					b1.cEntity = c;
					b2.cEntity = c;

					session.persist( a );
					session.persist( b1 );
					session.persist( b2 );
					session.persist( c );
				}
		);

		final String hql = "Select distinct a from AEntity a left join fetch a.bEntities order by a.id";

		inStatelessTransaction(
				session -> {
					List result = session.createQuery( hql ).list();
					assertEquals( 1, result.size() );
					for ( Object obj : result ) {
						AEntity a = (AEntity) obj;
						assertTrue( Hibernate.isInitialized( a.bEntities ) );
						CEntity cEntity = null;
						for ( BEntity b : a.bEntities ) {
							assertSame( a, b.aEntity );
							if ( cEntity != null ) {
								// don't use assertSame here because, if the assertion fails,
								// cEntity#toString will be called, then Hibernate will try to load it
								assertTrue( cEntity == b.cEntity );
							}
							else {
								cEntity = b.cEntity;
							}
						}
						assertTrue( HibernateProxy.class.isInstance( cEntity ) );
						assertFalse( Hibernate.isInitialized( cEntity ) );
					}
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13405" )
	public void testDistinctScrollInStatelessSession() {
		inTransaction(
				session -> {
					AEntity a = new AEntity( 1 );
					BEntity b1 = new BEntity( 2 );
					BEntity b2 = new BEntity( 3 );
					CEntity c = new CEntity( 4 );

					b1.aEntity = a;
					b2.aEntity = a;
					b1.cEntity = c;
					b2.cEntity = c;

					session.persist( a );
					session.persist( b1 );
					session.persist( b2 );
					session.persist( c );
				}
		);

		final String hql = "Select distinct a from AEntity a left join fetch a.bEntities order by a.id";

		inStatelessTransaction(
				session -> {
					final ScrollableResults rs = session.createQuery( hql ).scroll( ScrollMode.SCROLL_INSENSITIVE );
					assertTrue( rs.next() );
					AEntity a = (AEntity) rs.get( 0 );
					assertTrue( Hibernate.isInitialized( a.bEntities ) );
					CEntity cEntity = null;
					for ( BEntity b : a.bEntities ) {
						assertSame( a, b.aEntity );
						if ( cEntity != null ) {
							// don't use assertSame here because, if the assertion fails,
							// cEntity#toString will be called, then Hibernate will try to load it
							assertTrue( cEntity == b.cEntity );
						}
						else {
							cEntity = b.cEntity;
						}
					}
					assertTrue( HibernateProxy.class.isInstance( cEntity ) );
					assertFalse( Hibernate.isInitialized( cEntity ) );
					assertFalse( rs.next() );
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

		@OneToMany(mappedBy="aEntity", fetch= FetchType.LAZY, cascade = CascadeType.ALL)
		private Set<BEntity> bEntities = new HashSet<>();

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

		@ManyToOne(fetch=FetchType.LAZY)
		private AEntity aEntity = null;

		@ManyToOne(fetch=FetchType.LAZY, cascade = CascadeType.ALL)
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

		public CEntity(int id) {
			this();
			this.id = id;
			bEntities = new LinkedHashSet();
		}

		protected CEntity() {
			// this form used by Hibernate
		}

		public Set<BEntity> getBEntities() {
			return bEntities;
		}

		@OneToMany(mappedBy="cEntity", fetch=FetchType.LAZY)
		protected Set<BEntity> bEntities = null;
	}
}
