/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import org.hibernate.internal.AbstractSharedSessionContract;
import org.hibernate.query.ParameterMetadata;

/**
 * Acts as a cache for various things used by (and produced by) the translation
 * of a query.
 *
 * @author Steve Ebersole
 */
public interface QueryInterpretations {
	interface ParameterMetadataKey {
	}

	ParameterMetadata getParameterMetadata(ParameterMetadataKey key);

	void cacheParameterMetadata(ParameterMetadataKey key, ParameterMetadata parameterMetadata);

}
