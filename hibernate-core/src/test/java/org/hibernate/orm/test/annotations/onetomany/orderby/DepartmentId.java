/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany.orderby;

import java.io.Serializable;

public class DepartmentId implements Serializable {

	private ECompany company;
	private String departmentCode;

}
