/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.fetch;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Copied over from annotations test suite...
 *
 * @author Emmanuel Bernard
 */
public class Person implements Serializable {

	// member declaration
	private Long id;
	private String firstName;
	private String lastName;
	private String companyName;
	private Collection stays;
	private Collection oldStays;
	private Collection veryOldStays;

	// constructors
	public Person() {
	}

	public Person(String firstName, String lastName, String companyName) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.companyName = companyName;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public Collection getStays() {
		return stays;
	}

	public void setStays(Collection stays) {
		this.stays = stays;
	}

	public Collection getOldStays() {
		return oldStays;
	}

	public void setOldStays(Collection oldStays) {
		this.oldStays = oldStays;
	}

	public Collection getVeryOldStays() {
		return veryOldStays;
	}

	public void setVeryOldStays(Collection veryOldStays) {
		this.veryOldStays = veryOldStays;
	}


	// business logic
	public void addStay(Date startDate, Date endDate, String vessel, String authoriser, String comments) {
		Stay stay = new Stay( this, startDate, endDate, vessel, authoriser, comments );
		addStay( stay );
	}

	public void addStay(Stay stay) {
		Collection stays = getStays();
		if ( stays == null ) {
			stays = new ArrayList();
		}
		stays.add( stay );

		this.stays = stays;
	}

	public void addOldStay(Date startDate, Date endDate, String vessel, String authoriser, String comments) {
		Stay stay = new Stay( this, startDate, endDate, vessel, authoriser, comments );
		addOldStay( stay );
	}

	public void addOldStay(Stay stay) {
		Collection stays = getOldStays();
		if ( stays == null ) {
			stays = new ArrayList();
		}
		stays.add( stay );

		this.oldStays = stays;
	}

	public void addVeryOldStay(Date startDate, Date endDate, String vessel, String authoriser, String comments) {
		Stay stay = new Stay( this, startDate, endDate, vessel, authoriser, comments );
		addVeryOldStay( stay );
	}

	public void addVeryOldStay(Stay stay) {
		Collection stays = getVeryOldStays();
		if ( stays == null ) {
			stays = new ArrayList();
		}
		stays.add( stay );

		this.veryOldStays = stays;
	}
}
