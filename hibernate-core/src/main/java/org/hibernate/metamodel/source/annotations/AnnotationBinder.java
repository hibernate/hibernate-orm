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

import java.util.Set;

import org.jboss.jandex.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.metamodel.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.source.annotations.entity.ConfiguredClassHierarchy;
import org.hibernate.metamodel.source.annotations.entity.EntityBinder;
import org.hibernate.metamodel.source.annotations.global.FilterDefBinder;
import org.hibernate.metamodel.source.annotations.global.IdGeneratorBinder;
import org.hibernate.metamodel.source.annotations.global.QueryBinder;
import org.hibernate.metamodel.source.annotations.global.TypeDefBinder;
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
 */
public class AnnotationBinder {

	private static final Logger LOG = LoggerFactory.getLogger( AnnotationBinder.class );

	private final MetadataImpl metadata;
	private final Index index;

	public AnnotationBinder(MetadataImpl metadata, Index index) {
		this.metadata = metadata;
		this.index = index;
	}

	public void bind() {
		preEntityBindings();
		bindMappedClasses();
		postEntityBindings();
	}

	/**
	 * Binds global configuration data prior to entity binding. This includes generators and type definitions.
	 */
	private void preEntityBindings() {
        TypeDefBinder.bind(metadata, index);
        IdGeneratorBinder.bind(metadata, index);
	}

	/**
	 * Does the actual entity binding (see {@link org.hibernate.metamodel.binding.EntityBinding}.
	 */
	private void bindMappedClasses() {
		// need to order our annotated entities into an order we can process
		Set<ConfiguredClassHierarchy> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				index, metadata.getServiceRegistry()
		);

		// now we process each hierarchy one at the time
		for ( ConfiguredClassHierarchy hierarchy : hierarchies ) {
			for ( ConfiguredClass configuredClass : hierarchy ) {
				LOG.info( "Binding entity from annotated class: {}", configuredClass.getName() );
				EntityBinder entityBinder = new EntityBinder( metadata, configuredClass );
				entityBinder.bind();
			}
		}
	}

	/**
	 * Binds global configuration data post entity binding. This includes mappings which live outside of the configuration for a single
	 * entity or entity hierarchy, for example sequence generators, fetch profiles, etc
	 */
	private void postEntityBindings() {
		TableBinder.bind( metadata, index );
        FetchProfileBinder.bind( metadata, index );
        QueryBinder.bind(metadata, index);
        FilterDefBinder.bind(metadata, index);
	}
}


