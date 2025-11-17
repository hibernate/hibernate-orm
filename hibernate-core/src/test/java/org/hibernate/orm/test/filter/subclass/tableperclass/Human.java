/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.subclass.tableperclass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@Table(name="ZOOLOGY_HUMAN")
@FilterDef(name="iqRange", parameters=
{
		@ParamDef(name="min", type=Integer.class),
		@ParamDef(name="max", type=Integer.class)
})
@Filter(name="iqRange", condition="HUMAN_IQ BETWEEN :min AND :max")
public class Human extends Mammal {
	@Column(name="HUMAN_IQ")
	private int iq;

	public int getIq() {
		return iq;
	}

	public void setIq(int iq) {
		this.iq = iq;
	}

}
