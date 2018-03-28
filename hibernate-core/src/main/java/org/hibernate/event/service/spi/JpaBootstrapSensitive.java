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
 */
public interface JpaBootstrapSensitive {
	void wasJpaBootstrap(boolean wasJpaBootstrap);
}
