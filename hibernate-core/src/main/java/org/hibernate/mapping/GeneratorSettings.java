/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;


import org.hibernate.Incubating;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;

/**
 * Exposes the default catalog and schema to the
 * {@linkplain KeyValue#createGenerator(Dialect, RootClass, Property, GeneratorSettings)
 * generator creation process}. The defaults specified here are ultimately
 * passed to the {@linkplain org.hibernate.generator.Generator generator}
 * itself via the {@link org.hibernate.generator.GeneratorCreationContext}.
 *
 * @see org.hibernate.cfg.MappingSettings#DEFAULT_CATALOG
 * @see org.hibernate.cfg.MappingSettings#DEFAULT_SCHEMA
 * @see org.hibernate.boot.spi.SessionFactoryOptions#getDefaultCatalog()
 * @see org.hibernate.boot.spi.SessionFactoryOptions#getDefaultSchema()
 *
 * @since 7
 *
 * @author Gavin King
 */
@Incubating
public interface GeneratorSettings {
	String getDefaultCatalog();
	String getDefaultSchema();
	SqlStringGenerationContext getSqlStringGenerationContext();
}
