/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.fetch;
import java.io.Serializable;
import java.util.Date;

/**
 * @author Emmanuel Bernard
 */
public class Stay implements Serializable {

	// member declaration
	private Long id;
	private Person person;
	private Person oldPerson;
	private Person veryOldPerson;
	private Date startDate;
	private Date endDate;
	private String vessel;
	private String authoriser;
	private String comments;


	// constructors
	public Stay() {
	}

	public Stay(Person person, Date startDate, Date endDate, String vessel, String authoriser, String comments) {
		this.authoriser = authoriser;
		this.endDate = endDate;
		this.person = person;
		this.startDate = startDate;
		this.vessel = vessel;
		this.comments = comments;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	public Person getOldPerson() {
		return oldPerson;
	}

	public void setOldPerson(Person oldPerson) {
		this.oldPerson = oldPerson;
	}

	public Person getVeryOldPerson() {
		return veryOldPerson;
	}

	public void setVeryOldPerson(Person veryOldPerson) {
		this.veryOldPerson = veryOldPerson;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getVessel() {
		return vessel;
	}

	public void setVessel(String vessel) {
		this.vessel = vessel;
	}

	public String getAuthoriser() {
		return authoriser;
	}

	public void setAuthoriser(String authoriser) {
		this.authoriser = authoriser;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}
}
