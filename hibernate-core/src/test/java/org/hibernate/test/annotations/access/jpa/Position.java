//$Id: Being.java 18260 2009-12-17 21:14:07Z hardy.ferentschik $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
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
