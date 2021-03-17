/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.event.internal;

import org.hibernate.jpa.event.spi.CallbackBuilder;
import org.hibernate.jpa.event.spi.CallbackRegistry;

public interface CallbackRegistryImplementor extends CallbackRegistry, CallbackBuilder.CallbackRegistrar {

	void release();

}
