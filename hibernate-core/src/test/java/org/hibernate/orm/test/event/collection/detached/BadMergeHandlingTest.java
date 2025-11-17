/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.detached;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.cfg.Configuration;
import org.hibernate.metamodel.CollectionClassification;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-7928" )
public class BadMergeHandlingTest extends BaseCoreFunctionalTestCase {
	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setImplicitNamingStrategy( ImplicitNamingStrategyLegacyJpaImpl.INSTANCE );
		configuration.setProperty( DEFAULT_LIST_SEMANTICS, CollectionClassification.BAG );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Character.class, Alias.class };
	}

	@Test
	@JiraKey( value = "HHH-7928" )
	public void testMergeAndHold() {
		Session s = openSession();
		s.beginTransaction();

		Character paul = new Character( 1, "Paul Atreides" );
		s.persist( paul );
		Character paulo = new Character( 2, "Paulo Atreides" );
		s.persist( paulo );

		Alias alias1 = new Alias( 1, "Paul Muad'Dib" );
		s.persist( alias1 );

		Alias alias2 = new Alias( 2, "Usul" );
		s.persist( alias2 );

		Alias alias3 = new Alias( 3, "The Preacher" );
		s.persist( alias3 );

		s.getTransaction().commit();
		s.close();

		// set up relationships
		s = openSession();
		s.beginTransaction();

		// customer 1
		alias1.getCharacters().add( paul );
		s.merge( alias1 );
		alias2.getCharacters().add( paul );
		s.merge( alias2 );
		alias3.getCharacters().add( paul );
		s.merge( alias3 );

		s.flush();

		// customer 2
		alias1.getCharacters().add( paulo );
		s.merge( alias1 );
		alias2.getCharacters().add( paulo );
		s.merge( alias2 );
		alias3.getCharacters().add( paulo );
		s.merge( alias3 );
		s.flush();

		s.getTransaction().commit();
		s.close();

		// now try to read them back (I guess)
		s = openSession();
		s.beginTransaction();
		List results = s.createQuery( "select c from Character c join c.aliases a where a.alias = :aParam" )
				.setParameter( "aParam", "Usul" )
				.list();
		assertEquals( 2, results.size() );
		s.getTransaction().commit();
		s.close();
	}

}
