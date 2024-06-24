/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$

package org.hibernate.orm.test.annotations.query;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.Transaction;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.stat.Statistics;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.orm.test.annotations.A320;
import org.hibernate.orm.test.annotations.A320b;
import org.hibernate.orm.test.annotations.Plane;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test named queries
 *
 * @author Emmanuel Bernard
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/annotations/query/orm.xml",
		annotatedClasses = {
				Darkness.class,
				Plane.class,
				A320.class,
				A320b.class,
				Night.class,
				Twilight.class,
				Area.class,
				SpaceShip.class,
				Dictionary.class,
				SynonymousDictionary.class,
				Captain.class,
				Chaos.class,
				CasimirParticle.class,
				AllTables.class,
				Attrset.class,
				Attrvalue.class,
				Employee.class,
				Employeegroup.class
		},
		annotatedPackageNames = {
				"org.hibernate.test.annotations.query"
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
		settingProviders = @SettingProvider(settingName = AvailableSettings.IMPLICIT_NAMING_STRATEGY, provider = QueryAndSQLTest.NamingStrategyProvider.class)
)
public class QueryAndSQLTest {

	public static class NamingStrategyProvider implements SettingProvider.Provider<String> {

		@Override
		public String getSetting() {
			return ImplicitNamingStrategyLegacyJpaImpl.INSTANCE.getClass().getName();
		}
	}

	@Test
	public void testNativeQueryWithFormulaAttribute(SessionFactoryScope scope) {
        final String dateFunctionRendered = scope.getSessionFactory().getJdbcServices().getDialect()
				.currentDate();

		String sql = String.format(
				"select t.table_name as {t.tableName}, %s as {t.daysOld} from ALL_TABLES t where t.table_name = 'AUDIT_ACTIONS' ",
				dateFunctionRendered
		);
		String sql2 = String.format(
				"select table_name as t_name, %s as t_time from ALL_TABLES where table_name = 'AUDIT_ACTIONS' ",
				dateFunctionRendered
		);


		scope.inTransaction(
				session -> {
					session.createNativeQuery( sql ).addEntity( "t", AllTables.class ).list();
					List<AllTables> allTables = session.createNativeQuery( sql, AllTables.class, "t" ).list();
					session.createNativeQuery( sql2, "all" ).list();
					List<String> allTableNames = session.createNativeQuery( sql2, "all", String.class ).list();
					NativeQuery q = session.createNativeQuery( sql2 );
					q.addRoot( "t", AllTables.class ).addProperty( "tableName", "t_name" ).addProperty(
							"daysOld",
							"t_time"
					);
					q.list();
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = AbstractHANADialect.class, matchSubTypes = true, reason = "invalid name of function or procedure: SYSDATE")
	public void testNativeQueryWithFormulaAttributeWithoutAlias(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
                    String sql = "select table_name , " + scope.getSessionFactory().getJdbcServices().getDialect()
							.currentDate() + " as days_old from ALL_TABLES  where table_name = 'AUDIT_ACTIONS' ";
					session.createNativeQuery( sql ).addEntity( "t", AllTables.class ).list();
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10161")
	public void testQueryWithNullParameter(SessionFactoryScope scope) {
		Chaos c0 = new Chaos();
		c0.setId( 0L );
		c0.setName( "c0" );
		c0.setSize( 0L );
		Chaos c1 = new Chaos();
		c1.setId( 1L );
		c1.setName( "c1" );
		c1.setSize( 1L );
		Chaos c2 = new Chaos();
		c2.setId( 2L );
		c2.setName( "c2" );
		c2.setSize( null );

		scope.inTransaction(
				session -> {
					session.persist( c0 );
					session.persist( c1 );
					session.persist( c2 );

					session.flush();
					session.clear();

					List chaoses = session.createQuery( "from Chaos where size is null or size = :size" )
							.setParameter( "size", null )
							.list();
					assertEquals( 1, chaoses.size() );

					chaoses = session.createQuery( "from Chaos where size = :size" )
							.setParameter( "size", null )
							.list();
					// should be no results because null != null
					assertEquals( 0, chaoses.size() );
				}
		);

		scope.inTransaction(
				session ->
						session.createQuery( "delete from Chaos" ).executeUpdate()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10161")
	public void testQueryWithNullParameterTyped(SessionFactoryScope scope) {
		Chaos c0 = new Chaos();
		c0.setId( 0L );
		c0.setName( "c0" );
		c0.setSize( 0L );
		Chaos c1 = new Chaos();
		c1.setId( 1L );
		c1.setName( "c1" );
		c1.setSize( 1L );
		Chaos c2 = new Chaos();
		c2.setId( 2L );
		c2.setName( "c2" );
		c2.setSize( null );

		scope.inTransaction(
				session -> {
					session.persist( c0 );
					session.persist( c1 );
					session.persist( c2 );

					session.flush();
					session.clear();

					List chaoses = session.createQuery( "from Chaos where size is null or size = :size" )
							.setParameter( "size", null, StandardBasicTypes.LONG )
							.list();
					assertEquals( 1, chaoses.size() );

					chaoses = session.createQuery( "from Chaos where size = :size" )
							.setParameter( "size", null, StandardBasicTypes.LONG )
							.list();
					// should be no results because null != null
					assertEquals( 0, chaoses.size() );
				}
		);

		scope.inTransaction(
				session ->
						session.createQuery( "delete from Chaos" ).executeUpdate()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10161")
	public void testNativeQueryWithNullParameter(SessionFactoryScope scope) {
		Chaos c0 = new Chaos();
		c0.setId( 0L );
		c0.setName( "c0" );
		c0.setSize( 0L );
		Chaos c1 = new Chaos();
		c1.setId( 1L );
		c1.setName( "c1" );
		c1.setSize( 1L );
		Chaos c2 = new Chaos();
		c2.setId( 2L );
		c2.setName( "c2" );
		c2.setSize( null );

		scope.inTransaction(
				session -> {
					session.persist( c0 );
					session.persist( c1 );
					session.persist( c2 );

					session.flush();
					session.clear();

					List chaoses = session.createNativeQuery(
									"select * from CHAOS where chaos_size is null or chaos_size = :chaos_size" )
							.setParameter( "chaos_size", null )
							.list();
					assertEquals( 1, chaoses.size() );

					chaoses = session.createNativeQuery( "select * from CHAOS where chaos_size = :chaos_size" )
							.setParameter( "chaos_size", null )
							.list();
					// should be no results because null != null
					assertEquals( 0, chaoses.size() );
				}
		);

		scope.inTransaction(
				session ->
						session.createQuery( "delete from Chaos" ).executeUpdate()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10161")
	public void testNativeQueryWithNullParameterTyped(SessionFactoryScope scope) {
		Chaos c0 = new Chaos();
		c0.setId( 0L );
		c0.setName( "c0" );
		c0.setSize( 0L );
		Chaos c1 = new Chaos();
		c1.setId( 1L );
		c1.setName( "c1" );
		c1.setSize( 1L );
		Chaos c2 = new Chaos();
		c2.setId( 2L );
		c2.setName( "c2" );
		c2.setSize( null );

		scope.inTransaction(
				session -> {
					session.persist( c0 );
					session.persist( c1 );
					session.persist( c2 );

					session.flush();
					session.clear();

					List chaoses = session.createNativeQuery(
									"select * from CHAOS where chaos_size is null or chaos_size = :chaos_size" )
							.setParameter( "chaos_size", null, StandardBasicTypes.LONG )
							.list();
					assertEquals( 1, chaoses.size() );

					chaoses = session.createNativeQuery( "select * from CHAOS where chaos_size = :chaos_size" )
							.setParameter( "chaos_size", null, StandardBasicTypes.LONG )
							.list();
					// should be no results because null != null
					assertEquals( 0, chaoses.size() );
				}
		);

		scope.inTransaction(
				session ->
						session.createQuery( "delete from Chaos" ).executeUpdate()
		);
	}

	@Test
	public void testPackageQueries(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Plane p = new Plane();
					session.persist( p );
					Query q = session.getNamedQuery( "plane.getAll" );
					assertEquals( 1, q.list().size() );
					session.delete( q.list().get( 0 ) );
				}
		);
	}

	@Test
	public void testClassQueries(SessionFactoryScope scope) {
		Night n = new Night();
		Calendar c = new GregorianCalendar();
		c.set( 2000, 2, 2 );
		Date now = c.getTime();
		c.add( Calendar.MONTH, -1 );
		Date aMonthAgo = c.getTime();
		c.add( Calendar.MONTH, 2 );
		Date inAMonth = c.getTime();
		n.setDate( now );
		n.setDuration( 14 );

		scope.inTransaction(
				session ->
						session.persist( n )
		);

		scope.inTransaction(
				session -> {
					Query q = session.getNamedQuery( "night.moreRecentThan" );
					q.setParameter( "date", aMonthAgo, StandardBasicTypes.DATE );
					assertEquals( 1, q.list().size() );
					q = session.getNamedQuery( "night.moreRecentThan" );
					q.setParameter( "date", inAMonth, StandardBasicTypes.DATE );
					assertEquals( 0, q.list().size() );
					Statistics stats = scope.getSessionFactory().getStatistics();
					stats.setStatisticsEnabled( true );
					stats.clear();
					q = session.getNamedQuery( "night.duration" );
					q.setParameter( "duration", 14l );
					assertEquals( 1, q.list().size() );
					assertEquals( 1, stats.getQueryCachePutCount() );
					q = session.getNamedQuery( "night.duration" );
					q.setParameter( "duration", 14l );
					session.delete( q.list().get( 0 ) );
					assertEquals( 1, stats.getQueryCacheHitCount() );
				}
		);
	}

	@Test
	public void testSQLQuery(SessionFactoryScope scope) {
		Night n = new Night();
		Calendar c = new GregorianCalendar();
		c.set( 2000, 2, 2 );
		Date now = c.getTime();
		c.add( Calendar.MONTH, -1 );
		Date aMonthAgo = c.getTime();
		c.add( Calendar.MONTH, 2 );
		Date inAMonth = c.getTime();
		n.setDate( now );
		n.setDuration( 9999 );
		Area area = new Area();
		area.setName( "Monceau" );

		scope.inSession(
				session -> {
					Transaction tx = session.beginTransaction();
					try {

						session.persist( n );
						session.persist( area );
						tx.commit();
						session.clear();
						tx = session.beginTransaction();
						Query q = session.getNamedQuery( "night.getAll.bySQL" );
						q.setParameter( 1, 9990 );
						List result = q.list();
						assertEquals( 1, result.size() );
						Night n2 = (Night) result.get( 0 );
						assertEquals( n2.getDuration(), n.getDuration() );
						List areas = session.getNamedQuery( "getAreaByNative" ).list();
						assertTrue( 1 == areas.size() );
						assertEquals( area.getName(), ( (Area) areas.get( 0 ) ).getName() );
						session.delete( areas.get( 0 ) );
						session.delete( n2 );
						tx.commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);
	}


	/**
	 * We are testing 2 things here:
	 * 1. The query 'night.olderThan' is defined in a MappedSuperClass - Darkness.
	 * We are verifying that queries defined in a MappedSuperClass are processed.
	 * 2. There are 2 Entity classes that extend from Darkness - Night and Twilight.
	 * We are verifying that this does not cause any issues.eg. Double processing of the
	 * MappedSuperClass
	 */
	@Test
	public void testImportQueryFromMappedSuperclass(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					try {
						session.getNamedQuery( "night.olderThan" );
					}
					catch (MappingException ex) {
						fail( "Query imported from MappedSuperclass" );
					}
				}
		);
	}

	@Test
	public void testSQLQueryWithManyToOne(SessionFactoryScope scope) {
		cleanupCache( scope );
		Night n = new Night();
		Calendar c = new GregorianCalendar();
		c.set( 2000, 2, 2 );
		Date now = c.getTime();
		c.add( Calendar.MONTH, -1 );
		Date aMonthAgo = c.getTime();
		c.add( Calendar.MONTH, 2 );
		Date inAMonth = c.getTime();
		n.setDate( now );
		n.setDuration( 9999 );
		Area a = new Area();
		a.setName( "Paris" );
		n.setArea( a );

		scope.inTransaction(
				session -> {
					session.persist( a );
					session.persist( n );
				}
		);

		scope.inTransaction(
				session -> {
					Statistics stats = scope.getSessionFactory().getStatistics();
					stats.setStatisticsEnabled( true );
					stats.clear();
					Query q = session.getNamedQuery( "night&areaCached" );
					q.setCacheable( true );
					List result = q.list();
					assertEquals( 1, result.size() );
					assertEquals( 1, stats.getQueryCachePutCount() );
					q.setCacheable( true );
					q.list();
					assertEquals( 1, stats.getQueryCacheHitCount() );
					Night n2 = (Night) ( (Object[]) result.get( 0 ) )[0];
					assertEquals( n2.getDuration(), n.getDuration() );
					session.delete( n2.getArea() );
					session.delete( n2 );
				}
		);

	}

	@Test
	public void testImplicitNativeQuery(SessionFactoryScope scope) {

		scope.inSession(
				session -> {
					Transaction tx = session.beginTransaction();
					try {
						SpaceShip ship = new SpaceShip();
						ship.setModel( "X-Wing" );
						ship.setName( "YuBlue" );
						ship.setSpeed( 2000 );
						ship.setDimensions( new Dimensions() );
						session.persist( ship );
						tx.commit();
						session.clear();
						tx = session.beginTransaction();
						Query q = session.getNamedQuery( "implicitSample" );
						List result = q.list();
						assertEquals( 1, result.size() );
						assertEquals( ship.getModel(), ( (SpaceShip) result.get( 0 ) ).getModel() );
						session.delete( result.get( 0 ) );
						tx.commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	public void testNativeQueryAndCompositePKAndComponents(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Transaction tx = session.beginTransaction();
					try {
						SpaceShip ship = new SpaceShip();
						ship.setModel( "X-Wing" );
						ship.setName( "YuBlue" );
						ship.setSpeed( 2000 );
						ship.setDimensions( new Dimensions() );
						ship.getDimensions().setLength( 10 );
						ship.getDimensions().setWidth( 5 );
						Captain captain = new Captain();
						captain.setFirstname( "Luke" );
						captain.setLastname( "Skywalker" );
						ship.setCaptain( captain );
						session.persist( captain );
						session.persist( ship );
						tx.commit();
						session.clear();
						tx = session.beginTransaction();
						Query q = session.getNamedQuery( "compositekey" );
						List result = q.list();
						assertEquals( 1, result.size() );
						Object[] row = (Object[]) result.get( 0 );
						SpaceShip spaceShip = (SpaceShip) row[0];
						assertEquals( ship.getModel(), spaceShip.getModel() );
						assertNotNull( spaceShip.getDimensions() );
						assertEquals( ship.getDimensions().getWidth(), spaceShip.getDimensions().getWidth() );
						assertEquals( ship.getDimensions().getLength(), spaceShip.getDimensions().getLength() );
						assertEquals( ship.getCaptain().getFirstname(), ship.getCaptain().getFirstname() );
						assertEquals( ship.getCaptain().getLastname(), ship.getCaptain().getLastname() );
						//FIXME vary depending on databases
						assertTrue( row[1].toString().startsWith( "50" ) );
						assertTrue( row[2].toString().startsWith( "500" ) );
						session.delete( spaceShip.getCaptain() );
						session.delete( spaceShip );
						tx.commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	public void testDiscriminator(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Transaction tx = session.beginTransaction();
					try {
						Dictionary dic = new Dictionary();
						dic.setName( "Anglais-Francais" );
						dic.setEditor( "Harrap's" );
						SynonymousDictionary syn = new SynonymousDictionary();
						syn.setName( "Synonymes de tous les temps" );
						syn.setEditor( "Imagination edition" );
						session.persist( dic );
						session.persist( syn );
						tx.commit();
						session.clear();
						tx = session.beginTransaction();
						List results = session.getNamedQuery( "all.dictionaries" ).list();
						assertEquals( 2, results.size() );
						assertTrue(
								results.get( 0 ) instanceof SynonymousDictionary
										|| results.get( 1 ) instanceof SynonymousDictionary
						);
						session.delete( results.get( 0 ) );
						session.delete( results.get( 1 ) );
						tx.commit();
					}
					finally {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
					}
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = PostgreSQLDialect.class, matchSubTypes = true,
			reason = "postgresql jdbc driver does not implement the setQueryTimeout method")
	public void testCache(SessionFactoryScope scope) {
		Plane plane = new Plane();
		scope.inTransaction(
				session -> {
					plane.setNbrOfSeats( 5 );
					session.persist( plane );
				}
		);

		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		sessionFactory.getStatistics().clear();
		sessionFactory.getStatistics().setStatisticsEnabled( true );

		scope.inTransaction(
				session -> {
					Query query = session.getNamedQuery( "plane.byId" ).setParameter( "id", plane.getId() );
					query.uniqueResult();
					assertEquals( 1, sessionFactory.getStatistics().getQueryCachePutCount() );
					session.getNamedQuery( "plane.byId" ).setParameter( "id", plane.getId() ).uniqueResult();
					assertEquals( 1, sessionFactory.getStatistics().getQueryCacheHitCount() );
				}
		);


		scope.inTransaction(
				session ->
						session.delete( session.get( Plane.class, plane.getId() ) )
		);
	}

	@Test
	public void testEntitySQLOverriding(SessionFactoryScope scope) {
		Chaos chaos = new Chaos();
		scope.inTransaction(
				session -> {
					chaos.setSize( 123l );
					chaos.setId( 1l );

					String lowerName = "hello";
					String upperName = lowerName.toUpperCase( Locale.ROOT );
					assertFalse( lowerName.equals( upperName ) );

					chaos.setName( "hello" );
					chaos.setNickname( "NickName" );
					session.persist( chaos );
					session.flush();
					session.clear();
					session.getSessionFactory().getCache().evictEntityData( Chaos.class );

					Chaos resultChaos = session.load( Chaos.class, chaos.getId() );
					assertEquals( upperName, resultChaos.getName() );
					assertEquals( "nickname", resultChaos.getNickname() );
				}
		);

		scope.inTransaction(
				session ->
						session.createQuery( "delete from Chaos" ).executeUpdate()
		);
	}

	@Test
	public void testCollectionSQLOverriding(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Chaos chaos = new Chaos();
					chaos.setSize( 123l );
					chaos.setId( 1l );

					chaos.setName( "hello" );
					session.persist( chaos );
					CasimirParticle p = new CasimirParticle();
					p.setId( 1l );
					session.persist( p );
					chaos.getParticles().add( p );
					p = new CasimirParticle();
					p.setId( 2l );
					session.persist( p );
					chaos.getParticles().add( p );
					session.flush();
					session.clear();
					session.getSessionFactory().getCache().evictEntityData( Chaos.class );

					Chaos resultChaos = session.load( Chaos.class, chaos.getId() );
					assertEquals( 2, resultChaos.getParticles().size() );
					resultChaos.getParticles().remove( resultChaos.getParticles().iterator().next() );
					resultChaos.getParticles().remove( resultChaos.getParticles().iterator().next() );
					session.flush();

					session.clear();
					resultChaos = session.load( Chaos.class, chaos.getId() );
					assertEquals( 0, resultChaos.getParticles().size() );
				}
		);

		scope.inTransaction(
				session ->
						session.createQuery( "delete from Chaos" ).executeUpdate()
		);
	}

	protected void cleanupCache(SessionFactoryScope scope) {
		final SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		if ( sessionFactory != null ) {
			sessionFactory.getCache().evictAllRegions();
		}
	}
}
