/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * @author Sharath Reddy
 */
@FilterDef(name = "byName", parameters = {@ParamDef(name = "name", type = String.class)})
@Filter(name = "byName", condition = ":name = name")
@MappedSuperclass
public class Widget {

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
