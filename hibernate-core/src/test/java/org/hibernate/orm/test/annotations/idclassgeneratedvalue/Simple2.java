/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idclassgeneratedvalue;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import org.hibernate.annotations.GenericGenerator;

/**
 * The {@link Simple} entity redone with a generated value {@link #id1} as part of its
 * composite pk
 *
 * @author Stale W. Pedersen
 */
@Entity
@IdClass(SimplePK.class)
@SuppressWarnings("serial")
public class Simple2 implements Serializable {
	@Id
	@GenericGenerator(name = "increment", strategy = "increment")
	@GeneratedValue(generator = "increment")
	private Long id1;

	@Id
	private Long id2;

	private int quantity;

	public Simple2() {
	}

	public Simple2(Long id, int quantity) {
		this.id2 = id;
		this.quantity = quantity;
	}

	public Long getId1() {
		return id1;
	}

	public Long getId2() {
		return id2;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
}
