/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.repository;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.restrict.Restriction;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import org.hibernate.processor.test.integ.model.Employee;

import java.util.List;
import java.util.stream.Stream;

@Repository
public interface EmployeeDirectory extends DataRepository<Employee, Long>, DepartmentQueries {

	@Save
	void save(Employee employee);

	@Query("where salary >= :threshold order by salary desc")
	List<Employee> highEarners(@Param("threshold") double threshold);

	@Query("from Employee where department = :dept")
	Stream<Employee> sortedByDepartment(@Param("dept") String department, Sort<? super Employee>... sorts);

	@Query("from Employee where department = :dept")
	List<Employee> orderedByDepartment(@Param("dept") String department, Order<? super Employee> order);

	@Query("from Employee where department = :dept")
	List<Employee> pagedByDepartment(@Param("dept") String department, Limit limit, Sort<? super Employee>... sorts);

	@Query("from Employee")
	List<Employee> allEmployees(Restriction<? super Employee> restriction);
}
