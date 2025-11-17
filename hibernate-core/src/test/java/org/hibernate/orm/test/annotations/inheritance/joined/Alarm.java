/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.joined;

import java.time.Instant;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "alarms")
@PrimaryKeyJoinColumn(name = "alarm_event_fk")
@DiscriminatorValue("alarm")
public class Alarm extends EventInformation {
	private Instant notification;
	protected EventInformation eventInfo;

	public Instant getNotification() {
		return notification;
	}

	public void setNotification(Instant notification) {
		this.notification = notification;
	}

	@OneToOne
	@JoinColumn(name = "event_info_fk")
	public EventInformation getEventInfo() {
		return eventInfo;
	}

	public void setEventInfo(EventInformation value) {
		this.eventInfo = value;
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String eventId = ( getEventInfo() != null ?
				getEventInfo().getNotificationId() : null );
		sb.append(
				"AlarmT: id = " + getNotificationId() + "\t" +
						"has event id = " + eventId
		);
		return sb.toString();
	}

}
