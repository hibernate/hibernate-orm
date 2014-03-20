/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.enhanced;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.ModifiedEntityNames;

/**
 * Extension of standard {@link SequenceIdRevisionEntity} that allows tracking entity names changed in each revision.
 * This revision entity is implicitly used when {@code org.hibernate.envers.track_entities_changed_in_revision}
 * parameter is set to {@code true}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@MappedSuperclass
public class SequenceIdTrackingModifiedEntitiesRevisionEntity extends SequenceIdRevisionEntity {
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "REVCHANGES", joinColumns = @JoinColumn(name = "REV"))
	@Column(name = "ENTITYNAME")
	@Fetch(FetchMode.JOIN)
	@ModifiedEntityNames
	private Set<String> modifiedEntityNames = new HashSet<String>();

	@SuppressWarnings("UnusedDeclaration")
	public Set<String> getModifiedEntityNames() {
		return modifiedEntityNames;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setModifiedEntityNames(Set<String> modifiedEntityNames) {
		this.modifiedEntityNames = modifiedEntityNames;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof SequenceIdTrackingModifiedEntitiesRevisionEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		final SequenceIdTrackingModifiedEntitiesRevisionEntity that = (SequenceIdTrackingModifiedEntitiesRevisionEntity) o;

		if ( modifiedEntityNames == null ) {
			return that.modifiedEntityNames == null;
		}
		else {
			return modifiedEntityNames.equals( that.modifiedEntityNames );
		}
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (modifiedEntityNames != null ? modifiedEntityNames.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "SequenceIdTrackingModifiedEntitiesRevisionEntity(" + super.toString()
				+ ", modifiedEntityNames = " + modifiedEntityNames + ")";
	}
}
