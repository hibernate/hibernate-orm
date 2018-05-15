/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.component.cascading.collection;

import java.util.Locale;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class CascadeToComponentCollectionTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "component/cascading/collection/Mappings.hbm.xml" };
	}

	@Test
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
		for ( Object o : definition.getValues() ) {
			assertEquals( 1, ((Value) o).getLocalizedStrings().getStringsCopy().size() );
		}
		session.getTransaction().commit();
		session.close();
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	@Test
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
		for ( Object o : definition.getValues() ) {
			assertEquals( 1, ((Value) o).getLocalizedStrings().getStringsCopy().size() );
		}
		session.getTransaction().commit();
		session.close();
	}
}
