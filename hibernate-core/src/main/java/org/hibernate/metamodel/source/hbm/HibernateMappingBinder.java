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

import java.util.Iterator;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Element;

import org.hibernate.MappingException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.xml.XmlDocument;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.MetadataSource;
import org.hibernate.metamodel.binding.MappingDefaults;
import org.hibernate.metamodel.source.util.DomHelper;

/**
 * Responsible for performing binding of the {@code <hibernate-mapping/>} DOM element
 */
class HibernateMappingBinder implements MappingDefaults {
	private final HibernateXmlBinder hibernateXmlBinder;
	private final XmlDocument xmlDocument;
	private final Element hibernateMappingElement;

	private final String defaultSchemaName;
	private final String defaultCatalogName;
	private final String defaultCascade;
	private final String defaultAccess;
	private final boolean defaultLazy;
	private final String packageName;
	private final boolean autoImport;

	private Map<String, MetaAttribute> mappingMetas;

	HibernateMappingBinder(HibernateXmlBinder hibernateXmlBinder, XmlDocument xmlDocument) {
		this.hibernateXmlBinder = hibernateXmlBinder;
		this.xmlDocument = xmlDocument;
		this.hibernateMappingElement = xmlDocument.getDocumentTree().getRootElement();

		defaultSchemaName = DomHelper.extractAttributeValue( hibernateMappingElement, "schema" );
		defaultCatalogName = DomHelper.extractAttributeValue( hibernateMappingElement, "catalog" );
		defaultCascade = DomHelper.extractAttributeValue( hibernateMappingElement, "default-cascade", "none" );
		defaultAccess = DomHelper.extractAttributeValue( hibernateMappingElement, "default-access", "property" );
		defaultLazy = "true".equals( DomHelper.extractAttributeValue( hibernateMappingElement, "default-lazy", "false" ) );
		packageName = DomHelper.extractAttributeValue( hibernateMappingElement, "package" );
		autoImport = "true".equals( DomHelper.extractAttributeValue( hibernateMappingElement, "auto-import", "false" ) );

		mappingMetas = HbmHelper.extractMetas( hibernateMappingElement, true, hibernateXmlBinder.getGlobalMetas() );
	}

	HibernateXmlBinder getHibernateXmlBinder() {
		return hibernateXmlBinder;
	}

	XmlDocument getXmlDocument() {
		return xmlDocument;
	}

	public String getDefaultSchemaName() {
		return defaultSchemaName;
	}

	public String getDefaultCatalogName() {
		return defaultCatalogName;
	}

	public String getDefaultCascade() {
		return defaultCascade;
	}

	public String getDefaultAccess() {
		return defaultAccess;
	}

	public boolean isDefaultLazy() {
		return defaultLazy;
	}

	String getPackageName() {
		return packageName;
	}

	boolean isAutoImport() {
		return autoImport;
	}

	public Map<String, MetaAttribute> getMappingMetas() {
		return mappingMetas;
	}

	void processElement() {
		Iterator rootChildren = hibernateMappingElement.elementIterator();
		while ( rootChildren.hasNext() ) {
			final Element element = (Element) rootChildren.next();
			final String elementName = element.getName();

			if ( "filter-def".equals( elementName ) ) {
//				parseFilterDef( element, mappings );
			}
			else if ( "fetch-profile".equals( elementName ) ) {
				parseFetchProfile( element, null );
			}
			else if ( "identifier-generator".equals( elementName ) ) {
//				parseIdentifierGeneratorRegistration( element, mappings );
			}
			else if ( "typedef".equals( elementName ) ) {
//				bindTypeDef( element, mappings );
			}
			else if ( "class".equals( elementName ) ) {
				new RootEntityBinder( this, element ).process( element );
			}
			else if ( "subclass".equals( elementName ) ) {
//				PersistentClass superModel = getSuperclass( mappings, element );
//				handleSubclass( superModel, mappings, element, inheritedMetas );
			}
			else if ( "joined-subclass".equals( elementName ) ) {
//				PersistentClass superModel = getSuperclass( mappings, element );
//				handleJoinedSubclass( superModel, mappings, element, inheritedMetas );
			}
			else if ( "union-subclass".equals( elementName ) ) {
//				PersistentClass superModel = getSuperclass( mappings, element );
//				handleUnionSubclass( superModel, mappings, element, inheritedMetas );
			}
			else if ( "query".equals( elementName ) ) {
//				bindNamedQuery( element, null, mappings );
			}
			else if ( "sql-query".equals( elementName ) ) {
//				bindNamedSQLQuery( element, null, mappings );
			}
			else if ( "resultset".equals( elementName ) ) {
//				bindResultSetMappingDefinition( element, null, mappings );
			}
			else if ( "import".equals( elementName ) ) {
				processImport( element );
			}
			else if ( "database-object".equals( elementName ) ) {
//				bindAuxiliaryDatabaseObject( element, mappings );
			}
		}
	}

	private void processImport(Element importNode) {
		String className = getClassName( importNode.attribute( "class" ) );
		Attribute renameNode = importNode.attribute( "rename" );
		String rename = ( renameNode == null ) ? StringHelper.unqualify( className ) : renameNode.getValue();
		hibernateXmlBinder.getMetadata().addImport( className, rename );
	}

	protected void parseFetchProfile(Element element, String containingEntityName) {
		String profileName = element.attributeValue( "name" );
		FetchProfile profile = hibernateXmlBinder.getMetadata().findOrCreateFetchProfile( profileName, MetadataSource.HBM );
		Iterator itr = element.elementIterator( "fetch" );
		while ( itr.hasNext() ) {
			final Element fetchElement = ( Element ) itr.next();
			final String association = fetchElement.attributeValue( "association" );
			final String style = fetchElement.attributeValue( "style" );
			String entityName = fetchElement.attributeValue( "entity" );
			if ( entityName == null ) {
				entityName = containingEntityName;
			}
			if ( entityName == null ) {
				throw new MappingException( "could not determine entity for fetch-profile fetch [" + profileName + "]:[" + association + "]" );
			}
			profile.addFetch( entityName, association, style );
		}
	}

	String extractEntityName(Element element) {
		return HbmHelper.extractEntityName( element, packageName );
	}

	String getClassName(Attribute attribute) {
		return HbmHelper.getClassName( attribute, packageName );
	}

	String getClassName(String unqualifiedName) {
		return HbmHelper.getClassName( unqualifiedName, packageName );
	}

}
