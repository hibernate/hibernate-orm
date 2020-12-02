/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.cascade;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@DomainModel(annotatedClasses = {
		Troop2.class,
		Soldier2.class
})
@SessionFactory
public class FetchTest2 {
	@Test
	public void testProxyTransientStuff(SessionFactoryScope scope) {
		Troop2 disney = new Troop2();
		disney.setName( "Disney" );

		Soldier2 mickey = new Soldier2();
		mickey.setName( "Mickey" );
		mickey.setTroop( disney );

		scope.inTransaction(
				session -> {
					session.persist( disney );
					session.persist( mickey );
				}
		);

		scope.inTransaction(
				session -> {
					Soldier2 _soldier = session.find( Soldier2.class, mickey.getId() );
					_soldier.getTroop().getId();
					try {
						session.flush();
					}
					catch (IllegalStateException e) {
						fail( "Should not raise an exception" );
					}
				}
		);

		scope.inTransaction(
				session -> {
					//load troop wo a proxy
					Troop2 _troop = session.find( Troop2.class, disney.getId() );
					Soldier2 _soldier = session.find( Soldier2.class, mickey.getId() );

					try {
						session.flush();
					}
					catch (IllegalStateException e) {
						fail( "Should not raise an exception" );
					}
					session.remove( _troop );
					session.remove( _soldier );
				}
		);
	}
}
