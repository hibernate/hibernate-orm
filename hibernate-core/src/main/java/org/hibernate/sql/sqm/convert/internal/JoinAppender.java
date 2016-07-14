/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.convert.internal;

import org.hibernate.sql.sqm.ast.from.TableBinding;
import org.hibernate.sqm.query.JoinType;

/**
 * @author Steve Ebersole
 */
public interface JoinAppender {
	interface JoinPredicateAppender {
		void appendJoinPredicate();
	}

	JoinPredicateAppender appendJoin(TableBinding tableBinding, JoinType joinType);
}
