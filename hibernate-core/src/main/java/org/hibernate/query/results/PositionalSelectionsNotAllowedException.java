/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public class PositionalSelectionsNotAllowedException extends HibernateException {
	public PositionalSelectionsNotAllowedException(String message) {
		super( message );
	}
}
