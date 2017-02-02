/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.spi;

/**
 * Additional optional contract for connection observers to indicate that they should be released when the physical
 * connection is released.
 *
 * @author Steve Ebersole
 */
public interface NonDurableConnectionObserver extends ConnectionObserver {
}
