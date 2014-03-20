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
package org.hibernate.envers.test.entities.manytomany;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyJoinColumn;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.annotations.SortComparator;
import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.StrTestEntityComparator;

/**
 * Entity with custom-ordered SortedSet and SortedMap
 *
 * @author Michal Skowronek (mskowr at o2 pl)
 */
@Entity
public class SortedSetEntity {
	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@ManyToMany
	@SortComparator(value = StrTestEntityComparator.class)
	private SortedSet<StrTestEntity> sortedSet = new TreeSet<StrTestEntity>( StrTestEntityComparator.INSTANCE );
	@Audited
	@ElementCollection
	@MapKeyJoinColumn
	@SortComparator(value = StrTestEntityComparator.class)
	private SortedMap<StrTestEntity, String> sortedMap = new TreeMap<StrTestEntity, String>( StrTestEntityComparator.INSTANCE );

	public SortedSetEntity() {
	}

	public SortedSetEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
	}

	public SortedSetEntity(String data) {
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

	public SortedSet<StrTestEntity> getSortedSet() {
		return sortedSet;
	}

	public void setSortedSet(SortedSet<StrTestEntity> sortedSet) {
		this.sortedSet = sortedSet;
	}

	public SortedMap<StrTestEntity, String> getSortedMap() {
		return sortedMap;
	}

	public void setSortedMap(SortedMap<StrTestEntity, String> sortedMap) {
		this.sortedMap = sortedMap;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof SortedSetEntity) ) {
			return false;
		}

		SortedSetEntity that = (SortedSetEntity) o;

		return !(data != null ? !data.equals( that.data ) : that.data != null) && !(id != null ?
				!id.equals( that.id ) :
				that.id != null);
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "SetOwnedEntity(id = " + id + ", data = " + data + ")";
	}
}
