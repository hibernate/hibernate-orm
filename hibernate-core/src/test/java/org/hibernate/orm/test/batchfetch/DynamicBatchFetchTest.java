/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.batchfetch;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = { A.class, B.class }
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false")
)
public class DynamicBatchFetchTest {
	private static int currentId = 1;

	@Test
	public void testDynamicBatchFetch(SessionFactoryScope scope) {
		Integer aId1 = createAAndB( "foo_1", scope );
		Integer aId2 = createAAndB( "foo_2", scope );

		scope.inTransaction(
				session -> {
					List resultList = session.createQuery( "from A where id in (" + aId1 + "," + aId2 + ") order by id" )
							.list();
					A a1 = (A) resultList.get( 0 );
					A a2 = (A) resultList.get( 1 );
					assertEquals( aId1, a1.getId() );
					assertEquals( aId2, a2.getId() );
					assertFalse( Hibernate.isInitialized( a1.getB() ) );
					assertFalse( Hibernate.isInitialized( a2.getB() ) );
					B b = a1.getB();
					assertFalse( Hibernate.isInitialized( b ) );
					assertEquals( "foo_1", b.getOtherProperty() );
					assertTrue( Hibernate.isInitialized( a1.getB() ) );
					assertTrue( Hibernate.isInitialized( a2.getB() ) );
					assertEquals( "foo_2", a2.getB().getOtherProperty() );
				}
		);
	}

	private int createAAndB(String otherProperty, SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					B b = new B();
					b.setIdPart1( currentId );
					b.setIdPart2( currentId );
					b.setOtherProperty( otherProperty );
					session.save( b );

					A a = new A();
					a.setId( currentId );
					a.setB( b );

					session.save( a );
				}
		);

		currentId++;

		return currentId - 1;
	}
}
