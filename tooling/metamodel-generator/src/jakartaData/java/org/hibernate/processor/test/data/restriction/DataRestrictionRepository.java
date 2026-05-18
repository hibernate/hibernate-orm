/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.restriction;

import jakarta.data.Order;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.restrict.Restriction;

import java.util.List;

@Repository
public interface DataRestrictionRepository extends CrudRepository<DataRestrictionBook, String> {
	@Find
	List<DataRestrictionBook> search(Restriction<? super DataRestrictionBook> restriction);

	@Find
	List<DataRestrictionBook> search(
			String title,
			Restriction<? super DataRestrictionBook> restriction,
			Order<? super DataRestrictionBook> order);

	@Find
	List<DataRestrictionBook> searchAll(List<Restriction<? super DataRestrictionBook>> restrictions);

	@Find
	List<DataRestrictionBook> searchAny(Restriction<? super DataRestrictionBook>... restrictions);

	@Query("from DataRestrictionBook")
	List<DataRestrictionBook> query(Restriction<? super DataRestrictionBook> queryRestriction);

	@Query("from DataRestrictionBook where title = :title")
	List<DataRestrictionBook> query(String title, Restriction<? super DataRestrictionBook> queryRestriction);

	@Delete
	long delete(Restriction<? super DataRestrictionBook> deleteRestriction);
}
