/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.boot;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/**
* @author Jan Schatteman
*/
@Entity(name = "Event")
public class Event {
	@Id
	@OneToOne
	private EventId id;

	public EventId getId() {
		return id;
	}

	public void setId(EventId id) {
		this.id = id;
	}
}
