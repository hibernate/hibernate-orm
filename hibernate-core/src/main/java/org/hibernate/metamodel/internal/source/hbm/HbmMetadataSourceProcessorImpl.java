/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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

import org.hibernate.internal.jaxb.JaxbRoot;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbHibernateMapping;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.MetadataSourceProcessor;
import org.hibernate.metamodel.spi.source.FilterDefinitionSource;
import org.hibernate.metamodel.spi.source.MetadataImplementor;
import org.hibernate.metamodel.spi.source.TypeDescriptorSource;

/**
 * The {@link org.hibernate.metamodel.spi.MetadataSourceProcessor} implementation responsible for processing {@code hbm.xml} sources.
 *
 * @author Steve Ebersole
 */
public class HbmMetadataSourceProcessorImpl implements MetadataSourceProcessor {
	private final MetadataImplementor metadata;

	private List<HibernateMappingProcessor> processors = new ArrayList<HibernateMappingProcessor>();
	private List<EntityHierarchyImpl> entityHierarchies;

	public HbmMetadataSourceProcessorImpl(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public void prepare(MetadataSources sources) {
		final HierarchyBuilder hierarchyBuilder = new HierarchyBuilder();

		for ( JaxbRoot jaxbRoot : sources.getJaxbRootList() ) {
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
	public Iterable<TypeDescriptorSource> extractTypeDefinitionSources(MetadataSources sources) {
		final List<TypeDescriptorSource> typeDescriptorSources = new ArrayList<TypeDescriptorSource>();
		for ( HibernateMappingProcessor processor : processors ) {
			processor.collectTypeDescriptorSources( typeDescriptorSources );
		}
		return typeDescriptorSources;
	}

	@Override
	public Iterable<FilterDefinitionSource> extractFilterDefinitionSources(MetadataSources sources) {
		final List<FilterDefinitionSource> filterDefinitionSources = new ArrayList<FilterDefinitionSource>();
		for ( HibernateMappingProcessor processor : processors ) {
			processor.collectFilterDefSources( filterDefinitionSources );
		}
		return filterDefinitionSources;
	}

	@Override
	public Iterable<EntityHierarchyImpl> extractEntityHierarchies(MetadataSources sources) {
		return entityHierarchies;
	}

	@Override
	public void processMappingDependentMetadata(MetadataSources sources) {
		for ( HibernateMappingProcessor processor : processors ) {
			processor.processMappingDependentMetadata();
		}
	}
}
