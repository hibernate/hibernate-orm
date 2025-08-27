/*
 * SPDX-License-Identifier: Apache-2.0
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
