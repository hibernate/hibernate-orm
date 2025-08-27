/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idclassgeneratedvalue;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/**
 * A Simple entity class.
 *
 * @author Stale W. Pedersen
 */
@Entity
@IdClass(SimplePK.class)
@SuppressWarnings("serial")
public class Simple implements Serializable {
	@Id
	private Long id1;

	@Id
	private Long id2;

	private int quantity;

	public Simple() {
	}

	public Simple(Long id1, Long id2, int quantity) {
		this.id1 = id1;
		this.id2 = id2;
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
