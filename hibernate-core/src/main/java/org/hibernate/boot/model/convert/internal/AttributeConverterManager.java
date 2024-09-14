/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

import org.hibernate.AnnotationException;
import org.hibernate.HibernateException;
import org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterAutoApplyHandler;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MemberDetails;

import org.jboss.logging.Logger;

import com.fasterxml.classmate.ResolvedType;

import static java.util.stream.Collectors.toList;
import static org.hibernate.boot.model.convert.internal.ConverterHelper.resolveAttributeType;
import static org.hibernate.boot.model.convert.internal.ConverterHelper.resolveConverterClassParamTypes;

/**
 * @implNote It is important that all {@link RegisteredConversion} be registered
 * prior to attempts to register any {@link ConverterDescriptor}
 *
 * @author Steve Ebersole
 */
public class AttributeConverterManager implements ConverterAutoApplyHandler {
	private static final Logger log = Logger.getLogger( AttributeConverterManager.class );

	private Map<Class<?>, ConverterDescriptor> attributeConverterDescriptorsByClass;
	private Map<Class<?>, RegisteredConversion> registeredConversionsByDomainType;

	public RegisteredConversion findRegisteredConversion(Class<?> domainType) {
		if ( registeredConversionsByDomainType == null ) {
			return null;
		}
		return registeredConversionsByDomainType.get( domainType );
	}

	public void addConverter(ConverterDescriptor descriptor) {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Starting AttributeConverterManager#addConverter : `%s`",
					descriptor.getAttributeConverterClass().getName() );
		}

		if ( registeredConversionsByDomainType != null ) {
			final Class<?> domainType = descriptor.getDomainValueResolvedType().getErasedType();
			final RegisteredConversion registeredConversion = registeredConversionsByDomainType.get( domainType );
			if ( registeredConversion != null ) {
				// we can skip registering the converter, the RegisteredConversion will always take precedence
				if ( log.isDebugEnabled() ) {
					log.debugf( "Skipping registration of discovered AttributeConverter `%s` for auto-apply",
							descriptor.getAttributeConverterClass().getName() );
				}
				return;
			}
		}

		if ( attributeConverterDescriptorsByClass == null ) {
			attributeConverterDescriptorsByClass = new ConcurrentHashMap<>();
		}

		final Object old = attributeConverterDescriptorsByClass.put(
				descriptor.getAttributeConverterClass(),
				descriptor
		);

		if ( old != null ) {
			throw new HibernateException(
					String.format(
							Locale.ENGLISH,
							"AttributeConverter class [%s] registered multiple times",
							descriptor.getAttributeConverterClass()
					)
			);
		}
	}

	public void addRegistration(RegisteredConversion conversion, BootstrapContext context) {
		if ( registeredConversionsByDomainType == null ) {
			registeredConversionsByDomainType = new ConcurrentHashMap<>();
		}

		final Class<?> domainType;
		if ( conversion.getExplicitDomainType().equals( void.class ) ) {
			// the registration did not define an explicit domain-type, so inspect the converter
			final List<ResolvedType> converterParamTypes =
					resolveConverterClassParamTypes( conversion.getConverterType(), context.getClassmateContext() );
			domainType = converterParamTypes.get( 0 ).getErasedType();
		}
		else {
			domainType = conversion.getExplicitDomainType();
		}

		// make sure we are not overriding a previous conversion registration
		final RegisteredConversion existingRegistration = registeredConversionsByDomainType.get( domainType );
		if ( existingRegistration != null ) {
			if ( !conversion.equals( existingRegistration ) ) {
				throw new AnnotationException( "Conflicting '@ConverterRegistration' descriptors for attribute converter '"
						+ conversion.getConverterType().getName() + "'" );
			}
			else {
				if ( log.isDebugEnabled() ) {
					log.debugf( "Skipping duplicate '@ConverterRegistration' for '%s'",
							conversion.getConverterType().getName() );
				}
			}
		}

		// see if we have a matching entry in `attributeConverterDescriptorsByClass`.
		// if so, remove it.  The conversion being registered will always take precedence
		if ( attributeConverterDescriptorsByClass != null ) {
			final ConverterDescriptor removed = attributeConverterDescriptorsByClass.remove( conversion.getConverterType() );
			if ( removed != null && log.isDebugEnabled() ) {
				log.debugf( "Removed potentially auto-applicable converter `%s` due to @ConverterRegistration",
						removed.getAttributeConverterClass().getName() );
			}
		}

		registeredConversionsByDomainType.put( domainType, conversion );
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
			MemberDetails attributeMember,
			MetadataBuildingContext context) {
		return locateMatchingConverter(
				attributeMember,
				ConversionSite.ATTRIBUTE,
				(autoApplyDescriptor) -> autoApplyDescriptor.getAutoAppliedConverterDescriptorForAttribute(
						attributeMember, context ),
				context
		);
	}

	private static final StringHelper.Renderer<ConverterDescriptor> RENDERER = value -> value.getAttributeConverterClass().getName();

	private ConverterDescriptor locateMatchingConverter(
			MemberDetails memberDetails,
			ConversionSite conversionSite,
			Function<AutoApplicableConverterDescriptor, ConverterDescriptor> matcher,
			MetadataBuildingContext context) {
		if ( registeredConversionsByDomainType != null ) {
			// we had registered conversions - see if any of them match and, if so, use that conversion
			final ResolvedType resolveAttributeType = resolveAttributeType( memberDetails, context );
			final RegisteredConversion registrationForDomainType =
					registeredConversionsByDomainType.get( resolveAttributeType.getErasedType() );
			if ( registrationForDomainType != null ) {
				return registrationForDomainType.isAutoApply() ? registrationForDomainType.getConverterDescriptor() : null;
			}
		}

		final List<ConverterDescriptor> matches = new ArrayList<>();

		for ( ConverterDescriptor descriptor : converterDescriptors() ) {
			if ( log.isDebugEnabled() ) {
				log.debugf(
						"Checking auto-apply AttributeConverter [%s] (domain-type=%s) for match against %s : %s.%s (type=%s)",
						descriptor.getAttributeConverterClass().getName(),
						descriptor.getDomainValueResolvedType().getSignature(),
						conversionSite.getSiteDescriptor(),
						memberDetails.getDeclaringType().getName(),
						memberDetails.getName(),
						memberDetails.getType().getName()
				);
			}

			final ConverterDescriptor match = matcher.apply( descriptor.getAutoApplyDescriptor() );

			if ( match != null ) {
				matches.add( descriptor );
			}
		}

		if ( matches.isEmpty() ) {
			return null;
		}

		if ( matches.size() == 1 ) {
			return matches.get(0);
		}

		List<ConverterDescriptor> filtered = matches.stream().filter( match -> !match.overrideable() ).collect( toList() );
		if ( filtered.size() == 1 ) {
			return filtered.get(0);
		}

		// otherwise, we had multiple matches
		throw new HibernateException(
				String.format(
						Locale.ROOT,
						"Multiple auto-apply converters matched %s [%s.%s] : %s",
						conversionSite.getSiteDescriptor(),
						memberDetails.getDeclaringType().getName(),
						memberDetails.getName(),
						StringHelper.join( matches, RENDERER )
				)
		);
	}

	@Override
	public ConverterDescriptor findAutoApplyConverterForCollectionElement(
			MemberDetails attributeMember,
			MetadataBuildingContext context) {
		return locateMatchingConverter(
				attributeMember,
				ConversionSite.COLLECTION_ELEMENT,
				(autoApplyDescriptor) -> autoApplyDescriptor.getAutoAppliedConverterDescriptorForCollectionElement(
						attributeMember, context ),
				context
		);
	}

	@Override
	public ConverterDescriptor findAutoApplyConverterForMapKey(
			MemberDetails attributeMember,
			MetadataBuildingContext context) {
		return locateMatchingConverter(
				attributeMember,
				ConversionSite.MAP_KEY,
				(autoApplyDescriptor) -> autoApplyDescriptor.getAutoAppliedConverterDescriptorForMapKey( attributeMember, context ),
				context
		);
	}

}
