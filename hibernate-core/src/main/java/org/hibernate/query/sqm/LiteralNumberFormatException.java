package org.hibernate.query.sqm;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.query.SemanticException;

/**
 * @author Steve Ebersole
 */
public class LiteralNumberFormatException extends SemanticException {
	public LiteralNumberFormatException(@Nonnull String message) {
		super( message );
	}

	public LiteralNumberFormatException(@Nonnull String message, @Nullable Exception cause) {
		super( message, cause );
	}
}
