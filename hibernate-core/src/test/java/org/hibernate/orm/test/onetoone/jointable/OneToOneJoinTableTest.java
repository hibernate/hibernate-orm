/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetoone.jointable;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				Event.class,
				Message.class,
				ABlockableEntity.class,
				OtherEntity.class
		}
)
@SessionFactory
@FailureExpected(jiraKey = "HHH-9188")
public class OneToOneJoinTableTest {

	@Test
	@JiraKey(value = "HHH-9188")
	public void test(SessionFactoryScope scope) throws Exception {
		Long id = scope.fromTransaction( s -> {
			Event childEvent = new Event();
			childEvent.setDescription( "childEvent" );
			s.persist( childEvent );

			Event parentEvent = new Event();
			parentEvent.setDescription( "parentEvent" );
			s.persist( parentEvent );

			OtherEntity otherEntity = new OtherEntity();
			otherEntity.setId( "123" );
			s.persist( otherEntity );

			childEvent.setOther( otherEntity );
			s.persist( childEvent );
			s.flush();

			// Test updates and deletes
			childEvent.setOther( new OtherEntity( "456" ) );
			childEvent.setOther2( new Event( "randomEvent" ) );
			s.flush();

			s.remove( otherEntity );
			s.remove( parentEvent );

			s.createQuery( "DELETE FROM OtherEntity e WHERE e.id IS NULL" ).executeUpdate();
			s.createQuery( "DELETE FROM ABlockableEntity e WHERE e.description IS NULL" ).executeUpdate();
			s.createQuery( "DELETE FROM ABlockableEntity e WHERE e.other IS NULL AND e.description <> 'randomEvent'" )
					.executeUpdate();
			s.createQuery( "DELETE FROM Event e WHERE e.description IS NULL" ).executeUpdate();
			s.createQuery( "DELETE FROM Event e WHERE e.other IS NULL AND e.description <> 'randomEvent'" )
					.executeUpdate();

			s.createQuery( "UPDATE OtherEntity e SET id = 'test' WHERE e.id IS NULL" ).executeUpdate();
			s.createQuery( "UPDATE ABlockableEntity  e SET description = 'test' WHERE e.description IS NULL" )
					.executeUpdate();
			s.createQuery( "UPDATE ABlockableEntity e SET description = 'test' WHERE e.other IS NULL" ).executeUpdate();
			s.createQuery( "UPDATE Event e SET description = 'test' WHERE e.description IS NULL" ).executeUpdate();
			s.createQuery( "UPDATE Event e SET description = 'test' WHERE e.other IS NULL" ).executeUpdate();

			return childEvent.getId();
		} );
		scope.inTransaction( s -> {
			Event saved = s.find( Event.class, id );
			assertNotNull( saved );
		} );
	}
}
