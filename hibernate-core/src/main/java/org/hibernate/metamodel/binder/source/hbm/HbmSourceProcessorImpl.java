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
package org.hibernate.metamodel.binder.source.hbm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.binder.source.BindingContext;
import org.hibernate.metamodel.binder.source.MappingDefaults;
import org.hibernate.metamodel.binder.source.MetadataImplementor;
import org.hibernate.metamodel.binder.source.SourceProcessor;
import org.hibernate.metamodel.binder.source.hbm.xml.mapping.EntityElement;
import org.hibernate.metamodel.binder.source.hbm.xml.mapping.SubclassEntityElement;
import org.hibernate.metamodel.binder.source.internal.JaxbRoot;
import org.hibernate.metamodel.domain.JavaType;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping;
import org.hibernate.service.ServiceRegistry;

/**
 * The {@link SourceProcessor} implementation responsible for processing {@code hbm.xml} sources.
 *
 * @author Steve Ebersole
 */
public class HbmSourceProcessorImpl implements SourceProcessor, BindingContext {
	private final MetadataImplementor metadata;
	private List<HibernateMappingProcessor> processors;

	public HbmSourceProcessorImpl(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public void prepare(MetadataSources sources) {
		this.processors = new ArrayList<HibernateMappingProcessor>();
		for ( JaxbRoot jaxbRoot : sources.getJaxbRootList() ) {
			if ( jaxbRoot.getRoot() instanceof XMLHibernateMapping ) {
				processors.add( new HibernateMappingProcessor( this, (JaxbRoot<XMLHibernateMapping>) jaxbRoot ) );
			}
		}
	}

	@Override
	public void processIndependentMetadata(MetadataSources sources) {
		for ( HibernateMappingProcessor processor : processors ) {
			processor.processIndependentMetadata();
		}
	}

	@Override
	public void processTypeDependentMetadata(MetadataSources sources) {
		for ( HibernateMappingProcessor processor : processors ) {
			processor.processTypeDependentMetadata();
		}
	}

	@Override
	public void processMappingMetadata(MetadataSources sources, List<String> processedEntityNames) {
		// Lets get the entities (the mapping processors really) into a better order for processing based on
		// inheritance hierarchy to avoid the need for an "extends queue".  Really, correctly speaking, we are
		// localizing the "extends queue" to just this method stack.

		// 'orderedProcessors' represents the processors we know are not waiting on any super-types in the entity
		// hierarchy to be found.  'extendsQueue', conversely, holds processors (and other information) relating
		// we know have to wait on at least one of the super-types in their entity hierarchy to be found.
		final LinkedHashSet<HibernateMappingProcessor> orderedProcessors = new LinkedHashSet<HibernateMappingProcessor>();
		final Set<ExtendsQueueEntry> extendsQueue = new HashSet<ExtendsQueueEntry>();

		// 'availableEntityNames' holds all of the available entity names.  This means the incoming set of
		// 'processedEntityNames' as well as any entity-names found here as they are added to 'orderedProcessors'
		final Set<String> availableEntityNames = new HashSet<String>();
		availableEntityNames.addAll( processedEntityNames );

		// this loop is essentially splitting processors into those that can be processed immediately and those that
		// have to wait on entities not yet seen.  Those that have to wait go in the extendsQueue.  Those that can be
		// processed immediately go into 'orderedProcessors'.
		for ( HibernateMappingProcessor processor : processors ) {
			final HibernateMappingInformation hibernateMappingInformation = new HibernateMappingInformation( processor );
			ExtendsQueueEntry extendsQueueEntry = null;
			for ( Object entityElementO : processor.getHibernateMapping().getClazzOrSubclassOrJoinedSubclass() ) {
				final EntityElement entityElement = (EntityElement) entityElementO;
				final String entityName = processor.determineEntityName( entityElement );
				hibernateMappingInformation.includedEntityNames.add( entityName );
				if ( SubclassEntityElement.class.isInstance( entityElement ) ) {
					final String entityItExtends = ( (SubclassEntityElement) entityElement ).getExtends();
					if ( ! availableEntityNames.contains( entityItExtends ) ) {
						if ( extendsQueueEntry == null ) {
							extendsQueueEntry = new ExtendsQueueEntry( hibernateMappingInformation );
							extendsQueue.add( extendsQueueEntry );
						}
						extendsQueueEntry.waitingOnEntityNames.add( entityItExtends );
					}
				}
			}
			if ( extendsQueueEntry == null ) {
				// we found no extends names that we have to wait on
				orderedProcessors.add( processor );
				availableEntityNames.addAll( hibernateMappingInformation.includedEntityNames );
			}
		}

		// This loop tries to move entries from 'extendsQueue' into 'orderedProcessors', stopping when we cannot
		// process any more or they have all been processed.
		while ( ! extendsQueue.isEmpty() ) {
			// set up a pass over the queue
			int numberOfMappingsProcessed = 0;
			Iterator<ExtendsQueueEntry> iterator = extendsQueue.iterator();
			while ( iterator.hasNext() ) {
				final ExtendsQueueEntry entry = iterator.next();
				if ( availableEntityNames.containsAll( entry.waitingOnEntityNames ) ) {
					// all the entity names this entry was waiting on have been made available
					iterator.remove();
					orderedProcessors.add( entry.hibernateMappingInformation.processor );
					availableEntityNames.addAll( entry.hibernateMappingInformation.includedEntityNames );
					numberOfMappingsProcessed++;
				}
			}

			if ( numberOfMappingsProcessed == 0 ) {
				// todo : we could log the waiting dependencies...
				throw new MappingException( "Unable to process extends dependencies in hbm files" );
			}
		}

		// This loop executes the processors.
		for ( HibernateMappingProcessor processor : orderedProcessors ) {
			processor.processMappingMetadata( processedEntityNames );
		}
	}

	private static class HibernateMappingInformation {
		private final HibernateMappingProcessor processor;
		private final Set<String> includedEntityNames = new HashSet<String>();

		private HibernateMappingInformation(HibernateMappingProcessor processor) {
			this.processor = processor;
		}
	}

	private static class ExtendsQueueEntry {
		private HibernateMappingInformation hibernateMappingInformation;
		private final Set<String> waitingOnEntityNames = new HashSet<String>();

		private ExtendsQueueEntry(HibernateMappingInformation hibernateMappingInformation) {
			this.hibernateMappingInformation = hibernateMappingInformation;
		}
	}

	@Override
	public void processMappingDependentMetadata(MetadataSources sources) {
		for ( HibernateMappingProcessor processor : processors ) {
			processor.processMappingDependentMetadata();
		}
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return metadata.getServiceRegistry();
	}

	@Override
	public NamingStrategy getNamingStrategy() {
		return metadata.getNamingStrategy();
	}

	@Override
	public MappingDefaults getMappingDefaults() {
		return metadata.getMappingDefaults();
	}

	@Override
	public MetadataImplementor getMetadataImplementor() {
		return metadata;
	}

	@Override
	public <T> Class<T> locateClassByName(String name) {
		return metadata.locateClassByName( name );
	}

	@Override
	public JavaType makeJavaType(String className) {
		return metadata.makeJavaType( className );
	}
}
