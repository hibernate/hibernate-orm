/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.Transaction;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;
import org.hibernate.orm.test.bootstrap.binding.annotations.embedded.FloatLeg.RateIndex;
import org.hibernate.orm.test.bootstrap.binding.annotations.embedded.Leg.Frequency;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.hibernate.orm.test.util.SchemaUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Person.class,
				WealthyPerson.class,
				RegionalArticle.class,
				AddressType.class,
				VanillaSwap.class,
				SpreadDeal.class,
				Book.class,
				InternetProvider.class,
				CorpType.class,
				Nationality.class,
				Manager.class,
				FavoriteThings.class
		}
)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.IMPLICIT_NAMING_STRATEGY, value = "jpa")
})
public class EmbeddedTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( Person person : session.createQuery( "from Person", Person.class ).getResultList() ) {
				session.delete( person );
			}
			session.createQuery( "delete from InternetProvider" ).executeUpdate();
			session.createQuery( "delete from Manager" ).executeUpdate();
			session.createQuery( "delete from Book" ).executeUpdate();
			session.createQuery( "delete from FavoriteThings" ).executeUpdate();
		} );
	}

	@Test
	public void testSimple(SessionFactoryScope scope) {
		Person person = new Person();
		Address a = new Address();
		Country c = new Country();
		Country bornCountry = new Country();
		c.setIso2( "DM" );
		c.setName( "Matt Damon Land" );
		bornCountry.setIso2( "US" );
		bornCountry.setName( "United States of America" );

		a.address1 = "colorado street";
		a.city = "Springfield";
		a.country = c;
		person.address = a;
		person.bornIn = bornCountry;
		person.name = "Homer";

		scope.inTransaction( session -> session.persist( person ) );

		scope.inTransaction( session -> {
			Person p = session.get( Person.class, person.id );
			assertNotNull( p );
			assertNotNull( p.address );
			assertEquals( "Springfield", p.address.city );
			assertEquals( (Integer) 1, p.address.formula );

			assertNotNull( p.address.country );
			assertEquals( "DM", p.address.country.getIso2() );
			assertNotNull( p.bornIn );
			assertEquals( "US", p.bornIn.getIso2() );
			assertEquals( (Integer) 2, p.addressBis.formula );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8172")
	public void testQueryWithEmbeddedIsNull(SessionFactoryScope scope) {
		Person person = new Person();
		Address a = new Address();
		Country c = new Country();
		Country bornCountry = new Country();
		c.setIso2( "DM" );
		c.setName( "Matt Damon Land" );
		assertNull( bornCountry.getIso2() );
		assertNull( bornCountry.getName() );

		a.address1 = "colorado street";
		a.city = "Springfield";
		a.country = c;
		person.address = a;
		person.bornIn = bornCountry;
		person.name = "Homer";
		scope.inTransaction( session -> session.persist( person ) );

		scope.inTransaction( session -> {
			Person p = (Person) session.createQuery( "from Person p where p.bornIn is null" ).uniqueResult();
			assertNotNull( p );
			assertNotNull( p.address );
			assertEquals( "Springfield", p.address.city );
			assertNotNull( p.address.country );
			assertEquals( "DM", p.address.country.getIso2() );
			assertNull( p.bornIn );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8172")
	public void testQueryWithEmbeddedParameterAllNull(SessionFactoryScope scope) {
		Person person = new Person();
		Address a = new Address();
		Country c = new Country();
		Country bornCountry = new Country();
		c.setIso2( "DM" );
		c.setName( "Matt Damon Land" );
		assertNull( bornCountry.getIso2() );
		assertNull( bornCountry.getName() );

		a.address1 = "colorado street";
		a.city = "Springfield";
		a.country = c;
		person.address = a;
		person.bornIn = bornCountry;
		person.name = "Homer";

		scope.inTransaction( session -> session.persist( person ) );

		scope.inTransaction( session -> {
			Person p = (Person) session.createQuery( "from Person p where p.bornIn is not distinct from :b" )
					.setParameter( "b", person.bornIn )
					.uniqueResult();
			assertNotNull( p );
			assertNotNull( p.address );
			assertEquals( "Springfield", p.address.city );
			assertNotNull( p.address.country );
			assertEquals( "DM", p.address.country.getIso2() );
			assertNull( p.bornIn );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8172")
	public void testQueryWithEmbeddedParameterOneNull(SessionFactoryScope scope) {
		Person person = new Person();
		Address a = new Address();
		Country c = new Country();
		Country bornCountry = new Country();
		c.setIso2( "DM" );
		c.setName( "Matt Damon Land" );
		bornCountry.setIso2( "US" );
		assertNull( bornCountry.getName() );

		a.address1 = "colorado street";
		a.city = "Springfield";
		a.country = c;
		person.address = a;
		person.bornIn = bornCountry;
		person.name = "Homer";

		scope.inTransaction( session -> session.persist( person ) );

		scope.inTransaction( session -> {
			Person p = (Person) session.createQuery( "from Person p where p.bornIn is not distinct from :b" )
					.setParameter( "b", person.bornIn )
					.uniqueResult();
			assertNotNull( p );
			assertNotNull( p.address );
			assertEquals( "Springfield", p.address.city );
			assertNotNull( p.address.country );
			assertEquals( "DM", p.address.country.getIso2() );
			assertEquals( "US", p.bornIn.getIso2() );
			assertNull( p.bornIn.getName() );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-8172")
	public void testQueryWithEmbeddedWithNullUsingSubAttributes(SessionFactoryScope scope) {
		Person person = new Person();
		Address a = new Address();
		Country c = new Country();
		Country bornCountry = new Country();
		c.setIso2( "DM" );
		c.setName( "Matt Damon Land" );
		bornCountry.setIso2( "US" );
		assertNull( bornCountry.getName() );

		a.address1 = "colorado street";
		a.city = "Springfield";
		a.country = c;
		person.address = a;
		person.bornIn = bornCountry;
		person.name = "Homer";

		scope.inTransaction( session -> session.persist( person ) );

		scope.inTransaction( session -> {
			Query query = session.createQuery( "from Person p " +
																  "where ( p.bornIn.iso2 is null or p.bornIn.iso2 = :i ) and " +
																  "( p.bornIn.name is null or p.bornIn.name = :n )"
			);
			query.setParameter( "i", person.bornIn.getIso2() );
			query.setParameter( "n", person.bornIn.getName() );
			Person p = (Person) query.uniqueResult();
			assertNotNull( p );
			assertNotNull( p.address );
			assertEquals( "Springfield", p.address.city );
			assertNotNull( p.address.country );
			assertEquals( "DM", p.address.country.getIso2() );
			assertEquals( "US", p.bornIn.getIso2() );
			assertNull( p.bornIn.getName() );
		} );
	}

	@Test
	public void testCompositeId(SessionFactoryScope scope) {

		RegionalArticle reg = new RegionalArticle();

		scope.inTransaction(
				session -> {
					RegionalArticlePk pk = new RegionalArticlePk();
					pk.iso2 = "FR";
					pk.localUniqueKey = "1234567890123";
					reg.setName( "Je ne veux pes rester sage - Dolly" );
					reg.setPk( pk );
					session.persist( reg );
				}
		);

		scope.inTransaction(
				session -> {
					RegionalArticle reg1 = session.get( RegionalArticle.class, reg.getPk() );
					assertNotNull( reg1 );
					assertNotNull( reg1.getPk() );
					assertEquals( "Je ne veux pes rester sage - Dolly", reg1.getName() );
					assertEquals( "FR", reg1.getPk().iso2 );
				}
		);
	}

	@Test
	public void testManyToOneInsideComponent(SessionFactoryScope scope) {
		Address add = new Address();
		AddressType type = new AddressType();

		scope.inTransaction(
				session -> {
					Person p = new Person();
					Country bornIn = new Country();
					bornIn.setIso2( "FR" );
					bornIn.setName( "France" );
					p.bornIn = bornIn;
					p.name = "Emmanuel";

					type.setName( "Primary Home" );
					session.persist( type );
					Country currentCountry = new Country();
					currentCountry.setIso2( "US" );
					currentCountry.setName( "USA" );

					add.address1 = "4 square street";
					add.city = "San diego";
					add.country = currentCountry;
					add.type = type;
					p.address = add;
					session.persist( p );
				}
		);

		scope.inTransaction(
				session -> {
					Query q = session.createQuery( "select p from Person p where p.address.city = :city" );
					q.setParameter( "city", add.city );
					List<Person> result = q.list();
					Person samePerson = result.get( 0 );
					assertNotNull( samePerson.address.type );
					assertEquals( type.getName(), samePerson.address.type.getName() );
				}
		);
	}

	@Test
	public void testEmbeddedSuperclass(SessionFactoryScope scope) {
		VanillaSwap swap = new VanillaSwap();
		scope.inTransaction(
				session -> {
					swap.setInstrumentId( "US345421" );
					swap.setCurrency( VanillaSwap.Currency.EUR );
					FixedLeg fixed = new FixedLeg();
					fixed.setPaymentFrequency( Frequency.SEMIANNUALLY );
					fixed.setRate( 5.6 );
					FloatLeg floating = new FloatLeg();
					floating.setPaymentFrequency( Frequency.QUARTERLY );
					floating.setRateIndex( RateIndex.LIBOR );
					floating.setRateSpread( 1.1 );
					swap.setFixedLeg( fixed );
					swap.setFloatLeg( floating );
					session.persist( swap );
				}
		);

		scope.inTransaction(
				session -> {
					VanillaSwap vanillaSwap = session.get( VanillaSwap.class, swap.getInstrumentId() );
					// All fields must be filled with non-default values
					FixedLeg fixed = vanillaSwap.getFixedLeg();
					assertNotNull( "Fixed leg retrieved as null", fixed );
					FloatLeg floating = vanillaSwap.getFloatLeg();
					assertNotNull( "Floating leg retrieved as null", floating );
					assertEquals( Frequency.SEMIANNUALLY, fixed.getPaymentFrequency() );
					assertEquals( Frequency.QUARTERLY, floating.getPaymentFrequency() );
					session.delete( vanillaSwap );
				}
		);
	}

	@Test
	public void testDottedProperty(SessionFactoryScope scope) {
		SpreadDeal deal = scope.fromTransaction(
				session -> {
					// Create short swap
					Swap shortSwap = new Swap();
					shortSwap.setTenor( 2 );

					FixedLeg shortFixed = new FixedLeg();
					shortFixed.setPaymentFrequency( Frequency.SEMIANNUALLY );
					shortFixed.setRate( 5.6 );

					FloatLeg shortFloating = new FloatLeg();
					shortFloating.setPaymentFrequency( Frequency.QUARTERLY );
					shortFloating.setRateIndex( RateIndex.LIBOR );
					shortFloating.setRateSpread( 1.1 );
					shortSwap.setFixedLeg( shortFixed );
					shortSwap.setFloatLeg( shortFloating );
					// Create medium swap
					Swap swap = new Swap();
					swap.setTenor( 7 );

					FixedLeg fixed = new FixedLeg();
					fixed.setPaymentFrequency( Frequency.MONTHLY );
					fixed.setRate( 7.6 );

					FloatLeg floating = new FloatLeg();
					floating.setPaymentFrequency( Frequency.MONTHLY );
					floating.setRateIndex( RateIndex.TIBOR );
					floating.setRateSpread( 0.8 );
					swap.setFixedLeg( fixed );
					swap.setFloatLeg( floating );
					// Create long swap
					Swap longSwap = new Swap();
					longSwap.setTenor( 7 );

					FixedLeg longFixed = new FixedLeg();
					longFixed.setPaymentFrequency( Frequency.MONTHLY );
					longFixed.setRate( 7.6 );

					FloatLeg longFloating = new FloatLeg();
					longFloating.setPaymentFrequency( Frequency.MONTHLY );
					longFloating.setRateIndex( RateIndex.TIBOR );
					longFloating.setRateSpread( 0.8 );
					longSwap.setFixedLeg( longFixed );
					longSwap.setFloatLeg( longFloating );
					// Compose a curve spread deal
					SpreadDeal spreadDeal = new SpreadDeal();
					spreadDeal.setId( "FX45632" );
					spreadDeal.setNotional( 450000.0 );
					spreadDeal.setShortSwap( shortSwap );
					spreadDeal.setSwap( swap );
					spreadDeal.setLongSwap( longSwap );
					session.persist( spreadDeal );
					return spreadDeal;
				}
		);

		scope.inTransaction(
				session -> {
					SpreadDeal spreadDeal = session.get( SpreadDeal.class, deal.getId() );
					// All fields must be filled with non-default values
					assertNotNull( "Short swap is null.", spreadDeal.getShortSwap() );
					assertNotNull( "Swap is null.", spreadDeal.getSwap() );
					assertNotNull( "Long swap is null.", spreadDeal.getLongSwap() );
					assertEquals( 2, spreadDeal.getShortSwap().getTenor() );
					assertEquals( 7, spreadDeal.getSwap().getTenor() );
					assertEquals( 7, spreadDeal.getLongSwap().getTenor() );
					assertNotNull( "Short fixed leg is null.", spreadDeal.getShortSwap().getFixedLeg() );
					assertNotNull( "Short floating leg is null.", spreadDeal.getShortSwap().getFloatLeg() );
					assertNotNull( "Fixed leg is null.", spreadDeal.getSwap().getFixedLeg() );
					assertNotNull( "Floating leg is null.", spreadDeal.getSwap().getFloatLeg() );
					assertNotNull( "Long fixed leg is null.", spreadDeal.getLongSwap().getFixedLeg() );
					assertNotNull( "Long floating leg is null.", spreadDeal.getLongSwap().getFloatLeg() );
					assertEquals(
							Frequency.SEMIANNUALLY,
							spreadDeal.getShortSwap().getFixedLeg().getPaymentFrequency()
					);
					assertEquals( Frequency.QUARTERLY, spreadDeal.getShortSwap().getFloatLeg().getPaymentFrequency() );
					assertEquals( Frequency.MONTHLY, spreadDeal.getSwap().getFixedLeg().getPaymentFrequency() );
					assertEquals( Frequency.MONTHLY, spreadDeal.getSwap().getFloatLeg().getPaymentFrequency() );
					assertEquals( Frequency.MONTHLY, spreadDeal.getLongSwap().getFixedLeg().getPaymentFrequency() );
					assertEquals( Frequency.MONTHLY, spreadDeal.getLongSwap().getFloatLeg().getPaymentFrequency() );
					assertEquals( 5.6, spreadDeal.getShortSwap().getFixedLeg().getRate(), 0.01 );
					assertEquals( 7.6, spreadDeal.getSwap().getFixedLeg().getRate(), 0.01 );
					assertEquals( 7.6, spreadDeal.getLongSwap().getFixedLeg().getRate(), 0.01 );
					assertEquals( RateIndex.LIBOR, spreadDeal.getShortSwap().getFloatLeg().getRateIndex() );
					assertEquals( RateIndex.TIBOR, spreadDeal.getSwap().getFloatLeg().getRateIndex() );
					assertEquals( RateIndex.TIBOR, spreadDeal.getLongSwap().getFloatLeg().getRateIndex() );
					assertEquals( 1.1, spreadDeal.getShortSwap().getFloatLeg().getRateSpread(), 0.01 );
					assertEquals( 0.8, spreadDeal.getSwap().getFloatLeg().getRateSpread(), 0.01 );
					assertEquals( 0.8, spreadDeal.getLongSwap().getFloatLeg().getRateSpread(), 0.01 );
					session.delete( spreadDeal );
				}
		);
	}

	@Test
	public void testEmbeddedInSecondaryTable(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.getTransaction().begin();
					try {
						Book book = new Book();
						book.setIsbn( "1234" );
						book.setName( "HiA Second Edition" );
						Summary summary = new Summary();
						summary.setText( "This is a HiA SE summary" );
						summary.setSize( summary.getText().length() );
						book.setSummary( summary );
						session.persist( book );
						session.getTransaction().commit();

						session.clear();

						Transaction tx = session.beginTransaction();
						Book loadedBook = session.get( Book.class, book.getIsbn() );
						assertNotNull( loadedBook.getSummary() );
						assertEquals( book.getSummary().getText(), loadedBook.getSummary().getText() );
						session.delete( loadedBook );
						tx.commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	public void testParent(SessionFactoryScope scope) {
		Book book = new Book();
		scope.inTransaction(
				session -> {
					book.setIsbn( "1234" );
					book.setName( "HiA Second Edition" );
					Summary summary = new Summary();
					summary.setText( "This is a HiA SE summary" );
					summary.setSize( summary.getText().length() );
					book.setSummary( summary );
					session.persist( book );
				}
		);

		scope.inTransaction(
				session -> {
					Book loadedBook = session.get( Book.class, book.getIsbn() );
					assertNotNull( loadedBook.getSummary() );
					assertEquals( loadedBook, loadedBook.getSummary().getSummarizedBook() );
					session.delete( loadedBook );
				}
		);
	}

	@Test
	public void testEmbeddedAndMultipleManyToOne(SessionFactoryScope scope) {
		CorpType type = new CorpType();
		InternetProvider provider = new InternetProvider();
		Nationality nat = new Nationality();

		scope.inTransaction(
				session -> {
					type.setType( "National" );
					session.persist( type );
					nat.setName( "Canadian" );
					session.persist( nat );
					provider.setBrandName( "Fido" );
					LegalStructure structure = new LegalStructure();
					structure.setCorporationType( type );
					structure.setCountry( "Canada" );
					structure.setName( "Rogers" );
					provider.setOwner( structure );
					structure.setOrigin( nat );
					session.persist( provider );
				}
		);

		scope.inTransaction(
				session -> {
					InternetProvider internetProvider = session.get( InternetProvider.class, provider.getId() );
					assertNotNull( internetProvider.getOwner() );
					assertNotNull( "Many to one not set", internetProvider.getOwner().getCorporationType() );
					assertEquals(
							"Wrong link",
							type.getType(),
							internetProvider.getOwner().getCorporationType().getType()
					);
					assertNotNull( "2nd Many to one not set", internetProvider.getOwner().getOrigin() );
					assertEquals( "Wrong 2nd link", nat.getName(), internetProvider.getOwner().getOrigin().getName() );
					session.delete( internetProvider );
					session.delete( internetProvider.getOwner().getCorporationType() );
					session.delete( internetProvider.getOwner().getOrigin() );
				}
		);
	}

	@Test
	public void testEmbeddedAndOneToMany(SessionFactoryScope scope) {
		InternetProvider provider = new InternetProvider();
		scope.inTransaction(
				session -> {
					provider.setBrandName( "Fido" );
					LegalStructure structure = new LegalStructure();
					structure.setCountry( "Canada" );
					structure.setName( "Rogers" );
					provider.setOwner( structure );
					session.persist( provider );
					Manager manager = new Manager();
					manager.setName( "Bill" );
					manager.setEmployer( provider );
					structure.getTopManagement().add( manager );
					session.persist( manager );
				}
		);

		scope.inTransaction(
				session -> {
					InternetProvider internetProvider = session.get( InternetProvider.class, provider.getId() );
					assertNotNull( internetProvider.getOwner() );
					Set<Manager> topManagement = internetProvider.getOwner().getTopManagement();
					assertNotNull( "OneToMany not set", topManagement );
					assertEquals( "Wrong number of elements", 1, topManagement.size() );
					Manager manager = topManagement.iterator().next();
					assertEquals( "Wrong element", "Bill", manager.getName() );
					session.delete( manager );
					session.delete( internetProvider );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9642")
	public void testEmbeddedAndOneToManyHql(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					InternetProvider provider = new InternetProvider();
					provider.setBrandName( "Fido" );
					LegalStructure structure = new LegalStructure();
					structure.setCountry( "Canada" );
					structure.setName( "Rogers" );
					provider.setOwner( structure );
					session.persist( provider );
					Manager manager = new Manager();
					manager.setName( "Bill" );
					manager.setEmployer( provider );
					structure.getTopManagement().add( manager );
					session.persist( manager );
				}
		);

		scope.inTransaction(
				session -> {
					InternetProvider internetProviderQueried =
							(InternetProvider) session.createQuery( "from InternetProvider" ).uniqueResult();
					assertFalse( Hibernate.isInitialized( internetProviderQueried.getOwner().getTopManagement() ) );

				}
		);

		scope.inTransaction(
				session -> {
					InternetProvider internetProviderQueried =
							(InternetProvider) session.createQuery(
									"from InternetProvider i join fetch i.owner.topManagement" )
									.uniqueResult();
					assertTrue( Hibernate.isInitialized( internetProviderQueried.getOwner().getTopManagement() ) );

				}
		);

		InternetProvider provider = scope.fromTransaction(
				session -> {
					InternetProvider internetProviderQueried =
							(InternetProvider) session.createQuery(
									"from InternetProvider i join fetch i.owner o join fetch o.topManagement" )
									.uniqueResult();
					assertTrue( Hibernate.isInitialized( internetProviderQueried.getOwner().getTopManagement() ) );
					return internetProviderQueried;
				}
		);

		scope.inTransaction(
				session -> {
					InternetProvider internetProvider = session.get( InternetProvider.class, provider.getId() );
					Manager manager = internetProvider.getOwner().getTopManagement().iterator().next();
					session.delete( manager );
					session.delete( internetProvider );
				}
		);
	}


	@Test
	public void testDefaultCollectionTable(SessionFactoryScope scope) {
		//are the tables correct?
		MetadataImplementor metadata = scope.getMetadataImplementor();
		assertTrue( SchemaUtil.isTablePresent( "WealthyPerson_vacationHomes", metadata ) );
		assertTrue( SchemaUtil.isTablePresent( "WelPers_LegacyVacHomes", metadata ) );
		assertTrue( SchemaUtil.isTablePresent( "WelPers_VacHomes", metadata ) );

		//just to make sure, use the mapping
		WealthyPerson p = new WealthyPerson();
		Address a = new Address();
		Address vacation = new Address();
		Country c = new Country();
		Country bornCountry = new Country();
		c.setIso2( "DM" );
		c.setName( "Matt Damon Land" );
		bornCountry.setIso2( "US" );
		bornCountry.setName( "United States of America" );

		a.address1 = "colorado street";
		a.city = "Springfield";
		a.country = c;
		vacation.address1 = "rock street";
		vacation.city = "Plymouth";
		vacation.country = c;
		p.vacationHomes.add( vacation );
		p.address = a;
		p.bornIn = bornCountry;
		p.name = "Homer";
		scope.inTransaction(
				session -> session.persist( p )
		);

		scope.inTransaction(
				session -> {
					WealthyPerson wealthyPerson = session.get( WealthyPerson.class, p.id );
					assertNotNull( wealthyPerson );
					assertNotNull( wealthyPerson.address );
					assertEquals( "Springfield", wealthyPerson.address.city );
					assertNotNull( wealthyPerson.address.country );
					assertEquals( "DM", wealthyPerson.address.country.getIso2() );
					assertNotNull( wealthyPerson.bornIn );
					assertEquals( "US", wealthyPerson.bornIn.getIso2() );
				}
		);
	}

	// make sure we support collection of embeddable objects inside embeddable objects
	@Test
	public void testEmbeddableInsideEmbeddable(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Collection<URLFavorite> urls = new ArrayList<>();
					URLFavorite urlFavorite = new URLFavorite();
					urlFavorite.setUrl( "http://highscalability.com/" );
					urls.add( urlFavorite );

					urlFavorite = new URLFavorite();
					urlFavorite.setUrl( "http://www.jboss.org/" );
					urls.add( urlFavorite );

					urlFavorite = new URLFavorite();
					urlFavorite.setUrl( "http://www.hibernate.org/" );
					urls.add( urlFavorite );

					urlFavorite = new URLFavorite();
					urlFavorite.setUrl( "http://www.jgroups.org/" );
					urls.add( urlFavorite );

					Collection<String> ideas = new ArrayList<>();
					ideas.add( "lionheart" );
					ideas.add( "xforms" );
					ideas.add( "dynamic content" );
					ideas.add( "http" );

					InternetFavorites internetFavorites = new InternetFavorites();
					internetFavorites.setLinks( urls );
					internetFavorites.setIdeas( ideas );

					FavoriteThings favoriteThings = new FavoriteThings();
					favoriteThings.setWeb( internetFavorites );

					Transaction tx = session.beginTransaction();
					try {
						session.persist( favoriteThings );
						tx.commit();

						tx = session.beginTransaction();
						session.flush();

						favoriteThings = session.get( FavoriteThings.class, favoriteThings.getId() );
						assertTrue( "has web", favoriteThings.getWeb() != null );
						assertTrue( "has ideas", favoriteThings.getWeb().getIdeas() != null );
						assertTrue( "has favorite idea 'http'", favoriteThings.getWeb().getIdeas().contains( "http" ) );
						assertTrue(
								"has favorite idea 'http'",
								favoriteThings.getWeb().getIdeas().contains( "dynamic content" )
						);

						urls = favoriteThings.getWeb().getLinks();
						assertTrue( "has urls", urls != null );
						URLFavorite[] favs = new URLFavorite[4];
						urls.toArray( favs );
						assertTrue(
								"has http://www.hibernate.org url favorite link",
								"http://www.hibernate.org/".equals( favs[0].getUrl() ) ||
										"http://www.hibernate.org/".equals( favs[1].getUrl() ) ||
										"http://www.hibernate.org/".equals( favs[2].getUrl() ) ||
										"http://www.hibernate.org/".equals( favs[3].getUrl() )
						);
						tx.commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-3868")
	public void testTransientMergeComponentParent(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Book b = new Book();
					b.setIsbn( SafeRandomUUIDGenerator.safeRandomUUIDAsString() );
					b.setSummary( new Summary() );
					b = (Book) session.merge( b );
				}
		);
	}

}
