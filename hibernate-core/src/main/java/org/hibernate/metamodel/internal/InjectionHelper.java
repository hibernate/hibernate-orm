/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.query.NamedQueryDefinition;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;

import java.lang.reflect.Field;

import static java.lang.Character.charCount;
import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.reflect.Modifier.isPublic;

public class InjectionHelper {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( MetadataContext.class );

	public static void injectEntityGraph(
			NamedEntityGraphDefinition definition,
			Class<?> metamodelClass,
			JpaMetamodelImplementor jpaMetamodel) {
		if ( metamodelClass != null ) {
			final String name = definition.name();
			final String fieldName = '_' + javaIdentifier( name );
			final var graph = jpaMetamodel.findEntityGraphByName( name );
			try {
				injectField( metamodelClass, fieldName, graph, false );
			}
			catch ( NoSuchFieldException e ) {
				// ignore
			}
		}
	}

	public static void injectTypedQueryReference(NamedQueryDefinition<?> definition, Class<?> metamodelClass) {
		if ( metamodelClass != null ) {
			final String fieldName =
					'_' + javaIdentifier( definition.getRegistrationName() ) + '_';
			try {
				injectField( metamodelClass, fieldName, definition, false );
			}
			catch ( NoSuchFieldException e ) {
				// ignore
			}
		}
	}

	public static String javaIdentifier(String name) {
		final StringBuilder result = new StringBuilder();
		int position = 0;
		while ( position < name.length() ) {
			final int codePoint = name.codePointAt( position );
			result.appendCodePoint( isJavaIdentifierPart( codePoint ) ? codePoint : '_' );
			position += charCount( codePoint );
		}
		return result.toString();
	}

	public static void injectField(
			Class<?> metamodelClass, String name, Object model,
			boolean allowNonDeclaredFieldReference)
			throws NoSuchFieldException {
		final Field field =
				allowNonDeclaredFieldReference
						? metamodelClass.getField( name )
						: metamodelClass.getDeclaredField( name );
		try {
			if ( !isPublic( metamodelClass.getModifiers() ) ) {
				ReflectHelper.ensureAccessibility( field );
			}
			field.set( null, model);
		}
		catch (IllegalAccessException e) {
			// todo : exception type?
			throw new AssertionFailure(
					"Unable to inject attribute '" + name
						+ "' of static metamodel class '" + metamodelClass.getName() + "'",
					e
			);
		}
		catch (IllegalArgumentException e) {
			// most likely a mismatch in the type we are injecting and the defined field; this represents a
			// mismatch in how the annotation processor interpreted the attribute and how our metamodel
			// and/or annotation binder did.

//              This is particularly the case as arrays are not handled properly by the StaticMetamodel generator

//				throw new AssertionFailure(
//						"Illegal argument on static metamodel field injection : " + metamodelClass.getName() + '#' + name
//								+ "; expected type :  " + attribute.getClass().getName()
//								+ "; encountered type : " + field.getType().getName()
//				);
			log.illegalArgumentOnStaticMetamodelFieldInjection(
					metamodelClass.getName(),
					name,
					model.getClass().getName(),
					field.getType().getName()
			);
		}
	}

}
