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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.JoinedIterator;
import org.hibernate.internal.util.xml.XmlDocument;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.source.internal.JaxbRoot;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.metamodel.source.hbm.xml.mapping.HibernateMapping;

/**
 * Binder for {@code hbm.xml} files
 *
 * @author Steve Ebersole
 */
public class HibernateXmlBinder {
	private static final Logger log = LoggerFactory.getLogger( HibernateXmlBinder.class );

	private final MetadataImpl metadata;
	private final Map<String, MetaAttribute> globalMetas;

	public HibernateXmlBinder(MetadataImpl metadata) {
		this( metadata, Collections.<String, MetaAttribute>emptyMap() );
	}

	public HibernateXmlBinder(MetadataImpl metadata, Map<String, MetaAttribute> globalMetas) {
		this.metadata = metadata;
		this.globalMetas = globalMetas;
	}

	public void bindRoot(JaxbRoot<HibernateMapping> jaxbRoot) {
		log.debug( jaxbRoot.toString() );
		bindRoot( jaxbRoot, Collections.<String>emptySet() );
	}

	public void bindRoot(JaxbRoot<HibernateMapping> jaxbRoot, Set<String> entityNames) {
		final HibernateMappingBinder mappingBinder = new HibernateMappingBinder( this, jaxbRoot );

// this is irrelevant due to HHH-6118 and the fact that now all sources should be
//		List<String> names = locateEntityNamesAwaitingExtends( metadataXml, mappingBinder );
//		if ( !names.isEmpty() ) {
//			// classes mentioned in extends not available - so put it in queue
//			for ( String name : names ) {
//				metadata.getExtendsQueue()
//						.add( new ExtendsQueueEntry( name, mappingBinder.getPackageName(), metadataXml, entityNames ) );
//			}
//			return;
//		}

		mappingBinder.processHibernateMapping();
	}

	MetadataImpl getMetadata() {
		return metadata;
	}

	Map<String, MetaAttribute> getGlobalMetas() {
		return globalMetas;
	}

	private List<String> locateEntityNamesAwaitingExtends(XmlDocument metadataXml, HibernateMappingBinder mappingBinder) {
		final String unqualifiedPackageName = mappingBinder.getPackageName();
		final Element rootElement = metadataXml.getDocumentTree().getRootElement();

		List<String> awaitingExtends = new ArrayList<String>();

		// first, iterate over all elements capable of defining an extends attribute
		// collecting all found extends references if they cannot be resolved
		// against the already processed mappings.
		Iterator[] subclasses = new Iterator[3];
		subclasses[0] = rootElement.elementIterator( "subclass" );
		subclasses[1] = rootElement.elementIterator( "joined-subclass" );
		subclasses[2] = rootElement.elementIterator( "union-subclass" );

		Iterator iterator = new JoinedIterator( subclasses );
		while ( iterator.hasNext() ) {
			final Element element = (Element) iterator.next();
			final String extendsName = element.attributeValue( "extends" );
			// mappings might contain either the "raw" extends name (in the case of
			// an entity-name mapping) or a FQN (in the case of a POJO mapping).
			if ( getMetadata().getEntityBinding( extendsName ) == null
					&& getMetadata().getEntityBinding(
					HbmHelper.getClassName(
							extendsName, unqualifiedPackageName
					)
			) == null ) {
				awaitingExtends.add( extendsName );
			}
		}

		if ( !awaitingExtends.isEmpty() ) {
			// So far, the 'awaitingExtends' list containg all entity names found as extends in this
			// current document which were not previously processed in earlier documents.
			//
			// Now we will go through and remove the ones that are contained in this document
			final java.util.Set<String> set = new HashSet<String>( awaitingExtends );
			EntityElementHandler handler = new EntityElementHandler() {
				public void handleEntity(String entityName, String className) {
					if ( entityName != null ) {
						set.remove( entityName );
					}
					else {
						String fqn = HbmHelper.getClassName( className, unqualifiedPackageName );
						set.remove( fqn );
						if ( unqualifiedPackageName != null ) {
							set.remove( StringHelper.unqualify( fqn ) );
						}
					}
				}
			};
			recognizeEntities( rootElement, handler );
			awaitingExtends.clear();
			awaitingExtends.addAll( set );
		}

		return awaitingExtends;
	}


	/**
	 * Given an entity-containing-element (startNode) recursively locate all
	 * entity names defined within that element.
	 *
	 * @param startNode The containing element
	 * @param handler The thing that knows what to do whenever we recognize an
	 * entity-name
	 */
	private static void recognizeEntities(final Element startNode, EntityElementHandler handler) {
		Iterator[] classes = new Iterator[4];
		classes[0] = startNode.elementIterator( "class" );
		classes[1] = startNode.elementIterator( "subclass" );
		classes[2] = startNode.elementIterator( "joined-subclass" );
		classes[3] = startNode.elementIterator( "union-subclass" );

		Iterator classIterator = new JoinedIterator( classes );
		while ( classIterator.hasNext() ) {
			Element element = (Element) classIterator.next();
			handler.handleEntity(
					element.attributeValue( "entity-name" ),
					element.attributeValue( "name" )
			);
			recognizeEntities( element, handler );
		}
	}

	private static interface EntityElementHandler {
		public void handleEntity(String entityName, String className);
	}
}
