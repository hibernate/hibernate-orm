/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
