/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.manytomany.sametable;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.hibernate.annotations.WhereJoinTable;
import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class Child1Entity {
	@Id
	@GeneratedValue
	private Integer id;

	private String child1Data;

	public Child1Entity() {
	}

	public Child1Entity(String child1Data) {
		this.child1Data = child1Data;
	}

	public Child1Entity(String child1Data, Integer id) {
		this.child1Data = child1Data;
		this.id = id;
	}

	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
			name = "children",
			joinColumns = @JoinColumn(name = "child1_id"),
			inverseJoinColumns = @JoinColumn(name = "parent_id", insertable = false, updatable = false)
	)
	@WhereJoinTable(clause = "child1_id is not null")
	private List<ParentEntity> parents = new ArrayList<ParentEntity>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getChild1Data() {
		return child1Data;
	}

	public void setChild1Data(String child1Data) {
		this.child1Data = child1Data;
	}

	public List<ParentEntity> getParents() {
		return parents;
	}

	public void setParents(List<ParentEntity> parents) {
		this.parents = parents;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Child1Entity that = (Child1Entity) o;

		if ( child1Data != null ? !child1Data.equals( that.child1Data ) : that.child1Data != null ) {
			return false;
		}
		//noinspection RedundantIfStatement
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (child1Data != null ? child1Data.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "C1E(id = " + id + ", child1Data = " + child1Data + ")";
	}
}
