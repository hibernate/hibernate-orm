/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.hbm;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table( name = "ENTITYB" )
public class BImpl extends AImpl implements B {
	private static final long serialVersionUID = 1L;

	private Integer bId = 0;

	public BImpl() {
		super();
	}

	public Integer getBId() {
		return bId;
	}

	public void setBId(Integer bId) {
		this.bId = bId;
	}
}
