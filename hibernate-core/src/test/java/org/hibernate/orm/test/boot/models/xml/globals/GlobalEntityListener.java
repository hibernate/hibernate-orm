/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.boot.models.xml.globals;

/**
 * JPA entity listener
 *
 * @author Steve Ebersole
 */
public class GlobalEntityListener {
	public void entityCreated(Object entity) {
		System.out.println( "Entity was created - " + entity );
	}

	public Object entityCreated() {
		throw new RuntimeException( "Should not be called" );
	}

	public void entityCreated(Object e1, Object e2) {
		throw new RuntimeException( "Should not be called" );
	}
}
