/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.annotations.Parameter;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.sources.CollectionSource;
import org.hibernate.boot.spi.InFlightMetadataCollector.CollectionTypeRegistrationDescriptor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.usertype.UserCollectionType;

import static org.hibernate.mapping.MappingHelper.createUserTypeBean;

class CollectionMappingHelper {
	private CollectionMappingHelper() {
	}

	static Collection createCollection(
			CollectionSource source,
			PersistentClass ownerBinding,
			BindingState bindingState) {
		final CustomCollectionType customType = resolveCustomCollectionType( source, bindingState );
		final CollectionClassification classification = customType == null
				? source.classification()
				: customType.classification();
		final Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver = customType == null
				? null
				: customType.customTypeBeanResolver();

		return switch ( classification ) {
			case SET, ORDERED_SET, SORTED_SET -> new org.hibernate.mapping.Set(
					customTypeBeanResolver,
					ownerBinding,
					bindingState.getMetadataBuildingContext()
			);
			case LIST -> new org.hibernate.mapping.List(
					customTypeBeanResolver,
					ownerBinding,
					bindingState.getMetadataBuildingContext()
			);
			case MAP, ORDERED_MAP, SORTED_MAP -> new org.hibernate.mapping.Map(
					customTypeBeanResolver,
					ownerBinding,
					bindingState.getMetadataBuildingContext()
			);
			case BAG -> new org.hibernate.mapping.Bag(
					customTypeBeanResolver,
					ownerBinding,
					bindingState.getMetadataBuildingContext()
			);
			case ID_BAG -> new IdentifierBag(
					customTypeBeanResolver,
					ownerBinding,
					bindingState.getMetadataBuildingContext()
			);
			case ARRAY -> {
				final var elementClass = source.elementType().determineRawClass();
				if ( elementClass.isPrimitive() ) {
					yield new org.hibernate.mapping.PrimitiveArray(
							customTypeBeanResolver,
							ownerBinding,
							bindingState.getMetadataBuildingContext()
					);
				}
				final org.hibernate.mapping.Array array = new org.hibernate.mapping.Array(
						customTypeBeanResolver,
						ownerBinding,
						bindingState.getMetadataBuildingContext()
				);
				array.setElementClassName( elementClass.getClassName() );
				yield array;
			}
		};
	}

	private static CustomCollectionType resolveCustomCollectionType(
			CollectionSource source,
			BindingState bindingState) {
		final var collectionType = source.collectionType();
		if ( collectionType != null ) {
			final Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver =
					() -> createUserTypeBean(
							source.member().getDeclaringType().getName() + "." + source.member().getName(),
							collectionType.type(),
							extractParameters( collectionType.parameters() ),
							bindingState.getMetadataBuildingContext().getBootstrapContext(),
							bindingState.getMetadataBuildingContext()
									.getMetadataCollector()
									.getMetadataBuildingOptions()
									.isAllowExtensionsInCdi()
					);
			return new CustomCollectionType(
					customTypeBeanResolver.get().getBeanInstance().getClassification(),
					customTypeBeanResolver
			);
		}

		final CollectionTypeRegistrationDescriptor registration = findTypeRegistration( source, bindingState );
		if ( registration == null ) {
			return null;
		}

		final CollectionClassification classification = registrationClassification( source, bindingState );
		return new CustomCollectionType(
				classification,
				() -> createUserTypeBean(
						source.member().getDeclaringType().getName() + "#" + source.member().getName(),
						registration.implementation(),
						registration.parameters(),
						bindingState.getMetadataBuildingContext().getBootstrapContext(),
						bindingState.getMetadataBuildingContext()
								.getMetadataCollector()
								.getMetadataBuildingOptions()
								.isAllowExtensionsInCdi()
				)
		);
	}

	private static CollectionTypeRegistrationDescriptor findTypeRegistration(
			CollectionSource source,
			BindingState bindingState) {
		final CollectionTypeRegistrationDescriptor registration =
				bindingState.getMetadataBuildingContext()
						.getMetadataCollector()
						.findCollectionTypeRegistration( source.classification() );
		if ( registration != null ) {
			return registration;
		}

		final CollectionClassification declaredClassification = declaredClassification( source.member() );
		if ( declaredClassification == source.classification() ) {
			return null;
		}
		return bindingState.getMetadataBuildingContext()
				.getMetadataCollector()
				.findCollectionTypeRegistration( declaredClassification );
	}

	private static CollectionClassification registrationClassification(
			CollectionSource source,
			BindingState bindingState) {
		if ( bindingState.getMetadataBuildingContext()
				.getMetadataCollector()
				.findCollectionTypeRegistration( source.classification() ) != null ) {
			return source.classification();
		}
		return declaredClassification( source.member() );
	}

	private static CollectionClassification declaredClassification(MemberDetails member) {
		final Class<?> collectionType = member.getType().determineRawClass().toJavaClass();
		if ( collectionType.isArray() ) {
			return CollectionClassification.ARRAY;
		}
		if ( java.util.List.class.isAssignableFrom( collectionType ) ) {
			return CollectionClassification.LIST;
		}
		if ( java.util.Map.class.isAssignableFrom( collectionType ) ) {
			return CollectionClassification.MAP;
		}
		if ( java.util.Set.class.isAssignableFrom( collectionType ) ) {
			return CollectionClassification.SET;
		}
		return CollectionClassification.BAG;
	}

	private static Map<String, String> extractParameters(Parameter[] parameters) {
		final Map<String, String> result = new HashMap<>();
		for ( Parameter parameter : parameters ) {
			result.put( parameter.name(), parameter.value() );
		}
		return result;
	}

	private record CustomCollectionType(
			CollectionClassification classification,
			Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver) {
	}
}
