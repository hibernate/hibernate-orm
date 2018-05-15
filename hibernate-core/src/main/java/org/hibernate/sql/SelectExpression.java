/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;


import org.hibernate.annotations.Remove;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 *
 * @deprecated Converting to use SQL AST
 */
@Deprecated
@Remove
public interface SelectExpression {
	public String getExpression();
	public String getAlias();
}
