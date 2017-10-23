/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytomany.ternary;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.IntTestPrivSeqEntity;
import org.hibernate.envers.test.entities.StrTestPrivSeqEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
public class TernaryMapEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@ManyToMany
	@javax.persistence.MapKeyJoinColumn
	private Map<IntTestPrivSeqEntity, StrTestPrivSeqEntity> map;

	public TernaryMapEntity() {
		map = new HashMap<IntTestPrivSeqEntity, StrTestPrivSeqEntity>();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Map<IntTestPrivSeqEntity, StrTestPrivSeqEntity> getMap() {
		return map;
	}

	public void setMap(Map<IntTestPrivSeqEntity, StrTestPrivSeqEntity> map) {
		this.map = map;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof TernaryMapEntity) ) {
			return false;
		}

		TernaryMapEntity that = (TernaryMapEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		return (id != null ? id.hashCode() : 0);
	}

	public String toString() {
		return "TME(id = " + id + ", map = " + map + ")";
	}
}