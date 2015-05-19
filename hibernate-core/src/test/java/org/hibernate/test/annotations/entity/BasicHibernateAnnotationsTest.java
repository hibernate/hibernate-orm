/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.entity;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import org.hibernate.AnnotationException;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.TeradataDialect;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class BasicHibernateAnnotationsTest extends BaseCoreFunctionalTestCase {
	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}
	@Test
	@RequiresDialectFeature( DialectChecks.SupportsExpectedLobUsagePattern.class )
	public void testEntity() throws Exception {
		Forest forest = new Forest();
		forest.setName( "Fontainebleau" );
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( forest );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		forest = (Forest) s.get( Forest.class, forest.getId() );
		assertNotNull( forest );
		forest.setName( "Fontainebleau" );
		//should not execute SQL update
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		forest = (Forest) s.get( Forest.class, forest.getId() );
		assertNotNull( forest );
		forest.setLength( 23 );
		//should execute dynamic SQL update
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		s.delete( s.get( Forest.class, forest.getId() ) );
		tx.commit();
		s.close();
	}

	@Test
	@RequiresDialectFeature( DialectChecks.SupportsExpectedLobUsagePattern.class )
	@SkipForDialect(value = TeradataDialect.class , comment = "One transaction hangs the other")
	public void testVersioning() throws Exception {
		Forest forest = new Forest();
		forest.setName( "Fontainebleau" );
		forest.setLength( 33 );
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( forest );
		tx.commit();
		s.close();

		Session parallelSession = openSession();
		Transaction parallelTx = parallelSession.beginTransaction();
		s = openSession();
		tx = s.beginTransaction();

		forest = (Forest) parallelSession.get( Forest.class, forest.getId() );
		Forest reloadedForest = (Forest) s.get( Forest.class, forest.getId() );
		reloadedForest.setLength( 11 );
		assertNotSame( forest, reloadedForest );
		tx.commit();
		s.close();

		forest.setLength( 22 );
		try {
			parallelTx.commit();
			fail( "All optimistic locking should have make it fail" );
		}
		catch (HibernateException e) {
			if ( parallelTx != null ) parallelTx.rollback();
		}
		finally {
			parallelSession.close();
		}

		s = openSession();
		tx = s.beginTransaction();
		s.delete( s.get( Forest.class, forest.getId() ) );
		tx.commit();
		s.close();
	}

	@Test
	@RequiresDialectFeature( DialectChecks.SupportsExpectedLobUsagePattern.class )
	public void testPolymorphism() throws Exception {
		Forest forest = new Forest();
		forest.setName( "Fontainebleau" );
		forest.setLength( 33 );
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( forest );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		Query query = s.createQuery( "from java.lang.Object" );
		assertEquals( 0, query.list().size() );
		query = s.createQuery( "from Forest" );
		assertTrue( 0 < query.list().size() );
		tx.commit();
		s.close();
	}

	@Test
	@RequiresDialectFeature( DialectChecks.SupportsExpectedLobUsagePattern.class )
	public void testType() throws Exception {
		Forest f = new Forest();
		f.setName( "Broceliande" );
		String description = "C'est une enorme foret enchantee ou vivais Merlin et toute la clique";
		f.setLongDescription( description );
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( f );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		f = (Forest) s.get( Forest.class, f.getId() );
		assertNotNull( f );
		assertEquals( description, f.getLongDescription() );
		s.delete( f );
		tx.commit();
		s.close();

	}

	/*
	 * Test import of TypeDefs from MappedSuperclass and 
	 * Embedded classes.
	 * The classes 'Name' and 'FormalLastName' both embed the same 
	 * component 'LastName'. This is to verify that processing the 
	 * typedef defined in the component TWICE does not create any 
	 * issues.  
	 */
	@Test
	public void testImportTypeDefinitions() throws Exception {
		LastName lastName = new LastName();
		lastName.setName("reddy");
				
		Name name = new Name();
		name.setFirstName("SHARATH");
		name.setLastName(lastName);
		
		FormalLastName formalName = new FormalLastName();
		formalName.setLastName(lastName);
		formalName.setDesignation("Mr");
				
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist(name);
		s.persist(formalName);
		tx.commit();
		s.close();
		 
		s = openSession();
		tx = s.beginTransaction();
		name = (Name) s.get( Name.class, name.getId() );
		assertNotNull( name );
		assertEquals( "sharath", name.getFirstName() );
		assertEquals( "REDDY", name.getLastName().getName() );
		
		formalName = (FormalLastName) s.get(FormalLastName.class, formalName.getId());
		assertEquals( "REDDY", formalName.getLastName().getName() );
		
		s.delete(name);
		s.delete(formalName);
		tx.commit();
		s.close();
	}

	@Test
	public void testNonLazy() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Forest f = new Forest();
		Tree t = new Tree();
		t.setName( "Basic one" );
		s.persist( f );
		s.persist( t );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		f = (Forest) s.load( Forest.class, f.getId() );
		t = (Tree) s.load( Tree.class, t.getId() );
		assertFalse( "Default should be lazy", Hibernate.isInitialized( f ) );
		assertTrue( "Tree is not lazy", Hibernate.isInitialized( t ) );
		tx.commit();
		s.close();
	}

	@Test
	public void testCache() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		ZipCode zc = new ZipCode();
		zc.code = "92400";
		s.persist( zc );
		tx.commit();
		s.close();
		sessionFactory().getStatistics().clear();
		sessionFactory().getStatistics().setStatisticsEnabled( true );
		sessionFactory().getCache().evictEntityRegion( ZipCode.class );
		s = openSession();
		tx = s.beginTransaction();
		s.get( ZipCode.class, zc.code );
		assertEquals( 1, sessionFactory().getStatistics().getSecondLevelCachePutCount() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		s.get( ZipCode.class, zc.code );
		assertEquals( 1, sessionFactory().getStatistics().getSecondLevelCacheHitCount() );
		tx.commit();
		s.close();
	}

	@Test
	public void testFilterOnCollection() {
		
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		
		Topic topic = new Topic();
		Narrative n1 = new Narrative();
		n1.setState("published");
		topic.addNarrative(n1);
		
		Narrative n2 = new Narrative();
		n2.setState("draft");
		topic.addNarrative(n2);
		
		s.persist(topic);
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		topic = (Topic) s.load( Topic.class, topic.getId() );
		
		s.enableFilter("byState").setParameter("state", "published");
		topic = (Topic) s.load( Topic.class, topic.getId() );
		assertNotNull(topic); 
		assertTrue(topic.getNarratives().size() == 1); 
		assertEquals("published", topic.getNarratives().iterator().next().getState());
		tx.commit();
		s.close();
		
		s = openSession();
		tx = s.beginTransaction();
		s.createQuery( "delete from " + Narrative.class.getSimpleName() ).executeUpdate();
		tx.commit();
		s.close();
	} 

	@Test
	public void testCascadedDeleteOfChildEntitiesBug2() {
		// Relationship is one SoccerTeam to many Players.
		// Create a SoccerTeam (parent) and three Players (child).
		// Verify that the count of Players is correct.
		// Clear the SoccerTeam reference Players.
		// The orphanRemoval should remove the Players automatically.
		// @OneToMany(mappedBy="name", orphanRemoval=true)
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		SoccerTeam team = new SoccerTeam();
		int teamid = team.getId();
		Player player1 = new Player();
		player1.setName("Shalrie Joseph");
		team.addPlayer(player1);

		Player player2 = new Player();
		player2.setName("Taylor Twellman");
		team.addPlayer(player2);

		Player player3 = new Player();
		player3.setName("Steve Ralston");
		team.addPlayer(player3);
		s.persist(team);
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		team = (SoccerTeam)s.merge(team);
		int count = ( (Long) s.createQuery( "select count(*) from Player" ).iterate().next() ).intValue();
		assertEquals("expected count of 3 but got = " + count, count, 3);

		// clear references to players, this should orphan the players which should
		// in turn trigger orphanRemoval logic.
		team.getPlayers().clear();
//		count = ( (Long) s.createQuery( "select count(*) from Player" ).iterate().next() ).intValue();
//		assertEquals("expected count of 0 but got = " + count, count, 0);
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		count = ( (Long) s.createQuery( "select count(*) from Player" ).iterate().next() ).intValue();
		assertEquals("expected count of 0 but got = " + count, count, 0);
		tx.commit();
		s.close();
	}

	@Test
	public void testCascadedDeleteOfChildOneToOne() {
		// create two single player teams (for one versus one match of soccer)
		// and associate teams with players via the special OneVOne methods.
		// Clear the Team reference to players, which should orphan the teams.
		// Orphaning the team should delete the team. 

		Session s = openSession();
		Transaction tx = s.beginTransaction();

		SoccerTeam team = new SoccerTeam();
		team.setName("Shalrie's team");
		Player player1 = new Player();
		player1.setName("Shalrie Joseph");
		team.setOneVonePlayer(player1);
		player1.setOneVoneTeam(team);

		s.persist(team);

		SoccerTeam team2 = new SoccerTeam();
		team2.setName("Taylor's team");
		Player player2 = new Player();
		player2.setName("Taylor Twellman");
		team2.setOneVonePlayer(player2);
		player2.setOneVoneTeam(team2);
		s.persist(team2);
		tx.commit();

		tx = s.beginTransaction();
		s.clear();
		team2 = (SoccerTeam)s.load(team2.getClass(), team2.getId());
		team = (SoccerTeam)s.load(team.getClass(), team.getId());
		int count = ( (Long) s.createQuery( "select count(*) from Player" ).iterate().next() ).intValue();
		assertEquals("expected count of 2 but got = " + count, count, 2);

		// clear references to players, this should orphan the players which should
		// in turn trigger orphanRemoval logic.
		team.setOneVonePlayer(null);
		team2.setOneVonePlayer(null);
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		count = ( (Long) s.createQuery( "select count(*) from Player" ).iterate().next() ).intValue();
		assertEquals("expected count of 0 but got = " + count, count, 0);
		tx.commit();
		s.close();
	}

	@Test
	public void testFilter() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.createQuery( "delete Forest" ).executeUpdate();
		Forest f1 = new Forest();
		f1.setLength( 2 );
		s.persist( f1 );
		Forest f2 = new Forest();
		f2.setLength( 20 );
		s.persist( f2 );
		Forest f3 = new Forest();
		f3.setLength( 200 );
		s.persist( f3 );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		s.enableFilter( "betweenLength" ).setParameter( "minLength", 5 ).setParameter( "maxLength", 50 );
		long count = ( (Long) s.createQuery( "select count(*) from Forest" ).iterate().next() ).intValue();
		assertEquals( 1, count );
		s.disableFilter( "betweenLength" );
		s.enableFilter( "minLength" ).setParameter( "minLength", 5 );
		count = ( (Long) s.createQuery( "select count(*) from Forest" ).iterate().next() ).longValue();
		assertEquals( 2l, count );
		s.disableFilter( "minLength" );
		tx.rollback();
		s.close();
	}
	  
	/**
	 * Tests the functionality of inheriting @Filter and @FilterDef annotations
	 * defined on a parent MappedSuperclass(s)
	 */
	@Test
	public void testInheritFiltersFromMappedSuperclass() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.createQuery( "delete Drill" ).executeUpdate();
		Drill d1 = new PowerDrill();
		d1.setName("HomeDrill1");
		d1.setCategory("HomeImprovment");
		s.persist( d1 );
		Drill d2 = new PowerDrill();
		d2.setName("HomeDrill2");
		d2.setCategory("HomeImprovement");
		s.persist(d2);
		Drill d3 = new PowerDrill();
		d3.setName("HighPowerDrill");
		d3.setCategory("Industrial");
		s.persist( d3 );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		 
		//We test every filter with 2 queries, the first on the base class of the 
		//inheritance hierarchy (Drill), and the second on a subclass (PowerDrill)
		s.enableFilter( "byName" ).setParameter( "name", "HomeDrill1");
		long count = ( (Long) s.createQuery( "select count(*) from Drill" ).iterate().next() ).intValue();
		assertEquals( 1, count );
		count = ( (Long) s.createQuery( "select count(*) from PowerDrill" ).iterate().next() ).intValue();
		assertEquals( 1, count );
		s.disableFilter( "byName" );
		
		s.enableFilter( "byCategory" ).setParameter( "category", "Industrial" );
		count = ( (Long) s.createQuery( "select count(*) from Drill" ).iterate().next() ).longValue();
		assertEquals( 1, count );
		count = ( (Long) s.createQuery( "select count(*) from PowerDrill" ).iterate().next() ).longValue();
		assertEquals( 1, count );
		s.disableFilter( "byCategory" );
		
		tx.rollback();
		s.close();
	}
	
	@Test
	@RequiresDialectFeature( DialectChecks.SupportsExpectedLobUsagePattern.class )
	public void testParameterizedType() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Forest f = new Forest();
		f.setSmallText( "ThisIsASmallText" );
		f.setBigText( "ThisIsABigText" );
		s.persist( f );
		tx.commit();
		s.close();
		s = openSession();
		tx = s.beginTransaction();
		Forest f2 = (Forest) s.get( Forest.class, f.getId() );
		assertEquals( f.getSmallText().toLowerCase(Locale.ROOT), f2.getSmallText() );
		assertEquals( f.getBigText().toUpperCase(Locale.ROOT), f2.getBigText() );
		tx.commit();
		s.close();
	}

	@Test
	@RequiresDialectFeature( DialectChecks.SupportsExpectedLobUsagePattern.class )
	public void testSerialized() throws Exception {
		Forest forest = new Forest();
		forest.setName( "Shire" );
		Country country = new Country();
		country.setName( "Middle Earth" );
		forest.setCountry( country );
		Set<Country> near = new HashSet<Country>();
		country = new Country();
		country.setName("Mordor");
		near.add(country);
		country = new Country();
		country.setName("Gondor");
		near.add(country);
		country = new Country();
		country.setName("Eriador");
		near.add(country);
		forest.setNear(near);
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( forest );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		forest = (Forest) s.get( Forest.class, forest.getId() );
		assertNotNull( forest );
		country = forest.getCountry();
		assertNotNull( country );
		assertEquals( country.getName(), forest.getCountry().getName() );
		near = forest.getNear();
		assertTrue("correct number of nearby countries", near.size() == 3);
		for (Iterator iter = near.iterator(); iter.hasNext();) {
			country = (Country)iter.next();
			String name = country.getName();
			assertTrue("found expected nearby country " + name,
				(name.equals("Mordor") || name.equals("Gondor") || name.equals("Eriador")));
		}
		tx.commit();
		s.close();
	}

	@Test
	public void testCompositeType() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Ransom r = new Ransom();
		r.setKidnapperName( "Se7en" );
		r.setDate( new Date() );
		MonetaryAmount amount = new MonetaryAmount(
				new BigDecimal( 100000 ),
				Currency.getInstance( "EUR" )
		);
		r.setAmount( amount );
		s.persist( r );
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		r = (Ransom) s.get( Ransom.class, r.getId() );
		assertNotNull( r );
		assertNotNull( r.getAmount() );
		assertTrue( 0 == new BigDecimal( 100000 ).compareTo( r.getAmount().getAmount() ) );
		assertEquals( Currency.getInstance( "EUR" ), r.getAmount().getCurrency() );
		tx.commit();
		s.close();
	}

	@Test
	public void testFormula() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		org.hibernate.test.annotations.entity.Flight airFrance = new Flight();
		airFrance.setId( new Long( 747 ) );
		airFrance.setMaxAltitude( 10000 );
		s.persist( airFrance );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		airFrance = (Flight) s.get( Flight.class, airFrance.getId() );
		assertNotNull( airFrance );
		assertEquals( 10000000, airFrance.getMaxAltitudeInMilimeter() );
		s.delete( airFrance );
		tx.commit();
		s.close();
	}
		
	@Test
	public void testTypeDefNameAndDefaultForTypeAttributes() {
		ContactDetails contactDetails = new ContactDetails();
		contactDetails.setLocalPhoneNumber(new PhoneNumber("999999"));
		contactDetails.setOverseasPhoneNumber(
				new OverseasPhoneNumber("041", "111111"));
		
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist(contactDetails);
		tx.commit();
		s.close();
		
		s = openSession();
		tx = s.beginTransaction();
		contactDetails = 
			(ContactDetails) s.get( ContactDetails.class, contactDetails.getId() );
		assertNotNull( contactDetails );
		assertEquals( "999999", contactDetails.getLocalPhoneNumber().getNumber() );
		assertEquals( "041111111", contactDetails.getOverseasPhoneNumber().getNumber() );
		s.delete(contactDetails);
		tx.commit();
		s.close();
	
	}
	
	@Test
	public void testTypeDefWithoutNameAndDefaultForTypeAttributes() {
		SessionFactory sf=null;
		try {
			Configuration config = new Configuration();
			config.addAnnotatedClass(LocalContactDetails.class);
			sf = config.buildSessionFactory( ServiceRegistryBuilder.buildServiceRegistry( config.getProperties() ) );
			fail("Did not throw expected exception");
		}
		catch( AnnotationException ex ) {
			assertEquals(
					"Either name or defaultForType (or both) attribute should be set in TypeDef having typeClass org.hibernate.test.annotations.entity.PhoneNumberType", 
					ex.getMessage());
		} finally {
			if( sf != null){
				sf.close();
			}
		}
	}

	
	/**
	 * A custom type is used in the base class, but defined in the derived class. 
	 * This would have caused an exception, because the base class is processed 
	 * BEFORE the derived class, and the custom type is not yet defined. However, 
	 * it works now because we are setting the typeName for SimpleValue in the second 
	 * pass. 
	 * 
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSetSimpleValueTypeNameInSecondPass() throws Exception {
		Peugot derived = new Peugot();
		derived.setName("sharath");
		
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist(derived);
		tx.commit();
		s.close();
		
		s = openSession();
		tx = s.beginTransaction();
		derived = (Peugot) s.get( Peugot.class, derived.getId() );
		assertNotNull( derived );
		assertEquals( "SHARATH", derived.getName() );
		s.delete(derived);
		tx.commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				Forest.class,
				Tree.class,
				Ransom.class,
				ZipCode.class,
				Flight.class,
				Name.class,
				FormalLastName.class,
				Car.class,
				Peugot.class,
				ContactDetails.class,
				Topic.class,
				Narrative.class,
				Drill.class,
				PowerDrill.class,
				SoccerTeam.class,
				Player.class
		};
	}

	@Override
	protected String[] getAnnotatedPackages() {
		return new String[]{
				"org.hibernate.test.annotations.entity"
		};
	}


}
