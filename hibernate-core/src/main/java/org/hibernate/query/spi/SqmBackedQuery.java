/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.spi;

import org.hibernate.query.ParameterMetadata;
import org.hibernate.sqm.query.SqmStatement;

/**
 * @author Steve Ebersole
 */
public interface SqmBackedQuery<R> {
	SqmStatement getSqmStatement();
	Class<R> getResultType();
	EntityGraphQueryHint getEntityGraphHint();
	QueryOptions getQueryOptions();
	ParameterMetadata getParameterMetadata();
	QueryParameterBindings getQueryParameterBindings();
}
