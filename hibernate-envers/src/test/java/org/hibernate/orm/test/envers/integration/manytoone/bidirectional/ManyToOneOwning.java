/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytoone.bidirectional;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;

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

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ManyToOneOwning) ) {
			return false;
		}

		ManyToOneOwning that = (ManyToOneOwning) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ManyToOneOwning(id = " + id + ", data = " + data + ")";
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
}
