//$Id$
package org.hibernate.test.annotations.collectionelement;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.hibernate.Filter;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.test.annotations.Country;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public class CollectionElementTest extends TestCase {
	
	public void testSimpleElement() throws Exception {
		assertEquals(
				"BoyFavoriteNumbers",
				getCfg().getCollectionMapping( Boy.class.getName() + '.' + "favoriteNumbers" )
						.getCollectionTable().getName()
		);
		Session s = openSession();
		s.getTransaction().begin();
		Boy boy = new Boy();
		boy.setFirstName( "John" );
		boy.setLastName( "Doe" );
		boy.getNickNames().add( "Johnny" );
		boy.getNickNames().add( "Thing" );
		boy.getScorePerNickName().put( "Johnny", new Integer( 3 ) );
		boy.getScorePerNickName().put( "Thing", new Integer( 5 ) );
		int[] favNbrs = new int[4];
		for (int index = 0; index < favNbrs.length - 1; index++) {
			favNbrs[index] = index * 3;
		}
		boy.setFavoriteNumbers( favNbrs );
		boy.getCharacters().add( Character.GENTLE );
		boy.getCharacters().add( Character.CRAFTY );

		HashMap<String,FavoriteFood> foods = new HashMap<String,FavoriteFood>();
		foods.put( "breakfast", FavoriteFood.PIZZA);
		foods.put( "lunch", FavoriteFood.KUNGPAOCHICKEN);
		foods.put( "dinner", FavoriteFood.SUSHI);
		boy.setFavoriteFood(foods);
		s.persist( boy );
		s.getTransaction().commit();
		s.clear();
		Transaction tx = s.beginTransaction();
		boy = (Boy) s.get( Boy.class, boy.getId() );
		assertNotNull( boy.getNickNames() );
		assertTrue( boy.getNickNames().contains( "Thing" ) );
		assertNotNull( boy.getScorePerNickName() );
		assertTrue( boy.getScorePerNickName().containsKey( "Thing" ) );
		assertEquals( new Integer( 5 ), boy.getScorePerNickName().get( "Thing" ) );
		assertNotNull( boy.getFavoriteNumbers() );
		assertEquals( 3, boy.getFavoriteNumbers()[1] );
		assertTrue( boy.getCharacters().contains( Character.CRAFTY ) );
		assertTrue( boy.getFavoriteFood().get("dinner").equals(FavoriteFood.SUSHI));
		assertTrue( boy.getFavoriteFood().get("lunch").equals(FavoriteFood.KUNGPAOCHICKEN));
		assertTrue( boy.getFavoriteFood().get("breakfast").equals(FavoriteFood.PIZZA));
		List result = s.createQuery( "select boy from Boy boy join boy.nickNames names where names = :name" )
				.setParameter( "name", "Thing" ).list();
		assertEquals( 1, result.size() );
		s.delete( boy );
		tx.commit();
		s.close();
	}

	public void testCompositeElement() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Boy boy = new Boy();
		boy.setFirstName( "John" );
		boy.setLastName( "Doe" );
		Toy toy = new Toy();
		toy.setName( "Balloon" );
		toy.setSerial( "serial001" );
		toy.setBrand( new Brand() );
		toy.getBrand().setName( "Bandai" );
		boy.getFavoriteToys().add( toy );
		s.persist( boy );
		s.getTransaction().commit();
		s.clear();
		Transaction tx = s.beginTransaction();
		boy = (Boy) s.get( Boy.class, boy.getId() );
		assertNotNull( boy.getFavoriteToys() );
		assertTrue( boy.getFavoriteToys().contains( toy ) );
		assertEquals( "@Parent is failing", boy, boy.getFavoriteToys().iterator().next().getOwner() );
		s.delete( boy );
		tx.commit();
		s.close();
	}

	public void testAttributedJoin() throws Exception {
		Session s = openSession();
		s.getTransaction().begin();
		Country country = new Country();
		country.setName( "Australia" );
		s.persist( country );

		Boy boy = new Boy();
		boy.setFirstName( "John" );
		boy.setLastName( "Doe" );
		CountryAttitude attitude = new CountryAttitude();
		// TODO: doesn't work
		attitude.setBoy( boy );
		attitude.setCountry( country );
		attitude.setLikes( true );
		boy.getCountryAttitudes().add( attitude );
		s.persist( boy );
		s.getTransaction().commit();
		s.clear();

		Transaction tx = s.beginTransaction();
		boy = (Boy) s.get( Boy.class, boy.getId() );
		assertTrue( boy.getCountryAttitudes().contains( attitude ) );
		s.delete( boy );
		s.delete( s.get( Country.class, country.getId() ) );
		tx.commit();
		s.close();

	}

	public void testLazyCollectionofElements() throws Exception {
		assertEquals(
				"BoyFavoriteNumbers",
				getCfg().getCollectionMapping( Boy.class.getName() + '.' + "favoriteNumbers" )
						.getCollectionTable().getName()
		);
		Session s = openSession();
		s.getTransaction().begin();
		Boy boy = new Boy();
		boy.setFirstName( "John" );
		boy.setLastName( "Doe" );
		boy.getNickNames().add( "Johnny" );
		boy.getNickNames().add( "Thing" );
		boy.getScorePerNickName().put( "Johnny", new Integer( 3 ) );
		boy.getScorePerNickName().put( "Thing", new Integer( 5 ) );
		int[] favNbrs = new int[4];
		for (int index = 0; index < favNbrs.length - 1; index++) {
			favNbrs[index] = index * 3;
		}
		boy.setFavoriteNumbers( favNbrs );
		boy.getCharacters().add( Character.GENTLE );
		boy.getCharacters().add( Character.CRAFTY );
		s.persist( boy );
		s.getTransaction().commit();
		s.clear();
		Transaction tx = s.beginTransaction();
		boy = (Boy) s.get( Boy.class, boy.getId() );
		assertNotNull( boy.getNickNames() );
		assertTrue( boy.getNickNames().contains( "Thing" ) );
		assertNotNull( boy.getScorePerNickName() );
		assertTrue( boy.getScorePerNickName().containsKey( "Thing" ) );
		assertEquals( new Integer( 5 ), boy.getScorePerNickName().get( "Thing" ) );
		assertNotNull( boy.getFavoriteNumbers() );
		assertEquals( 3, boy.getFavoriteNumbers()[1] );
		assertTrue( boy.getCharacters().contains( Character.CRAFTY ) );
		List result = s.createQuery( "select boy from Boy boy join boy.nickNames names where names = :name" )
				.setParameter( "name", "Thing" ).list();
		assertEquals( 1, result.size() );
		s.delete( boy );
		tx.commit();
		s.close();
	}

	public void testFetchEagerAndFilter() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		TestCourse test = new TestCourse();

		LocalizedString title = new LocalizedString( "title in english" );
		title.getVariations().put( Locale.FRENCH.getLanguage(), "title en francais" );
		test.setTitle( title );
		s.save( test );

		s.flush();
		s.clear();

		Filter filter = s.enableFilter( "selectedLocale" );
		filter.setParameter( "param", "fr" );

		Query q = s.createQuery( "from TestCourse t" );
		List l = q.list();
		assertEquals( 1, l.size() );

		TestCourse t = (TestCourse) s.get( TestCourse.class, test.getTestCourseId() );
		assertEquals( 1, t.getTitle().getVariations().size() );

		tx.rollback();

		s.close();
	}

	public void testMapKeyType() throws Exception {
		Matrix m = new Matrix();
		m.getMvalues().put( 1, 1.1f );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( m );
		s.flush();
		s.clear();
		m = (Matrix) s.get( Matrix.class, m.getId() );
		assertEquals( 1.1f, m.getMvalues().get( 1 ) );
		tx.rollback();
		s.close();
	}

	public void testDefaultValueColumnForBasic() throws Exception {
		isDefaultValueCollectionColumnPresent( Boy.class.getName(), "hatedNames" );
		isDefaultValueCollectionColumnPresent( Boy.class.getName(), "preferredNames" );
		isCollectionColumnPresent( Boy.class.getName(), "nickNames", "element" );
		isDefaultValueCollectionColumnPresent( Boy.class.getName(), "scorePerPreferredName");
	}

	public void testDefaultFKNameForElementCollection() throws Exception {
		isCollectionColumnPresent( Boy.class.getName(), "hatedNames", "Boy_id" );
	}

	private void isLegacyValueCollectionColumnPresent(String collectionHolder, String propertyName) {

	}

	private void isDefaultValueCollectionColumnPresent(String collectionOwner, String propertyName) {
		isCollectionColumnPresent( collectionOwner, propertyName, propertyName );
	}

	private void isCollectionColumnPresent(String collectionOwner, String propertyName, String columnName) {
		final Collection collection = getCfg().getCollectionMapping( collectionOwner + "." + propertyName );
		final Iterator columnIterator = collection.getCollectionTable().getColumnIterator();
		boolean hasDefault = false;
		while ( columnIterator.hasNext() ) {
			Column column = (Column) columnIterator.next();
			if ( columnName.equals( column.getName() ) ) hasDefault = true;
		}
		assertTrue( "Could not find " + columnName, hasDefault );
	}

	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Boy.class,
				Country.class,
				TestCourse.class,
				Matrix.class
		};
	}
}
