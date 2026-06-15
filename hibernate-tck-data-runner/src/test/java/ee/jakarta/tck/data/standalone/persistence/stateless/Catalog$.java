/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package ee.jakarta.tck.data.standalone.persistence.stateless;

import jakarta.annotation.Generated;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository(dataStore = "")
@Generated("ee.jakarta.tck.data.tools.annp.RepositoryProcessor")
interface Catalog$ extends Catalog {
	@Override
	@Query("delete Product where productNum like ?1")
	public long deleteByProductNumLike(java.lang.String pattern);

	@Override
	@Query("select count(this) as Integer where price >= ?1")
	public long countByPriceGreaterThanEqual(java.lang.Double price);

	@Override
	@Query("where name like ?1")
	public java.util.List<ee.jakarta.tck.data.standalone.persistence.Product> findByNameLike(java.lang.String name);

	@Override
	@Query("where price is not null and price <= ?1")
	public java.util.stream.Stream<ee.jakarta.tck.data.standalone.persistence.Product> findByPriceNotNullAndPriceLessThanEqual(double maxPrice);

	@Override
	@Query("where price is null")
	public java.util.List<ee.jakarta.tck.data.standalone.persistence.Product> findByPriceNull();

	@Override
	@Query("where productNum between ?1 and ?2")
	public java.util.List<ee.jakarta.tck.data.standalone.persistence.Product> findByProductNumBetween(java.lang.String first, java.lang.String last, jakarta.data.Order<ee.jakarta.tck.data.standalone.persistence.Product> sorts);

	@Override
	@Query("where productNum like ?1")
	public java.util.List<ee.jakarta.tck.data.standalone.persistence.Product> findByProductNumLike(java.lang.String productNum);


	// TODO; Implement TCK overrides
}
