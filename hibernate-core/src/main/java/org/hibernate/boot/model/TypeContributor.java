/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import org.hibernate.service.ServiceRegistry;

/**
 * On object that contributes custom types and type descriptors, eventually
 * to a {@link org.hibernate.type.spi.TypeConfiguration}, via an instance
 * of {@link TypeContributions}.
 * <p>
 * The most common way to integrate a {@code TypeContributor} is by making
 * it discoverable via the Java {@link java.util.ServiceLoader} facility.
 * <p>
 * Alternatively, a {@code TypeContributor} may be supplied to
 * {@link org.hibernate.cfg.Configuration#registerTypeContributor(TypeContributor)}
 * or even {@link org.hibernate.boot.MetadataBuilder#applyTypes(TypeContributor)}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.type.spi.TypeConfiguration
 */
public interface TypeContributor {
	/**
	 * Contribute types
	 *
	 * @param typeContributions The callback for adding contributed types
	 * @param serviceRegistry The service registry
	 */
	void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry);
}
