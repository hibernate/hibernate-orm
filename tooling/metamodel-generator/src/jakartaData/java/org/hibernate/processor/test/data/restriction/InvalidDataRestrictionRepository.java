/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.restriction;

import java.util.List;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Repository;
import jakarta.data.restrict.Restriction;

@Repository
public interface InvalidDataRestrictionRepository extends CrudRepository<DataRestrictionBook, String> {

	@Delete
	long deleteWithLimit(String title, Limit limit);

	@Delete
	long deleteWithOrder(String title, Order<? super DataRestrictionBook> order);

	@Delete
	long deleteWithSort(String title, Sort<? super DataRestrictionBook> sort);

	@Delete
	long deleteWithPageRequest(String title, PageRequest pageRequest);

	@Delete
	long deleteWithTwoRestrictions(
			Restriction<? super DataRestrictionBook> restriction1,
			Restriction<? super DataRestrictionBook> restriction2);

	@Delete
	long deleteWithRestrictionBeforeAttribute(
			Restriction<? super DataRestrictionBook> restriction,
			String title);

	@Delete
	long deleteWithRestrictionList(List<Restriction<? super DataRestrictionBook>> restrictions);

	@Delete
	long deleteWithWrongRestriction(Restriction<? super DataRestrictionPublisher> restriction);
}
