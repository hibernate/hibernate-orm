/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.constraint;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Repository
public interface MyConstrainedRepository extends CrudRepository<MyEntity, Long> {

	@Valid
	@NotNull
	@Find
	MyEntity findByName(@NotNull @Size(min = 5) String name);
}
