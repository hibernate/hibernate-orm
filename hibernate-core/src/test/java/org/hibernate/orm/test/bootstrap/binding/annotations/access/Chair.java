/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Chair extends Furniture {

	@Transient
	private String pillow;

	public String getPillow() {
		return pillow;
	}

	public void setPillow(String pillow) {
		this.pillow = pillow;
	}
}
