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
package org.hibernate.metamodel.source.hbm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.hbm.xml.mapping.EntityElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.SubclassEntityElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping;
import org.hibernate.metamodel.source.internal.JaxbRoot;
import org.hibernate.metamodel.source.spi.MetadataImplementor;
import org.hibernate.metamodel.source.spi.SourceProcessor;

/**
 * Responsible for performing binding of hbm xml.
 */
public class HbmSourceProcessor implements SourceProcessor {
	private final MetadataImplementor metadata;
	private List<HibernateMappingProcessor> processors;

	public HbmSourceProcessor(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public void prepare(MetadataSources sources) {
		this.processors = new ArrayList<HibernateMappingProcessor>();
		for ( JaxbRoot jaxbRoot : sources.getJaxbRootList() ) {
			if ( jaxbRoot.getRoot() instanceof XMLHibernateMapping ) {
				processors.add( new HibernateMappingProcessor( metadata, (JaxbRoot<XMLHibernateMapping>) jaxbRoot ) );
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
		// Lets get the entities into a better order for processing based on inheritance hierarchy to avoid the need
		// for an "extends queue".  Really, for correctly, we localize the "extends queue" to just this method stack.
		//
		// The variable entityMappingByEntityNameMap holds the "resolved" mappings, keyed by entity name.  It uses a
		// linked map because the order is important here as we will use it to track which entities depend on which
		// other entities.
		//
		// The extendsQueue variable is a temporary queue where we place mappings which have an extends but for which
		// we could not find the referenced entity being extended.




		final Set<String> availableEntityNames = new HashSet<String>();
		availableEntityNames.addAll( processedEntityNames );








		final LinkedHashSet<HibernateMappingProcessor> orderedProcessors = new LinkedHashSet<HibernateMappingProcessor>();
		final Set<ExtendsQueueEntry> extendsQueue = new HashSet<ExtendsQueueEntry>();

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
}
