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
import org.hibernate.processor.test.integ.model.Project;

import java.util.List;

@Repository
public interface ProjectRegistry extends DataRepository<Project, Long>, DepartmentQueries {

	@Save
	void save(Project project);

	@Query("where budget > :min order by name")
	List<Project> fundedAbove(@Param("min") double min);
}
