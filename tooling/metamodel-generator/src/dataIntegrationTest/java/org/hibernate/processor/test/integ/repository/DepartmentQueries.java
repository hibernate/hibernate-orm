/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.repository;

import jakarta.data.repository.Param;
import jakarta.data.repository.Query;


public interface DepartmentQueries {

	@Query("select count(*) from Employee where department = :dept")
	long countEmployeesByDepartment(@Param("dept") String department);

}
