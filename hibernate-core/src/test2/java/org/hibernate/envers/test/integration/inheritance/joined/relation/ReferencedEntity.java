/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.envers.test.integration.inheritance.joined.relation;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class ReferencedEntity {
	@Id
	private Integer id;

	@OneToMany(mappedBy = "referenced")
	private Set<ParentIngEntity> referencing;

	public ReferencedEntity() {
	}

	public ReferencedEntity(Integer id) {
		this.id = id;
	}

	public Set<ParentIngEntity> getReferencing() {
		return referencing;
	}

	public void setReferencing(Set<ParentIngEntity> referencing) {
		this.referencing = referencing;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ReferencedEntity) ) {
			return false;
		}

		ReferencedEntity that = (ReferencedEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return id;
	}

	public String toString() {
		return "ReferencedEntity(id = " + getId() + ")";
	}
}