/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.modifiedflags;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.components.Component1;
import org.hibernate.envers.test.support.domains.components.Component2;

/**
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
@Entity
@Table(name = "PartialModFlags")
@Audited(withModifiedFlag = false)
public class PartialModifiedFlagsEntity {
	@Id
	private Integer id;

	@Audited(withModifiedFlag = true)
	private String data;

	@Audited(withModifiedFlag = true)
	@Embedded
	private Component1 comp1;

	@Audited(withModifiedFlag = false)
	@Embedded
	private Component2 comp2;

	@Audited(withModifiedFlag = true)
	@OneToOne(mappedBy = "reference")
	private WithModifiedFlagReferencingEntity referencing;

	@Audited(withModifiedFlag = false)
	@OneToOne(mappedBy = "secondReference")
	private WithModifiedFlagReferencingEntity referencing2;

	@Audited(withModifiedFlag = true)
	@ElementCollection
	@JoinTable(name = "PartialModFlags_StrSet")
	@AuditJoinTable(name = "PartialModFlags_StrSet_AUD")
	private Set<String> stringSet = new HashSet<>();

	@Audited(withModifiedFlag = true)
	@ManyToMany
	@CollectionTable(name = "ENTITIESSET")
	private Set<StrTestEntity> entitiesSet = new HashSet<>();

	@Audited(withModifiedFlag = true)
	@ElementCollection
	@MapKeyColumn(nullable = false)
	@JoinTable(name = "PartialModFlags_StrMap")
	@AuditJoinTable(name = "PartialModFlags_StrMap_AUD")
	private Map<String, String> stringMap = new HashMap<>();

	@Audited(withModifiedFlag = true)
	@ManyToMany
	@CollectionTable(name = "ENTITIESMAP")
	@MapKeyColumn(nullable = false)
	private Map<String, StrTestEntity> entitiesMap = new HashMap<>();

	public PartialModifiedFlagsEntity() {
	}

	public PartialModifiedFlagsEntity(Integer id) {
		this.id = id;
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

	public WithModifiedFlagReferencingEntity getReferencing() {
		return referencing;
	}

	public void setReferencing(WithModifiedFlagReferencingEntity referencing) {
		this.referencing = referencing;
	}

	public WithModifiedFlagReferencingEntity getReferencing2() {
		return referencing2;
	}

	public void setReferencing2(WithModifiedFlagReferencingEntity referencing) {
		this.referencing2 = referencing;
	}

	public Component1 getComp1() {
		return comp1;
	}

	public void setComp1(Component1 comp1) {
		this.comp1 = comp1;
	}

	public Component2 getComp2() {
		return comp2;
	}

	public void setComp2(Component2 comp2) {
		this.comp2 = comp2;
	}

	public Set<String> getStringSet() {
		return stringSet;
	}

	public void setStringSet(Set<String> stringSet) {
		this.stringSet = stringSet;
	}

	public Set<StrTestEntity> getEntitiesSet() {
		return entitiesSet;
	}

	public void setEntitiesSet(Set<StrTestEntity> entitiesSet) {
		this.entitiesSet = entitiesSet;
	}

	public Map<String, String> getStringMap() {
		return stringMap;
	}

	public void setStringMap(Map<String, String> stringMap) {
		this.stringMap = stringMap;
	}

	public Map<String, StrTestEntity> getEntitiesMap() {
		return entitiesMap;
	}

	public void setEntitiesMap(Map<String, StrTestEntity> entitiesMap) {
		this.entitiesMap = entitiesMap;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		PartialModifiedFlagsEntity that = (PartialModifiedFlagsEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}
}
