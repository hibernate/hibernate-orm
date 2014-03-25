/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.embedded;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.embedded.FloatLeg.RateIndex;
import org.hibernate.test.annotations.embedded.Leg.Frequency;
import org.hibernate.test.util.SchemaUtil;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class EmbeddedTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testSimple() throws Exception {
		Session s;
		Transaction tx;
		Person p = new Person();
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
		p.address = a;
		p.bornIn = bornCountry;
		p.name = "Homer";
		s = openSession();
		tx = s.beginTransaction();
		s.persist( p );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		p = (Person) s.get( Person.class, p.id );
		assertNotNull( p );
		assertNotNull( p.address );
		assertEquals( "Springfield", p.address.city );
		assertNotNull( p.address.country );
		assertEquals( "DM", p.address.country.getIso2() );
		assertNotNull( p.bornIn );
		assertEquals( "US", p.bornIn.getIso2() );
		tx.commit();
		s.close();
	}

	@Test
	public void testCompositeId() throws Exception {
		Session s;
		Transaction tx;
		RegionalArticlePk pk = new RegionalArticlePk();
		pk.iso2 = "FR";
		pk.localUniqueKey = "1234567890123";
		RegionalArticle reg = new RegionalArticle();
		reg.setName( "Je ne veux pes rester sage - Dolly" );
		reg.setPk( pk );
		s = openSession();
		tx = s.beginTransaction();
		s.persist( reg );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		reg = (RegionalArticle) s.get( RegionalArticle.class, reg.getPk() );
		assertNotNull( reg );
		assertNotNull( reg.getPk() );
		assertEquals( "Je ne veux pes rester sage - Dolly", reg.getName() );
		assertEquals( "FR", reg.getPk().iso2 );
		tx.commit();
		s.close();
	}

	@Test
	public void testManyToOneInsideComponent() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Person p = new Person();
		Country bornIn = new Country();
		bornIn.setIso2( "FR" );
		bornIn.setName( "France" );
		p.bornIn = bornIn;
		p.name = "Emmanuel";
		AddressType type = new AddressType();
		type.setName( "Primary Home" );
		s.persist( type );
		Country currentCountry = new Country();
		currentCountry.setIso2( "US" );
		currentCountry.setName( "USA" );
		Address add = new Address();
		add.address1 = "4 square street";
		add.city = "San diego";
		add.country = currentCountry;
		add.type = type;
		p.address = add;
		s.persist( p );
		tx.commit();

		s = openSession();
		tx = s.beginTransaction();
		Query q = s.createQuery( "select p from Person p where p.address.city = :city" );
		q.setString( "city", add.city );
		List result = q.list();
		Person samePerson = (Person) result.get( 0 );
		assertNotNull( samePerson.address.type );
		assertEquals( type.getName(), samePerson.address.type.getName() );
		tx.commit();
		s.close();
	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void testEmbeddedSuperclass() {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		VanillaSwap swap = new VanillaSwap();
		swap.setInstrumentId( "US345421" );
		swap.setCurrency( VanillaSwap.Currency.EUR );
		FixedLeg fixed = new FixedLeg();
		fixed.setPaymentFrequency( Leg.Frequency.SEMIANNUALLY );
		fixed.setRate( 5.6 );
		FloatLeg floating = new FloatLeg();
		floating.setPaymentFrequency( Leg.Frequency.QUARTERLY );
		floating.setRateIndex( FloatLeg.RateIndex.LIBOR );
		floating.setRateSpread( 1.1 );
		swap.setFixedLeg( fixed );
		swap.setFloatLeg( floating );
		s.persist( swap );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		swap = (VanillaSwap) s.get( VanillaSwap.class, swap.getInstrumentId() );
		// All fields must be filled with non-default values
		fixed = swap.getFixedLeg();
		assertNotNull( "Fixed leg retrieved as null", fixed );
		floating = swap.getFloatLeg();
		assertNotNull( "Floating leg retrieved as null", floating );
		assertEquals( Leg.Frequency.SEMIANNUALLY, fixed.getPaymentFrequency() );
		assertEquals( Leg.Frequency.QUARTERLY, floating.getPaymentFrequency() );
		s.delete( swap );
		tx.commit();
		s.close();
	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void testDottedProperty() {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
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
		SpreadDeal deal = new SpreadDeal();
		deal.setId( "FX45632" );
		deal.setNotional( 450000.0 );
		deal.setShortSwap( shortSwap );
		deal.setSwap( swap );
		deal.setLongSwap( longSwap );
		s.persist( deal );

		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		deal = (SpreadDeal) s.get( SpreadDeal.class, deal.getId() );
		// All fields must be filled with non-default values
		assertNotNull( "Short swap is null.", deal.getShortSwap() );
		assertNotNull( "Swap is null.", deal.getSwap() );
		assertNotNull( "Long swap is null.", deal.getLongSwap() );
		assertEquals( 2, deal.getShortSwap().getTenor() );
		assertEquals( 7, deal.getSwap().getTenor() );
		assertEquals( 7, deal.getLongSwap().getTenor() );
		assertNotNull( "Short fixed leg is null.", deal.getShortSwap().getFixedLeg() );
		assertNotNull( "Short floating leg is null.", deal.getShortSwap().getFloatLeg() );
		assertNotNull( "Fixed leg is null.", deal.getSwap().getFixedLeg() );
		assertNotNull( "Floating leg is null.", deal.getSwap().getFloatLeg() );
		assertNotNull( "Long fixed leg is null.", deal.getLongSwap().getFixedLeg() );
		assertNotNull( "Long floating leg is null.", deal.getLongSwap().getFloatLeg() );
		assertEquals( Frequency.SEMIANNUALLY, deal.getShortSwap().getFixedLeg().getPaymentFrequency() );
		assertEquals( Frequency.QUARTERLY, deal.getShortSwap().getFloatLeg().getPaymentFrequency() );
		assertEquals( Frequency.MONTHLY, deal.getSwap().getFixedLeg().getPaymentFrequency() );
		assertEquals( Frequency.MONTHLY, deal.getSwap().getFloatLeg().getPaymentFrequency() );
		assertEquals( Frequency.MONTHLY, deal.getLongSwap().getFixedLeg().getPaymentFrequency() );
		assertEquals( Frequency.MONTHLY, deal.getLongSwap().getFloatLeg().getPaymentFrequency() );
		assertEquals( 5.6, deal.getShortSwap().getFixedLeg().getRate(), 0.01 );
		assertEquals( 7.6, deal.getSwap().getFixedLeg().getRate(), 0.01 );
		assertEquals( 7.6, deal.getLongSwap().getFixedLeg().getRate(), 0.01 );
		assertEquals( RateIndex.LIBOR, deal.getShortSwap().getFloatLeg().getRateIndex() );
		assertEquals( RateIndex.TIBOR, deal.getSwap().getFloatLeg().getRateIndex() );
		assertEquals( RateIndex.TIBOR, deal.getLongSwap().getFloatLeg().getRateIndex() );
		assertEquals( 1.1, deal.getShortSwap().getFloatLeg().getRateSpread(), 0.01 );
		assertEquals( 0.8, deal.getSwap().getFloatLeg().getRateSpread(), 0.01 );
		assertEquals( 0.8, deal.getLongSwap().getFloatLeg().getRateSpread(), 0.01 );
		s.delete( deal );
		tx.commit();
		s.close();
	}

	@Test
	public void testEmbeddedInSecondaryTable() throws Exception {
		Session s;
		s = openSession();
		s.getTransaction().begin();
		Book book = new Book();
		book.setIsbn( "1234" );
		book.setName( "HiA Second Edition" );
		Summary summary = new Summary();
		summary.setText( "This is a HiA SE summary" );
		summary.setSize( summary.getText().length() );
		book.setSummary( summary );
		s.persist( book );
		s.getTransaction().commit();

		s.clear();

		Transaction tx = s.beginTransaction();
		Book loadedBook = (Book) s.get( Book.class, book.getIsbn() );
		assertNotNull( loadedBook.getSummary() );
		assertEquals( book.getSummary().getText(), loadedBook.getSummary().getText() );
		s.delete( loadedBook );
		tx.commit();
		s.close();
	}

	@Test
	public void testParent() throws Exception {
		Session s;
		s = openSession();
		s.getTransaction().begin();
		Book book = new Book();
		book.setIsbn( "1234" );
		book.setName( "HiA Second Edition" );
		Summary summary = new Summary();
		summary.setText( "This is a HiA SE summary" );
		summary.setSize( summary.getText().length() );
		book.setSummary( summary );
		s.persist( book );
		s.getTransaction().commit();

		s.clear();

		Transaction tx = s.beginTransaction();
		Book loadedBook = (Book) s.get( Book.class, book.getIsbn() );
		assertNotNull( loadedBook.getSummary() );
		assertEquals( loadedBook, loadedBook.getSummary().getSummarizedBook() );
		s.delete( loadedBook );
		tx.commit();
		s.close();
	}

	@Test
	public void testEmbeddedAndMultipleManyToOne() throws Exception {
		Session s;
		s = openSession();
		Transaction tx = s.beginTransaction();
		CorpType type = new CorpType();
		type.setType( "National" );
		s.persist( type );
		Nationality nat = new Nationality();
		nat.setName( "Canadian" );
		s.persist( nat );
		InternetProvider provider = new InternetProvider();
		provider.setBrandName( "Fido" );
		LegalStructure structure = new LegalStructure();
		structure.setCorporationType( type );
		structure.setCountry( "Canada" );
		structure.setName( "Rogers" );
		provider.setOwner( structure );
		structure.setOrigin( nat );
		s.persist( provider );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		provider = (InternetProvider) s.get( InternetProvider.class, provider.getId() );
		assertNotNull( provider.getOwner() );
		assertNotNull( "Many to one not set", provider.getOwner().getCorporationType() );
		assertEquals( "Wrong link", type.getType(), provider.getOwner().getCorporationType().getType() );
		assertNotNull( "2nd Many to one not set", provider.getOwner().getOrigin() );
		assertEquals( "Wrong 2nd link", nat.getName(), provider.getOwner().getOrigin().getName() );
		s.delete( provider );
		s.delete( provider.getOwner().getCorporationType() );
		s.delete( provider.getOwner().getOrigin() );
		tx.commit();
		s.close();
	}

	@Test
	public void testEmbeddedAndOneToMany() throws Exception {
		Session s;
		s = openSession();
		Transaction tx = s.beginTransaction();
		InternetProvider provider = new InternetProvider();
		provider.setBrandName( "Fido" );
		LegalStructure structure = new LegalStructure();
		structure.setCountry( "Canada" );
		structure.setName( "Rogers" );
		provider.setOwner( structure );
		s.persist( provider );
		Manager manager = new Manager();
		manager.setName( "Bill" );
		manager.setEmployer( provider );
		structure.getTopManagement().add( manager );
		s.persist( manager );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		provider = (InternetProvider) s.get( InternetProvider.class, provider.getId() );
		assertNotNull( provider.getOwner() );
		Set<Manager> topManagement = provider.getOwner().getTopManagement();
		assertNotNull( "OneToMany not set", topManagement );
		assertEquals( "Wrong number of elements", 1, topManagement.size() );
		manager = topManagement.iterator().next();
		assertEquals( "Wrong element", "Bill", manager.getName() );
		s.delete( manager );
		s.delete( provider );
		tx.commit();
		s.close();
	}

	@Test
	public void testDefaultCollectionTable() throws Exception {
		//are the tables correct?
		assertTrue( SchemaUtil.isTablePresent("WealthyPerson_vacationHomes", metadata() ) );
		assertTrue( SchemaUtil.isTablePresent("WealthyPerson_legacyVacationHomes", metadata() ) );
		assertTrue( SchemaUtil.isTablePresent("WelPers_VacHomes", metadata() ) );
		//just to make sure, use the mapping
		Session s;
		Transaction tx;
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
		p.vacationHomes.add(vacation);
		p.address = a;
		p.bornIn = bornCountry;
		p.name = "Homer";
		s = openSession();
		tx = s.beginTransaction();
		s.persist( p );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		p = (WealthyPerson) s.get( WealthyPerson.class, p.id );
		assertNotNull( p );
		assertNotNull( p.address );
		assertEquals( "Springfield", p.address.city );
		assertNotNull( p.address.country );
		assertEquals( "DM", p.address.country.getIso2() );
		assertNotNull( p.bornIn );
		assertEquals( "US", p.bornIn.getIso2() );
		tx.commit();
		s.close();
	}

	// make sure we support collection of embeddable objects inside embeddable objects
	@Test
	public void testEmbeddableInsideEmbeddable() throws Exception {
		Session s;
		Transaction tx;

		Collection<URLFavorite> urls = new ArrayList<URLFavorite>();
		URLFavorite urlFavorite = new URLFavorite();
		urlFavorite.setUrl( "http://highscalability.com/" );
		urls.add(urlFavorite);

		urlFavorite = new URLFavorite();
		urlFavorite.setUrl( "http://www.jboss.org/" );
		urls.add(urlFavorite);

		urlFavorite = new URLFavorite();
		urlFavorite.setUrl( "http://www.hibernate.org/" );
		urls.add(urlFavorite);

		urlFavorite = new URLFavorite();
		urlFavorite.setUrl( "http://www.jgroups.org/" );
		urls.add( urlFavorite );

 		Collection<String>ideas = new ArrayList<String>();
		ideas.add( "lionheart" );
		ideas.add( "xforms" );
		ideas.add( "dynamic content" );
		ideas.add( "http" );

		InternetFavorites internetFavorites = new InternetFavorites();
		internetFavorites.setLinks( urls );
		internetFavorites.setIdeas( ideas );

		FavoriteThings favoriteThings = new FavoriteThings();
		favoriteThings.setWeb( internetFavorites );

		s = openSession();

		tx = s.beginTransaction();
		s.persist(favoriteThings);
		tx.commit();

		tx = s.beginTransaction();
		s.flush();
		favoriteThings = (FavoriteThings) s.get( FavoriteThings.class,  favoriteThings.getId() );
		assertTrue( "has web", favoriteThings.getWeb() != null );
		assertTrue( "has ideas", favoriteThings.getWeb().getIdeas() != null );
		assertTrue( "has favorite idea 'http'",favoriteThings.getWeb().getIdeas().contains("http") );
		assertTrue( "has favorite idea 'http'",favoriteThings.getWeb().getIdeas().contains("dynamic content") );

		urls = favoriteThings.getWeb().getLinks();
		assertTrue( "has urls", urls != null);
		URLFavorite[] favs = new URLFavorite[4];
		urls.toArray(favs);
		assertTrue( "has http://www.hibernate.org url favorite link",
			"http://www.hibernate.org/".equals( favs[0].getUrl() ) ||
			"http://www.hibernate.org/".equals( favs[1].getUrl() ) ||
			"http://www.hibernate.org/".equals( favs[2].getUrl() ) ||
			"http://www.hibernate.org/".equals( favs[3].getUrl() ));
		tx.commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
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
				FavoriteThings.class,
				Address.class,
				Country.class,
				InternetFavorites.class,
				FixedLeg.class,
				FloatLeg.class,
				Swap.class,
				RegionalArticlePk.class,
				LegalStructure.class,
				Summary.class,
				URLFavorite.class
		};
	}
}
