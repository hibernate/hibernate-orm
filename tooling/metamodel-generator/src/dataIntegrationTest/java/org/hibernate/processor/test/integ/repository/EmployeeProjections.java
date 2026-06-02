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
import org.hibernate.processor.test.integ.model.EmployeeInfo;
import org.hibernate.processor.test.integ.model.EmployeeSummary;
import org.hibernate.processor.test.integ.model.ProjectionEmployee;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface EmployeeProjections extends DataRepository<ProjectionEmployee, Long> {

	@Save
	void save(ProjectionEmployee employee);

	@Query("where department = :dept order by name")
	EmployeeInfo[] infoArrayByDepartment(@Param("dept") String department);

	@Query("where department = :dept order by name")
	List<EmployeeInfo> infoListByDepartment(@Param("dept") String department);

	@Query("where department = :dept order by name")
	Stream<EmployeeInfo> infoStreamByDepartment(@Param("dept") String department);

	@Query("where id = :id")
	EmployeeInfo infoById(@Param("id") long id);

	@Query("where id = :id")
	Optional<EmployeeInfo> optionalInfoById(@Param("id") long id);

	@Query("where department = :dept order by name")
	List<EmployeeSummary> summaryByDepartment(@Param("dept") String department);

	@Query("where department = ?1 order by name")
	List<EmployeeSummary> summaryByDepartmentPositional(String department);

	@Query("where id = :id")
	Optional<EmployeeSummary> summaryById(@Param("id") long id);
}
