/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.spi;

import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.domain.SqmNavigableReference;

/**
 * @asciidoc
 *
 * Contract for things that can be part of a path structure, including:
 *
 * 		* package name
 * 		* class name
 * 		* field name
 * 		* enum name
 * 		* {@link SqmNavigableReference}
 *
 * @author Steve Ebersole
 */
public interface SemanticPathPart {
	SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState);

	SqmPath resolveIndexedAccess(
			SqmExpression selector,
			boolean isTerminal,
			SqmCreationState creationState);
}
