/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.results.dynamic;

import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.ResultBuilder;

/**
 * ResultBuilder specialization for results added through the Hibernate-specific
 * {@link NativeQuery} result definition methods.
 *
 * @see NativeQuery#addScalar
 * @see NativeQuery#addInstantiation
 * @see NativeQuery#addAttributeResult
 * @see NativeQuery#addEntity
 * @see NativeQuery#addRoot
 *
 * @author Steve Ebersole
 */
public interface DynamicResultBuilder extends ResultBuilder, NativeQuery.ReturnableResultNode {
	DynamicResultBuilder cacheKeyInstance();
}
