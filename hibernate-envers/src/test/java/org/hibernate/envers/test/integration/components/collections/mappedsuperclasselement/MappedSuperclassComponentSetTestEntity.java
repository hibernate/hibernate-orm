/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.test.integration.components.collections.mappedsuperclasselement;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

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