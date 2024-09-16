/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.joined;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
public class SymbolicLink extends File {

	@ManyToOne(optional = false)
	File target;

	SymbolicLink() {
	}

	public SymbolicLink(File target) {
		this.target = target;
	}

	public File getTarget() {
		return target;
	}

	public void setTarget(File target) {
		this.target = target;
	}


}
