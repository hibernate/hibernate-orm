/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import java.util.List;

import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * The general contract for INSERT statements.  At the moment only the INSERT-SELECT
 * forms is implemented/supported.
 *
 * @author Steve Ebersole
 */
public interface SqmInsertStatement extends SqmNonSelectStatement {
	SqmRoot getInsertTarget();
	List<SqmSingularAttributeReference> getStateFields();
}
