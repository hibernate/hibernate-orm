/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple.entity;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;

@Incubating
public class CompositeValueGenerationException extends HibernateException {
	public CompositeValueGenerationException(String message) {
		super(message);
	}
}
