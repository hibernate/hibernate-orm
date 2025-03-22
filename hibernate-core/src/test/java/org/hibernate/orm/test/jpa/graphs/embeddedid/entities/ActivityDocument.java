/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs.embeddedid.entities;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "activity_documents")
public class ActivityDocument {

	@EmbeddedId
	private ActivityDocumentId activityDocumentId;

	private String name;

	public ActivityDocument setActivityDocumentId(ActivityDocumentId activityDocumentId) {
		this.activityDocumentId = activityDocumentId;
		return this;
	}

	public ActivityDocument setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ActivityDocument that = (ActivityDocument) o;
		return Objects.equals( activityDocumentId, that.activityDocumentId );
	}

	@Override
	public int hashCode() {
		return Objects.hashCode( activityDocumentId );
	}
}
