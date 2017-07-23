/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import org.hibernate.query.NativeQuery.ResultNode;
import org.hibernate.query.sql.ResultRegistration;

/**
 * Producer for SQL AST QueryResult references specifically for
 * native queries.
 *
 * @author Steve Ebersole
 */
public interface ResultNodeImplementor extends ResultRegistration, ResultNode {
}
