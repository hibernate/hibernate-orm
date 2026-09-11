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
interface NaturalNumbers$ extends NaturalNumbers {
	@Override
	@Query("select count(this) as Integer where id between ?1 and ?2")
	public long countByIdBetween(long minimum, long maximum);

	@Override
	@Query("select count(this)>0 where id = ?1")
	public boolean existsById(long id);

	@Override
	@Query("select count(this) as Integer")
	public long countAll();

	@Override
	@Query("where id between ?1 and ?2")
	@OrderBy(value = "numTypeOrdinal", descending = false)
	public java.util.stream.Stream<ee.jakarta.tck.data.framework.read.only.NaturalNumber> findByIdBetweenOrderByNumTypeOrdinalAsc(long minimum, long maximum, jakarta.data.Order<ee.jakarta.tck.data.framework.read.only.NaturalNumber> sorts);

	@Override
	@Query("where id < ?1")
	public ee.jakarta.tck.data.framework.read.only.NaturalNumber[] findByIdLessThan(long exclusiveMax, jakarta.data.Sort<ee.jakarta.tck.data.framework.read.only.NaturalNumber> primarySort, jakarta.data.Sort<ee.jakarta.tck.data.framework.read.only.NaturalNumber> secondarySort);

	@Override
	@Query("where id <= ?1")
	public java.util.List<ee.jakarta.tck.data.framework.read.only.NaturalNumber> findByIdLessThanEqual(long maximum, jakarta.data.Sort<?>[] sorts);

	@Override
	@Query("where id < ?1")
	@OrderBy(value = "floorOfSquareRoot", descending = true)
	public jakarta.data.page.Page<ee.jakarta.tck.data.framework.read.only.NaturalNumber> findByIdLessThanOrderByFloorOfSquareRootDesc(long exclusiveMax, jakarta.data.page.PageRequest pagination, jakarta.data.Order<ee.jakarta.tck.data.framework.read.only.NaturalNumber> order);

	@Override
	@Query("where numType <> ?1")
	public ee.jakarta.tck.data.framework.read.only.NaturalNumber[] findByNumTypeNot(ee.jakarta.tck.data.framework.read.only.NaturalNumber.NumberType notThisType, jakarta.data.Limit limit, jakarta.data.Order<ee.jakarta.tck.data.framework.read.only.NaturalNumber> sorts);

	// TODO; Implement TCK overrides
}
