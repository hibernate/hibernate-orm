/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.basic;

import jakarta.data.repository.Repository;

@Repository
public interface Concrete extends IdOperations<Thing> {
}
