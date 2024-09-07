/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria;

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
	Expression<?> getExpression();
}
