/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.convert.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterAutoApplyHandler;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class AttributeConverterManager implements ConverterAutoApplyHandler {
	private static final Logger log = Logger.getLogger( AttributeConverterManager.class );

	private Map<Class, ConverterDescriptor> attributeConverterDescriptorsByClass;

	public void addConverter(ConverterDescriptor descriptor) {
		if ( attributeConverterDescriptorsByClass == null ) {
			attributeConverterDescriptorsByClass = new ConcurrentHashMap<>();
		}

		final Object old = attributeConverterDescriptorsByClass.put(
				descriptor.getAttributeConverterClass(),
				descriptor
		);

		if ( old != null ) {
			throw new AssertionFailure(
					String.format(
							Locale.ENGLISH,
							"AttributeConverter class [%s] registered multiple times",
							descriptor.getAttributeConverterClass()
					)
			);
		}
	}

	private Collection<ConverterDescriptor> converterDescriptors() {
		if ( attributeConverterDescriptorsByClass == null ) {
			return Collections.emptyList();
		}
		return attributeConverterDescriptorsByClass.values();
	}

	enum ConversionSite {
		ATTRIBUTE( "basic attribute" ),
		COLLECTION_ELEMENT( "collection attribute's element" ),
		MAP_KEY( "map attribute's key" );

		private final String siteDescriptor;

		ConversionSite(String siteDescriptor) {
			this.siteDescriptor = siteDescriptor;
		}

		public String getSiteDescriptor() {
			return siteDescriptor;
		}
	}
	@Override
	public ConverterDescriptor findAutoApplyConverterForAttribute(
			XProperty xProperty,
			MetadataBuildingContext context) {
		return locateMatchingConverter(
				xProperty,
				ConversionSite.ATTRIBUTE,
				(autoApplyDescriptor) -> autoApplyDescriptor.getAutoAppliedConverterDescriptorForAttribute( xProperty, context )
		);
	}

	private static StringHelper.Renderer<ConverterDescriptor> RENDERER = value -> value.getAttributeConverterClass().getName();

	private ConverterDescriptor locateMatchingConverter(
			XProperty xProperty,
			ConversionSite conversionSite,
			Function<AutoApplicableConverterDescriptor, ConverterDescriptor> matcher) {
		List<ConverterDescriptor> matches = new ArrayList<>();

		for ( ConverterDescriptor descriptor : converterDescriptors() ) {
			log.debugf(
					"Checking auto-apply AttributeConverter [%s] (domain-type=%s) for match against %s : %s.%s (type=%s)",
					descriptor.getAttributeConverterClass().getName(),
					descriptor.getDomainValueResolvedType().getSignature(),
					conversionSite.getSiteDescriptor(),
					xProperty.getDeclaringClass().getName(),
					xProperty.getName(),
					xProperty.getType().getName()
			);

			final ConverterDescriptor match = matcher.apply( descriptor.getAutoApplyDescriptor() );

			if ( match != null ) {
				matches.add( descriptor );
			}
		}

		if ( matches.isEmpty() ) {
			return null;
		}

		if ( matches.size() == 1 ) {
			return matches.get( 0 );
		}

		// otherwise, we had multiple matches
		throw new RuntimeException(
				String.format(
						Locale.ROOT,
						"Multiple auto-apply converters matched %s [%s.%s] : %s",
						conversionSite.getSiteDescriptor(),
						xProperty.getDeclaringClass().getName(),
						xProperty.getName(),
						StringHelper.join( matches, RENDERER )
				)
		);
	}

	@Override
	public ConverterDescriptor findAutoApplyConverterForCollectionElement(
			XProperty xProperty,
			MetadataBuildingContext context) {
		return locateMatchingConverter(
				xProperty,
				ConversionSite.COLLECTION_ELEMENT,
				(autoApplyDescriptor) -> autoApplyDescriptor.getAutoAppliedConverterDescriptorForCollectionElement( xProperty, context )
		);
	}

	@Override
	public ConverterDescriptor findAutoApplyConverterForMapKey(
			XProperty xProperty,
			MetadataBuildingContext context) {
		return locateMatchingConverter(
				xProperty,
				ConversionSite.MAP_KEY,
				(autoApplyDescriptor) -> autoApplyDescriptor.getAutoAppliedConverterDescriptorForMapKey( xProperty, context )
		);
	}

}
