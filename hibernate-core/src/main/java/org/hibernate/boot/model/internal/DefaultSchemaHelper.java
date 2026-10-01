/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.annotations.DefaultSchema;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotBlank;
import static org.hibernate.internal.util.StringHelper.qualifier;

/**
 * Utilities for interpreting {@link DefaultSchema}.
 */
public final class DefaultSchemaHelper {
	private DefaultSchemaHelper() {
	}

	public static String defaultSchema(
			String schema,
			AnnotationTarget annotationTarget,
			MetadataBuildingContext context) {
		return defaultSchema( schema, annotationTarget, context.getBootstrapContext().getModelsContext() );
	}

	public static String defaultSchema(
			String schema,
			AnnotationTarget annotationTarget,
			ModelsContext modelsContext) {
		if ( isNotBlank( schema ) ) {
			return schema;
		}
		else {
			final String packageDefaultSchema = getDefaultSchema( annotationTarget, modelsContext );
			return packageDefaultSchema == null ? schema : packageDefaultSchema;
		}
	}

	public static String getDefaultSchema(AnnotationTarget annotationTarget, ModelsContext modelsContext) {
		return annotationTarget == null ? null : switch ( annotationTarget.getKind() ) {
			case CLASS -> getDefaultSchema( (ClassDetails) annotationTarget, modelsContext );
			case FIELD, METHOD, RECORD_COMPONENT -> getDefaultSchema(
					((MemberDetails) annotationTarget).getDeclaringType(),
					modelsContext
			);
			default -> null;
		};

	}

	public static String getDefaultSchema(ClassDetails classDetails, ModelsContext modelsContext) {
		if ( classDetails == null ) {
			return null;
		}
		else {
			final var packageInfo = resolvePackageInfo( classDetails, modelsContext );
			if ( packageInfo == null ) {
				return null;
			}
			else {
				final var defaultSchema = packageInfo.getAnnotationUsage( DefaultSchema.class, modelsContext );
				return defaultSchema == null || isBlank( defaultSchema.value() ) ? null : defaultSchema.value();
			}
		}
	}

	private static ClassDetails resolvePackageInfo(ClassDetails classDetails, ModelsContext modelsContext) {
		final String className = classDetails.getName();
		final String packageInfoName;
		if ( className.endsWith( ".package-info" ) ) {
			packageInfoName = className;
		}
		else {
			final String packageName = qualifier( className );
			if ( isEmpty( packageName ) ) {
				return null;
			}
			packageInfoName = packageName + ".package-info";
		}

		try {
			return modelsContext.getClassDetailsRegistry().resolveClassDetails( packageInfoName );
		}
		catch (ClassLoadingException ignore) {
			return null;
		}
	}
}
