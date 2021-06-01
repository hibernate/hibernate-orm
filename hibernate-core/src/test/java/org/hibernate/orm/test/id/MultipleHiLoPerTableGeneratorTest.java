/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.id;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/id/Car.hbm.xml",
				"org/hibernate/orm/test/id/Plane.hbm.xml",
				"org/hibernate/orm/test/id/Radio.hbm.xml"
		}
)
@SessionFactory
public class MultipleHiLoPerTableGeneratorTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Car" ).executeUpdate();
					session.createQuery( "delete from Plane" ).executeUpdate();
					session.createQuery( "delete from Radio" ).executeUpdate();
				}
		);
	}

	@Test
	public void testDistinctId(SessionFactoryScope scope) {
		int testLength = 8;
		Car[] cars = new Car[testLength];
		scope.inTransaction(
				session -> {
					Plane[] planes = new Plane[testLength];
					for ( int i = 0; i < testLength; i++ ) {
						cars[i] = new Car();
						cars[i].setColor( "Color" + i );
						planes[i] = new Plane();
						planes[i].setNbrOfSeats( i );
						session.persist( cars[i] );
						//s.persist(planes[i]);
					}
				}
		);

		for ( int i = 0; i < testLength; i++ ) {
			assertEquals( i + 1, cars[i].getId().intValue() );
			//assertEquals(i+1, planes[i].getId().intValue());
		}
	}

	@Test
	public void testAllParams(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Radio radio = new Radio();
					radio.setFrequency( "32 MHz" );
					session.persist( radio );
					assertEquals( new Integer( 1 ), radio.getId() );
					radio = new Radio();
					radio.setFrequency( "32 MHz" );
					session.persist( radio );
					assertEquals( new Integer( 2 ), radio.getId() );
				}
		);
	}
}
