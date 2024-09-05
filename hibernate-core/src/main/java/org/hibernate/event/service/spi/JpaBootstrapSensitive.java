/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.service.spi;

/**
 * Defines an event listener that is sensitive to whether a native or jpa bootstrap was performed
 *
 * @author Steve Ebersole
 *
 * @deprecated This is no longer implemented by any listener
 */
@Deprecated(since = "7")
public interface JpaBootstrapSensitive {
	void wasJpaBootstrap(boolean wasJpaBootstrap);
}
