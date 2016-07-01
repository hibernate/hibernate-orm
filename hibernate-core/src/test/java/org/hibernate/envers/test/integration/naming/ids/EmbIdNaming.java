/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.naming.ids;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Embeddable
public class EmbIdNaming implements Serializable {
	@Column(name = "XX")
	private Integer x;

	@Column(name = "YY")
	private Integer y;

	public EmbIdNaming() {
	}

	public EmbIdNaming(Integer x, Integer y) {
		this.x = x;
		this.y = y;
	}

	public Integer getX() {
		return x;
	}

	public void setX(Integer x) {
		this.x = x;
	}

	public Integer getY() {
		return y;
	}

	public void setY(Integer y) {
		this.y = y;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof EmbIdNaming) ) {
			return false;
		}

		EmbIdNaming embId = (EmbIdNaming) o;

		if ( x != null ? !x.equals( embId.x ) : embId.x != null ) {
			return false;
		}
		if ( y != null ? !y.equals( embId.y ) : embId.y != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (x != null ? x.hashCode() : 0);
		result = 31 * result + (y != null ? y.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "EmbIdNaming(" + x + ", " + y + ")";
	}
}