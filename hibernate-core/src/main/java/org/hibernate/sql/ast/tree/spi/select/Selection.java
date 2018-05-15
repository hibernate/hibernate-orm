/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.select;

import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * todo (6.0) : remove this in favor of {@link DomainResultProducer} approach
 *
 * @author Steve Ebersole
 */
public interface Selection {
	DomainResultProducer getQueryResultProducer();

	/**
	 * The (optional) "result variable" for the selection.  This is the JPA
	 * term for the selection's alias; JPA uses this term to more easily
	 * distinguish it from the alias used to qualify any domain references
	 * (think "table alias"), which it terms an "identification variable".
	 *
	 * May return `null`
	 */
	String getResultVariable();

	Expression getSelectedExpression();
}
