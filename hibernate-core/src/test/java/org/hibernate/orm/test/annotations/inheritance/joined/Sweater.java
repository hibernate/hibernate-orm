/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.joined;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
@PrimaryKeyJoinColumn(name = "clothing_id")
public class Sweater extends Clothing {
	private boolean isSweat;

	public boolean isSweat() {
		return isSweat;
	}

	public void setSweat(boolean sweat) {
		isSweat = sweat;
	}
}
