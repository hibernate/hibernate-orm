/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
