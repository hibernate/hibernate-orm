/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package ee.jakarta.tck.data.standalone.nosql.example;

import jakarta.annotation.Generated;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository(dataStore = "")
@Generated("ee.jakarta.tck.data.tools.annp.RepositoryProcessor")
interface Catalog$ extends Catalog {
	@Override
	@Query("delete Product where id = ?1")
	public void deleteById(java.lang.Long id);

	@Override
	@Query("select count(this) as Integer where price >= ?1")
	public long countByPriceGreaterThanEqual(java.lang.Double price);

	@Override
	@Query("select count(this) as Integer where surgePrice >= ?1")
	public long countBySurgePriceGreaterThanEqual(java.lang.Double price);

	@Override
	@Query("where name = ?1")
	public java.util.List<ee.jakarta.tck.data.standalone.nosql.example.Product> findByName(java.lang.String name);


	// TODO; Implement TCK overrides
}
