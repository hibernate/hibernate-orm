/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.internal;

import java.io.Serializable;
import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.config.spi.ConfigurationService;

/**
 * @author Gail Badner
 */
public class IdentifierGeneratorResolver {

	private final MetadataImplementor metadata;

	IdentifierGeneratorResolver(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	// IdentifierGeneratorResolver.resolve() must execute after AttributeTypeResolver.resolve()
	// to ensure that identifier type is resolved.
	@SuppressWarnings( {"unchecked"} )
	void resolve() {
		for ( EntityBinding entityBinding : metadata.getEntityBindings() ) {
			if ( entityBinding.isRoot() ) {
				Properties properties = new Properties( );
				properties.putAll(
						metadata.getServiceRegistry()
								.getService( ConfigurationService.class )
								.getSettings()
				);
				//TODO: where should these be added???
				if ( ! properties.contains( AvailableSettings.PREFER_POOLED_VALUES_LO ) ) {
					properties.put( AvailableSettings.PREFER_POOLED_VALUES_LO, "false" );
				}
				if ( ! properties.contains( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER ) ) {
					properties.put(
							PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER,
							new ObjectNameNormalizerImpl( metadata )
					);
				}
				entityBinding.getHierarchyDetails().getEntityIdentifier().createIdentifierGenerator(
						metadata.getIdentifierGeneratorFactory(),
						properties
				);
			}
		}
	}

	private static class ObjectNameNormalizerImpl extends ObjectNameNormalizer implements Serializable {
		private final boolean useQuotedIdentifiersGlobally;
		private final NamingStrategy namingStrategy;

		private ObjectNameNormalizerImpl(MetadataImplementor metadata ) {
			this.useQuotedIdentifiersGlobally = metadata.isGloballyQuotedIdentifiers();
			this.namingStrategy = metadata.getNamingStrategy();
		}

		@Override
		protected boolean isUseQuotedIdentifiersGlobally() {
			return useQuotedIdentifiersGlobally;
		}

		@Override
		protected NamingStrategy getNamingStrategy() {
			return namingStrategy;
		}
	}
}
