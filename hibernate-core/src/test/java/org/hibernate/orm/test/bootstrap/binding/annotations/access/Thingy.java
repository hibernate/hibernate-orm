/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class Thingy {
	private String god;

	@Transient
	public String getGod() {
		return god;
	}

	public void setGod(String god) {
		this.god = god;
	}
}
