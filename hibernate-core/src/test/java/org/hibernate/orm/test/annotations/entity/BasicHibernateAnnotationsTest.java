/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.hibernate.Hibernate;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

/**
 * @author Emmanuel Bernard
 */
@SessionFactory
@DomainModel(
		annotatedClasses = {
				Forest.class,
				Tree.class,
				Ransom.class,
				ZipCode.class,
				Flight.class,
				Name.class,
				FormalLastName.class,
				ContactDetails.class,
				Topic.class,
				Narrative.class,
				Drill.class,
				PowerDrill.class,
				SoccerTeam.class,
				Player.class,
				Doctor.class,
				PhoneNumberConverter.class
		},
		annotatedPackageNames = "org.hibernate.orm.test.annotations.entity"
)
public class BasicHibernateAnnotationsTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class)
	public void testEntity(SessionFactoryScope scope) throws Exception {
		Forest forest = new Forest();
		forest.setName( "Fontainebleau" );
		scope.inTransaction(
				session -> session.persist( forest )
		);

		scope.inTransaction(
				session -> {
					Forest f = session.find( Forest.class, forest.getId() );
					assertThat( f ).isNotNull();
					f.setName( "Fontainebleau" );
					//should not execute SQL update
				}
		);

		scope.inTransaction(
				session -> {
					Forest f = session.find( Forest.class, forest.getId() );
					assertThat( f ).isNotNull();
					f.setLength( 23 );
					//should execute dynamic SQL update
				}
		);

		scope.inTransaction(
				session -> session.remove( session.find( Forest.class, forest.getId() ) )
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	public void testVersioning(SessionFactoryScope scope) {
		Forest forest = new Forest();
		forest.setName( "Fontainebleau" );
		forest.setLength( 33 );

		scope.inTransaction(
				session -> session.persist( forest )
		);

		scope.inSession(
				parallelSession -> {
					try {
						parallelSession.getTransaction().begin();
						Forest forestFromParallelSession = scope.fromTransaction(
								s -> {
									Forest f = parallelSession.find( Forest.class, forest.getId() );
									Forest reloadedForest = s.find( Forest.class, forest.getId() );
									reloadedForest.setLength( 11 );
									assertThat( reloadedForest ).isNotSameAs( f );
									return f;
								}
						);
						forestFromParallelSession.setLength( 22 );
						parallelSession.getTransaction().commit();
						fail( "All optimistic locking should have make it fail" );
					}
					catch (Exception e) {
						if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof CockroachDialect ) {
							// CockroachDB always runs in SERIALIZABLE isolation, and throws a RollbackException
							assertThat( e ).isInstanceOf( RollbackException.class );
						}
						else {
							assertThat( e ).isInstanceOf( OptimisticLockException.class );
						}

						if ( parallelSession.getTransaction().isActive() ) {
							parallelSession.getTransaction().rollback();
						}
					}
				}
		);

		scope.inTransaction(
				session -> session.remove( session.find( Forest.class, forest.getId() ) )
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class)
	public void testWhereClause(SessionFactoryScope scope) {
		List<Doctor> doctors = new ArrayList<>();

		Doctor goodDoctor = new Doctor();
		goodDoctor.setName( "goodDoctor" );
		goodDoctor.setYearsExperience( 15 );
		goodDoctor.setActiveLicense( true );
		doctors.add( goodDoctor );

		Doctor docNotExperiencedLicensed = new Doctor();
		docNotExperiencedLicensed.setName( "docNotExperiencedLicensed" );
		docNotExperiencedLicensed.setYearsExperience( 1 );
		docNotExperiencedLicensed.setActiveLicense( true );
		doctors.add( docNotExperiencedLicensed );

		Doctor docExperiencedUnlicensed = new Doctor();
		docExperiencedUnlicensed.setName( "docExperiencedNotlicensed" );
		docExperiencedUnlicensed.setYearsExperience( 10 );
		doctors.add( docExperiencedUnlicensed );

		Doctor badDoctor = new Doctor();
		badDoctor.setName( "badDoctor" );
		badDoctor.setYearsExperience( 2 );
		doctors.add( badDoctor );

		SoccerTeam team = new SoccerTeam();
		team.setName( "New team" );
		team.getPhysiologists().addAll( doctors );

		scope.inTransaction(
				session -> {
					for ( Doctor doctor : doctors ) {
						session.persist( doctor );
					}

					session.persist( team );
				}
		);

		scope.inTransaction(
				session -> {
					List<Doctor> list = session.createSelectionQuery( "from " + Doctor.class.getName() + " order by id", Doctor.class )
							.getResultList();

					assertThat( list.size() ).isEqualTo( 2 );

					assertThat( list.get( 0 ).getName() ).isEqualTo( goodDoctor.getName() );
					assertThat( list.get( 1 ).getName() ).isEqualTo( docExperiencedUnlicensed.getName() );

					SoccerTeam loadedTeam = session.find( SoccerTeam.class, team.getId() );

					assertThat( loadedTeam.getPhysiologists().size() ).isEqualTo( 1 );
					assertThat( loadedTeam.getPhysiologists().get( 0 ).getName() ).isEqualTo( goodDoctor.getName() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class)
	public void testType(SessionFactoryScope scope) {
		Forest f = new Forest();
		f.setName( "Broceliande" );
		String description = "C'est une enorme foret enchantee ou vivais Merlin et toute la clique";
		f.setLongDescription( description );
		scope.inTransaction(
				session -> session.persist( f )
		);

		scope.inTransaction(
				session -> {
					Forest forest = session.find( Forest.class, f.getId() );
					assertThat( forest ).isNotNull();
					assertThat( forest.getLongDescription() ).isEqualTo( description );
					session.remove( forest );
				}
		);
	}

	@Test
	public void testLoading(SessionFactoryScope scope) {
		final Forest created = scope.fromTransaction( (session) -> {
			Forest f = new Forest();
			session.persist( f );
			return f;
		} );

		// getReference
		scope.inTransaction( (session) -> {
			final Forest reference = session.getReference( Forest.class, created.getId() );
			assertThat( Hibernate.isInitialized( reference ) ).isFalse();
		} );

		// find
		scope.inTransaction( (session) -> {
			final Forest reference = session.find( Forest.class, created.getId() );
			assertThat( Hibernate.isInitialized( reference ) ).isTrue();
		} );
	}

	@Test
	public void testCache(SessionFactoryScope scope) {
		ZipCode zc = new ZipCode();
		scope.inTransaction(
				session -> {
					zc.code = "92400";
					session.persist( zc );
				}
		);
		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		sessionFactory.getStatistics().clear();
		sessionFactory.getStatistics().setStatisticsEnabled( true );
		sessionFactory.getCache().evictEntityData( ZipCode.class );

		scope.inTransaction(
				session -> {
					session.find( ZipCode.class, zc.code );
					assertThat( sessionFactory.getStatistics().getSecondLevelCachePutCount() ).isEqualTo( 1 );
				}
		);

		scope.inTransaction(
				session -> {
					session.find( ZipCode.class, zc.code );
					assertThat( sessionFactory.getStatistics().getSecondLevelCacheHitCount() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testFilterOnCollection(SessionFactoryScope scope) {
		Topic t = new Topic();
		scope.inTransaction(
				session -> {
					Narrative n1 = new Narrative();
					n1.setState( "published" );
					t.addNarrative( n1 );

					Narrative n2 = new Narrative();
					n2.setState( "draft" );
					t.addNarrative( n2 );

					session.persist( t );
				}
		);

		scope.inTransaction(
				session -> {
					Topic topic = session.getReference( Topic.class, t.getId() );

					session.enableFilter( "byState" ).setParameter( "state", "published" );
					topic = session.getReference( Topic.class, topic.getId() );
					assertThat( topic ).isNotNull();
					assertThat( topic.getNarratives().size() ).isEqualTo( 1 );
					assertThat( topic.getNarratives().iterator().next().getState() ).isEqualTo( "published" );
				}
		);

		scope.inTransaction(
				session -> session.createMutationQuery( "delete from " + Narrative.class.getSimpleName() )
						.executeUpdate()
		);
	}

	@Test
	public void testCascadedDeleteOfChildEntitiesBug2(SessionFactoryScope scope) {
		// Relationship is one SoccerTeam to many Players.
		// Create a SoccerTeam (parent) and three Players (child).
		// Verify that the count of Players is correct.
		// Clear the SoccerTeam reference Players.
		// The orphanRemoval should remove the Players automatically.
		// @OneToMany(mappedBy="name", orphanRemoval=true)
		SoccerTeam t = new SoccerTeam();
		scope.inTransaction(
				session -> {
					Player player1 = new Player();
					player1.setName( "Shalrie Joseph" );
					t.addPlayer( player1 );

					Player player2 = new Player();
					player2.setName( "Taylor Twellman" );
					t.addPlayer( player2 );

					Player player3 = new Player();
					player3.setName( "Steve Ralston" );
					t.addPlayer( player3 );
					session.persist( t );
				}
		);

		scope.inTransaction(
				session -> {
					SoccerTeam team = session.merge( t );
					Long count = session.createSelectionQuery( "select count(*) from Player", Long.class ).list()
							.get( 0 );
					assertThat( count ).isEqualTo( 3 );

					// clear references to players, this should orphan the players which should
					// in turn trigger orphanRemoval logic.
					team.getPlayers().clear();
//		count = ( (Long) s.createQuery( "select count(*) from Player" ).iterate().next() ).intValue();
//		assertEquals("expected count of 0 but got = " + count, count, 0);
				}
		);


		scope.inTransaction(
				session -> {
					Long count = session.createSelectionQuery( "select count(*) from Player", Long.class ).list()
							.get( 0 );
					assertThat( count ).isEqualTo( 0 );
				}
		);
	}

	@Test
	public void testCascadedDeleteOfChildOneToOne(SessionFactoryScope scope) {
		// create two single player teams (for one versus one match of soccer)
		// and associate teams with players via the special OneVOne methods.
		// Clear the Team reference to players, which should orphan the teams.
		// Orphaning the team should delete the team.

		scope.inTransaction(
				session -> {
					SoccerTeam team = new SoccerTeam();
					team.setName( "Shalrie's team" );
					Player player1 = new Player();
					player1.setName( "Shalrie Joseph" );
					team.setOneVonePlayer( player1 );
					player1.setOneVoneTeam( team );

					session.persist( team );
					SoccerTeam team2 = new SoccerTeam();
					team2.setName( "Taylor's team" );
					Player player2 = new Player();
					player2.setName( "Taylor Twellman" );
					team2.setOneVonePlayer( player2 );
					player2.setOneVoneTeam( team2 );
					session.persist( team2 );

					session.getTransaction().commit();
					session.getTransaction().begin();
					session.clear();
					team2 = session.getReference( team2.getClass(), team2.getId() );
					team = session.getReference( team.getClass(), team.getId() );
					Long count = session.createSelectionQuery( "select count(*) from Player", Long.class ).list()
							.get( 0 );
					assertThat( count ).isEqualTo( 2 );

					// clear references to players, this should orphan the players which should
					// in turn trigger orphanRemoval logic.
					team.setOneVonePlayer( null );
					team2.setOneVonePlayer( null );
				}
		);

		scope.inTransaction(
				session -> {
					Long count = session.createSelectionQuery( "select count(*) from Player", Long.class ).list()
							.get( 0 );
					assertThat( count ).isEqualTo( 0 );
				}
		);
	}

	@Test
	public void testFilter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete Forest" ).executeUpdate();
					Forest f1 = new Forest();
					f1.setLength( 2 );
					session.persist( f1 );
					Forest f2 = new Forest();
					f2.setLength( 20 );
					session.persist( f2 );
					Forest f3 = new Forest();
					f3.setLength( 200 );
					session.persist( f3 );
				}
		);

		scope.inTransaction(
				session -> {
					session.enableFilter( "betweenLength" ).setParameter( "minLength", 5 )
							.setParameter( "maxLength", 50 );
					long count = session.createSelectionQuery( "select count(*) from Forest", Long.class ).list()
							.get( 0 );
					assertThat( count ).isEqualTo( 1 );
					session.disableFilter( "betweenLength" );
					session.enableFilter( "minLength" ).setParameter( "minLength", 5 );
					count = session.createSelectionQuery( "select count(*) from Forest", Long.class ).list().get( 0 );
					assertThat( count ).isEqualTo( 2 );

					session.disableFilter( "minLength" );
				}
		);


	}

	/**
	 * Tests the functionality of inheriting @Filter and @FilterDef annotations
	 * defined on a parent MappedSuperclass(s)
	 */
	@Test
	public void testInheritFiltersFromMappedSuperclass(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete Drill" ).executeUpdate();
					Drill d1 = new PowerDrill();
					d1.setName( "HomeDrill1" );
					d1.setCategory( "HomeImprovment" );
					session.persist( d1 );
					Drill d2 = new PowerDrill();
					d2.setName( "HomeDrill2" );
					d2.setCategory( "HomeImprovement" );
					session.persist( d2 );
					Drill d3 = new PowerDrill();
					d3.setName( "HighPowerDrill" );
					d3.setCategory( "Industrial" );
					session.persist( d3 );
				}
		);

		scope.inTransaction(
				session -> {
					//We test every filter with 2 queries, the first on the base class of the
					//inheritance hierarchy (Drill), and the second on a subclass (PowerDrill)
					session.enableFilter( "byName" ).setParameter( "name", "HomeDrill1" );
					long count = session.createSelectionQuery( "select count(*) from Drill", Long.class ).list()
							.get( 0 );
					assertThat( count ).isEqualTo( 1 );
					count = session.createSelectionQuery( "select count(*) from PowerDrill", Long.class ).list()
							.get( 0 );
					assertThat( count ).isEqualTo( 1 );
					session.disableFilter( "byName" );

					session.enableFilter( "byCategory" ).setParameter( "category", "Industrial" );
					count = session.createSelectionQuery( "select count(*) from Drill", Long.class ).list().get( 0 );
					assertThat( count ).isEqualTo( 1 );
					count = session.createSelectionQuery( "select count(*) from PowerDrill", Long.class ).list()
							.get( 0 );
					assertThat( count ).isEqualTo( 1 );
					session.disableFilter( "byCategory" );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class)
	public void testParameterizedType(SessionFactoryScope scope) {
		Forest f = new Forest();
		scope.inTransaction(
				session -> {
					f.setSmallText( "ThisIsASmallText" );
					f.setBigText( "ThisIsABigText" );
					session.persist( f );
				}
		);

		scope.inTransaction(
				session -> {
					Forest f2 = session.find( Forest.class, f.getId() );
					assertThat( f2.getSmallText() ).isEqualTo( f.getSmallText().toLowerCase( Locale.ROOT ) );
					assertThat( f2.getBigText() ).isEqualTo( f.getBigText().toUpperCase( Locale.ROOT ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class)
	@SkipForDialect(
			dialectClass = SybaseDialect.class,
			reason = "Sybase does not support LOB comparisons, and data cleanup plus OptimisticLockType.ALL on Forest triggers LOB comparison"
	)
	@SkipForDialect(
			dialectClass = PostgreSQLDialect.class,
			reason = "PGSQL does not support LOB comparisons, and data cleanup plus OptimisticLockType.ALL on Forest triggers LOB comparison"
	)
	@SkipForDialect(
			dialectClass = DerbyDialect.class,
			reason = "Derby does not support LOB comparisons, and data cleanup plus OptimisticLockType.ALL on Forest triggers LOB comparison"
	)
	@SkipForDialect(
			dialectClass = OracleDialect.class,
			reason = "Oracle does not support LOB comparisons, and data cleanup plus OptimisticLockType.ALL on Forest triggers LOB comparison"
	)
	public void testSerialized(SessionFactoryScope scope) {
		Forest f = new Forest();
		f.setName( "Shire" );
		Country c = new Country();
		c.setName( "Middle Earth" );
		f.setCountry( c );
		Set<Country> nearCountries = new HashSet<>();
		c = new Country();
		c.setName( "Mordor" );
		nearCountries.add( c );
		c = new Country();
		c.setName( "Gondor" );
		nearCountries.add( c );
		c = new Country();
		c.setName( "Eriador" );
		nearCountries.add( c );
		f.setNear( nearCountries );
		scope.inTransaction(
				session -> session.persist( f )
		);

		scope.inTransaction(
				session -> {
					Forest forest = session.find( Forest.class, f.getId() );
					assertThat( forest ).isNotNull();
					Country country = forest.getCountry();
					assertThat( country ).isNotNull();
					assertThat( country.getName() ).isEqualTo( forest.getCountry().getName() );
					Set<Country> near = forest.getNear();
					assertThat( near.size() ).isEqualTo( 3 );
					for ( Country n : near ) {
						String name = n.getName();
						assertThat( (name.equals( "Mordor" ) || name.equals( "Gondor" ) || name.equals( "Eriador" )) )
								.isTrue();
					}
				}
		);
	}

	@Test
	public void testCompositeType(SessionFactoryScope scope) {
		Ransom ransom = new Ransom();
		scope.inTransaction(
				session -> {
					ransom.setKidnapperName( "Se7en" );
					ransom.setDate( new Date() );
					MonetaryAmount amount = new MonetaryAmount(
							new BigDecimal( 100000 ),
							Currency.getInstance( "EUR" )
					);
					ransom.setAmount( amount );
					session.persist( ransom );
				}
		);

		scope.inTransaction(
				session -> {
					Ransom r = session.find( Ransom.class, ransom.getId() );
					assertThat( r ).isNotNull();
					assertThat( r.getAmount() ).isNotNull();
					assertThat( r.getAmount().getAmount() ).isEqualByComparingTo( new BigDecimal( 100000 ) );
					assertThat( r.getAmount().getCurrency() ).isEqualTo( Currency.getInstance( "EUR" ) );
				}
		);
	}

	@Test
	public void testFormula(SessionFactoryScope scope) {
		Flight flight = new Flight();
		scope.inTransaction(
				session -> {
					flight.setId( 747L );
					flight.setMaxAltitude( 10000 );
					session.persist( flight );
				}
		);

		scope.inTransaction(
				session -> {
					Flight airFrance = session.find( Flight.class, flight.getId() );
					assertThat( airFrance ).isNotNull();
					assertThat( airFrance.getMaxAltitudeInMilimeter() ).isEqualTo( 10000000 );
					session.remove( airFrance );
				}
		);
	}

	@Test
	public void testTypeDefNameAndDefaultForTypeAttributes(SessionFactoryScope scope) {
		ContactDetails details = new ContactDetails();
		details.setLocalPhoneNumber( new PhoneNumber( "999999" ) );
		details.setOverseasPhoneNumber(
				new OverseasPhoneNumber( "041", "111111" ) );

		scope.inTransaction(
				session -> session.persist( details )
		);

		scope.inTransaction(
				session -> {
					ContactDetails contactDetails =
							session.find( ContactDetails.class, details.getId() );
					assertThat( contactDetails ).isNotNull();
					assertThat( contactDetails.getLocalPhoneNumber().getNumber() ).isEqualTo( "999999" );
					assertThat( contactDetails.getOverseasPhoneNumber().getNumber() ).isEqualTo( "041111111" );
					session.remove( contactDetails );
				}
		);
	}

}
