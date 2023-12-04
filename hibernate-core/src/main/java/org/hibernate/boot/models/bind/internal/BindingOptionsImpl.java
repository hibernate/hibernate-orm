/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import java.lang.annotation.Annotation;
import java.util.EnumSet;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.QuotedIdentifierTarget;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * @author Steve Ebersole
 */
public class BindingOptionsImpl implements BindingOptions {
	private final Identifier defaultCatalogName;
	private final Identifier defaultSchemaName;
	private final EnumSet<QuotedIdentifierTarget> globallyQuotedIdentifierTargets;

	public BindingOptionsImpl(MetadataBuildingContext metadataBuildingContext) {
		final boolean globallyQuote = metadataBuildingContext.getMappingDefaults().shouldImplicitlyQuoteIdentifiers();
		final boolean skipColumnDefinitions = metadataBuildingContext
				.getBootstrapContext()
				.getServiceRegistry()
				.getService( ConfigurationService.class )
				.getSetting(
						AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS_SKIP_COLUMN_DEFINITIONS,
						StandardConverters.BOOLEAN,
						false
				);

		if ( !globallyQuote ) {
			globallyQuotedIdentifierTargets = EnumSet.noneOf( QuotedIdentifierTarget.class );
		}
		else {
			globallyQuotedIdentifierTargets = EnumSet.allOf( QuotedIdentifierTarget.class );
			if ( skipColumnDefinitions ) {
				globallyQuotedIdentifierTargets.remove( QuotedIdentifierTarget.COLUMN_DEFINITION );
			}
		}

		final JdbcEnvironment jdbcEnvironment = metadataBuildingContext.getMetadataCollector()
				.getDatabase()
				.getJdbcEnvironment();

		defaultCatalogName = toIdentifier(
				metadataBuildingContext.getMappingDefaults().getImplicitCatalogName(),
				QuotedIdentifierTarget.CATALOG_NAME,
				globallyQuotedIdentifierTargets,
				jdbcEnvironment
		);
		defaultSchemaName = toIdentifier(
				metadataBuildingContext.getMappingDefaults().getImplicitSchemaName(),
				QuotedIdentifierTarget.SCHEMA_NAME,
				globallyQuotedIdentifierTargets,
				jdbcEnvironment
		);
	}

	public static <A extends Annotation> Identifier toIdentifier(
			String name,
			QuotedIdentifierTarget target,
			EnumSet<QuotedIdentifierTarget> globallyQuotedIdentifierTargets,
			JdbcEnvironment jdbcEnvironment) {
		final boolean globallyQuoted = globallyQuotedIdentifierTargets.contains( target );
		return jdbcEnvironment.getIdentifierHelper().toIdentifier( name, globallyQuoted );
	}

	public BindingOptionsImpl(Identifier defaultCatalogName, Identifier defaultSchemaName) {
		this( defaultCatalogName, defaultSchemaName, EnumSet.noneOf( QuotedIdentifierTarget.class ) );
	}

	public BindingOptionsImpl(
			Identifier defaultCatalogName,
			Identifier defaultSchemaName,
			EnumSet<QuotedIdentifierTarget> globallyQuotedIdentifierTargets) {
		this.defaultCatalogName = defaultCatalogName;
		this.defaultSchemaName = defaultSchemaName;
		this.globallyQuotedIdentifierTargets = globallyQuotedIdentifierTargets;
	}

	@Override
	public Identifier getDefaultCatalogName() {
		return defaultCatalogName;
	}

	@Override
	public Identifier getDefaultSchemaName() {
		return defaultSchemaName;
	}

	@Override
	public EnumSet<QuotedIdentifierTarget> getGloballyQuotedIdentifierTargets() {
		return globallyQuotedIdentifierTargets;
	}
}
