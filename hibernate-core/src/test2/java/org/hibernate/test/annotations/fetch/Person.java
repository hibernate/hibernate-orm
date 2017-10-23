/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.fetch;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;


/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "Person")
public class Person implements Serializable {

	// member declaration
	private int id;
	private String firstName;
	private String lastName;
	private String companyName;
	private Collection<Stay> stays;
	private Collection<Stay> oldStays;
	private Collection<Stay> veryOldStays;
	private List<Stay> orderedStay = new ArrayList<Stay>();

	// constructors
	public Person() {
	}

	public Person(String firstName, String lastName, String companyName) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.companyName = companyName;
	}

	// properties
	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Id
	@GeneratedValue
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	// relationships

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "person")
	public Collection<Stay> getStays() {
		return this.stays;
	}

	public void setStays(List<Stay> stays) {
		this.stays = stays;
	}

	@OneToMany(cascade=CascadeType.ALL, mappedBy = "oldPerson")
	@LazyCollection(LazyCollectionOption.EXTRA)
	@Fetch(FetchMode.SUBSELECT)
	public Collection<Stay> getOldStays() {
		return oldStays;
	}

	public void setOldStays(Collection<Stay> oldStays) {
		this.oldStays = oldStays;
	}

	@OneToMany(cascade=CascadeType.ALL, mappedBy = "veryOldPerson")
	@Fetch(FetchMode.SELECT)
	public Collection<Stay> getVeryOldStays() {
		return veryOldStays;
	}

	public void setVeryOldStays(Collection<Stay> veryOldStays) {
		this.veryOldStays = veryOldStays;
	}

	@OneToMany(cascade=CascadeType.ALL)
	@LazyCollection(LazyCollectionOption.EXTRA)
	@Fetch(FetchMode.SUBSELECT)
	@OrderColumn(name="orderedStayIndex")
	public List<Stay> getOrderedStay() {
		return orderedStay;
	}

	public void setOrderedStay(List<Stay> orderedStay) {
		this.orderedStay = orderedStay;
	}


	// business logic
	public void addStay(Date startDate, Date endDate, String vessel, String authoriser, String comments) {
		Stay stay = new Stay( this, startDate, endDate, vessel, authoriser, comments );
		addStay( stay );
	}

	public void addStay(Stay stay) {
		Collection<Stay> stays = getStays();
		if ( stays == null ) {
			stays = new ArrayList<Stay>();
		}
		stays.add( stay );

		this.stays = stays;
	}

	public void addOldStay(Date startDate, Date endDate, String vessel, String authoriser, String comments) {
		Stay stay = new Stay( this, startDate, endDate, vessel, authoriser, comments );
		addOldStay( stay );
	}

	public void addOldStay(Stay stay) {
		Collection<Stay> stays = getOldStays();
		if ( stays == null ) {
			stays = new ArrayList<Stay>();
		}
		stays.add( stay );

		this.oldStays = stays;
	}

	public void addVeryOldStay(Date startDate, Date endDate, String vessel, String authoriser, String comments) {
		Stay stay = new Stay( this, startDate, endDate, vessel, authoriser, comments );
		addVeryOldStay( stay );
	}

	public void addVeryOldStay(Stay stay) {
		Collection<Stay> stays = getVeryOldStays();
		if ( stays == null ) {
			stays = new ArrayList<Stay>();
		}
		stays.add( stay );

		this.veryOldStays = stays;
	}
}
