/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.internal;

import java.util.ArrayList;
import java.util.Collection;
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
import org.hibernate.models.spi.MemberDetails;

import org.jboss.logging.Logger;

import com.fasterxml.classmate.ResolvedType;

import static java.util.Collections.emptyList;
import static org.hibernate.boot.model.convert.internal.ConverterHelper.resolveAttributeType;
import static org.hibernate.boot.model.convert.internal.ConverterHelper.resolveConverterClassParamTypes;
import static org.hibernate.internal.util.StringHelper.join;

/**
 * @implNote It is important that all {@link RegisteredConversion} be registered
 * prior to attempts to register any {@link ConverterDescriptor}
 *
 * @author Steve Ebersole
 */
public class AttributeConverterManager implements ConverterAutoApplyHandler {
	private static final Logger log = Logger.getLogger( AttributeConverterManager.class );

	private Map<Class<?>, ConverterDescriptor<?,?>> attributeConverterDescriptorsByClass;
	private Map<Class<?>, RegisteredConversion> registeredConversionsByDomainType;

	public RegisteredConversion findRegisteredConversion(Class<?> domainType) {
		return registeredConversionsByDomainType == null ? null : registeredConversionsByDomainType.get( domainType );
	}

	public void addConverter(ConverterDescriptor<?,?> descriptor) {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Registering AttributeConverter '%s'",
					descriptor.getAttributeConverterClass().getName() );
		}

		if ( registeredConversionsByDomainType != null ) {
			final Class<?> domainType = descriptor.getDomainValueResolvedType().getErasedType();
			final RegisteredConversion registeredConversion = registeredConversionsByDomainType.get( domainType );
			if ( registeredConversion != null ) {
				// we can skip registering the converter, the RegisteredConversion will always take precedence
				if ( log.isDebugEnabled() ) {
					log.debugf( "Skipping registration of discovered AttributeConverter '%s' for auto-apply",
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

		final Class<?> domainType = getDomainType( conversion, context );

		// make sure we are not overriding a previous conversion registration
		final RegisteredConversion existingRegistration =
				registeredConversionsByDomainType.get( domainType );
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
			final var removed = attributeConverterDescriptorsByClass.remove( conversion.getConverterType() );
			if ( removed != null && log.isDebugEnabled() ) {
				log.debugf( "Removed potentially auto-applicable converter '%s' due to @ConverterRegistration",
						removed.getAttributeConverterClass().getName() );
			}
		}

		registeredConversionsByDomainType.put( domainType, conversion );
	}

	private static Class<?> getDomainType(RegisteredConversion conversion, BootstrapContext context) {
		if ( conversion.getExplicitDomainType().equals( void.class ) ) {
			// the registration did not define an explicit domain-type, so inspect the converter
			final List<ResolvedType> converterParamTypes =
					resolveConverterClassParamTypes( conversion.getConverterType(), context.getClassmateContext() );
			return converterParamTypes.get( 0 ).getErasedType();
		}
		else {
			return conversion.getExplicitDomainType();
		}
	}

	private Collection<ConverterDescriptor<?,?>> converterDescriptors() {
		return attributeConverterDescriptorsByClass == null
				? emptyList()
				: attributeConverterDescriptorsByClass.values();
	}

	enum ConversionSite {
		ATTRIBUTE,
		COLLECTION_ELEMENT,
		MAP_KEY;

		public String getSiteDescriptor() {
			return switch ( this ) {
				case ATTRIBUTE -> "basic attribute";
				case COLLECTION_ELEMENT -> "collection attribute's element";
				case MAP_KEY -> "map attribute's key";
			};
		}
	}

	@Override
	public ConverterDescriptor<?,?> findAutoApplyConverterForAttribute(
			MemberDetails attributeMember,
			MetadataBuildingContext context) {
		return locateMatchingConverter(
				attributeMember,
				ConversionSite.ATTRIBUTE,
				(autoApplyDescriptor) ->
						autoApplyDescriptor.getAutoAppliedConverterDescriptorForAttribute( attributeMember, context ),
				context
		);
	}

	private ConverterDescriptor<?,?> locateMatchingConverter(
			MemberDetails memberDetails,
			ConversionSite conversionSite,
			Function<AutoApplicableConverterDescriptor, ConverterDescriptor<?,?>> matcher,
			MetadataBuildingContext context) {
		if ( registeredConversionsByDomainType != null ) {
			// we had registered conversions - see if any of them match and, if so, use that conversion
			final ResolvedType resolveAttributeType = resolveAttributeType( memberDetails, context );
			final RegisteredConversion registrationForDomainType =
					registeredConversionsByDomainType.get( resolveAttributeType.getErasedType() );
			if ( registrationForDomainType != null ) {
				return registrationForDomainType.isAutoApply()
						? registrationForDomainType.getConverterDescriptor()
						: null;
			}
		}

		return pickUniqueMatch( memberDetails, conversionSite,
				getMatches( memberDetails, conversionSite, matcher ) );
	}

	private static ConverterDescriptor<?,?> pickUniqueMatch(
			MemberDetails memberDetails,
			ConversionSite conversionSite,
			List<ConverterDescriptor<?,?>> matches) {
		return switch ( matches.size() ) {
			case 0 -> null;
			case 1 -> matches.get( 0 );
			default -> {
				final var filtered =
						matches.stream()
								.filter( match -> !match.overrideable() )
								.toList();
				if ( filtered.size() == 1 ) {
					yield filtered.get( 0 );
				}
				else {
					// otherwise, we had multiple matches
					throw new HibernateException(
							String.format(
									Locale.ROOT,
									"Multiple auto-apply converters matched %s [%s.%s] : %s",
									conversionSite.getSiteDescriptor(),
									memberDetails.getDeclaringType().getName(),
									memberDetails.getName(),
									join( matches, value -> value.getAttributeConverterClass().getName() )
							)
					);
				}
			}
		};
	}

	private List<ConverterDescriptor<?,?>> getMatches(
			MemberDetails memberDetails,
			ConversionSite conversionSite,
			Function<AutoApplicableConverterDescriptor,
			ConverterDescriptor<?,?>> matcher) {
		final List<ConverterDescriptor<?,?>> matches = new ArrayList<>();
		for ( ConverterDescriptor<?,?> descriptor : converterDescriptors() ) {
			if ( log.isTraceEnabled() ) {
				log.tracef(
						"Checking auto-apply AttributeConverter [%s] (domain-type=%s) for match against %s : %s.%s (type=%s)",
						descriptor.getAttributeConverterClass().getName(),
						descriptor.getDomainValueResolvedType().getSignature(),
						conversionSite.getSiteDescriptor(),
						memberDetails.getDeclaringType().getName(),
						memberDetails.getName(),
						memberDetails.getType().getName()
				);
			}
			final ConverterDescriptor<?,?> match =
					matcher.apply( descriptor.getAutoApplyDescriptor() );
			if ( match != null ) {
				matches.add( descriptor );
			}
		}
		return matches;
	}

	@Override
	public ConverterDescriptor<?,?> findAutoApplyConverterForCollectionElement(
			MemberDetails attributeMember,
			MetadataBuildingContext context) {
		return locateMatchingConverter(
				attributeMember,
				ConversionSite.COLLECTION_ELEMENT,
				(autoApplyDescriptor) ->
						autoApplyDescriptor.getAutoAppliedConverterDescriptorForCollectionElement( attributeMember, context ),
				context
		);
	}

	@Override
	public ConverterDescriptor<?,?> findAutoApplyConverterForMapKey(
			MemberDetails attributeMember,
			MetadataBuildingContext context) {
		return locateMatchingConverter(
				attributeMember,
				ConversionSite.MAP_KEY,
				(autoApplyDescriptor) ->
						autoApplyDescriptor.getAutoAppliedConverterDescriptorForMapKey( attributeMember, context ),
				context
		);
	}

}
