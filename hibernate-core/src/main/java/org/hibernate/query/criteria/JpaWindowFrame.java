/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria;

import org.hibernate.query.sqm.FrameKind;

import jakarta.persistence.criteria.Expression;

/**
 * @author Marco Belladelli
 */
public interface JpaWindowFrame {
	FrameKind getKind();

	Expression<?> getExpression();
}
