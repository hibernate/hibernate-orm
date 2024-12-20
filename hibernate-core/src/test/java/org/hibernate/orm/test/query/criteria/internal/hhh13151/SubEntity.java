/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal.hhh13151;

import org.hibernate.annotations.*;
import org.hibernate.annotations.CascadeType;

import jakarta.persistence.*;
import jakarta.persistence.Entity;

@Entity
public class SubEntity extends SuperEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@Cascade(CascadeType.ALL)
	public SideEntity getSubField() {
		return subField;
	}

	public SubEntity setSubField(SideEntity subField) {
		this.subField = subField;
		return this;
	}

	private SideEntity subField;
}
