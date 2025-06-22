/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * @author Emmanuel Bernard
 */
//FIXME HBX-55 default for composite PK does not work yet
//FIXME Tomato is a fruit
@Entity()
@Inheritance(
		strategy = InheritanceType.JOINED
)
@OnDelete(action = OnDeleteAction.CASCADE)
public class Tomato extends Vegetable {
	private int size;

	@Column(name="tom_size")
	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
}
