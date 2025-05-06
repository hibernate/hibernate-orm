/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "Dummy_tbl")
public class Dummy<K> {

	@Id
	private Long id;

	@Transient
	transient private K dummyField;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public K getDummyField() {
		return dummyField;
	}

	public void setDummyField(K dummyField) {
		this.dummyField = dummyField;
	}

}
