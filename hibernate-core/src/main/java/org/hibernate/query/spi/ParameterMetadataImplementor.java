/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.query.spi;

import java.util.function.Predicate;

import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;

/**
 * @author Steve Ebersole
 */
public interface ParameterMetadataImplementor extends ParameterMetadata {
	interface ParameterCollector {
		void collect(QueryParameter<?> queryParameter);
	}

	void collectAllParameters(ParameterCollector collector);

	boolean hasAnyMatching(Predicate<? super QueryParameter> filter);
}
