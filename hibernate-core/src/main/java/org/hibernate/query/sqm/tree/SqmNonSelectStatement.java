/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

/**
 * Used to more easily identifier non-SELECT (DML) statements by gross type.
 *
 * @author Steve Ebersole
 */
public interface SqmNonSelectStatement extends SqmStatement {
}
