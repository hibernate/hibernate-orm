/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.includeexclude;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.processing.Exclude;

@Exclude
@Entity
public class Baz {
	@Id long id;
}
