/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.service.spi;

import org.hibernate.Incubating;

@Incubating
@FunctionalInterface
public interface EventActionWithParameter<T, U, X> {

	void applyEventToListener(T eventListener, U action, X param);

}
