/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.xmlembeddable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Foo {
	@Id
	long id;
}
