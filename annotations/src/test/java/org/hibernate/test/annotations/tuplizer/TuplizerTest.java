//$Id$
package org.hibernate.test.annotations.tuplizer;

import org.hibernate.test.annotations.TestCase;
import org.hibernate.Session;

/**
 * @author Emmanuel Bernard
 */
public class TuplizerTest extends TestCase {
	public void testEntityTuplizer() throws Exception {
		Cuisine cuisine = ProxyHelper.newCuisineProxy( null );
		cuisine.setName( "Francaise" );
		Country country = ProxyHelper.newCountryProxy( null );
		country.setName( "France" );
		cuisine.setCountry( country );
		Session s = openSession( new EntityNameInterceptor() );
		s.getTransaction().begin();
		s.persist( cuisine );
		s.flush();
		s.clear();
		cuisine = (Cuisine) s.get(Cuisine.class, cuisine.getId() );
		assertNotNull( cuisine );
		assertEquals( "Francaise", cuisine.getName() );
		assertEquals( "France", country.getName() );
		s.getTransaction().rollback();
		s.close();
	}
	protected Class[] getMappings() {
		return new Class[] {
				Cuisine.class
		};
	}
}
