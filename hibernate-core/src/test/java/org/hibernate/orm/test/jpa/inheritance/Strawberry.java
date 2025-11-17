/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.inheritance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Strawberry extends Fruit {
	private Long size;

	@Column(name="size_")
	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}
}
