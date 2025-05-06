/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetoone;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/**
 * @author Martin Simka
 */
@Entity
public class A implements Serializable {
	private Integer id;
	private B b;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@OneToOne(mappedBy = "a", orphanRemoval = true)
	public B getB() {
		return b;
	}

	public void setB(B b) {
		this.b = b;
	}
}
