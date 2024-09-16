/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks;
import jakarta.persistence.PrePersist;

/**
 * @author Emmanuel Bernard
 */
public class ExceptionListener {
	@PrePersist
	public void raiseException(Object e) {
		throw new ArithmeticException( "1/0 impossible" );
	}
}
