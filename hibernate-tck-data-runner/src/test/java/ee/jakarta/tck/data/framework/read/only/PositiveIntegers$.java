/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package ee.jakarta.tck.data.framework.read.only;

import jakarta.annotation.Generated;
import jakarta.data.repository.OrderBy;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository(dataStore = "")
@Generated("ee.jakarta.tck.data.tools.annp.RepositoryProcessor")
interface PositiveIntegers$ extends PositiveIntegers {
	@Override
	@Query("select count(this) as Integer where id < ?1")
	public long countByIdLessThan(long number);

	@Override
	@Query("select count(this)>0 where id > ?1")
	public boolean existsByIdGreaterThan(java.lang.Long number);

	@Override
	@Query("where floorOfSquareRoot <> ?1 and id < ?2")
	@OrderBy(value = "numBitsRequired", descending = true)
	public jakarta.data.page.CursoredPage<ee.jakarta.tck.data.framework.read.only.NaturalNumber> findByFloorOfSquareRootNotAndIdLessThanOrderByNumBitsRequiredDesc(long excludeSqrt, long eclusiveMax, jakarta.data.page.PageRequest pagination, jakarta.data.Order<ee.jakarta.tck.data.framework.read.only.NaturalNumber> order);

	@Override
	@Query("where isOdd=true and id <= ?1")
	@OrderBy(value = "id", descending = true)
	public java.util.List<ee.jakarta.tck.data.framework.read.only.NaturalNumber> findByIsOddTrueAndIdLessThanEqualOrderByIdDesc(long max);

	@Override
	@Query("where isOdd=false and id between ?1 and ?2")
	public java.util.List<ee.jakarta.tck.data.framework.read.only.NaturalNumber> findByIsOddFalseAndIdBetween(long min, long max);

	@Override
	@Query("where numType in ?1")
	@OrderBy(value = "id", descending = false)
	public java.util.stream.Stream<ee.jakarta.tck.data.framework.read.only.NaturalNumber> findByNumTypeInOrderByIdAsc(java.util.Set<ee.jakarta.tck.data.framework.read.only.NaturalNumber.NumberType> types, jakarta.data.Limit limit);

	@Override
	@Query("where numType = ?1 or floorOfSquareRoot = ?2")
	public java.util.stream.Stream<ee.jakarta.tck.data.framework.read.only.NaturalNumber> findByNumTypeOrFloorOfSquareRoot(ee.jakarta.tck.data.framework.read.only.NaturalNumber.NumberType type, long floor);


	// TODO; Implement TCK overrides
}
