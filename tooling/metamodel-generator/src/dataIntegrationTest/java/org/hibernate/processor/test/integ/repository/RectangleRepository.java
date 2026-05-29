/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.repository;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import org.hibernate.processor.test.integ.model.Rectangle;

import java.util.List;
import java.util.Optional;

@Repository
public interface RectangleRepository extends DataRepository<Rectangle, String> {

	@Save
	void save(Rectangle rectangle);

	@Find
	Optional<Rectangle> findById(String id);

	@Query("where x = :x and y = :y")
	List<Rectangle> findByPosition(@Param("x") long x, @Param("y") long y);

	@Query("where width >= :minWidth and height >= :minHeight order by width desc, height desc")
	List<Rectangle> largerThan(@Param("minWidth") long minWidth, @Param("minHeight") long minHeight);

	@Query("select count(*) from Rectangle where width * height >= :minArea")
	long countWithAreaAtLeast(@Param("minArea") long minArea);
}
