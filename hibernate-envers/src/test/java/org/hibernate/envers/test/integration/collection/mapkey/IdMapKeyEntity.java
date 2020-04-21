/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection.mapkey;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKey;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.StrTestEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "IdMapKey")
public class IdMapKeyEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@ManyToMany
	@MapKey
	private Map<Integer, StrTestEntity> idmap;

	public IdMapKeyEntity() {
		idmap = new HashMap<Integer, StrTestEntity>();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Map<Integer, StrTestEntity> getIdmap() {
		return idmap;
	}

	public void setIdmap(Map<Integer, StrTestEntity> idmap) {
		this.idmap = idmap;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof IdMapKeyEntity) ) {
			return false;
		}

		IdMapKeyEntity that = (IdMapKeyEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	public String toString() {
		return "IMKE(id = " + id + ", idmap = " + idmap + ")";
	}
}