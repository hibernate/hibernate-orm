/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "Instruction")
@Table(name = "instruction")
public class Instruction extends BaseEntity {
	private String summary;

	/**
	 * Used by Hibernate
	 */
	@SuppressWarnings("unused")
	public Instruction() {
		super();
	}

	public Instruction(Integer id, String summary) {
		super( id );
		this.summary = summary;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}
}
