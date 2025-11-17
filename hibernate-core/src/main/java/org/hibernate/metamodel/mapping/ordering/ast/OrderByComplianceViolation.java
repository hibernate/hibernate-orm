/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.HibernateException;
import org.hibernate.jpa.JpaComplianceViolation;
import org.hibernate.metamodel.mapping.NonTransientException;

/**
 * @author Steve Ebersole
 */
public class OrderByComplianceViolation extends HibernateException implements JpaComplianceViolation, NonTransientException {
	public OrderByComplianceViolation(String message) {
		super( message );
	}

	public OrderByComplianceViolation(String message, Throwable cause) {
		super( message, cause );
	}
}
