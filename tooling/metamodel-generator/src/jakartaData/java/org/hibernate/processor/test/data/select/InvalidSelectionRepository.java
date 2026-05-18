/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.select;

import java.util.List;

import jakarta.data.Limit;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.First;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Select;

@Repository
public interface InvalidSelectionRepository extends CrudRepository<SelectionBook, Long> {

	@Find
	@Select("missing")
	String missingAttribute(Long id);

	@Find
	@Select("publisher")
	String associatedAttribute(Long id);

	@Find
	@Select("title")
	Integer wrongReturnType(Long id);

	@Find
	@Select("title")
	@Select("pages")
	String tooManySelects(Long id);

	@Find
	@First(0)
	List<SelectionBook> badFirstValue(SelectionStatus status);

	@Find
	@First
	List<SelectionBook> firstWithLimit(SelectionStatus status, Limit limit);

	@Find
	@Query("where status = :status")
	List<SelectionBook> findAndQuery(SelectionStatus status);
}
