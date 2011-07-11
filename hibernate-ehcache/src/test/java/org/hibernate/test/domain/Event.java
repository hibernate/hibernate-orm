/**
 *  Copyright 2003-2010 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hibernate.test.domain;

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
