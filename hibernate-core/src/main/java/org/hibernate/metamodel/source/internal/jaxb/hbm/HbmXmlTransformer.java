/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.jaxb.hbm;

import java.util.Date;

import org.hibernate.FlushMode;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.jaxb.JaxbCacheModeType;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntity;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityMappings;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmFetchProfile;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmFilterDef;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmIdGeneratorDef;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmParam;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmToolingHint;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmTypeDef;
import org.hibernate.metamodel.source.internal.jaxb.JaxbNamedNativeQuery;
import org.hibernate.metamodel.source.internal.jaxb.JaxbNamedQuery;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPersistenceUnitMetadata;
import org.hibernate.metamodel.source.internal.jaxb.JaxbQueryParamType;

import org.jboss.logging.Logger;

/**
 * Transforms a JAXB binding of a hbm.xml file into a unified orm.xml representation
 *
 * @author Steve Ebersole
 */
public class HbmXmlTransformer {
	private static final Logger log = Logger.getLogger( HbmXmlTransformer.class );

	/**
	 * Singleton access
	 */
	public static final HbmXmlTransformer INSTANCE = new HbmXmlTransformer();

	public JaxbEntityMappings transform(JaxbHibernateMapping hbmXmlMapping) {
		final JaxbEntityMappings ormRoot = new JaxbEntityMappings();
		ormRoot.setDescription(
				"Hibernate orm.xml document auto-generated from legacy hbm.xml format via transformation " +
						"(generated at " +  new Date().toString() + ")"
		);

		final JaxbPersistenceUnitMetadata metadata = new JaxbPersistenceUnitMetadata();
		ormRoot.setPersistenceUnitMetadata( metadata );
		metadata.setDescription(
				"Defines information which applies to the persistence unit overall, not just to this mapping file.\n\n" +
						"This transformation only specifies xml-mapping-metadata-complete."
		);

		transferToolingHints( hbmXmlMapping, ormRoot );

		ormRoot.setPackage( hbmXmlMapping.getPackage() );
		ormRoot.setSchema( hbmXmlMapping.getSchema() );
		ormRoot.setCatalog( hbmXmlMapping.getCatalog() );
		ormRoot.setCustomAccess( hbmXmlMapping.getDefaultAccess() );
		ormRoot.setDefaultCascade( hbmXmlMapping.getDefaultCascade() );
		ormRoot.setAutoImport( hbmXmlMapping.isAutoImport() );
		ormRoot.setDefaultLazy( hbmXmlMapping.isDefaultLazy() );

		transferTypeDefs( hbmXmlMapping, ormRoot );
		transferIdentifierGenerators( hbmXmlMapping, ormRoot );
		transferFilterDefs( hbmXmlMapping, ormRoot );
		transferFetchProfiles( hbmXmlMapping, ormRoot );
		transferImports( hbmXmlMapping, ormRoot );

		transferResultSetMappings( hbmXmlMapping, ormRoot );
		transferNamedQuery( hbmXmlMapping, ormRoot );
		transferNamedSqlQuery( hbmXmlMapping, ormRoot );

		transferDatabaseObjects( hbmXmlMapping, ormRoot );

		transferEntities( hbmXmlMapping, ormRoot );

		return ormRoot;
	}

	private void transferToolingHints(JaxbMetaContainerElement hbmMetaContainer, JaxbEntityMappings entityMappings) {
		if ( hbmMetaContainer.getMeta().isEmpty() ) {
			return;
		}

		for ( JaxbMetaElement hbmMetaElement : hbmMetaContainer.getMeta() ) {
			final JaxbHbmToolingHint toolingHint = new JaxbHbmToolingHint();
			entityMappings.getToolingHint().add( toolingHint );
			toolingHint.setName( hbmMetaElement.getName() );
			toolingHint.setInheritable( hbmMetaElement.isInheritable() );
			toolingHint.setValue( hbmMetaElement.getValue() );
		}
	}

	private void transferTypeDefs(JaxbHibernateMapping hbmXmlMapping, JaxbEntityMappings ormRoot) {
		if ( hbmXmlMapping.getTypedef().isEmpty() ) {
			return;
		}

		for ( JaxbTypedefElement hbmXmlTypeDef : hbmXmlMapping.getTypedef() ) {
			final JaxbHbmTypeDef typeDef = new JaxbHbmTypeDef();
			ormRoot.getTypeDef().add( typeDef );
			typeDef.setName( hbmXmlTypeDef.getName() );
			typeDef.setClazz( hbmXmlTypeDef.getClazz() );

			if ( !hbmXmlTypeDef.getParam().isEmpty() ) {
				for ( JaxbParamElement hbmParam : hbmXmlTypeDef.getParam() ) {
					final JaxbHbmParam param = new JaxbHbmParam();
					typeDef.getParam().add( param );
					param.setName( hbmParam.getName() );
					param.setValue( hbmParam.getValue() );
				}
			}
		}
	}

	private void transferFilterDefs(JaxbHibernateMapping hbmXmlMapping, JaxbEntityMappings ormRoot) {
		if ( hbmXmlMapping.getFilterDef().isEmpty() ) {
			return;
		}

		for ( JaxbFilterDefElement hbmFilterDef : hbmXmlMapping.getFilterDef() ) {
			JaxbHbmFilterDef filterDef = new JaxbHbmFilterDef();
			ormRoot.getFilterDef().add( filterDef );
			filterDef.setName( hbmFilterDef.getName() );

			boolean foundCondition = false;
			for ( Object content : hbmFilterDef.getContent() ) {
				if ( String.class.isInstance( content ) ) {
					foundCondition = true;
					filterDef.setCondition( (String) content );
				}
				else {
					JaxbFilterParamElement hbmFilterParam = (JaxbFilterParamElement) content;
					JaxbHbmFilterDef.JaxbFilterParam param = new JaxbHbmFilterDef.JaxbFilterParam();
					filterDef.getFilterParam().add( param );
					param.setName( hbmFilterParam.getParameterName() );
					param.setType( hbmFilterParam.getParameterValueTypeName() );
				}
			}

			if ( !foundCondition ) {
				filterDef.setCondition( hbmFilterDef.getCondition() );
			}
		}
	}

	private void transferIdentifierGenerators(JaxbHibernateMapping hbmXmlMapping, JaxbEntityMappings ormRoot) {
		if ( hbmXmlMapping.getIdentifierGenerator().isEmpty() ) {
			return;
		}

		for ( JaxbIdentifierGeneratorElement hbmGenerator : hbmXmlMapping.getIdentifierGenerator() ) {
			final JaxbHbmIdGeneratorDef generatorDef = new JaxbHbmIdGeneratorDef();
			ormRoot.getIdentifierGeneratorDef().add( generatorDef );
			generatorDef.setName( hbmGenerator.getName() );
			generatorDef.setClazz( hbmGenerator.getClazz() );
		}
	}

	private void transferImports(JaxbHibernateMapping hbmXmlMapping, JaxbEntityMappings ormRoot) {
		if ( hbmXmlMapping.getImport().isEmpty() ) {
			return;
		}

		for ( JaxbImportElement hbmImport : hbmXmlMapping.getImport() ) {
			final JaxbEntityMappings.JaxbImport ormImport = new JaxbEntityMappings.JaxbImport();
			ormRoot.getImport().add( ormImport );
			ormImport.setClazz( hbmImport.getClazz() );
			ormImport.setRename( hbmImport.getRename() );
		}
	}

	private void transferResultSetMappings(JaxbHibernateMapping hbmXmlMapping, JaxbEntityMappings ormRoot) {
		if ( hbmXmlMapping.getResultset().isEmpty() ) {
			return;
		}

		// todo : implement this; or decide to not support it in transformation
		log.debugf( "skipping hbm.xml <resultset/> definitions" );
	}

	private void transferFetchProfiles(JaxbHibernateMapping hbmXmlMapping, JaxbEntityMappings ormRoot) {
		if ( hbmXmlMapping.getFetchProfile().isEmpty() ) {
			return;
		}

		for ( JaxbFetchProfileElement hbmFetchProfile : hbmXmlMapping.getFetchProfile() ) {
			final JaxbHbmFetchProfile fetchProfile = new JaxbHbmFetchProfile();
			ormRoot.getFetchProfile().add( fetchProfile );
			fetchProfile.setName( hbmFetchProfile.getName() );

			if ( hbmFetchProfile.getFetch().isEmpty() ) {
				// really this should be an error, right?
				continue;
			}
			for ( JaxbFetchProfileElement.JaxbFetch hbmFetch : hbmFetchProfile.getFetch() ) {
				final JaxbHbmFetchProfile.JaxbFetch fetch = new JaxbHbmFetchProfile.JaxbFetch();
				fetchProfile.getFetch().add( fetch );
				fetch.setEntity( hbmFetch.getEntity() );
				fetch.setAssociation( hbmFetch.getAssociation() );
				fetch.setStyle( hbmFetch.getStyle().value() );
			}
		}
	}

	private void transferNamedQuery(JaxbHibernateMapping hbmXmlMapping, JaxbEntityMappings ormRoot) {
		if ( hbmXmlMapping.getQuery().isEmpty() ) {
			return;
		}

		for ( JaxbQueryElement hbmQuery : hbmXmlMapping.getQuery() ) {
			final JaxbNamedQuery query = new JaxbNamedQuery();
			ormRoot.getNamedQuery().add( query );
			query.setName( hbmQuery.getName() );
			query.setCacheable( hbmQuery.isCacheable() );
			query.setCacheMode( convert( hbmQuery.getCacheMode() ) );
			query.setCacheRegion( hbmQuery.getCacheRegion() );
			query.setComment( hbmQuery.getComment() );
			query.setFetchSize( hbmQuery.getFetchSize() );
			query.setFlushMode( interpret( hbmQuery.getFlushMode() ) );
			query.setFetchSize( hbmQuery.getFetchSize() );
			query.setReadOnly( hbmQuery.isReadOnly() );
			query.setTimeout( hbmQuery.getTimeout() );

			// JaxbQueryElement#content elements can be either the query or parameters
			for ( Object content : hbmQuery.getContent() ) {
				if ( String.class.isInstance( content ) ) {
					query.setQuery( (String) content );
				}
				else {
					final JaxbQueryParamElement hbmQueryParam = (JaxbQueryParamElement) content;
					final JaxbQueryParamType queryParam = new JaxbQueryParamType();
					query.getQueryParam().add( queryParam );
					queryParam.setName( hbmQueryParam.getName() );
					queryParam.setType( hbmQueryParam.getType() );
				}
			}
		}
	}

	private JaxbCacheModeType convert(JaxbCacheModeAttribute cacheMode) {
		final String value = cacheMode == null ? null : cacheMode.value();
		if ( StringHelper.isEmpty( value ) ) {
			return JaxbCacheModeType.NORMAL;
		}

		return JaxbCacheModeType.fromValue( value );
	}

	private FlushMode interpret(JaxbFlushModeAttribute flushMode) {
		final String value = flushMode == null ? null : flushMode.value();
		if ( StringHelper.isEmpty( value ) ) {
			return null;
		}

		return FlushMode.valueOf( value );
	}

	private void transferNamedSqlQuery(JaxbHibernateMapping hbmXmlMapping, JaxbEntityMappings ormRoot) {
		if ( hbmXmlMapping.getSqlQuery().isEmpty() ) {
			return;
		}

		for ( JaxbSqlQueryElement hbmQuery : hbmXmlMapping.getSqlQuery() ) {
			final JaxbNamedNativeQuery query = new JaxbNamedNativeQuery();
			ormRoot.getNamedNativeQuery().add( query );
			query.setName( hbmQuery.getName() );
			query.setCacheable( hbmQuery.isCacheable() );
			query.setCacheMode( convert( hbmQuery.getCacheMode() ) );
			query.setCacheRegion( hbmQuery.getCacheRegion() );
			query.setComment( hbmQuery.getComment() );
			query.setFetchSize( hbmQuery.getFetchSize() );
			query.setFlushMode( interpret( hbmQuery.getFlushMode() ) );
			query.setFetchSize( hbmQuery.getFetchSize() );
			query.setReadOnly( hbmQuery.isReadOnly() );
			query.setTimeout( hbmQuery.getTimeout() );

			// JaxbQueryElement#content elements can be either the query or parameters
			for ( Object content : hbmQuery.getContent() ) {
				if ( String.class.isInstance( content ) ) {
					query.setQuery( (String) content );
				}
				else {
					final JaxbQueryParamElement hbmQueryParam = (JaxbQueryParamElement) content;
					final JaxbQueryParamType queryParam = new JaxbQueryParamType();
					query.getQueryParam().add( queryParam );
					queryParam.setName( hbmQueryParam.getName() );
					queryParam.setType( hbmQueryParam.getType() );
				}
			}
		}
	}

	private void transferDatabaseObjects(JaxbHibernateMapping hbmXmlMapping, JaxbEntityMappings ormRoot) {
		// todo : implement
	}

	private void transferEntities(JaxbHibernateMapping hbmXmlMapping, JaxbEntityMappings ormRoot) {
		// thoughts...
		//		1) We only need to transfer the "extends" attribute if the model is dynamic (map mode),
		//			otherwise it will be discovered via jandex
		//		2) ?? Have abstract hbm class mappings become MappedSuperclass mappings ??

		if ( !hbmXmlMapping.getClazz().isEmpty() ) {
			for ( JaxbClassElement hbmClass : hbmXmlMapping.getClazz() ) {
				final JaxbEntity entity = new JaxbEntity();
				ormRoot.getEntity().add( entity );
				transferEntity( hbmClass, entity );
			}
		}

		if ( !hbmXmlMapping.getSubclass().isEmpty() ) {
			for ( JaxbSubclassElement hbmSubclass : hbmXmlMapping.getSubclass() ) {
				final JaxbEntity entity = new JaxbEntity();
				ormRoot.getEntity().add( entity );

				transferDiscriminatorSubclass( hbmSubclass, entity );
			}
		}
//		<xs:element name="class" type="class-element"/>
//		<xs:element name="subclass" type="subclass-element"/>
//		<xs:element name="joined-subclass" type="joined-subclass-element"/>
//		<xs:element name="union-subclass" type="union-subclass-element"/>

	}

	private void transferEntity(JaxbClassElement hbmClass, JaxbEntity entity) {
		// todo : implement
	}

	private void transferDiscriminatorSubclass(JaxbSubclassElement hbmSubclass, JaxbEntity entity) {
		// todo : implement
	}

}
