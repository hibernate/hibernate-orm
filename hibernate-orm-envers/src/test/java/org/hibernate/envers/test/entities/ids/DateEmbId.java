/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.ids;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Embeddable;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Embeddable
public class DateEmbId implements Serializable {
	private Date x;
	private Date y;

	public DateEmbId() {
	}

	public DateEmbId(Date x, Date y) {
		this.x = x;
		this.y = y;
	}

	public Date getX() {
		return x;
	}

	public void setX(Date x) {
		this.x = x;
	}

	public Date getY() {
		return y;
	}

	public void setY(Date y) {
		this.y = y;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof DateEmbId) ) {
			return false;
		}

		DateEmbId embId = (DateEmbId) o;

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
		return "DateEmbId(" + x + ", " + y + ")";
	}
}