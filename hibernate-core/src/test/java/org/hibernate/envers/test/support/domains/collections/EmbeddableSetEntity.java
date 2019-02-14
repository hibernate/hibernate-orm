/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.collections;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.support.domains.components.PartialAuditedComponent;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 * @author Chris Cranford
 */
@Entity
@Table(name = "EmbSetEnt")
@Audited
public class EmbeddableSetEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@ElementCollection
	@CollectionTable(name = "EmbSetEnt_set")
	private Set<PartialAuditedComponent> componentSet = new HashSet<PartialAuditedComponent>();

	public EmbeddableSetEntity() {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Set<PartialAuditedComponent> getComponentSet() {
		return componentSet;
	}

	public void setComponentSet(Set<PartialAuditedComponent> componentSet) {
		this.componentSet = componentSet;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		EmbeddableSetEntity that = (EmbeddableSetEntity) o;
		return Objects.equals( id, that.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id );
	}

	@Override
	public String toString() {
		return "EmbeddableSetEntity{" +
				"id=" + id +
				", componentSet=" + componentSet +
				'}';
	}
}