/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.AssertionFailure;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.AttributeConverterAutoApplyHandler;
import org.hibernate.boot.spi.AttributeConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class AttributeConverterManager implements AttributeConverterAutoApplyHandler {
	private static final Logger log = Logger.getLogger( AttributeConverterManager.class );

	private Map<Class, AttributeConverterDescriptor> attributeConverterDescriptorsByClass;

	void addConverter(AttributeConverterDescriptor descriptor) {
		if ( attributeConverterDescriptorsByClass == null ) {
			attributeConverterDescriptorsByClass = new ConcurrentHashMap<Class, AttributeConverterDescriptor>();
		}

		final Object old = attributeConverterDescriptorsByClass.put(
				descriptor.getAttributeConverter().getClass(),
				descriptor
		);

		if ( old != null ) {
			throw new AssertionFailure(
					String.format(
							Locale.ENGLISH,
							"AttributeConverter class [%s] registered multiple times",
							descriptor.getAttributeConverter().getClass()
					)
			);
		}
	}

	private Collection<AttributeConverterDescriptor> converterDescriptors() {
		if ( attributeConverterDescriptorsByClass == null ) {
			return Collections.emptyList();
		}
		return attributeConverterDescriptorsByClass.values();
	}

	@Override
	public AttributeConverterDescriptor findAutoApplyConverterForAttribute(
			XProperty xProperty,
			MetadataBuildingContext context) {
		List<AttributeConverterDescriptor> matched = new ArrayList<AttributeConverterDescriptor>();

		for ( AttributeConverterDescriptor descriptor : converterDescriptors() ) {
			log.debugf(
					"Checking auto-apply AttributeConverter [%s] (type=%s) for match against attribute : %s.%s (type=%s)",
					descriptor.toString(),
					descriptor.getDomainType().getSimpleName(),
					xProperty.getDeclaringClass().getName(),
					xProperty.getName(),
					xProperty.getType().getName()
			);

			if ( descriptor.shouldAutoApplyToAttribute( xProperty, context ) ) {
				matched.add( descriptor );
			}
		}

		if ( matched.isEmpty() ) {
			return null;
		}

		if ( matched.size() == 1 ) {
			return matched.get( 0 );
		}

		// otherwise, we had multiple matches
		throw new RuntimeException(
				String.format(
						Locale.ROOT,
						"Multiple auto-apply converters matched attribute [%s.%s] : %s",
						xProperty.getDeclaringClass().getName(),
						xProperty.getName(),
						StringHelper.join( matched, RENDERER )
				)
		);
	}

	@Override
	public AttributeConverterDescriptor findAutoApplyConverterForCollectionElement(
			XProperty xProperty,
			MetadataBuildingContext context) {
		List<AttributeConverterDescriptor> matched = new ArrayList<AttributeConverterDescriptor>();

		for ( AttributeConverterDescriptor descriptor : converterDescriptors() ) {
			log.debugf(
					"Checking auto-apply AttributeConverter [%s] (type=%s) for match against collection attribute's element : %s.%s (type=%s)",
					descriptor.toString(),
					descriptor.getDomainType().getSimpleName(),
					xProperty.getDeclaringClass().getName(),
					xProperty.getName(),
					xProperty.getElementClass().getName()
			);
			if ( descriptor.shouldAutoApplyToCollectionElement( xProperty, context ) ) {
				matched.add( descriptor );
			}
		}

		if ( matched.isEmpty() ) {
			return null;
		}

		if ( matched.size() == 1 ) {
			return matched.get( 0 );
		}

		// otherwise, we had multiple matches
		throw new RuntimeException(
				String.format(
						Locale.ROOT,
						"Multiple auto-apply converters matched attribute [%s.%s] : %s",
						xProperty.getDeclaringClass().getName(),
						xProperty.getName(),
						StringHelper.join( matched, RENDERER )
				)
		);
	}

	@Override
	public AttributeConverterDescriptor findAutoApplyConverterForMapKey(
			XProperty xProperty,
			MetadataBuildingContext context) {
		List<AttributeConverterDescriptor> matched = new ArrayList<AttributeConverterDescriptor>();

		for ( AttributeConverterDescriptor descriptor : converterDescriptors() ) {
			log.debugf(
					"Checking auto-apply AttributeConverter [%s] (type=%s) for match against map attribute's key : %s.%s (type=%s)",
					descriptor.toString(),
					descriptor.getDomainType().getSimpleName(),
					xProperty.getDeclaringClass().getName(),
					xProperty.getName(),
					xProperty.getMapKey().getName()
			);
			if ( descriptor.shouldAutoApplyToMapKey( xProperty, context ) ) {
				matched.add( descriptor );
			}
		}

		if ( matched.isEmpty() ) {
			return null;
		}

		if ( matched.size() == 1 ) {
			return matched.get( 0 );
		}

		// otherwise, we had multiple matches
		throw new RuntimeException(
				String.format(
						Locale.ROOT,
						"Multiple auto-apply converters matched attribute [%s.%s] : %s",
						xProperty.getDeclaringClass().getName(),
						xProperty.getName(),
						StringHelper.join( matched, RENDERER )
				)
		);
	}

	private static StringHelper.Renderer<AttributeConverterDescriptor> RENDERER = new StringHelper.Renderer<AttributeConverterDescriptor>() {
		@Override
		public String render(AttributeConverterDescriptor value) {
			return value.getAttributeConverter().getClass().getName();
		}
	};

}
