/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.onetomany;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		RefEdMapKeyEntity that = (RefEdMapKeyEntity) o;
		return Objects.equals( id, that.id );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id );
	}

	@Override
	public String toString() {
		return "RefEdMapKeyEntity{" +
				"id=" + id +
				", idmap=" + idmap +
				'}';
	}
}