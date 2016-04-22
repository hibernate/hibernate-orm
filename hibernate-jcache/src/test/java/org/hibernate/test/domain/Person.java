/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Person {

	private Long id;
	private int age;
	private String firstname;
	private String lastname;
	private List events = new ArrayList(); // list semantics, e.g., indexed
	private Set emailAddresses = new HashSet();
	private Set phoneNumbers = new HashSet();
	private List talismans = new ArrayList(); // a Bag of good-luck charms.

	public Person() {
		//
	}

	public List getEvents() {
		return events;
	}

	protected void setEvents(List events) {
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

	public Set getEmailAddresses() {
		return emailAddresses;
	}

	public void setEmailAddresses(Set emailAddresses) {
		this.emailAddresses = emailAddresses;
	}

	public Set getPhoneNumbers() {
		return phoneNumbers;
	}

	public void setPhoneNumbers(Set phoneNumbers) {
		this.phoneNumbers = phoneNumbers;
	}

	public void addTalisman(String name) {
		talismans.add(name);
	}

	public List getTalismans() {
		return talismans;
	}

	public void setTalismans(List talismans) {
		this.talismans = talismans;
	}

	public String toString() {
		return getFirstname() + " " + getLastname();
	}
}
