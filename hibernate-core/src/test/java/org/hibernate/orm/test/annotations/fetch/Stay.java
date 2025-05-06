/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fetch;

import java.io.Serializable;
import java.util.Date;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "Stay")
public class Stay implements Serializable {

	// member declaration
	private int id;
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

	public Stay(int id) {
		this.id = id;
	}

	public Stay(Person person, Date startDate, Date endDate, String vessel, String authoriser, String comments) {
		this.authoriser = authoriser;
		this.endDate = endDate;
		this.person = person;
		this.startDate = startDate;
		this.vessel = vessel;
		this.comments = comments;
	}


	// properties
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

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	@Id
	@GeneratedValue
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "person")
	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "oldperson")
	public Person getOldPerson() {
		return oldPerson;
	}

	public void setOldPerson(Person oldPerson) {
		this.oldPerson = oldPerson;
	}

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@Fetch(FetchMode.JOIN)
	@JoinColumn(name = "veryoldperson")
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

	public String getVessel() {
		return vessel;
	}

	public void setVessel(String vessel) {
		this.vessel = vessel;
	}


}
