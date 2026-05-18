/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.select;

import java.util.List;
import java.util.Optional;

import jakarta.data.Order;
import jakarta.data.repository.By;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.First;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Select;

@Repository
public interface SelectionRepository extends CrudRepository<SelectionBook, Long> {

	record TitleAndPages(String title, Integer pages) {
	}

	record Renamed(@Select("title") String name, @Select("pages") Integer pageCount) {
	}

	record Named(String title, int pages) {
	}

	record QueryRenamed(@Select("title") String name, @Select("pages") Integer pageCount) {
	}

	@Find
	@Select("title")
	String titleById(@By("id") Long id);

	@Find
	@Select("pages")
	int pagesByTitle(String title);

	@Find
	@Select("title")
	Optional<String> optionalTitleById(@By("id") Long id);

	@Find
	@Select("title")
	List<String> titlesByStatus(SelectionStatus status);

	@Find
	@Select("title")
	String[] titleArrayByStatus(SelectionStatus status);

	@Find
	@Select("title")
	@Select("pages")
	List<TitleAndPages> titlePagesByStatus(SelectionStatus status);

	@Find
	List<Renamed> renamedByStatus(SelectionStatus status);

	@Find
	List<Named> namedByStatus(SelectionStatus status);

	@Find
	@First
	SelectionBook firstByStatus(SelectionStatus status);

	@Find
	@First
	SelectionBook firstByStatus(SelectionStatus status, Order<SelectionBook> order);

	@Find
	@First(3)
	List<SelectionBook> firstThreeByStatus(SelectionStatus status);

	@Query("where status = :status order by title")
	@First(2)
	List<SelectionBook> firstTwoByQuery(SelectionStatus status);

	@Query("where status = :status")
	@Select("title")
	List<String> queryTitlesByStatus(SelectionStatus status);

	@Query("where status = :status")
	List<QueryRenamed> queryRenamedByStatus(SelectionStatus status);
}
