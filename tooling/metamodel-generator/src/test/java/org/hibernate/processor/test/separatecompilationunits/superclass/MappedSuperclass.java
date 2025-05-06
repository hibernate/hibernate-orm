/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.separatecompilationunits.superclass;

import jakarta.persistence.Id;

/**
 * @author Hardy Ferentschik
 */
@jakarta.persistence.MappedSuperclass
public class MappedSuperclass {
	@Id
	private long id;
}
