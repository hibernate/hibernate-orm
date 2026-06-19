/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.xml;

import org.hibernate.HibernateException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.ModelsContext;

/**
 * Context for a specific XML mapping file
 *
 * @author Steve Ebersole
 */
public interface XmlDocumentContext {
	/**
	 * The XML document
	 */
	XmlDocument getXmlDocument();

	EffectiveMappingDefaults getEffectiveDefaults();

	/**
	 * Access to the containing ModelsContext
	 */
	ModelsContext getModelBuildingContext();

	/**
	 * Access to the containing BootstrapContext
	 */
	BootstrapContext getBootstrapContext();

	/**
	 * Resolve a ClassDetails by name, accounting for XML-defined package name if one.
	 */
	default MutableClassDetails resolveJavaType(String name) {
		try {
			return (MutableClassDetails) XmlAnnotationHelper.resolveJavaType( name, this );
		}
		catch (Exception e) {
			final HibernateException hibernateException = new HibernateException( "Unable to resolve Java type " + name );
			hibernateException.addSuppressed( e );
			throw hibernateException;
		}
	}

	/**
	 * Resolve a target entity name, checking for dynamic entities first.
	 * Dynamic entities are registered by entity-name without a backing Java class,
	 * so they must not be package-qualified.
	 */
	default String resolveTargetEntityName(String specifiedName) {
		final var classDetailsRegistry = getModelBuildingContext().getClassDetailsRegistry();
		final var classDetails = classDetailsRegistry.findClassDetails( specifiedName );
		if ( classDetails != null && !classDetails.isRealClass() ) {
			return specifiedName;
		}
		return resolveClassName( specifiedName );
	}

	default String resolveClassName(String specifiedName) {
		final SimpleTypeInterpretation simpleTypeInterpretation = SimpleTypeInterpretation.interpret( specifiedName );
		if ( simpleTypeInterpretation != null ) {
			return simpleTypeInterpretation.getJavaType().getName();
		}

		if ( specifiedName.contains( "." ) ) {
			return specifiedName;
		}

		return StringHelper.qualifyConditionallyIfNot(
				getXmlDocument().getDefaults().getPackage(),
				specifiedName
		);
	}
}
