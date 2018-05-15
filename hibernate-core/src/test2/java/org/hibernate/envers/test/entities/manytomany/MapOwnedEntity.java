/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.manytomany;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * Many-to-many not-owning entity
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "MapOwned")
public class MapOwnedEntity {
	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@ManyToMany(mappedBy = "references")
	private Set<MapOwningEntity> referencing;

	public MapOwnedEntity() {
	}

	public MapOwnedEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public MapOwnedEntity(String data) {
		this.data = data;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Set<MapOwningEntity> getReferencing() {
		return referencing;
	}

	public void setReferencing(Set<MapOwningEntity> referencing) {
		this.referencing = referencing;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof MapOwnedEntity) ) {
			return false;
		}

		MapOwnedEntity that = (MapOwnedEntity) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "MapOwnedEntity(id = " + id + ", data = " + data + ")";
	}
}