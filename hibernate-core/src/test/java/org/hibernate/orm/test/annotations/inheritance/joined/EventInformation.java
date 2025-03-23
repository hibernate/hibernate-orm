/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.joined;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;


@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "events")
@DiscriminatorColumn(name = "event_type", discriminatorType = DiscriminatorType.STRING, length = 80)
@DiscriminatorValue("event")
public class EventInformation implements java.io.Serializable {
	protected String notificationId;
	private String name;

	@Id
	public String getNotificationId() {
		return notificationId;
	}

	public void setNotificationId(String value) {
		this.notificationId = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "EventInformation(" + getNotificationId() + ")";
	}

}
