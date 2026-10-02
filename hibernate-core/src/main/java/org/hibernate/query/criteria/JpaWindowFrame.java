package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.query.common.FrameKind;

import jakarta.persistence.criteria.Expression;

/**
 * Common contract for a {@link JpaWindow} frame specification.
 *
 * @author Marco Belladelli
 */
@Incubating(since = "6.3")
public interface JpaWindowFrame {
	/**
	 * Get the {@link FrameKind} of this window frame.
	 *
	 * @return the window frame kind
	 */
	@Nonnull
	FrameKind getKind();

	/**
	 * Get the {@link Expression} of this window frame.
	 *
	 * @return the window frame expression
	 */
	@Nullable Expression<?> getExpression();
}
