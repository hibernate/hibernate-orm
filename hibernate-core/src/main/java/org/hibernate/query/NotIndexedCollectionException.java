package org.hibernate.query;

import jakarta.annotation.Nonnull;

/**
 * Indicates an attempt to use a non-indexed collection as indexed.
 *
 * @author Steve Ebersole
 */
public class NotIndexedCollectionException extends SemanticException {
	public NotIndexedCollectionException(@Nonnull String message) {
		super( message );
	}
}
