/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import org.hibernate.sql.ast.tree.spi.expression.GenericParameter;

/**
 * A resolver for Type based on a parameter value being bound, when no
 * explicit type information is supplied.
 *
 * @author Steve Ebersole
 */
public interface QueryParameterBindingTypeResolver {
	GenericParameter.AllowableType resolveParameterBindType(Object bindValue);
	GenericParameter.AllowableType resolveParameterBindType(Class clazz);
}
