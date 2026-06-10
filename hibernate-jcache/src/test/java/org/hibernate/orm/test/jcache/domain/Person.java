/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jcache.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "PERSON")
public class Person {

	@Id
	@GeneratedValue
	@Column(name = "PERSON_ID")
	private Long id;

	private int age;
	private String firstname;
	private String lastname;

	@ManyToMany(targetEntity = Event.class, fetch = FetchType.LAZY)
	@JoinTable(
			name = "PERSON_EVENT",
			joinColumns = @JoinColumn(name = "PERSON_ID"),
			inverseJoinColumns = @JoinColumn(name = "EVENT_ID")
	)
	@OrderColumn(name = "EVENT_ORDER")
	private List<Event> events = new ArrayList<>(); // list semantics, e.g., indexed

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "PERSON_EMAIL_ADDR", joinColumns = @JoinColumn(name = "PERSON_ID"))
	@Column(name = "EMAIL_ADDR")
	private Set<String> emailAddresses = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, targetEntity = PhoneNumber.class)
	@JoinColumn(name = "PERSON_ID")
	private Set<PhoneNumber> phoneNumbers = new HashSet<>();

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "PERSON_TALISMAN", joinColumns = @JoinColumn(name = "PERSON_ID"))
	@Column(name = "TALISMAN_NAME")
	private List<String> talismans = new ArrayList<>(); // a Bag of good-luck charms.

	public Person() {
		//
	}

	public List<Event> getEvents() {
		return events;
	}

	protected void setEvents(List<Event> events) {
		this.events = events;
	}

	public void addToEvent(Event event) {
		this.getEvents().add(event);
		event.getParticipants().add(this);
	}

	public void removeFromEvent(Event event) {
		this.getEvents().remove(event);
		event.getParticipants().remove(this);
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public Set<String> getEmailAddresses() {
		return emailAddresses;
	}

	public void setEmailAddresses(Set<String> emailAddresses) {
		this.emailAddresses = emailAddresses;
	}

	public Set<PhoneNumber> getPhoneNumbers() {
		return phoneNumbers;
	}

	public void setPhoneNumbers(Set<PhoneNumber> phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
	}

	public void addTalisman(String name) {
		talismans.add(name);
	}

	public List<String> getTalismans() {
		return talismans;
	}

	public void setTalismans(List<String> talismans) {
		this.talismans = talismans;
	}

	public String toString() {
		return getFirstname() + " " + getLastname();
	}
}
