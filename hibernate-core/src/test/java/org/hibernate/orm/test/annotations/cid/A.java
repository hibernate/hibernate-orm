/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;

import java.io.Serializable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

/**
 * @author Artur Legan
 */
@Entity
public class A implements Serializable{

	@EmbeddedId
	private AId aId;

	public AId getAId() {
		return aId;
	}

	public void setAId(AId aId) {
		this.aId = aId;
	}
}
