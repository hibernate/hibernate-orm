/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.repository;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import org.hibernate.processor.test.integ.model.Employee;

import java.util.List;

@Repository
public interface EmployeeDirectory extends DataRepository<Employee, Long>, DepartmentQueries {

	@Save
	void save(Employee employee);

	@Query("where salary >= :threshold order by salary desc")
	List<Employee> highEarners(@Param("threshold") double threshold);
}
