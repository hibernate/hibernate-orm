/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.results.graph;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;

/**
 * Represents an aggregated {@link FetchTiming} and {@link FetchStyle} value
 *
 * @author Steve Ebersole
 */
public interface FetchOptions extends FetchTimingAccess, FetchStyleAccess {

	static FetchOptions valueOf(FetchTiming fetchTiming, FetchStyle fetchStyle) {
		return new FetchOptionsImpl( fetchTiming, fetchStyle );
	}
}
