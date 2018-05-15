/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

/**
 * Marker interface for any types that are allowed as function returns
 *
 * @author Steve Ebersole
 */
public interface AllowableFunctionReturnType<T> extends ExpressableType<T> {
}
