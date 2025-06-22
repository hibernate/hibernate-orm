/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.collections.mappedsuperclasselement;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table( name = "SupCompSetTestEntity")
@Audited
public class MappedSuperclassComponentSetTestEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@ElementCollection
	@CollectionTable(name = "MCompTestEntityComps", joinColumns = @JoinColumn(name = "entity_id"))
	private Set<Code> comps = new HashSet<Code>();

	@NotAudited
	@ElementCollection
	@CollectionTable(name = "MCompTestEntityCompsNotAudited", joinColumns = @JoinColumn(name = "entity_id"))
	private Set<Code> compsNotAudited = new HashSet<Code>();

	public MappedSuperclassComponentSetTestEntity() {
	}

	public MappedSuperclassComponentSetTestEntity(Integer id) {
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<Code> getComps() {
		return comps;
	}

	public void setComps(Set<Code> comps) {
		this.comps = comps;
	}

	public Set<Code> getCompsNotAudited() {
		return compsNotAudited;
	}

	public void setCompsNotAudited(Set<Code> comps) {
		this.compsNotAudited = compsNotAudited;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof MappedSuperclassComponentSetTestEntity ) ) {
			return false;
		}

		MappedSuperclassComponentSetTestEntity that = (MappedSuperclassComponentSetTestEntity) o;

		if ( comps != null ? !comps.equals( that.comps ) : that.comps != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (comps != null ? comps.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ComponentSetTestEntity{" +
				"id=" + id +
				", comps=" + comps +
				'}';
	}
}
