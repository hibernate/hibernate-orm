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
package org.hibernate.metamodel.source.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.jboss.logging.Logger;

import org.hibernate.InvalidMappingException;
import org.hibernate.MappingException;
import org.hibernate.cfg.MetadataSourceType;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.JoinedIterator;
import org.hibernate.internal.util.xml.XmlDocument;

/**
 * Container for xml configuration documents and annotated classes.
 *
 * @author Steve Ebersole
 */
public class MetadataSourceQueue implements Serializable {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class, MetadataSourceQueue.class.getName()
	);
	private final MetadataImpl metadata;

	private LinkedHashMap<XmlDocument, Set<String>> hbmMetadataToEntityNamesMap
			= new LinkedHashMap<XmlDocument, Set<String>>();
	private Map<String, XmlDocument> hbmMetadataByEntityNameXRef = new HashMap<String, XmlDocument>();
	private transient List<Class> annotatedClasses = new ArrayList<Class>();

	public MetadataSourceQueue(MetadataImpl metadata) {
		this.metadata = metadata;
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	public void add(XmlDocument metadataXml) {
		final Document document = metadataXml.getDocumentTree();
		final Element hmNode = document.getRootElement();
		Attribute packNode = hmNode.attribute( "package" );
		String defaultPackage = packNode != null ? packNode.getValue() : "";
		Set<String> entityNames = new HashSet<String>();
		findClassNames( defaultPackage, hmNode, entityNames );
		for ( String entity : entityNames ) {
			hbmMetadataByEntityNameXRef.put( entity, metadataXml );
		}
		this.hbmMetadataToEntityNamesMap.put( metadataXml, entityNames );
	}

	private void findClassNames(String defaultPackage, Element startNode, Set<String> names) {
		// if we have some extends we need to check if those classes possibly could be inside the
		// same hbm.xml file...
		Iterator[] classes = new Iterator[4];
		classes[0] = startNode.elementIterator( "class" );
		classes[1] = startNode.elementIterator( "subclass" );
		classes[2] = startNode.elementIterator( "joined-subclass" );
		classes[3] = startNode.elementIterator( "union-subclass" );

		Iterator classIterator = new JoinedIterator( classes );
		while ( classIterator.hasNext() ) {
			Element element = (Element) classIterator.next();
			String entityName = element.attributeValue( "entity-name" );
			if ( entityName == null ) {
				entityName = getClassName( element.attribute( "name" ), defaultPackage );
			}
			names.add( entityName );
			findClassNames( defaultPackage, element, names );
		}
	}

	private String getClassName(Attribute name, String defaultPackage) {
		if ( name == null ) {
			return null;
		}
		String unqualifiedName = name.getValue();
		if ( unqualifiedName == null ) {
			return null;
		}
		if ( unqualifiedName.indexOf( '.' ) < 0 && defaultPackage != null ) {
			return defaultPackage + '.' + unqualifiedName;
		}
		return unqualifiedName;
	}

	public void add(Class annotatedClass) {
		annotatedClasses.add( annotatedClass );
	}

	protected void processMetadata(List<MetadataSourceType> order) {
		for ( MetadataSourceType type : order ) {
			if ( MetadataSourceType.HBM.equals( type ) ) {
				processHbmXmlQueue();
			}
			else if ( MetadataSourceType.CLASS.equals( type ) ) {
				processAnnotatedClassesQueue();
			}
		}
	}

	private void processHbmXmlQueue() {
		LOG.debug( "Processing hbm.xml files" );
		for ( Map.Entry<XmlDocument, Set<String>> entry : hbmMetadataToEntityNamesMap.entrySet() ) {
			// Unfortunately we have to create a Mappings instance for each iteration here
			processHbmXml( entry.getKey(), entry.getValue() );
		}
		hbmMetadataToEntityNamesMap.clear();
		hbmMetadataByEntityNameXRef.clear();
	}

	public void processHbmXml(XmlDocument metadataXml, Set<String> entityNames) {
		try {
			metadata.getHibernateXmlBinder().bindRoot( metadataXml, entityNames );
		}
		catch ( MappingException me ) {
			throw new InvalidMappingException(
					metadataXml.getOrigin().getType(),
					metadataXml.getOrigin().getName(),
					me
			);
		}
	}

	private void processAnnotatedClassesQueue() {
		LOG.debug( "Process annotated classes" );
		annotatedClasses.clear();
	}

	public boolean isEmpty() {
		return hbmMetadataToEntityNamesMap.isEmpty() && annotatedClasses.isEmpty();
	}
}
