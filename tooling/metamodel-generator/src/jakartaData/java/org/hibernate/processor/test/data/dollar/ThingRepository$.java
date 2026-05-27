/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.dollar;

import jakarta.annotation.Generated;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
@Generated("some.other.processor.Processor")
interface ThingRepository$ extends ThingRepository {
	@Override
	@Query("where name = ?1")
	Thing findByName(String name);
}
