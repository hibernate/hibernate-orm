/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.accesstype;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
@Entity
public class Shop {
	@Id
	long id;
	String name;
}
