/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.spi;

/**
 * Specialized {@link org.hibernate.service.ServiceRegistry} implementation that holds services which need access
 * to the {@link org.hibernate.SessionFactory} during initialization.
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryServiceRegistry extends ServiceRegistryImplementor {
}
