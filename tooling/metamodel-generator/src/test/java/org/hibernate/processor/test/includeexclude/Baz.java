/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
