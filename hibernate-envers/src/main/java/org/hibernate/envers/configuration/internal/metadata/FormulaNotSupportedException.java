/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata;

import org.hibernate.HibernateException;

/**
 * Exception indicating that a formula mapping was encountered where it is not currently supported
 *
 * @author Steve Ebersole
 */
public class FormulaNotSupportedException extends HibernateException {
	private static final String MSG = "Formula mappings (aside from @DiscriminatorValue) are currently not supported";

	/**
	 * Constructs a FormulaNotSupportedException using a standard message
	 */
	public FormulaNotSupportedException() {
		super( MSG );
	}
}
