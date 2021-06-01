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
				"org/hibernate/orm/test/id/Car.hbm.xml"
		}
)
@SessionFactory
public class MultipleHiLoPerTableGeneratorWithRollbackTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Car" ).executeUpdate();
				}
		);
	}


	@Test
	public void testRollingBack(SessionFactoryScope scope) {
		int testLength = 3;
		Long lastId = scope.fromSession(
				session -> {
					session.getTransaction().begin();
					try {
						Long id = null;
						for ( int i = 0; i < testLength; i++ ) {
							Car car = new Car();
							car.setColor( "color " + i );
							session.save( car );
							id = car.getId();
						}
						return id;
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);

		Car car = new Car();
		scope.inTransaction(
				session -> {
					car.setColor( "blue" );
					session.save( car );
					session.flush();
				}
		);

		assertEquals( lastId.longValue() + 1, car.getId().longValue(), "id generation was rolled back" );
	}

}
