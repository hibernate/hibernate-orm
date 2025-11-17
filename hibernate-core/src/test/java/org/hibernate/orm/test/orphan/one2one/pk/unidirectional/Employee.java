/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan.one2one.pk.unidirectional;


/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class Employee {
	private Long id;
	private EmployeeInfo info;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public EmployeeInfo getInfo() {
		return info;
	}

	public void setInfo(EmployeeInfo info) {
		this.info = info;
	}
}
