package org.hibernate.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.QueryException;

/**
 * Represents an error in the semantics (meaning) of a HQL/JPQL query.
 *
 * @author Steve Ebersole
 *
 * @see SyntaxException
 */
public class SemanticException extends QueryException {

	/**
	 * @deprecated this constructor does not carry information
	 *             about the query which caused the failure
	 */
	@Deprecated(since = "6.3")
	public SemanticException(@Nonnull String message) {
		super( message );
	}

	/**
	 * @deprecated this constructor does not carry information
	 *             about the query which caused the failure
	 */
	@Deprecated(since = "6.3")
	public SemanticException(@Nonnull String message, @Nullable Exception cause) {
		super( message, cause );
	}

	public SemanticException(@Nonnull String message, @Nullable String queryString) {
		super( message, queryString );
	}

	public SemanticException(@Nonnull String message, @Nullable String queryString, @Nullable Exception cause) {
		super( message, queryString, cause );
	}
}
