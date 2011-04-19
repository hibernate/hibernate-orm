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
import org.hibernate.metamodel.source.hbm.xml.mapping.HibernateMapping;
import org.hibernate.metamodel.source.util.MappingHelper;

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

	private LinkedHashMap<JaxbRoot, Set<String>> hbmMetadataToEntityNamesMap
			= new LinkedHashMap<JaxbRoot, Set<String>>();
	private Map<String, JaxbRoot> hbmMetadataByEntityNameXRef = new HashMap<String, JaxbRoot>();
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

	/* TODO: needed anymore???
	public void add(XmlDocument metadataXml) {
		final Document document = metadataXml.getDocumentTree();
		final Element hmNode = document.getRootElement();
		Attribute packNode = hmNode.attribute( "package" );
		String defaultPackage = packNode != null ? packNode.getValue() : "";
		Set<String> entityNames = new HashSet<String>();
		findClassNames( defaultPackage, hmNode, entityNames );
		for ( String entity : entityNames ) {
			hbmMetadataByEntityNameXRef.put( entity, jaxbRoot );
		}
		this.hbmMetadataToEntityNamesMap.put( jaxbRoot, entityNames );
	}
	*/

	public void add(JaxbRoot jaxbRoot) {
		// TODO: does this have to work for EntityMappings also?
		final HibernateMapping hibernateMapping = ( HibernateMapping ) jaxbRoot.getRoot();
		String defaultPackage = MappingHelper.getStringValue( hibernateMapping.getPackage(), "" );
		Set<String> entityNames = new HashSet<String>();
		findClassNames( defaultPackage, hibernateMapping.getClazzOrSubclassOrJoinedSubclass(), entityNames );
		for ( String entity : entityNames ) {
			hbmMetadataByEntityNameXRef.put( entity, jaxbRoot );
		}
		this.hbmMetadataToEntityNamesMap.put( jaxbRoot, entityNames );
	}

	private void findClassNames(String defaultPackage, List entityClasses, Set<String> names) {
		// if we have some extends we need to check if those classes possibly could be inside the
		// same hbm.xml file...

		// HibernateMapping.getClazzOrSubclassOrJoinedSubclass returns union-subclass objects
		// as well as class, subclass, and joined-subclass objects
		for ( Object entityClass : entityClasses) {
			String entityName;
			// TODO: can Class, Subclass, JoinedSubclass, and UnionSubclass implement the same interface
			// so this stuff doesn't need to be duplicated?
			if ( org.hibernate.metamodel.source.hbm.xml.mapping.Subclass.class.isInstance( entityClass ) ) {
				org.hibernate.metamodel.source.hbm.xml.mapping.Subclass clazz = org.hibernate.metamodel.source.hbm.xml.mapping.Subclass.class.cast( entityClass );
				names.add(
						clazz.getEntityName() != null ?
								clazz.getEntityName() :
								getClassName( clazz.getName(), defaultPackage )
				);
				findClassNames( defaultPackage, clazz.getSubclass(), names );
			}
			else if ( org.hibernate.metamodel.source.hbm.xml.mapping.Class.class.isInstance( entityClass ) ) {
				org.hibernate.metamodel.source.hbm.xml.mapping.Class clazz = org.hibernate.metamodel.source.hbm.xml.mapping.Class.class.cast( entityClass );
				names.add(
						clazz.getEntityName() != null ?
								clazz.getEntityName() :
								getClassName( clazz.getName(), defaultPackage )
				);
				findClassNames( defaultPackage, clazz.getSubclass(), names );
				findClassNames( defaultPackage, clazz.getJoinedSubclass(), names );
				findClassNames( defaultPackage, clazz.getUnionSubclass(), names );

			}
			else if ( org.hibernate.metamodel.source.hbm.xml.mapping.UnionSubclass.class.isInstance( entityClass ) ) {
				org.hibernate.metamodel.source.hbm.xml.mapping.UnionSubclass clazz = org.hibernate.metamodel.source.hbm.xml.mapping.UnionSubclass.class.cast( entityClass );
				names.add(
						clazz.getEntityName() != null ?
								clazz.getEntityName() :
								getClassName( clazz.getName(), defaultPackage )
				);
				findClassNames( defaultPackage, clazz.getUnionSubclass(), names );
			}
			else if ( org.hibernate.metamodel.source.hbm.xml.mapping.JoinedSubclass.class.isInstance( entityClass ) ) {
				org.hibernate.metamodel.source.hbm.xml.mapping.JoinedSubclass clazz = org.hibernate.metamodel.source.hbm.xml.mapping.JoinedSubclass.class.cast( entityClass );
				names.add(
						clazz.getEntityName() != null ?
								clazz.getEntityName() :
								getClassName( clazz.getName(), defaultPackage )
				);
				findClassNames( defaultPackage, clazz.getJoinedSubclass(), names );
			}
			else {
				throw new InvalidMappingException( "unknown type of entity class", entityClass.getClass().getName() );
			}
		}
	}

	private String getClassName(String unqualifiedName, String defaultPackage) {
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
		for ( Map.Entry<JaxbRoot, Set<String>> entry : hbmMetadataToEntityNamesMap.entrySet() ) {
			// Unfortunately we have to create a Mappings instance for each iteration here
			processHbmXml( entry.getKey(), entry.getValue() );
		}
		hbmMetadataToEntityNamesMap.clear();
		hbmMetadataByEntityNameXRef.clear();
	}

	public void processHbmXml(JaxbRoot jaxbRoot, Set<String> entityNames) {
		try {
			metadata.getHibernateXmlBinder().bindRoot( jaxbRoot, entityNames );
		}
		catch ( MappingException me ) {
			throw new InvalidMappingException(
					jaxbRoot.getOrigin().getType().toString(),
					jaxbRoot.getOrigin().getName(),
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
