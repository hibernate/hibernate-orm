/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import org.hibernate.HibernateException;

/**
 * Indicates an attempt to find an unknown query parameter or an attempt to
 * bind a value to an unknown query parameter
 *
 * @see Query#getParameter
 * @see Query#setParameter
 * @see Query#setParameterList
 *
 * @author Steve Ebersole
 */
public class UnknownParameterException extends HibernateException {
	public UnknownParameterException(String message) {
		super( message );
	}
}
