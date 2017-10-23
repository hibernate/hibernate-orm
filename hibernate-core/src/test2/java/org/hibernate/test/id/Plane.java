/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Plane.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.id;


/**
 * @author Emmanuel Bernard
 */
public class Plane {
	private Long id;
	private int nbrOfSeats;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getNbrOfSeats() {
		return nbrOfSeats;
	}

	public void setNbrOfSeats(int nbrOfSeats) {
		this.nbrOfSeats = nbrOfSeats;
	}
}
