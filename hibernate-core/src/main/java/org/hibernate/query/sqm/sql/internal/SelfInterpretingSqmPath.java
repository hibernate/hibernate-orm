/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;

/**
 * Optional contract for sqm-paths which need special interpretation handling
 *
 * @author Steve Ebersole
 */
public interface SelfInterpretingSqmPath<T> extends SqmPath<T> {
	/**
	 * Perform the interpretation
	 */
	SqmPathInterpretation<T> interpret(
			SqlAstCreationState sqlAstCreationState,
			SemanticQueryWalker<?> sqmWalker,
			boolean jpaQueryComplianceEnabled);
}
