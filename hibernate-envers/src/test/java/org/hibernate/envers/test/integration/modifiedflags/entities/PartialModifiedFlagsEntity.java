/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
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
package org.hibernate.envers.test.integration.modifiedflags.entities;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.components.Component1;
import org.hibernate.envers.test.entities.components.Component2;

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
	@CollectionTable(name = "PartialModFlags_StrSet")
	@AuditJoinTable(name = "PartialModFlags_StrSet_AUD")
	private Set<String> stringSet = new HashSet<String>();

	@Audited(withModifiedFlag = true)
	@ManyToMany
	@JoinTable(name = "ENTITIESSET")
	private Set<StrTestEntity> entitiesSet = new HashSet<StrTestEntity>();

	@Audited(withModifiedFlag = true)
	@ElementCollection
	@MapKeyColumn(nullable = false)
	@CollectionTable(name = "PartialModFlags_StrMap")
	@AuditJoinTable(name = "PartialModFlags_StrMap_AUD")
	private Map<String, String> stringMap = new HashMap<String, String>();

	@Audited(withModifiedFlag = true)
	@ManyToMany
	@JoinTable(name = "ENTITIESMAP")
	@MapKeyColumn(nullable = false)
	private Map<String, StrTestEntity> entitiesMap =
			new HashMap<String, StrTestEntity>();

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

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof PartialModifiedFlagsEntity) ) {
			return false;
		}

		PartialModifiedFlagsEntity that = (PartialModifiedFlagsEntity) o;

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
}
