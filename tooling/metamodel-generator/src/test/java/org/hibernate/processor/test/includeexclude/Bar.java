/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.includeexclude;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Bar {
	@Id long id;
}
