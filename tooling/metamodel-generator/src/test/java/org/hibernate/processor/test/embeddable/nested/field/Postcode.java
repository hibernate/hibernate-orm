/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.embeddable.nested.field;

import jakarta.persistence.Embeddable;

import java.util.Objects;


@Embeddable
public final class Postcode {
	private String zip;
	private String plusFour;

	public String getZip() {
		return zip;
	}

	public String getPlusFour() {
		return plusFour;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (Postcode) obj;
		return Objects.equals(this.zip, that.zip) &&
				Objects.equals(this.plusFour, that.plusFour);
	}

	@Override
	public int hashCode() {
		return Objects.hash(zip, plusFour);
	}

	@Override
	public String toString() {
		return "Postcode[" +
				"zip=" + zip + ", " +
				"plusFour=" + plusFour + ']';
	}

}
