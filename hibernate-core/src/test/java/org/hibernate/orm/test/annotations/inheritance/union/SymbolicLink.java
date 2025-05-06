/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.union;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "SymbolicLinkUnion")
public class SymbolicLink extends File {

	File target;

	SymbolicLink() {
	}

	public SymbolicLink(File target) {
		this.target = target;
	}

	@ManyToOne(optional = false)
	public File getTarget() {
		return target;
	}

	public void setTarget(File target) {
		this.target = target;
	}


}
