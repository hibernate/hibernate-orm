//$Id$
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
