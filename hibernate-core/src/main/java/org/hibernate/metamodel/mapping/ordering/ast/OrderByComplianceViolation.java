/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
