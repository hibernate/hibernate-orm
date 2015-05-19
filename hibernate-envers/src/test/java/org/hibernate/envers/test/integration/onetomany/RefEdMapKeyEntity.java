/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.onetomany;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class RefEdMapKeyEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@OneToMany(mappedBy = "reference")
	@MapKey(name = "data")
	private Map<String, RefIngMapKeyEntity> idmap;

	public RefEdMapKeyEntity() {
		idmap = new HashMap<String, RefIngMapKeyEntity>();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Map<String, RefIngMapKeyEntity> getIdmap() {
		return idmap;
	}

	public void setIdmap(Map<String, RefIngMapKeyEntity> idmap) {
		this.idmap = idmap;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof RefEdMapKeyEntity) ) {
			return false;
		}

		RefEdMapKeyEntity that = (RefEdMapKeyEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	public String toString() {
		return "RedMKE(id = " + id + ", idmap = " + idmap + ")";
	}
}