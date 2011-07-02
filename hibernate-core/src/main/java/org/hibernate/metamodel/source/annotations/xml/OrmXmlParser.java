/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
package org.hibernate.metamodel.source.annotations.xml;

import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.Index;

import org.hibernate.metamodel.binder.source.MetadataImplementor;
import org.hibernate.metamodel.binder.source.internal.JaxbRoot;
import org.hibernate.metamodel.source.annotation.xml.XMLEntityMappings;
import org.hibernate.metamodel.source.annotations.xml.mocker.EntityMappingsMocker;

/**
 * @author Hardy Ferentschik
 * @todo Is this still class really still necessary? Maybe it should be removed (HF)
 */
public class OrmXmlParser {
	private final MetadataImplementor metadata;

	public OrmXmlParser(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	/**
	 * Parses the given xml configuration files and returns a updated annotation index
	 *
	 * @param mappings list of {@code XMLEntityMappings} created from the specified orm xml files
	 * @param annotationIndex the annotation index based on scanned annotations
	 *
	 * @return a new updated annotation index, enhancing and modifying the existing ones according to the jpa xml rules
	 */
	public Index parseAndUpdateIndex(List<JaxbRoot<XMLEntityMappings>> mappings, Index annotationIndex) {
		List<XMLEntityMappings> list = new ArrayList<XMLEntityMappings>( mappings.size() );
		for ( JaxbRoot<XMLEntityMappings> jaxbRoot : mappings ) {
			list.add( jaxbRoot.getRoot() );
		}
		return new EntityMappingsMocker( list, annotationIndex, metadata.getServiceRegistry() ).mockNewIndex();
	}
}


