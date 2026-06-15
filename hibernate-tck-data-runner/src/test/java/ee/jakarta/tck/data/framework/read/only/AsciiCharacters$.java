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
interface AsciiCharacters$ extends AsciiCharacters {
	@Override
	@Query("select count(this) as Integer where id between ?1 and ?2")
	public long countByIdBetween(long minimum, long maximum);

	@Override
	@Query("select count(this)>0 where id = ?1")
	public boolean existsById(long id);

	@Override
	@Query("select count(this) as Integer where hexadecimal is not null")
	public long countByHexadecimalNotNull();

	@Override
	@Query("select count(this)>0 where thisCharacter = ?1")
	public boolean existsByThisCharacter(char ch);

	@Override
	@Query("where hexadecimal like '%'||?1||'%' and isControl <> ?2")
	public java.util.List<ee.jakarta.tck.data.framework.read.only.AsciiCharacter> findByHexadecimalContainsAndIsControlNot(java.lang.String substring, boolean isPrintable);

	@Override
	@Query("where lower(hexadecimal) between lower(?1) and lower(?2) and hexadecimal not in ?3")
	public java.util.stream.Stream<ee.jakarta.tck.data.framework.read.only.AsciiCharacter> findByHexadecimalIgnoreCaseBetweenAndHexadecimalNotIn(java.lang.String minHex, java.lang.String maxHex, java.util.Set<java.lang.String> excludeHex, jakarta.data.Order<ee.jakarta.tck.data.framework.read.only.AsciiCharacter> sorts);

	@Override
	@Query("where lower(hexadecimal) = lower(?1)")
	public ee.jakarta.tck.data.framework.read.only.AsciiCharacter findByHexadecimalIgnoreCase(java.lang.String hex);

	@Override
	@Query("where id between ?1 and ?2")
	public java.util.stream.Stream<ee.jakarta.tck.data.framework.read.only.AsciiCharacter> findByIdBetween(long minimum, long maximum, jakarta.data.Sort<ee.jakarta.tck.data.framework.read.only.AsciiCharacter> sort);

	@Override
	@Query("where isControl=true and numericValue between ?1 and ?2")
	public ee.jakarta.tck.data.framework.read.only.AsciiCharacter findByIsControlTrueAndNumericValueBetween(int min, int max);

	@Override
	@Query("where numericValue = ?1")
	public java.util.Optional<ee.jakarta.tck.data.framework.read.only.AsciiCharacter> findByNumericValue(int id);

	@Override
	@Query("where numericValue between ?1 and ?2")
	public jakarta.data.page.Page<ee.jakarta.tck.data.framework.read.only.AsciiCharacter> findByNumericValueBetween(int min, int max, jakarta.data.page.PageRequest pagination, jakarta.data.Order<ee.jakarta.tck.data.framework.read.only.AsciiCharacter> order);

	@Override
	@Query("where numericValue <= ?1 and numericValue >= ?2")
	public java.util.List<ee.jakarta.tck.data.framework.read.only.AsciiCharacter> findByNumericValueLessThanEqualAndNumericValueGreaterThanEqual(int max, int min);

	@Override
	@Query("where numericValue >= ?1 and right(hexadecimal, length(?2)) = ?2 limit 3")
	public ee.jakarta.tck.data.framework.read.only.AsciiCharacter[] findFirst3ByNumericValueGreaterThanEqualAndHexadecimalEndsWith(int minValue, java.lang.String lastHexDigit, jakarta.data.Sort<ee.jakarta.tck.data.framework.read.only.AsciiCharacter> sort);

	@Override
	@Query("where left(hexadecimal, length(?1)) = ?1 and isControl = ?2 limit 1")
	@OrderBy(value = "id", descending = false)
	public java.util.Optional<ee.jakarta.tck.data.framework.read.only.AsciiCharacter> findFirstByHexadecimalStartsWithAndIsControlOrderByIdAsc(java.lang.String firstHexDigit, boolean isControlChar);


	// TODO; Implement TCK overrides
}
