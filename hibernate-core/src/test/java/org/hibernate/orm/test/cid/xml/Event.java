/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cid.xml;

/**
 * Entity whose composite identifier is populated by a generator declared on the
 * {@code <embedded-id>} XML mapping element.
 */
public class Event {
	private EventId id;
	private String name;

	public EventId getId() {
		return id;
	}

	public void setId(EventId id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
