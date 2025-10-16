/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetomany.idclass;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class OneToManyOwned {
	@Id
	@GeneratedValue
	private Long id;

	private String data;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "oneToMany")
	private List<ManyToManyCompositeKey> manyToManyCompositeKeys;

	public OneToManyOwned() {

	}

	public OneToManyOwned(String data, List<ManyToManyCompositeKey> manyToManyCompositeKeys) {
		this.data = data;
		this.manyToManyCompositeKeys = manyToManyCompositeKeys;
	}

	public OneToManyOwned(Long id, String data, List<ManyToManyCompositeKey> manyToManyCompositeKeys) {
		this.id = id;
		this.data = data;
		this.manyToManyCompositeKeys = manyToManyCompositeKeys;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof OneToManyOwned ) ) {
			return false;
		}

		OneToManyOwned that = (OneToManyOwned) o;
		if ( data != null ? !data.equals( that.getData() ) : that.getData() != null ) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = 3;
		result = 7 * result + ( data != null ? data.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "OneToManyOwned(id = " + id + ", data = " + data + ")";
	}

	public Long getId() {
		return id;
	}

	public String getData() {
		return data;
	}

	public List<ManyToManyCompositeKey> getManyToManyCompositeKeys() {
		return manyToManyCompositeKeys;
	}
}
