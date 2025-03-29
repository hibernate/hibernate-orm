/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ops;
import java.io.Serializable;
import java.util.Collection;


/**
 * Employer in a employer-Employee relationship
 *
 * @author Emmanuel Bernard
 */

public class Employer implements Serializable {
	private Integer id;
	private Collection employees;


	public Collection getEmployees() {
		return employees;
	}


	public Integer getId() {
		return id;
	}

	public void setEmployees(Collection set) {
		employees = set;
	}

	public void setId(Integer integer) {
		id = integer;
	}
}
