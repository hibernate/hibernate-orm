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
import java.util.List;

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping;
import org.hibernate.metamodel.source.internal.JaxbRoot;
import org.hibernate.metamodel.source.spi.SourceProcessor;
import org.hibernate.metamodel.source.spi.MetadataImplementor;

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
			processor.bindIndependentMetadata();
		}
	}

	@Override
	public void processTypeDependentMetadata(MetadataSources sources) {
		for ( HibernateMappingProcessor processor : processors ) {
			processor.bindTypeDependentMetadata();
		}
	}

	@Override
	public void processMappingMetadata(MetadataSources sources, List<String> processedEntityNames) {
		for ( HibernateMappingProcessor processor : processors ) {
			processor.bindMappingMetadata( processedEntityNames );
		}
	}

	@Override
	public void processMappingDependentMetadata(MetadataSources sources) {
		for ( HibernateMappingProcessor processor : processors ) {
			processor.bindMappingDependentMetadata();
		}
	}
}
