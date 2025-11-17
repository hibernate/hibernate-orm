/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tutorial.envers;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

@Entity
@Table(name = "Events")
@Audited  // <--- this tells Envers to audit (track changes to) this entity
public class Event {

	@Id
	@GeneratedValue
	private Long id;

	private String title;

	@Column(name = "eventDate")
	private LocalDateTime date;

	public Event() {
		// this form used by Hibernate
	}

	public Event(String title, LocalDateTime date) {
		// for application use, to create new events
		this.title = title;
		this.date = date;
	}

	public Long getId() {
		return id;
	}

	private void setId(Long id) {
		this.id = id;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public void setDate(LocalDateTime date) {
		this.date = date;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}
