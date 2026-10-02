package org.hibernate.query.sqm.produce.function;

import jakarta.annotation.Nonnull;

import org.hibernate.query.SemanticException;

/**
 * Represents a problem with the argument list of a function in HQL/JPQL.
 *
 * @author Gavin King
 *
 * @since 6.3
 */
public class FunctionArgumentException extends SemanticException {
	public FunctionArgumentException(@Nonnull String message) {
		super(message);
	}
}
