/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria.internal.hhh13151;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.CascadeType;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
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
