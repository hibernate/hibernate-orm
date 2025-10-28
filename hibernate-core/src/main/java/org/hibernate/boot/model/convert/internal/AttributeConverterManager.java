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



import static java.util.Collections.emptyList;
import static org.hibernate.boot.BootLogging.BOOT_LOGGER;
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

	private Map<Class<?>, ConverterDescriptor<?,?>> attributeConverterDescriptorsByClass;
	private Map<Class<?>, RegisteredConversion> registeredConversionsByDomainType;

	public RegisteredConversion findRegisteredConversion(Class<?> domainType) {
		return registeredConversionsByDomainType == null ? null : registeredConversionsByDomainType.get( domainType );
	}

	public void addConverter(ConverterDescriptor<?,?> descriptor) {
		final var converterClass = descriptor.getAttributeConverterClass();
		if ( BOOT_LOGGER.isTraceEnabled() ) {
			BOOT_LOGGER.registeringAttributeConverter( converterClass.getName() );
		}

		if ( registeredConversionsByDomainType != null ) {
			final Class<?> domainType = descriptor.getDomainValueResolvedType().getErasedType();
			final var registeredConversion = registeredConversionsByDomainType.get( domainType );
			if ( registeredConversion != null ) {
				// we can skip registering the converter, the RegisteredConversion will always take precedence
				if ( BOOT_LOGGER.isDebugEnabled() ) {
					BOOT_LOGGER.skippingRegistrationAttributeConverterForAutoApply( converterClass.getName() );
				}
				return;
			}
		}

		if ( attributeConverterDescriptorsByClass == null ) {
			attributeConverterDescriptorsByClass = new ConcurrentHashMap<>();
		}

		final Object old = attributeConverterDescriptorsByClass.put( converterClass, descriptor );
		if ( old != null ) {
			throw new HibernateException(
					String.format(
							Locale.ENGLISH,
							"AttributeConverter class [%s] registered multiple times",
							converterClass
					)
			);
		}
	}

	public void addRegistration(RegisteredConversion conversion, BootstrapContext context) {
		if ( registeredConversionsByDomainType == null ) {
			registeredConversionsByDomainType = new ConcurrentHashMap<>();
		}

		final Class<?> domainType = getDomainType( conversion, context );
		checkNotOverriding( conversion, domainType );
		// See if we have a matching entry in attributeConverterDescriptorsByClass.
		// If so, remove it. The conversion being registered will always take precedence.
		if ( attributeConverterDescriptorsByClass != null ) {
			final var removed = attributeConverterDescriptorsByClass.remove( conversion.getConverterType() );
			if ( removed != null && BOOT_LOGGER.isDebugEnabled() ) {
				BOOT_LOGGER.removedPotentiallyAutoApplicableConverterDueToRegistration(
						removed.getAttributeConverterClass().getName() );
			}
		}
		registeredConversionsByDomainType.put( domainType, conversion );
	}

	private void checkNotOverriding(RegisteredConversion conversion, Class<?> domainType) {
		// make sure we are not overriding a previous conversion registration
		final var existingRegistration = registeredConversionsByDomainType.get( domainType );
		if ( existingRegistration != null ) {
			final String converterTypeName = conversion.getConverterType().getName();
			if ( !conversion.equals( existingRegistration ) ) {
				throw new AnnotationException( "Conflicting '@ConverterRegistration' descriptors for attribute converter '"
												+ converterTypeName + "'" );
			}
			else {
				BOOT_LOGGER.skippingDuplicateConverterRegistration( converterTypeName );
			}
		}
	}

	private static Class<?> getDomainType(RegisteredConversion conversion, BootstrapContext context) {
		// the registration did not define an explicit domain-type, so inspect the converter
		return conversion.getExplicitDomainType().equals( void.class )
				? resolveConverterClassParamTypes( conversion.getConverterType(), context.getClassmateContext() )
						.get( 0 ).getErasedType()
				: conversion.getExplicitDomainType();
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
			final var resolveAttributeType = resolveAttributeType( memberDetails, context );
			final var registrationForDomainType =
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
		for ( var descriptor : converterDescriptors() ) {
			if ( BOOT_LOGGER.isTraceEnabled() ) {
				BOOT_LOGGER.checkingAutoApplyAttributeConverter(
						descriptor.getAttributeConverterClass().getName(),
						descriptor.getDomainValueResolvedType().getSignature(),
						conversionSite.getSiteDescriptor(),
						memberDetails.getDeclaringType().getName(),
						memberDetails.getName(),
						memberDetails.getType().getName()
				);
			}
			final var match = matcher.apply( descriptor.getAutoApplyDescriptor() );
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
