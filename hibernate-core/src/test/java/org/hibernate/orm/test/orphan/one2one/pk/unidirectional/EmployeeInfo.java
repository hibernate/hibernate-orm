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
public class EmployeeInfo {
	private Long id;

	public EmployeeInfo() {
	}

	public EmployeeInfo(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
