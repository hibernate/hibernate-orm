/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.event.collection.detached;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-7928")
@DomainModel(
		annotatedClasses = {
				Character.class, Alias.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.IMPLICIT_NAMING_STRATEGY, value = "legacy-jpa"),
				@Setting(name = Environment.DEFAULT_LIST_SEMANTICS, value = "bag"), // CollectionClassification.BAG
		}
)
@SessionFactory
public class BadMergeHandlingTest {

	@Test
	@JiraKey(value = "HHH-7928")
	public void testMergeAndHold(SessionFactoryScope scope) {
		Character paul = new Character( 1, "Paul Atreides" );
		Character paulo = new Character( 2, "Paulo Atreides" );
		Alias alias1 = new Alias( 1, "Paul Muad'Dib" );
		Alias alias2 = new Alias( 2, "Usul" );
		Alias alias3 = new Alias( 3, "The Preacher" );

		scope.inTransaction( s -> {
			s.persist( paul );
			s.persist( paulo );
			s.persist( alias1 );
			s.persist( alias2 );
			s.persist( alias3 );

		} );

		// set up relationships
		scope.inTransaction( s -> {

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
		} );

		// now try to read them back (I guess)
		scope.inTransaction( s -> {
			List results = s.createQuery( "select c from Character c join c.aliases a where a.alias = :aParam" )
					.setParameter( "aParam", "Usul" )
					.list();
			assertEquals( 2, results.size() );
		} );
	}

}
