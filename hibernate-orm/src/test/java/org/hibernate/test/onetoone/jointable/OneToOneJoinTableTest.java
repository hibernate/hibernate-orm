/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetoone.jointable;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;

/**
 * @author Christian Beikov
 */
public class OneToOneJoinTableTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Event.class,
				Message.class,
				ABlockableEntity.class,
				OtherEntity.class
		};
	}

	@Override
	protected void buildSessionFactory() {
		try {
			super.buildSessionFactory();
		}
		catch (Exception failureExpected) {
			//HHH-9188
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9188")
	@FailureExpected( jiraKey = "HHH-9188" )
	public void test() throws Exception {
		Long id = doInHibernate( this::sessionFactory, s -> {
			Event childEvent = new Event();
			childEvent.setDescription( "childEvent" );
			s.save( childEvent );

			Event parentEvent = new Event();
			parentEvent.setDescription( "parentEvent" );
			s.save( parentEvent );

			OtherEntity otherEntity = new OtherEntity();
			otherEntity.setId( "123" );
			s.save( otherEntity );

			childEvent.setOther( otherEntity );
			s.save( childEvent );
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
		doInHibernate( this::sessionFactory, s -> {
			Event saved = s.find( Event.class, id );
			assertNotNull( saved );
		} );
	}
}
