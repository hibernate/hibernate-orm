/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;

/**
 * Contract for things that need to be aware of JPA {@code orm.xml}-defined persistence-unit-defaults.
 * Only {@link MetadataBuildingOptions} are supported to implement this contract.
 *
 * @since 5.0
 *
 * @author Steve Ebersole
 */
public interface JpaOrmXmlPersistenceUnitDefaultAware {
	/**
	 * Represents the {@code persistence-unit-defaults} to be applied
	 */
	interface JpaOrmXmlPersistenceUnitDefaults {
		String getDefaultSchemaName();
		String getDefaultCatalogName();
		boolean shouldImplicitlyQuoteIdentifiers();
	}

	/**
	 * Apply the {@code orm.xml}-defined {@code persistence-unit-defaults} values.
	 *
	 * @param jpaOrmXmlPersistenceUnitDefaults The {@code persistence-unit-defaults} values
	 */
	void apply(JpaOrmXmlPersistenceUnitDefaults jpaOrmXmlPersistenceUnitDefaults);

	void apply(PersistenceUnitMetadata persistenceUnitMetadata);
}
