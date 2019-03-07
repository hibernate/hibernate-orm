/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.manytomany.ternary;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyJoinColumn;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.support.domains.basic.IntTestPrivSeqEntity;
import org.hibernate.envers.test.support.domains.basic.StrTestPrivSeqEntity;


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
	@MapKeyJoinColumn
	private Map<IntTestPrivSeqEntity, StrTestPrivSeqEntity> map;

	public TernaryMapEntity() {
		map = new HashMap<>();
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

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		TernaryMapEntity that = (TernaryMapEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( map, that.map );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id );
	}

	@Override
	public String toString() {
		return "TernaryMapEntity{" +
				"id=" + id +
				", map=" + map +
				'}';
	}
}