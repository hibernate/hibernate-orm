/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.ArrayList;
import java.util.List;

import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jaxb.spi.JaxbRoot;
import org.hibernate.jaxb.spi.hbm.JaxbHibernateMapping;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.MetadataSourceProcessor;
import org.hibernate.metamodel.spi.source.EntityHierarchy;
import org.hibernate.metamodel.spi.source.FilterDefinitionSource;
import org.hibernate.metamodel.spi.source.IdentifierGeneratorSource;
import org.hibernate.metamodel.spi.source.TypeDescriptorSource;

import static java.util.Collections.emptyList;

/**
 * The {@link org.hibernate.metamodel.spi.MetadataSourceProcessor} implementation responsible for processing {@code hbm.xml} sources.
 *
 * @author Steve Ebersole
 */
public class HbmMetadataSourceProcessorImpl implements MetadataSourceProcessor {
	private static final CoreMessageLogger LOG = Logger
			.getMessageLogger( CoreMessageLogger.class, HbmMetadataSourceProcessorImpl.class.getName() );
	private final List<HibernateMappingProcessor> processors = new ArrayList<HibernateMappingProcessor>();
	private final List<EntityHierarchyImpl> entityHierarchies;

	public HbmMetadataSourceProcessorImpl(MetadataImplementor metadata, MetadataSources metadataSources) {
		this( metadata, metadataSources.getJaxbRootList() );
	}

	public HbmMetadataSourceProcessorImpl(MetadataImplementor metadata, List<JaxbRoot> jaxbRoots) {
		final HierarchyBuilder hierarchyBuilder = new HierarchyBuilder( metadata );

		for ( JaxbRoot jaxbRoot : jaxbRoots ) {
			if ( ! JaxbHibernateMapping.class.isInstance( jaxbRoot.getRoot() ) ) {
				continue;
			}

			final MappingDocument mappingDocument = new MappingDocument( jaxbRoot, metadata );
			processors.add( new HibernateMappingProcessor( metadata, mappingDocument ) );

			hierarchyBuilder.processMappingDocument( mappingDocument );
		}

		this.entityHierarchies = hierarchyBuilder.groupEntityHierarchies();
	}

	// todo : still need to deal with auxiliary database objects

	@Override
	public Iterable<TypeDescriptorSource> extractTypeDefinitionSources() {
		if( CollectionHelper.isEmpty( processors )){
			return emptyList();
		}
		final List<TypeDescriptorSource> typeDescriptorSources = new ArrayList<TypeDescriptorSource>();
		for ( HibernateMappingProcessor processor : processors ) {
			processor.collectTypeDescriptorSources( typeDescriptorSources );
		}
		return typeDescriptorSources;
	}

	@Override
	public Iterable<FilterDefinitionSource> extractFilterDefinitionSources() {
		if( CollectionHelper.isEmpty( processors )){
			return emptyList();
		}
		final List<FilterDefinitionSource> filterDefinitionSources = new ArrayList<FilterDefinitionSource>();
		for ( HibernateMappingProcessor processor : processors ) {
			processor.collectFilterDefSources( filterDefinitionSources );
		}
		return filterDefinitionSources;
	}

	@Override
	public Iterable<IdentifierGeneratorSource> extractGlobalIdentifierGeneratorSources() {
		return emptyList();
	}

	@Override
	@SuppressWarnings( {"unchecked", "RedundantCast"})
	public Iterable<EntityHierarchy> extractEntityHierarchies() {
		return (Iterable) entityHierarchies;
	}

	@Override
	public void processMappingDependentMetadata() {
		for ( HibernateMappingProcessor processor : processors ) {
			processor.processMappingDependentMetadata();
		}
	}
}
