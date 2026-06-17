/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import java.lang.annotation.Annotation;
import java.util.EnumSet;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.bind.BindingSettings;
import org.hibernate.boot.models.bind.spi.QuotedIdentifierTarget;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.metamodel.CollectionClassification;

import jakarta.persistence.FetchType;

/// Binding-time options derived from the active metadata-building context.
///
/// These options normalize global defaults that many binders need, especially
/// default catalog/schema names and globally quoted identifier handling.  Keeping
/// that logic in one place prevents table, column, and constraint binders from
/// re-reading bootstrap settings or applying quoting rules inconsistently.
///
/// @since 9.0
/// @author Steve Ebersole
public class BindingOptionsImpl implements BindingOptions {
	private final Identifier defaultCatalogName;
	private final Identifier defaultSchemaName;
	private final EnumSet<QuotedIdentifierTarget> globallyQuotedIdentifierTargets;
	private final boolean createImplicitDiscriminatorsForJoinedInheritance;
	private final boolean ignoreExplicitDiscriminatorsForJoinedInheritance;
	private final FetchType defaultToOneFetchType;
	private final CollectionClassification defaultListSemantics;

	public BindingOptionsImpl(MetadataBuildingContext metadataBuildingContext) {
		this(
				metadataBuildingContext,
				metadataBuildingContext.getBuildingOptions().createImplicitDiscriminatorsForJoinedInheritance(),
				metadataBuildingContext.getBuildingOptions().ignoreExplicitDiscriminatorsForJoinedInheritance(),
				FetchType.EAGER
		);
	}

	public BindingOptionsImpl(
			MetadataBuildingContext metadataBuildingContext,
			BindingSettings bindingSettings) {
		this(
				metadataBuildingContext,
				bindingSettings.createImplicitDiscriminatorsForJoinedInheritance(),
				bindingSettings.ignoreExplicitDiscriminatorsForJoinedInheritance(),
				bindingSettings.defaultToOneFetchType()
		);
	}

	private BindingOptionsImpl(
			MetadataBuildingContext metadataBuildingContext,
			boolean createImplicitDiscriminatorsForJoinedInheritance,
			boolean ignoreExplicitDiscriminatorsForJoinedInheritance,
			FetchType defaultToOneFetchType) {
		final boolean globallyQuote = metadataBuildingContext.getBuildingOptions().getMappingDefaults().shouldImplicitlyQuoteIdentifiers();
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
				metadataBuildingContext.getBuildingOptions().getMappingDefaults().getImplicitCatalogName(),
				QuotedIdentifierTarget.CATALOG_NAME,
				globallyQuotedIdentifierTargets,
				jdbcEnvironment
		);
		defaultSchemaName = toIdentifier(
				metadataBuildingContext.getBuildingOptions().getMappingDefaults().getImplicitSchemaName(),
				QuotedIdentifierTarget.SCHEMA_NAME,
				globallyQuotedIdentifierTargets,
				jdbcEnvironment
		);
		this.createImplicitDiscriminatorsForJoinedInheritance = createImplicitDiscriminatorsForJoinedInheritance;
		this.ignoreExplicitDiscriminatorsForJoinedInheritance = ignoreExplicitDiscriminatorsForJoinedInheritance;
		this.defaultToOneFetchType = defaultToOneFetchType;
		this.defaultListSemantics = metadataBuildingContext.getBuildingOptions()
				.getMappingDefaults()
				.getImplicitListClassification();
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
		this.createImplicitDiscriminatorsForJoinedInheritance = false;
		this.ignoreExplicitDiscriminatorsForJoinedInheritance = false;
		this.defaultToOneFetchType = FetchType.EAGER;
		this.defaultListSemantics = CollectionClassification.LIST;
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

	@Override
	public boolean createImplicitDiscriminatorsForJoinedInheritance() {
		return createImplicitDiscriminatorsForJoinedInheritance;
	}

	@Override
	public boolean ignoreExplicitDiscriminatorsForJoinedInheritance() {
		return ignoreExplicitDiscriminatorsForJoinedInheritance;
	}

	@Override
	public FetchType getDefaultToOneFetchType() {
		return defaultToOneFetchType;
	}

	@Override
	public CollectionClassification getDefaultListSemantics() {
		return defaultListSemantics;
	}
}
