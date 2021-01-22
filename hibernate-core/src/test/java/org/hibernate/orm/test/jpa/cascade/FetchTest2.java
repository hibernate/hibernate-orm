/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.cascade;

import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@Jpa(annotatedClasses = {
		Troop2.class,
		Soldier2.class
})
public class FetchTest2 {
	@Test
	public void testProxyTransientStuff(EntityManagerFactoryScope scope) {
		Troop2 disney = new Troop2();
		disney.setName( "Disney" );

		Soldier2 mickey = new Soldier2();
		mickey.setName( "Mickey" );
		mickey.setTroop( disney );

		scope.inTransaction(
				entityManager -> {
					entityManager.persist( disney );
					entityManager.persist( mickey );
				}
		);

		scope.inTransaction(
				entityManager -> {
					Soldier2 _soldier = entityManager.find( Soldier2.class, mickey.getId() );
					_soldier.getTroop().getId();
					try {
						entityManager.flush();
					}
					catch (IllegalStateException e) {
						fail( "Should not raise an exception" );
					}
				}
		);

		scope.inTransaction(
				entityManager -> {
					//load troop wo a proxy
					Troop2 _troop = entityManager.find( Troop2.class, disney.getId() );
					Soldier2 _soldier = entityManager.find( Soldier2.class, mickey.getId() );

					try {
						entityManager.flush();
					}
					catch (IllegalStateException e) {
						fail( "Should not raise an exception" );
					}
					entityManager.remove( _troop );
					entityManager.remove( _soldier );
				}
		);
	}
}
