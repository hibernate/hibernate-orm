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
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;
import org.jboss.logging.Logger;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.type.Type;

public class FilterDefBinder {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			FilterDefBinder.class.getName()
	);

	/**
	 * Binds all {@link FilterDefs} and {@link FilterDef} annotations to the supplied metadata.
	 *
	 * @param metadata the global metadata
	 * @param jandex the jandex index
	 */
	public static void bind(MetadataImpl metadata,
							Index jandex) {
		for ( AnnotationInstance filterDef : jandex.getAnnotations( HibernateDotNames.FILTER_DEF ) ) {
			bind( metadata, filterDef );
		}
		for ( AnnotationInstance filterDefs : jandex.getAnnotations( HibernateDotNames.FILTER_DEFS ) ) {
			for ( AnnotationInstance filterDef : JandexHelper.getValueAsArray( filterDefs, "value" ) ) {
				bind( metadata, filterDef );
			}
		}
	}

	private static void bind(MetadataImpl metadata,
							 AnnotationInstance filterDef) {
		String name = JandexHelper.getValueAsString( filterDef, "name" );
		Map<String, Type> prms = new HashMap<String, Type>();
		for ( AnnotationInstance prm : JandexHelper.getValueAsArray( filterDef, "parameters" ) ) {
			prms.put(
					JandexHelper.getValueAsString( prm, "name" ),
					metadata.typeResolver().heuristicType( JandexHelper.getValueAsString( prm, "type" ) )
			);
		}
		metadata.addFilterDef(
				new FilterDefinition(
						name,
						JandexHelper.getValueAsString( filterDef, "defaultCondition" ),
						prms
				)
		);
		LOG.debugf( "Binding filter definition: %s", name );
	}

	private FilterDefBinder() {
	}
}
