/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import org.hibernate.Incubating;
import org.hibernate.ScrollableResults;

/**
 * @author Steve Ebersole
 *
 * @since 5.2
 */
@Incubating
public interface ScrollableResultsImplementor extends ScrollableResults {
	boolean isClosed();
	int getNumberOfTypes();
}
