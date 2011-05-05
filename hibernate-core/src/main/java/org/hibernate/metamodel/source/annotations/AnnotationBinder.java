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
package org.hibernate.metamodel.source.annotations;

import java.util.Iterator;
import java.util.Set;

import org.jboss.jandex.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.metamodel.source.annotations.global.FetchProfileBinder;
import org.hibernate.metamodel.source.annotations.global.TableBinder;
import org.hibernate.metamodel.source.annotations.util.ConfiguredClassHierarchyBuilder;
import org.hibernate.metamodel.source.internal.MetadataImpl;

/**
 * Main class responsible to creating and binding the Hibernate meta-model from annotations.
 * This binder only has to deal with annotation index. XML configuration is already processed and pseudo annotations
 * are added to the annotation index.
 *
 * @author Hardy Ferentschik
 * @todo On top of the index we probably needs to pass some sort of XMLContext for global configuration data
 * @todo The annotation index should really be passed at construction time
 */
public class AnnotationBinder {
	private static final Logger log = LoggerFactory.getLogger( AnnotationBinder.class );
	private final MetadataImpl metadata;

	public AnnotationBinder(MetadataImpl metadata) {
		this.metadata = metadata;
	}

	public void bind(Index annotationIndex) {
		preEntityBindings( annotationIndex );
		bindMappedClasses( annotationIndex );
		postEntityBindings( annotationIndex );
	}

	/**
	 * Binds global configuration data prior to entity binding. This includes generators and type definitions
	 *
	 * @param annotationIndex the annotation repository/index
	 */
	private void preEntityBindings(Index annotationIndex) {
		FetchProfileBinder.bind( metadata, annotationIndex );
	}

	private void bindMappedClasses(Index annotationIndex) {
		// need to order our annotated entities into an order we can process
		Set<ConfiguredClassHierarchy> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				annotationIndex, metadata.getServiceRegistry()
		);

		// now we process each hierarchy one at the time
		for ( ConfiguredClassHierarchy hierarchy : hierarchies ) {
			Iterator<ConfiguredClass> iter = hierarchy.iterator();
			while ( iter.hasNext() ) {
				ConfiguredClass entity = iter.next();
				log.info( "Binding entity from annotated class: {}", entity.getName() );
				EntityBinder entityBinder = new EntityBinder( metadata, entity );
				entityBinder.bind();
			}
		}
	}

	/**
	 * Binds global configuration data post entity binding. This includes mappings which live outside of the configuration for a single
	 * entity or entity hierarchy, for example sequence generators, fetch profiles, etc
	 *
	 * @param annotationIndex the annotation repository/index
	 */
	private void postEntityBindings(Index annotationIndex) {
		TableBinder.bind( metadata, annotationIndex );
	}
}


