/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
	public SessionFactoryBuilder getSessionFactoryBuilder(MetadataImplementor metadata, SessionFactoryBuilder defaultBuilder);
}
