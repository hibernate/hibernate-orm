/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.ChangesetEntity;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Extension of {@link RevisionMapping} that tracks which
 * entity types were modified in each revision. The entity names
 * are stored in a {@code REVCHANGES} table as an
 * {@link ElementCollection @ElementCollection}.
 * <p>
 * When a revision entity extends this class (or has a property
 * annotated with {@link ChangesetEntity.ModifiedEntities @ModifiedEntities}),
 * cross-type revision queries are automatically enabled via
 * {@link AuditLog#getEntityTypesModifiedAt},
 * {@link AuditLog#findAllEntitiesModifiedAt}, and
 * {@link AuditLog#findAllEntitiesGroupedByModificationType}.
 * <p>
 * Extend this class to create a custom tracking revision entity,
 * or use the ready-made {@link DefaultTrackingModifiedEntitiesRevisionEntity}.
 *
 * @author Marco Belladelli
 * @see DefaultTrackingModifiedEntitiesRevisionEntity
 * @see ChangesetEntity.ModifiedEntities
 * @since 7.4
 */
@MappedSuperclass
public class TrackingModifiedEntitiesRevisionMapping extends RevisionMapping {
	@ElementCollection(fetch = FetchType.EAGER)
	@JoinTable(name = "REVCHANGES", joinColumns = @JoinColumn(name = "REV"))
	@Column(name = "ENTITYNAME")
	@Fetch(FetchMode.JOIN)
	@ChangesetEntity.ModifiedEntities
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
		if ( !(o instanceof TrackingModifiedEntitiesRevisionMapping that) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}
		return Objects.equals( modifiedEntityNames, that.modifiedEntityNames );
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (modifiedEntityNames != null ? modifiedEntityNames.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "TrackingModifiedEntitiesRevisionMapping(" + super.toString()
			+ ", modifiedEntityNames = " + modifiedEntityNames + ")";
	}
}
