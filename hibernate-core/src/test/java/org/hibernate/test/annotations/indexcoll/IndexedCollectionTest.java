/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.indexcoll;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.TeradataDialect;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test index collections
 *
 * @author Emmanuel Bernard
 */
public class IndexedCollectionTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testJPA2DefaultMapColumns() throws Exception {
		isDefaultKeyColumnPresent( Atmosphere.class.getName(), "gasesDef", "_KEY" );
		isDefaultKeyColumnPresent( Atmosphere.class.getName(), "gasesPerKeyDef", "_KEY" );
		isDefaultKeyColumnPresent( Atmosphere.class.getName(), "gasesDefLeg", "_KEY" );
	}

	@Test
	public void testJPA2DefaultIndexColumns() throws Exception {
		isDefaultKeyColumnPresent( Drawer.class.getName(), "dresses", "_ORDER" );
	}

	private void isDefaultKeyColumnPresent(String collectionOwner, String propertyName, String suffix) {
		assertTrue( "Could not find " + propertyName + suffix,
				isDefaultColumnPresent(collectionOwner, propertyName, suffix) );
	}

	private boolean isDefaultColumnPresent(String collectionOwner, String propertyName, String suffix) {
		final Collection collection = metadata().getCollectionBinding( collectionOwner + "." + propertyName );
		final Iterator columnIterator = collection.getCollectionTable().getColumnIterator();
		boolean hasDefault = false;
		while ( columnIterator.hasNext() ) {
			Column column = (Column) columnIterator.next();
			if ( (propertyName + suffix).equals( column.getName() ) ) hasDefault = true;
		}
		return hasDefault;
	}

	private void isNotDefaultKeyColumnPresent(String collectionOwner, String propertyName, String suffix) {
		assertFalse( "Could not find " + propertyName + suffix,
				isDefaultColumnPresent(collectionOwner, propertyName, suffix) );
	}

	@Test
	public void testFkList() throws Exception {
		Wardrobe w = new Wardrobe();
		Drawer d1 = new Drawer();
		Drawer d2 = new Drawer();
		w.setDrawers( new ArrayList<Drawer>() );
		w.getDrawers().add( d1 );
		w.getDrawers().add( d2 );
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( w );

		s.flush();
		s.clear();

		w = (Wardrobe) s.get( Wardrobe.class, w.getId() );
		assertNotNull( w );
		assertNotNull( w.getDrawers() );
		List<Drawer> result = w.getDrawers();
		assertEquals( 2, result.size() );
		assertEquals( d2.getId(), result.get( 1 ).getId() );
		result.remove( d1 );
		s.flush();
		d1 = (Drawer) s.merge( d1 );
		result.add( d1 );

		s.flush();
		s.clear();

		w = (Wardrobe) s.get( Wardrobe.class, w.getId() );
		assertNotNull( w );
		assertNotNull( w.getDrawers() );
		result = w.getDrawers();
		assertEquals( 2, result.size() );
		assertEquals( d1.getId(), result.get( 1 ).getId() );
		s.delete( result.get( 0 ) );
		s.delete( result.get( 1 ) );
		s.delete( w );
		s.flush();
		tx.rollback();
		s.close();
	}

	@Test
	public void testJoinedTableList() throws Exception {
		Wardrobe w = new Wardrobe();
		w.setDrawers( new ArrayList<Drawer>() );
		Drawer d = new Drawer();
		w.getDrawers().add( d );
		Dress d1 = new Dress();
		Dress d2 = new Dress();
		d.setDresses( new ArrayList<Dress>() );
		d.getDresses().add( d1 );
		d.getDresses().add( d2 );
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		s.persist( d1 );
		s.persist( d2 );
		s.persist( w );

		s.flush();
		s.clear();

		d = (Drawer) s.get( Drawer.class, d.getId() );
		assertNotNull( d );
		assertNotNull( d.getDresses() );
		List<Dress> result = d.getDresses();
		assertEquals( 2, result.size() );
		assertEquals( d2.getId(), result.get( 1 ).getId() );
		result.remove( d1 );
		s.flush();
		d1 = (Dress) s.merge( d1 );
		result.add( d1 );

		s.flush();
		s.clear();

		d = (Drawer) s.get( Drawer.class, d.getId() );
		assertNotNull( d );
		assertNotNull( d.getDresses() );
		result = d.getDresses();
		assertEquals( 2, result.size() );
		assertEquals( d1.getId(), result.get( 1 ).getId() );
		s.delete( result.get( 0 ) );
		s.delete( result.get( 1 ) );
		s.delete( d );
		s.flush();
		tx.rollback();
		s.close();
	}

	@Test
	public void testMapKey() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Software hibernate = new Software();
		hibernate.setName( "Hibernate" );
		Version v1 = new Version();
		v1.setCodeName( "HumbaHumba" );
		v1.setNumber( "1.0" );
		v1.setSoftware( hibernate );
		Version v2 = new Version();
		v2.setCodeName( "Copacabana" );
		v2.setNumber( "2.0" );
		v2.setSoftware( hibernate );
		Version v4 = new Version();
		v4.setCodeName( "Dreamland" );
		v4.setNumber( "4.0" );
		v4.setSoftware( hibernate );
		Map<String, Version> link = new HashMap<String, Version>();
		link.put( v1.getCodeName(), v1 );
		link.put( v2.getCodeName(), v2 );
		link.put( v4.getCodeName(), v4 );
		hibernate.setVersions( link );
		s.persist( hibernate );
		s.persist( v1 );
		s.persist( v2 );
		s.persist( v4 );

		s.flush();
		s.clear();

		hibernate = (Software) s.get( Software.class, "Hibernate" );
		assertEquals( 3, hibernate.getVersions().size() );
		assertEquals( "1.0", hibernate.getVersions().get( "HumbaHumba" ).getNumber() );
		assertEquals( "2.0", hibernate.getVersions().get( "Copacabana" ).getNumber() );
		hibernate.getVersions().remove( v4.getCodeName() );

		s.flush();
		s.clear();

		hibernate = (Software) s.get( Software.class, "Hibernate" );
		assertEquals( "So effect on collection changes", 3, hibernate.getVersions().size() );
		for ( Version v : hibernate.getVersions().values() ) {
			s.delete( v );
		}
		s.delete( hibernate );

		s.flush();

		tx.rollback();
		s.close();
	}

	@Test
	public void testDefaultMapKey() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		AddressBook book = new AddressBook();
		book.setOwner( "Emmanuel" );
		AddressEntryPk helene = new AddressEntryPk( "Helene", "Michau" );
		AddressEntry heleneEntry = new AddressEntry();
		heleneEntry.setBook( book );
		heleneEntry.setCity( "Levallois" );
		heleneEntry.setStreet( "Louis Blanc" );
		heleneEntry.setPerson( helene );
		AddressEntryPk primeMinister = new AddressEntryPk( "Dominique", "Villepin" );
		AddressEntry primeMinisterEntry = new AddressEntry();
		primeMinisterEntry.setBook( book );
		primeMinisterEntry.setCity( "Paris" );
		primeMinisterEntry.setStreet( "Hotel Matignon" );
		primeMinisterEntry.setPerson( primeMinister );
		book.getEntries().put( helene, heleneEntry );
		book.getEntries().put( primeMinister, primeMinisterEntry );
		s.persist( book );

		s.flush();
		s.clear();

		book = (AddressBook) s.get( AddressBook.class, book.getId() );
		assertEquals( 2, book.getEntries().size() );
		assertEquals( heleneEntry.getCity(), book.getEntries().get( helene ).getCity() );
		AddressEntryPk fake = new AddressEntryPk( "Fake", "Fake" );
		book.getEntries().put( fake, primeMinisterEntry );

		s.flush();
		s.clear();

		book = (AddressBook) s.get( AddressBook.class, book.getId() );
		assertEquals( 2, book.getEntries().size() );
		assertNull( book.getEntries().get( fake ) );
		s.delete( book );

		s.flush();
		tx.rollback();
		s.close();
	}

	@Test
	public void testMapKeyToEntity() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		AlphabeticalDirectory m = new AlphabeticalDirectory();
		m.setName( "M" );
		AlphabeticalDirectory v = new AlphabeticalDirectory();
		v.setName( "V" );
		s.persist( m );
		s.persist( v );

		AddressBook book = new AddressBook();
		book.setOwner( "Emmanuel" );
		AddressEntryPk helene = new AddressEntryPk( "Helene", "Michau" );
		AddressEntry heleneEntry = new AddressEntry();
		heleneEntry.setBook( book );
		heleneEntry.setCity( "Levallois" );
		heleneEntry.setStreet( "Louis Blanc" );
		heleneEntry.setPerson( helene );
		heleneEntry.setDirectory( m );
		AddressEntryPk primeMinister = new AddressEntryPk( "Dominique", "Villepin" );
		AddressEntry primeMinisterEntry = new AddressEntry();
		primeMinisterEntry.setBook( book );
		primeMinisterEntry.setCity( "Paris" );
		primeMinisterEntry.setStreet( "Hotel Matignon" );
		primeMinisterEntry.setPerson( primeMinister );
		primeMinisterEntry.setDirectory( v );
		book.getEntries().put( helene, heleneEntry );
		book.getEntries().put( primeMinister, primeMinisterEntry );
		s.persist( book );

		s.flush();
		s.clear();

		book = (AddressBook) s.get( AddressBook.class, book.getId() );
		assertEquals( 2, book.getEntries().size() );
		assertEquals( heleneEntry.getCity(), book.getEntries().get( helene ).getCity() );
		assertEquals( "M", book.getEntries().get( helene ).getDirectory().getName() );

		s.delete( book );
		tx.rollback();
		s.close();
	}

	@Test
	@RequiresDialect({HSQLDialect.class, H2Dialect.class})
	public void testComponentSubPropertyMapKey() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		AddressBook book = new AddressBook();
		book.setOwner( "Emmanuel" );
		AddressEntryPk helene = new AddressEntryPk( "Helene", "Michau" );
		AddressEntry heleneEntry = new AddressEntry();
		heleneEntry.setBook( book );
		heleneEntry.setCity( "Levallois" );
		heleneEntry.setStreet( "Louis Blanc" );
		heleneEntry.setPerson( helene );
		AddressEntryPk primeMinister = new AddressEntryPk( "Dominique", "Villepin" );
		AddressEntry primeMinisterEntry = new AddressEntry();
		primeMinisterEntry.setBook( book );
		primeMinisterEntry.setCity( "Paris" );
		primeMinisterEntry.setStreet( "Hotel Matignon" );
		primeMinisterEntry.setPerson( primeMinister );
		book.getEntries().put( helene, heleneEntry );
		book.getEntries().put( primeMinister, primeMinisterEntry );
		s.persist( book );

		s.flush();
		s.clear();

		book = (AddressBook) s.get( AddressBook.class, book.getId() );
		assertEquals( 2, book.getLastNameEntries().size() );
		assertEquals( heleneEntry.getCity(), book.getLastNameEntries().get( "Michau" ).getCity() );
		AddressEntryPk fake = new AddressEntryPk( "Fake", "Fake" );
		book.getEntries().put( fake, primeMinisterEntry );

		s.flush();
		s.clear();

		book = (AddressBook) s.get( AddressBook.class, book.getId() );
		assertEquals( 2, book.getEntries().size() );
		assertNull( book.getEntries().get( fake ) );
		s.delete( book );
		tx.rollback();
		s.close();
	}

	@Test
	@SkipForDialect(
			value = TeradataDialect.class,
			jiraKey = "HHH-8190",
			comment = "uses Teradata reserved word - title"
	)
	public void testMapKeyOnManyToMany() throws Exception {
		Session s;
		s = openSession();
		s.getTransaction().begin();
		News airplane = new News();
		airplane.setTitle( "Crash!" );
		airplane.setDetail( "An airplaned crashed." );
		s.persist( airplane );
		Newspaper lemonde = new Newspaper();
		lemonde.setName( "Lemonde" );
		lemonde.getNews().put( airplane.getTitle(), airplane );
		s.persist( lemonde );

		s.flush();
		s.clear();

		lemonde = (Newspaper) s.get( Newspaper.class, lemonde.getId() );
		assertEquals( 1, lemonde.getNews().size() );
		News news = lemonde.getNews().get( airplane.getTitle() );
		assertNotNull( news );
		assertEquals( airplane.getTitle(), news.getTitle() );
		s.delete( lemonde );
		s.delete( news );

		s.getTransaction().rollback();
		s.close();
	}

	@Test
	@SkipForDialect(
			value = TeradataDialect.class,
			jiraKey = "HHH-8190",
			comment = "uses Teradata reserved word - title"
	)
	public void testMapKeyOnManyToManyOnId() throws Exception {
		Session s;
		s = openSession();
		s.getTransaction().begin();
		News hibernate1 = new News();
		hibernate1.setTitle( "#1 ORM solution in the Java world" );
		hibernate1.setDetail( "Well, that's no news ;-)" );
		s.persist( hibernate1 );
		PressReleaseAgency schwartz = new PressReleaseAgency();
		schwartz.setName( "Schwartz" );
		schwartz.getProvidedNews().put( hibernate1.getId(), hibernate1 );
		s.persist( schwartz );

		s.flush();
		s.clear();

		schwartz = (PressReleaseAgency) s.get( PressReleaseAgency.class, schwartz.getId() );
		assertEquals( 1, schwartz.getProvidedNews().size() );
		News news = schwartz.getProvidedNews().get( hibernate1.getId() );
		assertNotNull( news );
		assertEquals( hibernate1.getTitle(), news.getTitle() );
		s.delete( schwartz );
		s.delete( news );

		s.getTransaction().rollback();
		s.close();
	}

	@Test
	public void testMapKeyAndIdClass() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Painter picasso = new Painter();
		Painting laVie = new Painting( "La Vie", "Picasso", 50, 20 );
		picasso.getPaintings().put( "La Vie", laVie );
		Painting famille = new Painting( "La Famille du Saltimbanque", "Picasso", 50, 20 );
		picasso.getPaintings().put( "La Famille du Saltimbanque", famille );
		s.persist( picasso );

		s.flush();
		s.clear();

		picasso = (Painter) s.get( Painter.class, picasso.getId() );
		Painting painting = picasso.getPaintings().get( famille.getName() );
		assertNotNull( painting );
		assertEquals( painting.getName(), famille.getName() );
		s.delete( picasso );
		tx.rollback();
		s.close();
	}

	@Test
	public void testRealMap() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Atmosphere atm = new Atmosphere();
		Atmosphere atm2 = new Atmosphere();
		GasKey key = new GasKey();
		key.setName( "O2" );
		Gas o2 = new Gas();
		o2.name = "oxygen";
		atm.gases.put( "100%", o2 );
		atm.gasesPerKey.put( key, o2 );
		atm2.gases.put( "100%", o2 );
		atm2.gasesPerKey.put( key, o2 );
		s.persist( key );
		s.persist( atm );
		s.persist( atm2 );

		s.flush();
		s.clear();

		atm = (Atmosphere) s.get( Atmosphere.class, atm.id );
		key = (GasKey) s.get( GasKey.class, key.getName() );
		assertEquals( 1, atm.gases.size() );
		assertEquals( o2.name, atm.gases.get( "100%" ).name );
		assertEquals( o2.name, atm.gasesPerKey.get( key ).name );
		tx.rollback();
		s.close();
	}

	@Test
	public void testTemporalKeyMap() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Atmosphere atm = new Atmosphere();
		atm.colorPerDate.put( new Date(1234567000), "red" );
		s.persist( atm );
		
		s.flush();
		s.clear();

		atm = (Atmosphere) s.get( Atmosphere.class, atm.id );
		assertEquals( 1, atm.colorPerDate.size() );
		final Date date = atm.colorPerDate.keySet().iterator().next();
		final long diff = new Date( 1234567000 ).getTime() - date.getTime();
		assertTrue( "24h diff max", diff > 0 && diff < 24*60*60*1000 );
		tx.rollback();
		s.close();
	}

	@Test
	public void testEnumKeyType() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Atmosphere atm = new Atmosphere();
		atm.colorPerLevel.put( Atmosphere.Level.HIGH, "red" );
		s.persist( atm );

		s.flush();
		s.clear();

		atm = (Atmosphere) s.get( Atmosphere.class, atm.id );
		assertEquals( 1, atm.colorPerLevel.size() );
		assertEquals( "red", atm.colorPerLevel.get(Atmosphere.Level.HIGH) );
		tx.rollback();
		s.close();
	}

	@Test
	public void testMapKeyEntityEntity() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		AddressBook book = new AddressBook();
		s.persist( book );
		AddressEntry entry = new AddressEntry();
		entry.setCity( "Atlanta");
		AddressEntryPk pk = new AddressEntryPk("Coca", "Cola" );
		entry.setPerson( pk );
		entry.setBook( book );
		AlphabeticalDirectory ad = new AlphabeticalDirectory();
		ad.setName( "C");
		s.persist( ad );
		entry.setDirectory( ad );
		s.persist( entry );
		book.getDirectoryEntries().put( ad, entry );

		s.flush();
		s.clear();

		book = (AddressBook) s.get( AddressBook.class, book.getId() );
		assertEquals( 1, book.getDirectoryEntries().size() );
		assertEquals( "C", book.getDirectoryEntries().keySet().iterator().next().getName() );
		tx.rollback();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-8879")
	public void testMapKeyEmbeddableWithEntityKey() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Currency currency1= new Currency();
		Currency currency2= new Currency();
		s.persist( currency1 );
		s.persist( currency2 );
		Integer id1 = currency1.getId();
		Integer id2 = currency2.getId();
		ExchangeRateKey cq = new ExchangeRateKey(20140101, currency1, currency2);

		ExchangeRate m = new ExchangeRate();
		m.setKey( cq );
		s.persist( m );
		ExchangeOffice wm = new ExchangeOffice();
		s.persist( wm );

		wm.getExchangeRates().put( cq, m );
		m.setParent( wm );
		Integer id = wm.getId();
		s.flush();
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		wm = (ExchangeOffice) s.byId(ExchangeOffice.class).load(id);
		assertNotNull(wm);
		wm.getExchangeRates().size();
		currency1 = (Currency) s.byId(Currency.class).load(id1);
		assertNotNull(currency1);
		currency2 = (Currency) s.byId(Currency.class).load(id2);
		assertNotNull(currency2);
		cq = new ExchangeRateKey(20140101, currency1, currency2);

		m = wm.getExchangeRates().get( cq );
		assertNotNull(m);
		tx.commit();
		s.close();

	}

	@Test
	@TestForIssue( jiraKey = "HHH-8994")
	public void testEmbeddableWithEntityKey() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Currency currency1= new Currency();
		Currency currency2= new Currency();
		s.persist( currency1 );
		s.persist( currency2 );
		Integer id1 = currency1.getId();
		Integer id2 = currency2.getId();
		ExchangeRateKey cq = new ExchangeRateKey(20140101, currency1, currency2);

		ExchangeOffice wm = new ExchangeOffice();
		s.persist( wm );

		final BigDecimal fee = BigDecimal.valueOf( 12, 2 );

		wm.getExchangeRateFees().put( cq, fee );
		Integer id = wm.getId();
		s.flush();
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		wm = (ExchangeOffice) s.byId(ExchangeOffice.class).load(id);
		assertNotNull(wm);
		wm.getExchangeRateFees().size();
		currency1 = (Currency) s.byId(Currency.class).load(id1);
		assertNotNull(currency1);
		currency2 = (Currency) s.byId(Currency.class).load(id2);
		assertNotNull(currency2);
		cq = new ExchangeRateKey(20140101, currency1, currency2);

		assertEquals( fee, wm.getExchangeRateFees().get( cq ) );

		tx.commit();
		s.close();
	}

	@Test
	public void testEntityKeyElementTarget() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Atmosphere atm = new Atmosphere();
		Gas o2 = new Gas();
		o2.name = "oxygen";
		atm.composition.put( o2, 94.3 );
		s.persist( o2 );
		s.persist( atm );

		s.flush();
		s.clear();

		atm = (Atmosphere) s.get( Atmosphere.class, atm.id );
		assertTrue( ! Hibernate.isInitialized( atm.composition ) );
		assertEquals( 1, atm.composition.size() );
		assertEquals( o2.name, atm.composition.keySet().iterator().next().name );
		tx.rollback();
		s.close();
	}

	@Test
	public void testSortedMap() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Training training = new Training();
		Trainee trainee = new Trainee();
		trainee.setName( "Jim" );
		Trainee trainee2 = new Trainee();
		trainee2.setName( "Emmanuel" );
		s.persist( trainee );
		s.persist( trainee2 );
		training.getTrainees().put( "Jim", trainee );
		training.getTrainees().put( "Emmanuel", trainee2 );
		s.persist( training );

		s.flush();
		s.clear();

		training = (Training) s.get( Training.class, training.getId() );
		assertEquals( "Emmanuel", training.getTrainees().firstKey() );
		assertEquals( "Jim", training.getTrainees().lastKey() );
		tx.rollback();
		s.close();
	}

	@Test
	public void testMapKeyLoad() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		Software hibernate = new Software();
		hibernate.setName( "Hibernate" );
		Version v1 = new Version();
		v1.setCodeName( "HumbaHumba" );
		v1.setNumber( "1.0" );
		v1.setSoftware( hibernate );
		hibernate.addVersion( v1 );
		s.persist( hibernate );
		s.persist( v1 );

		s.flush();
		s.clear();

		hibernate = (Software) s.get( Software.class, "Hibernate" );
		assertEquals(1, hibernate.getVersions().size() );
		Version v2 = new Version();
		v2.setCodeName( "HumbaHumba2" );
		v2.setNumber( "2.0" );
		v2.setSoftware( hibernate );
		hibernate.addVersion( v2 );
		assertEquals( "One loaded persisted version, and one just added", 2, hibernate.getVersions().size() );

		s.flush();
		s.clear();

		hibernate = (Software) s.get( Software.class, "Hibernate" );
		for ( Version v : hibernate.getVersions().values() ) {
			s.delete( v );
		}
		s.delete( hibernate );
		tx.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				Wardrobe.class,
				Drawer.class,
				Dress.class,
				Software.class,
				Version.class,
				AddressBook.class,
				AddressEntry.class,
				AddressEntryPk.class, //should be silently ignored
				Newspaper.class,
				News.class,
				PressReleaseAgency.class,
				Painter.class,
				Painting.class,
				Atmosphere.class,
				Gas.class,
				AlphabeticalDirectory.class,
				GasKey.class,
				Trainee.class,
				Training.class,
				Currency.class,
				ExchangeOffice.class,
				ExchangeRate.class,
		};
	}
}
