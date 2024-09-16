/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.xmlembeddable.foo;

import java.io.Serializable;

/**
 * @author Hardy Ferentschik
 */
public class BusinessId<T> implements Serializable {
	private long businessId;
}
