/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator;

import java.util.Properties;

import org.hibernate.Incubating;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Value;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import static org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl.fromExplicit;

/**
 * Access to information useful during {@linkplain Generator} creation and initialization.
 *
 * @see AnnotationBasedGenerator
 * @see org.hibernate.id.Configurable#configure(GeneratorCreationContext, Properties)
 *
 * @since 6.2
 */
@Incubating
public interface GeneratorCreationContext {
	/**
	 * View of the relational database objects (tables, sequences, etc.)
	 * and namespaces (catalogs and schemas). Generators may add new
	 * tables or sequences to the returned {@link Database}.
	 */
	Database getDatabase();

	/**
	 * Access to available services.
	 */
	ServiceRegistry getServiceRegistry();

	/**
	 * The default catalog name, if one.
	 */
	String getDefaultCatalog();

	/**
	 * The default schema name, if one.
	 */
	String getDefaultSchema();

	/**
	 * Mapping details for the entity.
	 */
	PersistentClass getPersistentClass();

	/**
	 * Mapping details for the root of the {@linkplain #getPersistentClass() entity} hierarchy.
	 */
	RootClass getRootClass();

	/**
	 * The entity identifier or id-bag property details.
	 */
	Property getProperty();

	/**
	 * The identifier.
	 */
	Value getValue();

	/**
	 * Mapping details for the identifier type.
	 */
	default Type getType() {
		return getProperty().getType();
	}

	/**
	 * The {@link SqlStringGenerationContext} to use when generating SQL.
	 */
	default SqlStringGenerationContext getSqlStringGenerationContext() {
		final Database database = getDatabase();
		return fromExplicit( database.getJdbcEnvironment(), database, getDefaultCatalog(), getDefaultSchema() );
	}
}
