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
package org.hibernate.test.annotations.tuplizer;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
public class TuplizerTest extends BaseCoreFunctionalTestCase {
	@Test
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

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Cuisine.class, Country.class };
	}
}
