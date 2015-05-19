/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Being.java 18260 2009-12-17 21:14:07Z hardy.ferentschik $

package org.hibernate.test.annotations.access.jpa;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;


/**
 * @author Hardy Ferentschik
 */
@Embeddable
@Access(AccessType.FIELD)
public class Position {
	public int x;
	public int y;

	public Position() {
	}

	public Position(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		throw new RuntimeException( );
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		throw new RuntimeException( );
	}

	public void setY(int y) {
		this.y = y;
	}
}
