/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.HashSet;
import java.util.Set;

/**
 * Extension of standard {@link RevisionMapping} that allows tracking entity names changed in each revision.
 * This revision entity is implicitly used when {@code org.hibernate.envers.track_entities_changed_in_revision}
 * parameter is set to {@code true}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@MappedSuperclass
public class TrackingModifiedEntitiesRevisionMapping extends RevisionMapping {
	@ElementCollection(fetch = FetchType.EAGER)
	@JoinTable(name = "REVCHANGES", joinColumns = @JoinColumn(name = "REV"))
	@Column(name = "ENTITYNAME")
	@Fetch(FetchMode.JOIN)
	@ModifiedEntityNames
	private Set<String> modifiedEntityNames = new HashSet<>();

	public Set<String> getModifiedEntityNames() {
		return modifiedEntityNames;
	}

	public void setModifiedEntityNames(Set<String> modifiedEntityNames) {
		this.modifiedEntityNames = modifiedEntityNames;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof TrackingModifiedEntitiesRevisionMapping) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		final TrackingModifiedEntitiesRevisionMapping that = (TrackingModifiedEntitiesRevisionMapping) o;

		if ( modifiedEntityNames != null ? !modifiedEntityNames.equals( that.modifiedEntityNames )
				: that.modifiedEntityNames != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (modifiedEntityNames != null ? modifiedEntityNames.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "DefaultTrackingModifiedEntitiesRevisionEntity(" + super.toString() + ", modifiedEntityNames = " + modifiedEntityNames + ")";
	}
}
