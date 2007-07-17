package org.hibernate.test.component.cascading.collection;

import java.util.Iterator;
import java.util.Locale;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class CascadeToComponentCollectionTest extends FunctionalTestCase {

	public CascadeToComponentCollectionTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] { "component/cascading/collection/Mappings.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CascadeToComponentCollectionTest.class );
	}

	public void testMerging() {
		// step1, we create a definition with one value
		Session session = openSession();
		session.beginTransaction();
		Definition definition = new Definition();
		Value value1 = new Value( definition );
		value1.getLocalizedStrings().addString( new Locale( "en_US" ), "hello" );
		session.persist( definition );
		session.getTransaction().commit();
		session.close();

		// step2, we verify that the definition has one value; then we detach it
		session = openSession();
		session.beginTransaction();
		definition = ( Definition ) session.get( Definition.class, definition.getId() );
		assertEquals( 1, definition.getValues().size() );
		session.getTransaction().commit();
		session.close();

		// step3, we add a new value during detachment
		Value value2 = new Value( definition );
		value2.getLocalizedStrings().addString( new Locale( "es" ), "hola" );

		// step4 we merge the definition
		session = openSession();
		session.beginTransaction();
		session.merge( definition );
		session.getTransaction().commit();
		session.close();

		// step5, final test
		session = openSession();
		session.beginTransaction();
		definition = ( Definition ) session.get( Definition.class, definition.getId() );
		assertEquals( 2, definition.getValues().size() );
		Iterator values = definition.getValues().iterator();
		while ( values.hasNext() ) {
			assertEquals( 1, ( ( Value ) values.next() ).getLocalizedStrings().getStringsCopy().size() );
		}
		session.getTransaction().commit();
		session.close();
	}

	public void testMergingOriginallyNullComponent() {
		// step1, we create a definition with one value, but with a null component
		Session session = openSession();
		session.beginTransaction();
		Definition definition = new Definition();
		Value value1 = new Value( definition );
		session.persist( definition );
		session.getTransaction().commit();
		session.close();

		// step2, we verify that the definition has one value; then we detach it
		session = openSession();
		session.beginTransaction();
		definition = ( Definition ) session.get( Definition.class, definition.getId() );
		assertEquals( 1, definition.getValues().size() );
		session.getTransaction().commit();
		session.close();

		// step3, we add a new value during detachment
		( ( Value ) definition.getValues().iterator().next() ).getLocalizedStrings().addString( new Locale( "en_US" ), "hello" );
		Value value2 = new Value( definition );
		value2.getLocalizedStrings().addString( new Locale( "es" ), "hola" );

		// step4 we merge the definition
		session = openSession();
		session.beginTransaction();
		session.merge( definition );
		session.getTransaction().commit();
		session.close();

		// step5, final test
		session = openSession();
		session.beginTransaction();
		definition = ( Definition ) session.get( Definition.class, definition.getId() );
		assertEquals( 2, definition.getValues().size() );
		Iterator values = definition.getValues().iterator();
		while ( values.hasNext() ) {
			assertEquals( 1, ( ( Value ) values.next() ).getLocalizedStrings().getStringsCopy().size() );
		}
		session.getTransaction().commit();
		session.close();
	}
}
