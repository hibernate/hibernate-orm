/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.dollar;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Find;

public interface ThingRepository extends DataRepository<Thing, Long> {
	@Find
	Thing findByName(String name);
}
