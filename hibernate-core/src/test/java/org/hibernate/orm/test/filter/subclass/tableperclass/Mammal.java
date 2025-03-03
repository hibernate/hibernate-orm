/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter.subclass.tableperclass;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.type.NumericBooleanConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name="ZOOLOGY_MAMMAL")
@FilterDef(name="pregnantOnly")
@Filter(name="pregnantOnly", condition="IS_PREGNANT = 1")
public class Mammal extends Animal{

	@Column(name="IS_PREGNANT")
	@Convert( converter = NumericBooleanConverter.class )
	private boolean isPregnant;

	public boolean isPregnant() {
		return isPregnant;
	}

	public void setPregnant(boolean isPregnant) {
		this.isPregnant = isPregnant;
	}
}
