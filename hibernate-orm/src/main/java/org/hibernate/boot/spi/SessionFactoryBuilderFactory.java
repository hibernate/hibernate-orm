/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.SessionFactoryBuilder;

/**
 * An extension point for integrators that wish to hook into the process of how a SessionFactory
 * is built.  Intended as a "discoverable service" ({@link java.util.ServiceLoader}).  There can
 * be at most one implementation discovered that returns a non-null SessionFactoryBuilder.
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryBuilderFactory {
	/**
	 * The contract method.  Return the SessionFactoryBuilder.  May return {@code null}
	 *
	 * @param metadata The metadata from which we will be building a SessionFactory.
	 * @param defaultBuilder The default SessionFactoryBuilder instance.  If the SessionFactoryBuilder being built
	 * here needs to use this passed SessionFactoryBuilder instance it is the responsibility of the built
	 * SessionFactoryBuilder impl to delegate configuration calls to the passed default impl.
	 *
	 * @return The SessionFactoryBuilder, or {@code null}
	 */
	public SessionFactoryBuilder getSessionFactoryBuilder(MetadataImplementor metadata, SessionFactoryBuilderImplementor defaultBuilder);
}
