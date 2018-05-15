/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
@PrimaryKeyJoinColumn(name = "BOAT_ID")
public class AmericaCupClass extends Boat {
	private Country country;

	@ManyToOne()
	@JoinColumn(name = "COUNTRY_ID")
	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}
}
