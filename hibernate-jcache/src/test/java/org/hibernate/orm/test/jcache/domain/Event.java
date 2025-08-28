/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jcache.domain;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class Event {
	private Long id;

	private String title;
	private Date date;
	private Set participants = new HashSet();
	private Person organizer;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setOrganizer(Person organizer) {
		this.organizer = organizer;
	}

	public Person getOrganizer() {
		return organizer;
	}

	public Set getParticipants() {
		return participants;
	}

	public void setParticipants(Set participants) {
		this.participants = participants;
	}

	public void addParticipant(Person person) {
		participants.add(person);
		person.getEvents().add(this);
	}

	public void removeParticipant(Person person) {
		participants.remove(person);
		person.getEvents().remove(this);
	}

	public String toString() {
		return getTitle() + ": " + getDate();
	}
}
