package org.hibernate.test.annotations.any;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

public class AnyTest extends TestCase {

	public void testDefaultAnyAssociation() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		PropertySet set1 = new PropertySet( "string" );
		Property property = new StringProperty( "name", "Alex" );
		set1.setSomeProperty( property );
		set1.addGeneratedProperty( property );
		s.save( set1 );

		PropertySet set2 = new PropertySet( "integer" );
		property = new IntegerProperty( "age", 33 );
		set2.setSomeProperty( property );
		set2.addGeneratedProperty( property );
		s.save( set2 );

		s.flush();
		s.clear();

		Query q = s
				.createQuery( "select s from PropertySet s where name = :name" );
		q.setString( "name", "string" );
		PropertySet result = (PropertySet) q.uniqueResult();

		assertNotNull( result );
		assertNotNull( result.getSomeProperty() );
		assertTrue( result.getSomeProperty() instanceof StringProperty );
		assertEquals( "Alex", result.getSomeProperty().asString() );
		assertNotNull( result.getGeneralProperties() );
		assertEquals( 1, result.getGeneralProperties().size() );
		assertEquals( "Alex", result.getGeneralProperties().get( 0 ).asString() );

		q.setString( "name", "integer" );
		result = (PropertySet) q.uniqueResult();
		assertNotNull( result );
		assertNotNull( result.getSomeProperty() );
		assertTrue( result.getSomeProperty() instanceof IntegerProperty );
		assertEquals( "33", result.getSomeProperty().asString() );
		assertNotNull( result.getGeneralProperties() );
		assertEquals( 1, result.getGeneralProperties().size() );
		assertEquals( "33", result.getGeneralProperties().get( 0 ).asString() );

		t.rollback();
		s.close();
	}

	public void testManyToAnyWithMap() throws Exception {

		Session s = openSession();
		Transaction t = s.beginTransaction();

		PropertyMap map = new PropertyMap( "sample" );
		map.getProperties().put( "name", new StringProperty( "name", "Alex" ) );
		map.getProperties().put( "age", new IntegerProperty( "age", 33 ) );

		s.save( map );

		s.flush();
		s.clear();

		Query q = s
				.createQuery( "SELECT map FROM PropertyMap map WHERE map.name = :name" );
		q.setString( "name", "sample" );
		PropertyMap actualMap = (PropertyMap) q.uniqueResult();

		assertNotNull( actualMap );
		assertNotNull( actualMap.getProperties() );

		Property property = actualMap.getProperties().get( "name" );
		assertNotNull( property );
		assertTrue( property instanceof StringProperty );
		assertEquals( "Alex", property.asString() );

		property = actualMap.getProperties().get( "age" );
		assertNotNull( property );
		assertTrue( property instanceof IntegerProperty );
		assertEquals( "33", property.asString() );

		t.rollback();
		s.close();

	}

	public void testMetaDataUseWithManyToAny() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		PropertyList list = new PropertyList( "sample" );
		StringProperty stringProperty = new StringProperty( "name", "Alex" );
		IntegerProperty integerProperty = new IntegerProperty( "age", 33 );
		LongProperty longProperty = new LongProperty( "distance", 121L );
		CharProperty charProp = new CharProperty( "Est", 'E' );

		list.setSomeProperty( longProperty );

		list.addGeneratedProperty( stringProperty );
		list.addGeneratedProperty( integerProperty );
		list.addGeneratedProperty( longProperty );
		list.addGeneratedProperty( charProp );

		s.save( list );

		s.flush();
		s.clear();

		Query q = s
				.createQuery( "SELECT list FROM PropertyList list WHERE list.name = :name" );
		q.setString( "name", "sample" );
		PropertyList<Property> actualList = (PropertyList<Property>) q
				.uniqueResult();

		assertNotNull( actualList );
		assertNotNull( actualList.getGeneralProperties() );
		assertEquals( 4, actualList.getGeneralProperties().size() );

		Property property = actualList.getSomeProperty();
		assertNotNull( property );
		assertTrue( property instanceof LongProperty );
		assertEquals( "121", property.asString() );

		assertEquals( "Alex", actualList.getGeneralProperties().get( 0 )
				.asString() );
		assertEquals( "33", actualList.getGeneralProperties().get( 1 ).asString() );
		assertEquals( "121", actualList.getGeneralProperties().get( 2 ).asString() );
		assertEquals( "E", actualList.getGeneralProperties().get( 3 ).asString() );

		t.rollback();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				StringProperty.class,
				IntegerProperty.class,
				LongProperty.class,
				PropertySet.class,
				PropertyMap.class,
				PropertyList.class,
				CharProperty.class
		};
	}

	protected String[] getAnnotatedPackages() {
		return new String[] {
				"org.hibernate.test.annotations.any"
		};
	}
}
