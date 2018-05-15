/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.compositeusertype.nested;

import java.io.Serializable;
import java.util.Objects;

public class Line implements Serializable {
	private static final long serialVersionUID = 1L;

	private final Point p1, p2;

	public Line(Point p1, Point p2) {
		this.p1 = Objects.requireNonNull( p1 );
		this.p2 = Objects.requireNonNull( p2 );
	}

	public Point getP1() {
		return p1;
	}

	public Point getP2() {
		return p2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Line line = (Line) o;
		return Objects.equals( p1, line.p1 ) &&
				Objects.equals( p2, line.p2 );
	}

	@Override
	public int hashCode() {
		return Objects.hash( p1, p2 );
	}

	@Override
	public String toString() {
		return "Line{" +
				"p1=" + p1 +
				", p2=" + p2 +
				'}';
	}
}
