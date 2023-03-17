/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.jta.platform.spi;

import org.hibernate.service.JavaServiceLoadable;

/**
 * A {@link java.util.ServiceLoader}-style provider of {@link JtaPlatform}
 * instances. Used when an explicit {@code JtaPlatform} is not provided.
 *
 * @see JtaPlatform
 * @see JtaPlatformResolver
 *
 * @author Steve Ebersole
 */
@JavaServiceLoadable
public interface JtaPlatformProvider {
	/**
	 * Retrieve the JtaPlatform provided by this environment.
	 *
	 * @return The provided JtaPlatform
	 */
	JtaPlatform getProvidedJtaPlatform();
}
