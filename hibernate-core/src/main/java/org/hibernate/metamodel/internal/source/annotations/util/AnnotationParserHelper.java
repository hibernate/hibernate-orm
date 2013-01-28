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
package org.hibernate.metamodel.internal.source.annotations.util;

import java.util.List;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import org.hibernate.EntityMode;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.binding.CustomSQL;

/**
 * Some annotation processing is identical between entity and attribute level (aka you can place the annotation on
 * entity as well as attribute level. This class tries to avoid code duplication for these cases.
 *
 * @author Hardy Ferentschik
 */
public class AnnotationParserHelper {
	// should not be instantiated
	private AnnotationParserHelper() {

	}

	public static CustomSQL processCustomSqlAnnotation(DotName annotationName, Map<DotName, List<AnnotationInstance>> annotations) {
		final AnnotationInstance sqlAnnotation = JandexHelper.getSingleAnnotation(
				annotations, annotationName
		);

		return createCustomSQL( sqlAnnotation );
	}

	private static CustomSQL createCustomSQL(AnnotationInstance customSqlAnnotation) {
		if ( customSqlAnnotation == null ) {
			return null;
		}

		final String sql = customSqlAnnotation.value( "sql" ).asString();
		final boolean isCallable = customSqlAnnotation.value( "callable" ) != null
				&& customSqlAnnotation.value( "callable" ).asBoolean();

		final ExecuteUpdateResultCheckStyle checkStyle = customSqlAnnotation.value( "check" ) == null
				? isCallable
				? ExecuteUpdateResultCheckStyle.NONE
				: ExecuteUpdateResultCheckStyle.COUNT
				: ExecuteUpdateResultCheckStyle.valueOf( customSqlAnnotation.value( "check" ).asEnum() );

		return new CustomSQL( sql, isCallable, checkStyle );
	}

	public static String determineCustomTuplizer(
			final AnnotationInstance tuplizersAnnotation,
			final AnnotationInstance tuplizerAnnotation) {
		if ( tuplizersAnnotation != null ) {
			AnnotationInstance[] annotations = JandexHelper.getValue(
					tuplizersAnnotation,
					"value",
					AnnotationInstance[].class
			);
			for ( final AnnotationInstance annotationInstance : annotations ) {
				final String impl = findTuplizerImpl( annotationInstance );
				if ( StringHelper.isNotEmpty( impl ) ) {
					return impl;
				}
			}
		}
		else if ( tuplizerAnnotation != null ) {
			final String impl = findTuplizerImpl( tuplizerAnnotation );
			if ( StringHelper.isNotEmpty( impl ) ) {
				return impl;
			}
		}
		return null;
	}

	private static String findTuplizerImpl(final AnnotationInstance tuplizerAnnotation) {
		final EntityMode mode;
		if ( tuplizerAnnotation.value( "entityModeType" ) != null ) {
			mode = EntityMode.valueOf( tuplizerAnnotation.value( "entityModeType" ).asEnum() );
		}
		else if ( tuplizerAnnotation.value( "entityMode" ) != null ) {
			mode = EntityMode.parse( tuplizerAnnotation.value( "entityMode" ).asString() );
		}
		else {
			mode = EntityMode.POJO;
		}
		return mode == EntityMode.POJO ? tuplizerAnnotation.value( "impl" ).asString() : null;
	}
}


