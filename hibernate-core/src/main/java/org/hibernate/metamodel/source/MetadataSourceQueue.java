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
package org.hibernate.metamodel.source;

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
import javax.persistence.Entity;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.InvalidMappingException;
import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.cfg.MetadataSourceType;
import org.hibernate.internal.util.collections.JoinedIterator;
import org.hibernate.internal.util.xml.XmlDocument;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class MetadataSourceQueue implements Serializable {
	private static final Logger log = LoggerFactory.getLogger( MetadataSourceQueue.class );
	private final Metadata metadata;

	private LinkedHashMap<XmlDocument, Set<String>> hbmMetadataToEntityNamesMap
			= new LinkedHashMap<XmlDocument, Set<String>>();
	private Map<String, XmlDocument> hbmMetadataByEntityNameXRef = new HashMap<String, XmlDocument>();

	//XClass are not serializable by default
	private transient List<XClass> annotatedClasses = new ArrayList<XClass>();
	//only used during the secondPhaseCompile pass, hence does not need to be serialized
	private transient Map<String, XClass> annotatedClassesByEntityNameMap = new HashMap<String, XClass>();

	public MetadataSourceQueue(Metadata metadata) {
		this.metadata = metadata;
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		annotatedClassesByEntityNameMap = new HashMap<String, XClass>();

		//build back annotatedClasses
		@SuppressWarnings("unchecked")
		List<Class> serializableAnnotatedClasses = (List<Class>) ois.readObject();
		annotatedClasses = new ArrayList<XClass>( serializableAnnotatedClasses.size() );
		for ( Class clazz : serializableAnnotatedClasses ) {
			annotatedClasses.add( metadata.getReflectionManager().toXClass( clazz ) );
		}
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		List<Class> serializableAnnotatedClasses = new ArrayList<Class>( annotatedClasses.size() );
		for ( XClass xClass : annotatedClasses ) {
			serializableAnnotatedClasses.add( metadata.getReflectionManager().toClass( xClass ) );
		}
		out.writeObject( serializableAnnotatedClasses );
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

	public void add(XClass annotatedClass) {
		annotatedClasses.add( annotatedClass );
	}

	protected void syncAnnotatedClasses() {
		final Iterator<XClass> itr = annotatedClasses.iterator();
		while ( itr.hasNext() ) {
			final XClass annotatedClass = itr.next();
			if ( annotatedClass.isAnnotationPresent( Entity.class ) ) {
				annotatedClassesByEntityNameMap.put( annotatedClass.getName(), annotatedClass );
				continue;
			}

			if ( !annotatedClass.isAnnotationPresent( javax.persistence.MappedSuperclass.class ) ) {
				itr.remove();
			}
		}
	}

	protected void processMetadata(List<MetadataSourceType> order) {
		syncAnnotatedClasses();

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
		log.debug( "Processing hbm.xml files" );
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

		for ( String entityName : entityNames ) {
			if ( annotatedClassesByEntityNameMap.containsKey( entityName ) ) {
				annotatedClasses.remove( annotatedClassesByEntityNameMap.get( entityName ) );
				annotatedClassesByEntityNameMap.remove( entityName );
			}
		}
	}

	private void processAnnotatedClassesQueue() {
		log.debug( "Process annotated classes" );
		//bind classes in the correct order calculating some inheritance state
		List<XClass> orderedClasses = orderAndFillHierarchy( annotatedClasses );
//		Map<XClass, InheritanceState> inheritanceStatePerClass = AnnotationBinder.buildInheritanceStates(
//				orderedClasses, mappings
//		);


		for ( XClass clazz : orderedClasses ) {
// todo : replace this with similar non-static code.
//			AnnotationBinder.bindClass( clazz, inheritanceStatePerClass, mappings );

			final String entityName = clazz.getName();
			if ( hbmMetadataByEntityNameXRef.containsKey( entityName ) ) {
				hbmMetadataToEntityNamesMap.remove( hbmMetadataByEntityNameXRef.get( entityName ) );
				hbmMetadataByEntityNameXRef.remove( entityName );
			}
		}
		annotatedClasses.clear();
		annotatedClassesByEntityNameMap.clear();
	}

	private List<XClass> orderAndFillHierarchy(List<XClass> original) {
		List<XClass> copy = new ArrayList<XClass>( original );
		insertMappedSuperclasses( original, copy );

		// order the hierarchy
		List<XClass> workingCopy = new ArrayList<XClass>( copy );
		List<XClass> newList = new ArrayList<XClass>( copy.size() );
		while ( workingCopy.size() > 0 ) {
			XClass clazz = workingCopy.get( 0 );
			orderHierarchy( workingCopy, newList, copy, clazz );
		}
		return newList;
	}

	private void insertMappedSuperclasses(List<XClass> original, List<XClass> copy) {
		for ( XClass clazz : original ) {
			XClass superClass = clazz.getSuperclass();
			while ( superClass != null
					&& !metadata.getReflectionManager().equals( superClass, Object.class )
					&& !copy.contains( superClass ) ) {
				if ( superClass.isAnnotationPresent( Entity.class )
						|| superClass.isAnnotationPresent( javax.persistence.MappedSuperclass.class ) ) {
					copy.add( superClass );
				}
				superClass = superClass.getSuperclass();
			}
		}
	}

	private void orderHierarchy(List<XClass> copy, List<XClass> newList, List<XClass> original, XClass clazz) {
		if ( clazz == null || metadata.getReflectionManager().equals( clazz, Object.class ) ) {
			return;
		}
		//process superclass first
		orderHierarchy( copy, newList, original, clazz.getSuperclass() );
		if ( original.contains( clazz ) ) {
			if ( !newList.contains( clazz ) ) {
				newList.add( clazz );
			}
			copy.remove( clazz );
		}
	}

	public boolean isEmpty() {
		return hbmMetadataToEntityNamesMap.isEmpty() && annotatedClasses.isEmpty();
	}

}
