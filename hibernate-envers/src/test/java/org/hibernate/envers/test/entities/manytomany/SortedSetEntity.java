/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.manytomany;

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyJoinColumn;

import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
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
	@Sort(type = SortType.COMPARATOR, comparator = StrTestEntityComparator.class)
	private SortedSet<StrTestEntity> sortedSet = new TreeSet<StrTestEntity>( StrTestEntityComparator.INSTANCE );
	@Audited
	@ElementCollection
	@MapKeyJoinColumn
	@Sort(type = SortType.COMPARATOR, comparator = StrTestEntityComparator.class)
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
