/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import org.hibernate.query.sqm.tree.expression.SqmParameter;

/**
 * Describes the context in which a parameter is declared.  This is  used mainly to
 * determine metadata about the parameter ({@link SqmParameter#allowMultiValuedBinding()}, e.g.)
 *
 * @author Steve Ebersole
 */
public interface ParameterDeclarationContext {
	/**
	 * Are multi-valued parameter bindings allowed in this context?
	 *
	 * @return {@code true} if they are; {@code false} otherwise.
	 */
	boolean isMultiValuedBindingAllowed();
}
