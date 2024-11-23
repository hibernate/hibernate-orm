/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

/**
 * Indicates an attempt to use a non-indexed collection as indexed.
 *
 * @author Steve Ebersole
 */
public class NotIndexedCollectionException extends SemanticException {
	public NotIndexedCollectionException(String message) {
		super( message );
	}
}
