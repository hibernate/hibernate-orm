/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.mapping.internal.sources.CollectionSource;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.MappedSuperclass;

/**
 * @author Steve Ebersole
 */
final class SoftDeleteBindingHelper {
	private SoftDeleteBindingHelper() {
	}

	static SoftDelete resolveEntitySoftDelete(ClassDetails classDetails, BindingState bindingState) {
		final ModelsContext modelsContext = modelsContext( bindingState );
		final SoftDelete fromClass = classDetails.getAnnotationUsage( SoftDelete.class, modelsContext );
		if ( fromClass != null ) {
			return fromClass;
		}

		ClassDetails classToCheck = classDetails.getSuperClass();
		while ( classToCheck != null ) {
			final SoftDelete fromSuper = classToCheck.getAnnotationUsage( SoftDelete.class, modelsContext );
			if ( fromSuper != null && classToCheck.hasAnnotationUsage( MappedSuperclass.class, modelsContext ) ) {
				return fromSuper;
			}
			classToCheck = classToCheck.getSuperClass();
		}

		return resolvePackageSoftDelete( classDetails, modelsContext );
	}

	static SoftDelete resolveCollectionSoftDelete(CollectionSource source, BindingState bindingState) {
		final MemberDetails member = source.member();
		final SoftDelete fromMember = member.getDirectAnnotationUsage( SoftDelete.class );
		if ( fromMember != null ) {
			return fromMember;
		}

		final ModelsContext modelsContext = modelsContext( bindingState );
		final ClassDetails declaringType = member.getDeclaringType();
		final SoftDelete fromClass = declaringType.getAnnotationUsage( SoftDelete.class, modelsContext );
		return fromClass == null ? resolvePackageSoftDelete( declaringType, modelsContext ) : fromClass;
	}

	private static SoftDelete resolvePackageSoftDelete(ClassDetails classDetails, ModelsContext modelsContext) {
		final ClassDetails packageInfo = classDetails.getContainer( modelsContext );
		return packageInfo == null ? null : packageInfo.getAnnotationUsage( SoftDelete.class, modelsContext );
	}

	private static ModelsContext modelsContext(BindingState bindingState) {
		return bindingState.getMetadataBuildingContext().getBootstrapContext().getModelsContext();
	}
}
