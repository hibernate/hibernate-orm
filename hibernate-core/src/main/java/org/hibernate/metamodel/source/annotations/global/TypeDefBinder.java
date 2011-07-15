/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.annotations.global;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.logging.Logger;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.TypeDefs;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binding.TypeDef;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.metamodel.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;

/**
 * Binds {@link org.hibernate.annotations.TypeDef} and {@link TypeDefs}.
 *
 * @author Hardy Ferentschik
 */
public class TypeDefBinder {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			TypeDefBinder.class.getName()
	);

	/**
	 * Binds all {@link org.hibernate.annotations.TypeDef} and {@link TypeDefs} annotations to the supplied metadata.
	 *
	 * @param bindingContext the context for annotation binding
	 */
	public static void bind(AnnotationBindingContext bindingContext) {
		List<AnnotationInstance> annotations = bindingContext.getIndex().getAnnotations( HibernateDotNames.TYPE_DEF );
		for ( AnnotationInstance typeDef : annotations ) {
			bind( bindingContext.getMetadataImplementor(), typeDef );
		}

		annotations = bindingContext.getIndex().getAnnotations( HibernateDotNames.TYPE_DEFS );
		for ( AnnotationInstance typeDefs : annotations ) {
			AnnotationInstance[] typeDefAnnotations = JandexHelper.getValue(
					typeDefs,
					"value",
					AnnotationInstance[].class
			);
			for ( AnnotationInstance typeDef : typeDefAnnotations ) {
				bind( bindingContext.getMetadataImplementor(), typeDef );
			}
		}
	}

	private static void bind(MetadataImplementor metadata, AnnotationInstance typeDefAnnotation) {
		String name = JandexHelper.getValue( typeDefAnnotation, "name", String.class );
		String defaultForType = JandexHelper.getValue( typeDefAnnotation, "defaultForType", String.class );
		String typeClass = JandexHelper.getValue( typeDefAnnotation, "typeClass", String.class );

		boolean noName = StringHelper.isEmpty( name );
		boolean noDefaultForType = defaultForType == null || defaultForType.equals( void.class.getName() );

		if ( noName && noDefaultForType ) {
			throw new AnnotationException(
					"Either name or defaultForType (or both) attribute should be set in TypeDef having typeClass "
							+ typeClass
			);
		}

		Map<String, String> parameterMaps = new HashMap<String, String>();
		AnnotationInstance[] parameterAnnotations = JandexHelper.getValue(
				typeDefAnnotation,
				"parameters",
				AnnotationInstance[].class
		);
		for ( AnnotationInstance parameterAnnotation : parameterAnnotations ) {
			parameterMaps.put(
					JandexHelper.getValue( parameterAnnotation, "name", String.class ),
					JandexHelper.getValue( parameterAnnotation, "value", String.class )
			);
		}

		if ( !noName ) {
			bind( name, typeClass, parameterMaps, metadata );
		}
		if ( !noDefaultForType ) {
			bind( defaultForType, typeClass, parameterMaps, metadata );
		}
	}

	private static void bind(
			String name,
			String typeClass,
			Map<String, String> prms,
			MetadataImplementor metadata) {
		LOG.debugf( "Binding type definition: %s", name );
		metadata.addTypeDefinition( new TypeDef( name, typeClass, prms ) );
	}

	private TypeDefBinder() {
	}
}
