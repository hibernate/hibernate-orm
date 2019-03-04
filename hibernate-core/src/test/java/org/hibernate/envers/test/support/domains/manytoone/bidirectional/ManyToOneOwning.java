/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.manytoone.bidirectional;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class ManyToOneOwning implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	private String data;

	@ManyToOne
	@JoinTable(name = "many_to_one_join_table", joinColumns = @JoinColumn(name = "owning_id"),
			   inverseJoinColumns = @JoinColumn(name = "owned_id"))
	private OneToManyOwned references;

	public ManyToOneOwning() {
	}

	public ManyToOneOwning(String data, OneToManyOwned references) {
		this.data = data;
		this.references = references;
	}

	public ManyToOneOwning(String data, OneToManyOwned references, Long id) {
		this.id = id;
		this.data = data;
		this.references = references;
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

	public OneToManyOwned getReferences() {
		return references;
	}

	public void setReferences(OneToManyOwned references) {
		this.references = references;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ManyToOneOwning that = (ManyToOneOwning) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "ManyToOneOwning{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}
