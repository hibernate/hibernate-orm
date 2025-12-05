/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.query.common.FrameKind;

import jakarta.persistence.criteria.Expression;

/**
 * Common contract for a {@link JpaWindow} frame specification.
 *
 * @author Marco Belladelli
 */
@Incubating
public interface JpaWindowFrame {
	/**
	 * Get the {@link FrameKind} of this window frame.
	 *
	 * @return the window frame kind
	 */
	FrameKind getKind();

	/**
	 * Get the {@link Expression} of this window frame.
	 *
	 * @return the window frame expression
	 */
	@Nullable Expression<?> getExpression();
}
