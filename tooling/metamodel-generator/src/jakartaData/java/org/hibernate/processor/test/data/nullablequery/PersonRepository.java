/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.nullablequery;

import jakarta.annotation.Nullable;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface PersonRepository {
	@Query("select p from Person p where (:firstName is null or p.firstName = :firstName) "
			+ "and (:lastName is null or p.lastName = :lastName)")
	Page<Person> search(@Nullable String firstName, @Nullable String lastName, PageRequest pageRequest);

	@Query("update Person p set p.firstName = :firstName, p.lastName = :lastName")
	int rename(@Nullable String firstName, @Nullable String lastName);

	@Query("from Person p where p.aliases = :aliases")
	List<Person> searchByAliases(@Nullable String[] aliases);

	@Query("from Person")
	List<Person> all();
}
