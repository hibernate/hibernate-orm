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
package org.hibernate.metamodel.binder.source.annotations.global;

import java.util.HashMap;
import java.util.Map;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;
import org.jboss.logging.Logger;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.binder.source.MetadataImplementor;
import org.hibernate.metamodel.binder.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.binder.source.annotations.JandexHelper;
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
	public static void bind(MetadataImplementor metadata, Index jandex) {
		for ( AnnotationInstance filterDef : jandex.getAnnotations( HibernateDotNames.FILTER_DEF ) ) {
			bind( metadata, filterDef );
		}
		for ( AnnotationInstance filterDefs : jandex.getAnnotations( HibernateDotNames.FILTER_DEFS ) ) {
			AnnotationInstance[] filterDefAnnotations = JandexHelper.getValue(
					filterDefs,
					"value",
					AnnotationInstance[].class
			);
			for ( AnnotationInstance filterDef : filterDefAnnotations ) {
				bind( metadata, filterDef );
			}
		}
	}

	private static void bind(MetadataImplementor metadata, AnnotationInstance filterDef) {
		String name = JandexHelper.getValue( filterDef, "name", String.class );
		Map<String, Type> prms = new HashMap<String, Type>();
		for ( AnnotationInstance prm : JandexHelper.getValue( filterDef, "parameters", AnnotationInstance[].class ) ) {
			prms.put(
					JandexHelper.getValue( prm, "name", String.class ),
					metadata.getTypeResolver().heuristicType( JandexHelper.getValue( prm, "type", String.class ) )
			);
		}
		metadata.addFilterDefinition(
				new FilterDefinition(
						name,
						JandexHelper.getValue( filterDef, "defaultCondition", String.class ),
						prms
				)
		);
		LOG.debugf( "Binding filter definition: %s", name );
	}

	private FilterDefBinder() {
	}
}
