/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.manytoone.bidirectional;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class OneToManyOwned implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	private String data;

	@OneToMany(mappedBy = "references")
	private Set<ManyToOneOwning> referencing = new HashSet<ManyToOneOwning>();

	public OneToManyOwned() {
	}

	public OneToManyOwned(String data, Set<ManyToOneOwning> referencing) {
		this.data = data;
		this.referencing = referencing;
	}

	public OneToManyOwned(String data, Set<ManyToOneOwning> referencing, Long id) {
		this.id = id;
		this.data = data;
		this.referencing = referencing;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Set<ManyToOneOwning> getReferencing() {
		return referencing;
	}

	public void setReferencing(Set<ManyToOneOwning> referencing) {
		this.referencing = referencing;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		OneToManyOwned that = (OneToManyOwned) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "OneToManyOwned{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}
