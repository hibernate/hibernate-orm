/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.boot.spi;

import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.spi.MetadataBuilderContributor;

/**
 * Enumerates SPI-related settings that are specific to the use of Hibernate
 * as a JPA {@link jakarta.persistence.spi.PersistenceProvider}.
 *
 * @author Gavin King
 *
 * @since 6.2
 */
public class JpaSettings {

	/**
	 * Names a {@link IntegratorProvider}
	 */
	public static final String INTEGRATOR_PROVIDER = "hibernate.integrator_provider";

	/**
	 * Names a {@link StrategyRegistrationProviderList}
	 */
	public static final String STRATEGY_REGISTRATION_PROVIDERS = "hibernate.strategy_registration_provider";

	/**
	 * Names a {@link TypeContributorList}
	 *
	 * @deprecated Consider using {@linkplain java.util.ServiceLoader discovery} instead to
	 * dynamically locate {@linkplain TypeContributor contributors}.
	 */
	@Deprecated(forRemoval = true)
	public static final String TYPE_CONTRIBUTORS = "hibernate.type_contributors";

	/**
	 * Names a {@link MetadataBuilderContributor}
	 *
	 * @deprecated Use {@linkplain java.util.ServiceLoader discovery} instead.
	 */
	@Deprecated(forRemoval = true)
	public static final String METADATA_BUILDER_CONTRIBUTOR = "hibernate.metadata_builder_contributor";

}
