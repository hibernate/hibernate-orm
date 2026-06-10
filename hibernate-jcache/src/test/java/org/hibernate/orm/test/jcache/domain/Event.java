/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jcache.domain;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name = "EVENTS")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Event {
	@Id
	@GeneratedValue
	@Column(name = "EVENT_ID")
	private Long id;

	@Column(name = "title")
	private String title;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "EVENT_DATE")
	private Date date;

	@ManyToMany(mappedBy = "events", fetch = FetchType.EAGER, targetEntity = Person.class)
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	private Set<Person> participants = new HashSet<>();

	@ManyToOne
	@JoinColumn(name = "EVENT_ORGANIZER")
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

	public Set<Person> getParticipants() {
		return participants;
	}

	public void setParticipants(Set<Person> participants) {
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
