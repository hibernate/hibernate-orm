/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;
import org.hibernate.type.Type;

/**
 * Interface for nodes which wish to be made aware of any determined "expected
 * type" based on the context within they appear in the query.
 *
 * @author Steve Ebersole
 */
public interface ExpectedTypeAwareNode {
	public void setExpectedType(Type expectedType);
	public Type getExpectedType();
}
