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

import java.util.List;
import java.util.Map;

import org.dom4j.Attribute;

import org.hibernate.MappingException;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.MetadataSource;
import org.hibernate.metamodel.binding.MappingDefaults;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.source.Origin;
import org.hibernate.metamodel.source.internal.JaxbRoot;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLClass;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLFetch;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLFetchProfile;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLImport;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLJoinedSubclass;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLQuery;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSqlQuery;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLSubclass;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLUnionSubclass;
import org.hibernate.metamodel.source.util.MappingHelper;

/**
 * Responsible for performing binding of the {@code <hibernate-mapping/>} DOM element
 */
class HibernateMappingBinder implements MappingDefaults {
	private final HibernateXmlBinder hibernateXmlBinder;
	private final JaxbRoot<XMLHibernateMapping> jaxbRoot;
	private final XMLHibernateMapping hibernateMapping;

	private final String defaultSchemaName;
	private final String defaultCatalogName;
	private final String defaultCascade;
	private final String defaultAccess;
	private final boolean defaultLazy;
	private final String packageName;
	private final boolean autoImport;

	private Map<String, MetaAttribute> mappingMetas;


	HibernateMappingBinder(HibernateXmlBinder hibernateXmlBinder, JaxbRoot<XMLHibernateMapping> jaxbRoot) {
		this.hibernateXmlBinder = hibernateXmlBinder;
		this.jaxbRoot = jaxbRoot;
		this.hibernateMapping = jaxbRoot.getRoot();

		defaultSchemaName = hibernateMapping.getSchema();
		defaultCatalogName = hibernateMapping.getCatalog();
		defaultCascade = MappingHelper.getStringValue( hibernateMapping.getDefaultCascade(), "none" );
		defaultAccess = MappingHelper.getStringValue( hibernateMapping.getDefaultAccess(), "property" );
		//TODO: shouldn't default-lazy default to true???
		defaultLazy = MappingHelper.getBooleanValue( hibernateMapping.getDefaultLazy(), false );
		packageName = hibernateMapping.getPackage();
		autoImport = MappingHelper.getBooleanValue( hibernateMapping.getAutoImport(), false );

		mappingMetas = HbmHelper.extractMetas( hibernateMapping.getMeta(), true, hibernateXmlBinder.getGlobalMetas() );
	}

	HibernateXmlBinder getHibernateXmlBinder() {
		return hibernateXmlBinder;
	}

	XMLHibernateMapping getHibernateMapping() {
		return hibernateMapping;
	}

	Origin getOrigin() {
		return jaxbRoot.getOrigin();
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

	public NamingStrategy getNamingStrategy() {
		return hibernateXmlBinder.getMetadata().getNamingStrategy();
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

	void processHibernateMapping() {
		if ( hibernateMapping.getFilterDef() != null ) {
//			parseFilterDefs(  hibernateMapping.getFilterDef() );
		}
		if ( hibernateMapping.getFetchProfile() != null ) {
			parseFetchProfiles( hibernateMapping.getFetchProfile(), null );
		}
		if ( hibernateMapping.getIdentifierGenerator() != null )  {
//			parseIdentifierGeneratorRegistrations( hibernateMapping.getIdentifierGenerator() );
		}
		if ( hibernateMapping.getTypedef() != null ) {
//			bindTypeDef( hibernateMapping.getTypedefs() );
		}
		if ( hibernateMapping.getClazzOrSubclassOrJoinedSubclass() != null ) {
			for ( Object clazzOrSubclass : hibernateMapping.getClazzOrSubclassOrJoinedSubclass() ) {
				if ( XMLClass.class.isInstance( clazzOrSubclass ) ) {
					XMLClass clazz =
							XMLClass.class.cast( clazzOrSubclass );
					new RootEntityBinder( this, clazz ).process( clazz );
				}
				else if ( XMLSubclass.class.isInstance( clazzOrSubclass ) ) {
//					PersistentClass superModel = getSuperclass( mappings, element );
//					handleSubclass( superModel, mappings, element, inheritedMetas );
				}
				else if ( XMLJoinedSubclass.class.isInstance( clazzOrSubclass ) ) {
//					PersistentClass superModel = getSuperclass( mappings, element );
//					handleJoinedSubclass( superModel, mappings, element, inheritedMetas );
				}
				else if ( XMLUnionSubclass.class.isInstance( clazzOrSubclass ) ) {
//					PersistentClass superModel = getSuperclass( mappings, element );
//					handleUnionSubclass( superModel, mappings, element, inheritedMetas );
				}
				else {
					throw new org.hibernate.metamodel.source.MappingException( "unknown type of class or subclass: " +
							clazzOrSubclass.getClass().getName(), jaxbRoot.getOrigin()
					);
				}
			}
		}
		if ( hibernateMapping.getQueryOrSqlQuery() != null ) {
			for ( Object queryOrSqlQuery : hibernateMapping.getQueryOrSqlQuery() ) {
				if ( XMLQuery.class.isInstance( queryOrSqlQuery ) ) {
//					bindNamedQuery( element, null, mappings );
				}
				else if ( XMLSqlQuery.class.isInstance( queryOrSqlQuery ) ) {
//				bindNamedSQLQuery( element, null, mappings );
				}
				else {
					throw new org.hibernate.metamodel.source.MappingException( "unknown type of query: " +
							queryOrSqlQuery.getClass().getName(), jaxbRoot.getOrigin()
					);
				}
			}
		}
		if ( hibernateMapping.getResultset() != null ) {
//			bindResultSetMappingDefinitions( element, null, mappings );
		}
		if ( hibernateMapping.getImport() != null ) {
			processImports( hibernateMapping.getImport() );
		}
		if ( hibernateMapping.getDatabaseObject() != null ) {
//			bindAuxiliaryDatabaseObjects( element, mappings );
		}
	}

	private void processImports(List<XMLImport> imports) {
		for ( XMLImport importValue : imports ) {
			String className = getClassName( importValue.getClazz() );
			String rename = importValue.getRename();
			rename = ( rename == null ) ? StringHelper.unqualify( className ) : rename;
			hibernateXmlBinder.getMetadata().addImport( className, rename );
		}
	}

	protected void parseFetchProfiles(List<XMLFetchProfile> fetchProfiles, String containingEntityName) {
		for ( XMLFetchProfile fetchProfile : fetchProfiles ) {
			String profileName = fetchProfile.getName();
			org.hibernate.metamodel.binding.FetchProfile profile = hibernateXmlBinder.getMetadata().findOrCreateFetchProfile( profileName, MetadataSource.HBM );
			for (  XMLFetch fetch : fetchProfile.getFetch() ) {
				String entityName = fetch.getEntity() == null ? containingEntityName : fetch.getEntity();
				if ( entityName == null ) {
					throw new MappingException( "could not determine entity for fetch-profile fetch [" + profileName + "]:[" + fetch.getAssociation() + "]" );
				}
				profile.addFetch( entityName, fetch.getAssociation(), fetch.getStyle() );
			}
		}
	}

	String extractEntityName( XMLClass entityClazz) {
		return HbmHelper.extractEntityName( entityClazz, packageName );
	}

	String getClassName(Attribute attribute) {
		return HbmHelper.getClassName( attribute, packageName );
	}

	String getClassName(String unqualifiedName) {
		return HbmHelper.getClassName( unqualifiedName, packageName );
	}

}
