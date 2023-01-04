/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.boot.spi;

import java.util.List;

import org.hibernate.integrator.spi.Integrator;

/**
 * An object that provides a list of {@link Integrator}s to the JPA persistence provider.
 * <p>
 * An implementation may be registered with the JPA provider using the property
 * {@value org.hibernate.jpa.boot.spi.JpaSettings#INTEGRATOR_PROVIDER}.
 *
 * @author Steve Ebersole
 */
public interface IntegratorProvider {
	List<Integrator> getIntegrators();
}
