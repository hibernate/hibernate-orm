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

import javax.persistence.FetchType;
import javax.xml.bind.JAXBElement;

import org.hibernate.FlushMode;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.jandex.MockHelper;
import org.hibernate.metamodel.source.internal.jaxb.JaxbAny;
import org.hibernate.metamodel.source.internal.jaxb.JaxbAttributes;
import org.hibernate.metamodel.source.internal.jaxb.JaxbBasic;
import org.hibernate.metamodel.source.internal.jaxb.JaxbCacheElement;
import org.hibernate.metamodel.source.internal.jaxb.JaxbCacheModeType;
import org.hibernate.metamodel.source.internal.jaxb.JaxbCascadeType;
import org.hibernate.metamodel.source.internal.jaxb.JaxbColumn;
import org.hibernate.metamodel.source.internal.jaxb.JaxbDiscriminatorColumn;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmbeddable;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmbeddableAttributes;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmbedded;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmbeddedId;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmptyType;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntity;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityMappings;
import org.hibernate.metamodel.source.internal.jaxb.JaxbForeignKey;
import org.hibernate.metamodel.source.internal.jaxb.JaxbGeneratedValue;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmCustomSql;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmCustomSqlCheckEnum;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmFetchProfile;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmFilterDef;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmIdGeneratorDef;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmLoader;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmMultiTenancy;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmParam;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmToolingHint;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmType;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmTypeDef;
import org.hibernate.metamodel.source.internal.jaxb.JaxbId;
import org.hibernate.metamodel.source.internal.jaxb.JaxbIdClass;
import org.hibernate.metamodel.source.internal.jaxb.JaxbJoinColumn;
import org.hibernate.metamodel.source.internal.jaxb.JaxbJoinTable;
import org.hibernate.metamodel.source.internal.jaxb.JaxbManyToOne;
import org.hibernate.metamodel.source.internal.jaxb.JaxbNamedNativeQuery;
import org.hibernate.metamodel.source.internal.jaxb.JaxbNamedQuery;
import org.hibernate.metamodel.source.internal.jaxb.JaxbNaturalId;
import org.hibernate.metamodel.source.internal.jaxb.JaxbOneToOne;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPersistenceUnitMetadata;
import org.hibernate.metamodel.source.internal.jaxb.JaxbQueryParamType;
import org.hibernate.metamodel.source.internal.jaxb.JaxbSynchronizeType;
import org.hibernate.metamodel.source.internal.jaxb.JaxbTable;
import org.hibernate.xml.spi.Origin;
import org.jboss.logging.Logger;

/**
 * Transforms a JAXB binding of a hbm.xml file into a unified orm.xml representation
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class HbmXmlTransformer {
	private static final Logger log = Logger.getLogger( HbmXmlTransformer.class );

	/**
	 * Singleton access
	 */
	public static final HbmXmlTransformer INSTANCE = new HbmXmlTransformer();

	private Origin origin;
	private JaxbEntityMappings ormRoot;

	public JaxbEntityMappings transform(JaxbHibernateMapping hbmXmlMapping, Origin origin) {
		this.origin = origin;

		ormRoot = new JaxbEntityMappings();
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
		ormRoot.setAttributeAccessor( hbmXmlMapping.getDefaultAccess() );
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
					JaxbFilterParamElement hbmFilterParam = ( (JAXBElement<JaxbFilterParamElement>) content ).getValue();
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
			ormRoot.getNamedQuery().add( convert( hbmQuery, hbmQuery.getName() ) );
		}
	}
	
	private JaxbNamedQuery convert(JaxbQueryElement hbmQuery, String name) {
		final JaxbNamedQuery query = new JaxbNamedQuery();
		query.setName( name );
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
		
		return query;
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
			ormRoot.getNamedNativeQuery().add( convert( hbmQuery, hbmQuery.getName() ) );
		}
	}
	
	private JaxbNamedNativeQuery convert(JaxbSqlQueryElement hbmQuery, String name) {
		final JaxbNamedNativeQuery query = new JaxbNamedNativeQuery();
		query.setName( name );
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
		
		return query;
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

		if ( !hbmXmlMapping.getJoinedSubclass().isEmpty() ) {
			for ( JaxbJoinedSubclassElement hbmSubclass : hbmXmlMapping.getJoinedSubclass() ) {
				final JaxbEntity entity = new JaxbEntity();
				ormRoot.getEntity().add( entity );

				transferJoinedSubclass( hbmSubclass, entity );
			}
		}

		if ( !hbmXmlMapping.getUnionSubclass().isEmpty() ) {
			for ( JaxbUnionSubclassElement hbmSubclass : hbmXmlMapping.getUnionSubclass() ) {
				final JaxbEntity entity = new JaxbEntity();
				ormRoot.getEntity().add( entity );

				transferUnionSubclass( hbmSubclass, entity );
			}
		}

//		<xs:element name="class" type="class-element"/>
//		<xs:element name="subclass" type="subclass-element"/>
//		<xs:element name="joined-subclass" type="joined-subclass-element"/>
//		<xs:element name="union-subclass" type="union-subclass-element"/>

	}

	private void transferEntity(JaxbClassElement hbmClass, JaxbEntity entity) {
		entity.setMetadataComplete( true );
		entity.setName( hbmClass.getEntityName() );
		entity.setClazz( hbmClass.getName() );
		entity.setAbstract( hbmClass.isAbstract() );
		entity.setMutable( hbmClass.isMutable() );
		entity.setLazy( hbmClass.isLazy() );
		entity.setProxy( hbmClass.getProxy() );

		entity.setBatchSize( hbmClass.getBatchSize() );

		entity.setTable( new JaxbTable() );
		entity.getTable().setCatalog( hbmClass.getCatalog() );
		entity.getTable().setSchema( hbmClass.getSchema() );
		entity.getTable().setName( hbmClass.getTable() );
		entity.getTable().setComment( hbmClass.getComment() );
		entity.getTable().setCheck( hbmClass.getCheck() );
		entity.setSubselect( hbmClass.getSubselect() );
		if ( !hbmClass.getSynchronize().isEmpty() ) {
			for ( JaxbSynchronizeElement hbmSync : hbmClass.getSynchronize() ) {
				final JaxbSynchronizeType sync = new JaxbSynchronizeType();
				sync.setTable( hbmSync.getTable() );
				entity.getSynchronize().add( sync );
			}
		}

		entity.setDynamicInsert( hbmClass.isDynamicInsert() );
		entity.setDynamicUpdate( hbmClass.isDynamicUpdate() );
		entity.setSelectBeforeUpdate( hbmClass.isSelectBeforeUpdate() );

		if ( hbmClass.getLoader() != null ) {
			entity.setLoader( new JaxbHbmLoader() );
			entity.getLoader().setQueryRef( hbmClass.getLoader().getQueryRef() );
		}
		if ( hbmClass.getSqlInsert() != null ) {
			entity.setSqlInsert( new JaxbHbmCustomSql() );
			entity.getSqlInsert().setValue( hbmClass.getSqlInsert().getValue() );
			entity.getSqlInsert().setCheck( convert( hbmClass.getSqlInsert().getCheck() ) );
			entity.getSqlInsert().setValue( hbmClass.getSqlInsert().getValue() );
		}
		if ( hbmClass.getSqlUpdate() != null ) {
			entity.setSqlUpdate( new JaxbHbmCustomSql() );
			entity.getSqlUpdate().setValue( hbmClass.getSqlUpdate().getValue() );
			entity.getSqlUpdate().setCheck( convert( hbmClass.getSqlUpdate().getCheck() ) );
			entity.getSqlUpdate().setValue( hbmClass.getSqlUpdate().getValue() );
		}
		if ( hbmClass.getSqlDelete() != null ) {
			entity.setSqlDelete( new JaxbHbmCustomSql() );
			entity.getSqlDelete().setValue( hbmClass.getSqlDelete().getValue() );
			entity.getSqlDelete().setCheck( convert( hbmClass.getSqlDelete().getCheck() ) );
			entity.getSqlDelete().setValue( hbmClass.getSqlDelete().getValue() );
		}
		entity.setRowid( hbmClass.getRowid() );
		entity.setWhere( hbmClass.getWhere() );

		entity.setPersister( hbmClass.getPersister() );
		if ( !hbmClass.getTuplizer().isEmpty() ) {
			if ( hbmClass.getTuplizer().size() > 1 ) {
				log.warn( "More than one entity-mode per entity not supported" );

			}
			final JaxbTuplizerElement tuplizerElement = hbmClass.getTuplizer().get( 0 );
			entity.setEntityMode( tuplizerElement.getEntityMode().value() );
			entity.setTuplizer( tuplizerElement.getClazz() );
		}

		entity.setOptimisticLock( hbmClass.getOptimisticLock().value() );
		if ( hbmClass.getOptimisticLock() == JaxbOptimisticLockAttribute.VERSION ) {
			// todo : transfer version/timestamp
			//final JaxbVersionElement hbmVersion = hbmClass.getVersion();
			//final JaxbTimestampElement hbmTimestamp = hbmClass.getTimestamp();

			// oddly the jpa xsd allows multiple <version/> elements :?
		}


		transferDiscriminator( entity, hbmClass );
		entity.setDiscriminatorValue( hbmClass.getDiscriminatorValue() );
		entity.setPolymorphism( hbmClass.getPolymorphism().value() );

		if ( hbmClass.getMultiTenancy() != null ) {
			entity.setMultiTenancy( new JaxbHbmMultiTenancy() );
			transferColumn(
					entity.getMultiTenancy().getColumn(),
					hbmClass.getMultiTenancy().getColumn(),
					null,
					true,
					true
			);
			entity.getMultiTenancy().setFormula( hbmClass.getMultiTenancy().getFormula() );
			entity.getMultiTenancy().setBindAsParam( hbmClass.getMultiTenancy().isBindAsParam() );
			entity.getMultiTenancy().setShared( hbmClass.getMultiTenancy().isShared() );
		}

		if ( hbmClass.getCache() != null ) {
			entity.setCache( new JaxbCacheElement() );
			entity.getCache().setRegion( hbmClass.getCache().getRegion() );
			entity.getCache().setUsage( hbmClass.getCache().getUsage().value() );
			entity.getCache().setInclude( hbmClass.getCache().getInclude().value() );
		}
		
		if (! hbmClass.getQuery().isEmpty() ) {
			for ( JaxbQueryElement hbmQuery : hbmClass.getQuery() ) {
				entity.getNamedQuery().add( convert( hbmQuery, entity.getName() + "." + hbmQuery.getName() ) );
			}
		}
		
		if (! hbmClass.getSqlQuery().isEmpty() ) {
			for ( JaxbSqlQueryElement hbmQuery : hbmClass.getSqlQuery() ) {
				entity.getNamedNativeQuery().add( convert( hbmQuery, entity.getName() + "." + hbmQuery.getName() ) );
			}
		}

		// todo : transfer filters
		// todo : transfer fetch-profiles

		transferAttributes( entity, hbmClass );
	}

	private JaxbHbmCustomSqlCheckEnum convert(JaxbCheckAttribute check) {
		if ( check == null ) {
			return null;
		}
		return JaxbHbmCustomSqlCheckEnum.valueOf( check.value() );
	}

	private void transferColumn(
			JaxbColumn column,
			JaxbColumnElement hbmColumn,
			String tableName,
			Boolean insertable,
			Boolean updateable) {
		column.setTable( tableName );
		column.setName( hbmColumn.getName() );
		column.setComment( hbmColumn.getComment() );
		column.setCheck( hbmColumn.getCheck() );
		column.setDefault( hbmColumn.getDefault() );
		column.setNullable( hbmColumn.isNotNull() == null ? null : !hbmColumn.isNotNull() );
		column.setColumnDefinition( hbmColumn.getSqlType() );
		column.setInsertable( insertable );
		column.setUpdatable( updateable );
		column.setLength( hbmColumn.getLength() );
		column.setPrecision( hbmColumn.getPrecision() );
		column.setScale( hbmColumn.getScale() );
		column.setRead( hbmColumn.getRead() );
		column.setWrite( hbmColumn.getWrite() );
		column.setUnique( hbmColumn.isUnique() );
	}

	private void transferDiscriminator(JaxbEntity entity, JaxbClassElement hbmClass) {
		if ( hbmClass.getDiscriminator() == null ) {
			return;
		}

		if ( StringHelper.isNotEmpty( hbmClass.getDiscriminator().getColumnAttribute() ) ) {
			entity.setDiscriminatorColumn( new JaxbDiscriminatorColumn() );
			entity.getDiscriminatorColumn().setName( hbmClass.getDiscriminator().getColumnAttribute() );
		}
		else if ( StringHelper.isEmpty( hbmClass.getDiscriminator().getFormulaAttribute() ) ) {
			entity.setDiscriminatorFormula( hbmClass.getDiscriminator().getFormulaAttribute() );
		}
		else if ( StringHelper.isEmpty( hbmClass.getDiscriminator().getFormula() ) ) {
			entity.setDiscriminatorFormula( hbmClass.getDiscriminator().getFormulaAttribute().trim() );
		}
		else {
			entity.setDiscriminatorColumn( new JaxbDiscriminatorColumn() );
			entity.getDiscriminatorColumn().setName( hbmClass.getDiscriminator().getColumn().getName() );
			entity.getDiscriminatorColumn().setColumnDefinition( hbmClass.getDiscriminator().getColumn().getSqlType() );
			entity.getDiscriminatorColumn().setLength( hbmClass.getDiscriminator().getColumn().getLength() );
			entity.getDiscriminatorColumn().setForceInSelect( hbmClass.getDiscriminator().isForce() );
		}
	}

	private void transferAttributes(JaxbEntity entity, JaxbClassElement hbmClass) {
		entity.setAttributes( new JaxbAttributes() );

		transferIdentifier( entity, hbmClass );
		transferBasicAttributes( entity, hbmClass );
		transferEmbeddedAttributes( entity, hbmClass );
		transferOneToOneAttributes( entity, hbmClass );
		transferManyToOneAttributes( entity, hbmClass );
		transferManyToManyAttributes( entity, hbmClass );
		transferAnyAttributes( entity, hbmClass );
		transferManyToAnyAttributes( entity, hbmClass );
		transferPrimitiveArrayAttributes( entity, hbmClass );
		transferPropertiesGrouping( entity, hbmClass );
		transferNaturalIdentifiers( entity, hbmClass );
	}

	private void transferIdentifier(JaxbEntity entity, JaxbClassElement hbmClass) {
		final JaxbIdElement hbmId = hbmClass.getId();
		if ( hbmId != null ) {
			// simple id
			final JaxbId id = new JaxbId();
			id.setName( hbmId.getName() );
			id.setAttributeAccessor( hbmId.getAccess() );
			
			if (hbmId.getGenerator() != null) {
				final JaxbGeneratedValue generator = new JaxbGeneratedValue();
				generator.setGenerator( hbmId.getGenerator().getClazz() );
				id.setGeneratedValue( generator );
			}
			
			if ( StringHelper.isNotEmpty( hbmId.getTypeAttribute() ) ) {
				id.setType( new JaxbHbmType() );
				id.getType().setName( hbmId.getTypeAttribute() );
			}
			else {
				if ( hbmId.getType() != null ) {
					id.setType( new JaxbHbmType() );
					id.getType().setName( hbmId.getType().getName() );
					if ( !hbmId.getType().getParam().isEmpty() ) {
						for ( JaxbParamElement hbmParam : hbmId.getType().getParam() ) {
							final JaxbHbmParam param = new JaxbHbmParam();
							param.setName( hbmParam.getName() );
							param.setValue( hbmParam.getValue() );
							id.getType().getParam().add( param );
						}
					}
				}
			}
			id.setUnsavedValue( hbmId.getUnsavedValue() );
			if ( StringHelper.isNotEmpty( hbmId.getColumnAttribute() ) ) {
				id.setColumn( new JaxbColumn() );
				id.getColumn().setName( hbmId.getColumnAttribute() );
			}
			else {
				if ( hbmId.column != null ) {
					assert hbmId.column.size() == 1;
					id.setColumn( new JaxbColumn() );
					transferColumn( id.getColumn(), hbmId.getColumn().get( 0 ), null, true, false );
				}
			}
			entity.getAttributes().getId().add( id );
			return;
		}

		final JaxbCompositeIdElement hbmCompositeId = hbmClass.getCompositeId();
		assert hbmCompositeId != null;

		// we have one of 2 forms of composite id...
		final boolean isAggregate;
		if ( StringHelper.isNotEmpty( hbmCompositeId.getClazz() ) ) {
			// we have  <composite-id class="XYZ">.
			if ( hbmCompositeId.isMapped() ) {
				// user explicitly said the class is an "IdClass"
				isAggregate = false;
			}
			else {
				isAggregate = true;
			}
		}
		else {
			// there was no class specified, can only be non-aggregated
			isAggregate = false;
		}

		if ( isAggregate ) {
			entity.getAttributes().setEmbeddedId( new JaxbEmbeddedId() );
			entity.getAttributes().getEmbeddedId().setName( hbmCompositeId.getName() );
			entity.getAttributes().getEmbeddedId().setAttributeAccessor( hbmCompositeId.getAccess() );

			final JaxbEmbeddable embeddable = new JaxbEmbeddable();
			embeddable.setClazz( hbmCompositeId.getClazz() );
			embeddable.setAttributes( new JaxbEmbeddableAttributes() );
			for ( Object hbmCompositeAttribute : hbmCompositeId.getKeyPropertyOrKeyManyToOne() ) {
				if ( JaxbKeyPropertyElement.class.isInstance( hbmCompositeAttribute ) ) {
					final JaxbKeyPropertyElement keyProp = (JaxbKeyPropertyElement) hbmCompositeAttribute;
					final JaxbBasic basic = new JaxbBasic();
					basic.setName( keyProp.getName() );
					basic.setAttributeAccessor( keyProp.getAccess() );
					if ( StringHelper.isNotEmpty( keyProp.getColumnAttribute() ) ) {
						final JaxbColumn column = new JaxbColumn();
						column.setName( keyProp.getColumnAttribute() );
						basic.getColumnOrFormula().add( column );
					}
					else {
						for ( JaxbColumnElement hbmColumn : keyProp.getColumn() ) {
							final JaxbColumn column = new JaxbColumn();
							transferColumn( column, hbmColumn, null, true, false );
							basic.getColumnOrFormula().add( column );
						}
					}
					embeddable.getAttributes().getBasic().add( basic );
				}
				else {
					final JaxbKeyManyToOneElement keyManyToOne = (JaxbKeyManyToOneElement) hbmCompositeAttribute;
					final JaxbManyToOne manyToOne = new JaxbManyToOne();
					manyToOne.setName( keyManyToOne.getName() );
					manyToOne.setAttributeAccessor( keyManyToOne.getAccess() );
					if ( StringHelper.isNotEmpty( keyManyToOne.getEntityName() ) ) {
						manyToOne.setTargetEntity( keyManyToOne.getEntityName() );
					}
					else {
						manyToOne.setTargetEntity( keyManyToOne.getClazz() );
					}
					// todo : cascade
					// TODO: should this check "proxy" instead?
					if ( keyManyToOne.getLazy() != null && "true".equals( keyManyToOne.getLazy().value() ) ) {
						manyToOne.setFetch( FetchType.LAZY );
					}
					manyToOne.setForeignKey( new JaxbForeignKey() );
					manyToOne.getForeignKey().setName( keyManyToOne.getForeignKey() );
					if ( StringHelper.isNotEmpty( keyManyToOne.getColumnAttribute() ) ) {
						final JaxbJoinColumn joinColumn = new JaxbJoinColumn();
						joinColumn.setName( keyManyToOne.getColumnAttribute() );
						manyToOne.getJoinColumn().add( joinColumn );
					}
					else {
						for ( JaxbColumnElement hbmColumn : keyManyToOne.getColumn() ) {
							final JaxbJoinColumn joinColumn = new JaxbJoinColumn();
							joinColumn.setName( hbmColumn.getName() );
							joinColumn.setNullable( !hbmColumn.isNotNull() );
							joinColumn.setUnique( hbmColumn.isUnique() );
							manyToOne.getJoinColumn().add( joinColumn );
						}
					}
					embeddable.getAttributes().getManyToOne().add( manyToOne );
				}
			}
			ormRoot.getEmbeddable().add( embeddable );
		}
		else {
			final JaxbIdClass idClass = new JaxbIdClass();
			idClass.setClazz( hbmCompositeId.getClazz() );
			entity.setIdClass( idClass );
			for ( Object hbmCompositeAttribute : hbmCompositeId.getKeyPropertyOrKeyManyToOne() ) {
				if ( JaxbKeyPropertyElement.class.isInstance( hbmCompositeAttribute ) ) {
					final JaxbKeyPropertyElement keyProp = (JaxbKeyPropertyElement) hbmCompositeAttribute;
					final JaxbId id = new JaxbId();
					id.setName( keyProp.getName() );
					id.setAttributeAccessor( keyProp.getAccess() );
					if ( StringHelper.isNotEmpty( keyProp.getColumnAttribute() ) ) {
						final JaxbColumn column = new JaxbColumn();
						column.setName( keyProp.getColumnAttribute() );
						id.setColumn( column );
					}
					else {
						if ( keyProp.column != null ) {
							assert keyProp.column.size() == 1;
							id.setColumn( new JaxbColumn() );
							transferColumn( id.getColumn(), keyProp.getColumn().get( 0 ), null, true, false );
						}
					}
					entity.getAttributes().getId().add( id );
				}
				else {
					final JaxbKeyManyToOneElement keyManyToOne = (JaxbKeyManyToOneElement) hbmCompositeAttribute;
					final JaxbManyToOne manyToOne = new JaxbManyToOne();
					manyToOne.setName( keyManyToOne.getName() );
					manyToOne.setId( true );
					manyToOne.setAttributeAccessor( keyManyToOne.getAccess() );
					if ( StringHelper.isNotEmpty( keyManyToOne.getEntityName() ) ) {
						manyToOne.setTargetEntity( keyManyToOne.getEntityName() );
					}
					else {
						manyToOne.setTargetEntity( keyManyToOne.getClazz() );
					}
					// todo : cascade
					// TODO: should this be "proxy", instead of "true"?
					if ( keyManyToOne.getLazy() != null && "true".equals( keyManyToOne.getLazy().value() ) ) {
						manyToOne.setFetch( FetchType.LAZY );
					}
					manyToOne.setForeignKey( new JaxbForeignKey() );
					manyToOne.getForeignKey().setName( keyManyToOne.getForeignKey() );
					if ( StringHelper.isNotEmpty( keyManyToOne.getColumnAttribute() ) ) {
						final JaxbJoinColumn joinColumn = new JaxbJoinColumn();
						joinColumn.setName( keyManyToOne.getColumnAttribute() );
						manyToOne.getJoinColumn().add( joinColumn );
					}
					else {
						for ( JaxbColumnElement hbmColumn : keyManyToOne.getColumn() ) {
							final JaxbJoinColumn joinColumn = new JaxbJoinColumn();
							joinColumn.setName( hbmColumn.getName() );
							joinColumn.setNullable( !hbmColumn.isNotNull() );
							joinColumn.setUnique( hbmColumn.isUnique() );
							manyToOne.getJoinColumn().add( joinColumn );
						}
					}
					entity.getAttributes().getManyToOne().add( manyToOne );
				}
			}
		}
	}

	private void transferBasicAttributes(JaxbEntity entity, JaxbClassElement hbmClass) {
		for ( JaxbPropertyElement hbmProp : hbmClass.getProperty() ) {
			entity.getAttributes().getBasic().add( transferBasicAttribute( hbmProp ) );
		}
	}

	private void transferNaturalIdentifiers(JaxbEntity entity, JaxbClassElement hbmClass) {
		if (hbmClass.getNaturalId() == null) {
			return;
		}
		
		JaxbNaturalId naturalId = new JaxbNaturalId();
		for ( JaxbPropertyElement hbmProp : hbmClass.getNaturalId().getProperty() ) {
			naturalId.getBasic().add( transferBasicAttribute( hbmProp ) );
		}
		for ( JaxbManyToOneElement hbmM2O : hbmClass.getNaturalId().getManyToOne() ) {
			naturalId.getManyToOne().add( transferManyToOneAttribute( hbmM2O ) );
		}
		for ( JaxbComponentElement hbmComponent : hbmClass.getNaturalId().getComponent() ) {
			naturalId.getEmbedded().add( transferEmbeddedAttribute( hbmComponent ) );
		}
		for ( JaxbAnyElement hbmAny : hbmClass.getNaturalId().getAny() ) {
			naturalId.getAny().add( transferAnyAttribute( hbmAny ) );
		}
		// TODO: hbmClass.getNaturalId().getDynamicComponent?
		naturalId.setMutable( hbmClass.getNaturalId().isMutable() );
		entity.getAttributes().setNaturalId( naturalId );
	}

	private JaxbBasic transferBasicAttribute(JaxbPropertyElement hbmProp) {
		final JaxbBasic basic = new JaxbBasic();
		basic.setName( hbmProp.getName() );
		basic.setOptional( hbmProp.isNotNull() == null ? true : !hbmProp.isNotNull() );
		basic.setFetch( FetchType.EAGER );
		basic.setAttributeAccessor( hbmProp.getAccess() );
		basic.setOptimisticLock( hbmProp.isOptimisticLock() );

		if ( StringHelper.isNotEmpty( hbmProp.getTypeAttribute() ) ) {
			basic.setType( new JaxbHbmType() );
			basic.getType().setName( hbmProp.getTypeAttribute() );
		}
		else {
			if ( hbmProp.getType() != null ) {
				basic.setType( new JaxbHbmType() );
				basic.getType().setName( hbmProp.getType().getName() );
				for ( JaxbParamElement hbmParam : hbmProp.getType().getParam() ) {
					final JaxbHbmParam param = new JaxbHbmParam();
					param.setName( hbmParam.getName() );
					param.setValue( hbmParam.getValue() );
					basic.getType().getParam().add( param );
				}
			}
		}
		
		// TODO: If hbmProp isUnique or notNull is set, bind the column?

		if ( StringHelper.isNotEmpty( hbmProp.getFormulaAttribute() ) ) {
			basic.getColumnOrFormula().add( hbmProp.getFormulaAttribute() );
		}
		else if ( StringHelper.isNotEmpty( hbmProp.getColumnAttribute() ) ) {
			final JaxbColumn column = new JaxbColumn();
			column.setName( hbmProp.getColumnAttribute() );
			basic.getColumnOrFormula().add( column );
		}
		else if ( !hbmProp.getFormula().isEmpty() ) {
			for ( String formula : hbmProp.getFormula() ) {
				basic.getColumnOrFormula().add( formula );
			}
		}
		else {
			for ( JaxbColumnElement hbmColumn : hbmProp.getColumn() ) {
				final JaxbColumn column = new JaxbColumn();
				transferColumn( column, hbmColumn, null, hbmProp.isInsert(), hbmProp.isUpdate() );
				basic.getColumnOrFormula().add( column );
			}
		}
		
		return basic;
	}

	private void transferEmbeddedAttributes(JaxbEntity entity, JaxbClassElement hbmClass) {
		for (JaxbComponentElement hbmComponent : hbmClass.getComponent()) {
			entity.getAttributes().getEmbedded().add( transferEmbeddedAttribute( hbmComponent ) );
			ormRoot.getEmbeddable().add( transferEmbeddable( hbmComponent ) );
		}
	}
	
	private JaxbEmbeddable transferEmbeddable(JaxbComponentElement hbmComponent) {
		final JaxbEmbeddable embeddable = new JaxbEmbeddable();
		embeddable.setClazz( hbmComponent.getClazz() );
		embeddable.setAttributes( new JaxbEmbeddableAttributes() );
		for (JaxbPropertyElement property : hbmComponent.getProperty()) {
			embeddable.getAttributes().getBasic().add( transferBasicAttribute( property ) );
		}
		for (JaxbManyToOneElement manyToOne : hbmComponent.getManyToOne()) {
			embeddable.getAttributes().getManyToOne().add( transferManyToOneAttribute( manyToOne ) );
		}
		for (JaxbOneToOneElement oneToOne : hbmComponent.getOneToOne()) {
			embeddable.getAttributes().getOneToOne().add( transferOneToOneAttribute( oneToOne ) );
		}
		for (JaxbComponentElement component : hbmComponent.getComponent()) {
			// TODO
		}
		return embeddable;
	}

	private JaxbEmbedded transferEmbeddedAttribute(JaxbComponentElement hbmComponent) {
		final JaxbEmbedded embedded = new JaxbEmbedded();
		embedded.setAttributeAccessor( hbmComponent.getAccess() );
		embedded.setName( hbmComponent.getName() );
		return embedded;
	}

	private void transferOneToOneAttributes(JaxbEntity entity, JaxbClassElement hbmClass) {
		for (JaxbOneToOneElement hbmO2O : hbmClass.getOneToOne()) {
			entity.getAttributes().getOneToOne().add( transferOneToOneAttribute( hbmO2O ) );
		}
	}

	private JaxbOneToOne transferOneToOneAttribute(JaxbOneToOneElement hbmO2O) {
		final JaxbOneToOne o2o = new JaxbOneToOne();
		o2o.setAttributeAccessor( hbmO2O.getAccess() );
		o2o.setCascade( convertCascadeType( hbmO2O.getCascade() ) );
		o2o.setFetch( convert( hbmO2O.getFetch() ) );
		o2o.setForeignKey( new JaxbForeignKey() );
		o2o.getForeignKey().setName( hbmO2O.getForeignKey() );
		if (! StringHelper.isEmpty( hbmO2O.getPropertyRef() )) {
			final JaxbJoinColumn joinColumn = new JaxbJoinColumn();
			joinColumn.setReferencedColumnName( hbmO2O.getPropertyRef() );
			o2o.getJoinColumn().add( joinColumn );
		}
		o2o.setName( hbmO2O.getName() );
		o2o.setTargetEntity( hbmO2O.getEntityName() );
		return o2o;
	}

	private void transferManyToOneAttributes(JaxbEntity entity, JaxbClassElement hbmClass) {
		for (JaxbManyToOneElement hbmM2O : hbmClass.getManyToOne()) {
			entity.getAttributes().getManyToOne().add( transferManyToOneAttribute( hbmM2O ) );
		}
	}

	private JaxbManyToOne transferManyToOneAttribute(JaxbManyToOneElement hbmM2O) {
		final JaxbManyToOne m2o = new JaxbManyToOne();
		m2o.setAttributeAccessor( hbmM2O.getAccess() );
		m2o.setCascade( convertCascadeType( hbmM2O.getCascade() ) );
		m2o.setFetch( convert( hbmM2O.getFetch() ) );
		m2o.setForeignKey( new JaxbForeignKey() );
		m2o.getForeignKey().setName( hbmM2O.getForeignKey() );
		final JaxbJoinColumn joinColumn = new JaxbJoinColumn();
		if (StringHelper.isEmpty( hbmM2O.getColumnAttribute() )) {
			// AbstractBasicBindingTests seems to imply this was the case
			joinColumn.setName( hbmM2O.getName() );
		}
		else {
			joinColumn.setName( hbmM2O.getColumnAttribute() );
		}
		if (! StringHelper.isEmpty( hbmM2O.getPropertyRef() )) {
			joinColumn.setReferencedColumnName( hbmM2O.getPropertyRef() );
		}
		m2o.getJoinColumn().add( joinColumn );
		m2o.setName( hbmM2O.getName() );
		m2o.setOptional( hbmM2O.isNotNull() == null ? true : !hbmM2O.isNotNull() );
		m2o.setTargetEntity( hbmM2O.getEntityName() );
		return m2o;
	}

	private void transferManyToManyAttributes(JaxbEntity entity, JaxbClassElement hbmClass) {

	}

	private void transferAnyAttributes(JaxbEntity entity, JaxbClassElement hbmClass) {

	}

	private JaxbAny transferAnyAttribute(JaxbAnyElement hbmAny) {
		// TODO
		return new JaxbAny();
	}

	private void transferManyToAnyAttributes(JaxbEntity entity, JaxbClassElement hbmClass) {

	}

	private void transferPrimitiveArrayAttributes(JaxbEntity entity, JaxbClassElement hbmClass) {
		if ( !hbmClass.getPrimitiveArray().isEmpty() ) {
			log.warnf(
					"Entity mapping [%s : %s] from hbm.xml [%s]  used <primitive-array/> construct " +
							"which is not supported in transformation; skipping",
					hbmClass.getName(),
					hbmClass.getEntityName(),
					origin
			);
		}
	}

	private void transferPropertiesGrouping(JaxbEntity entity, JaxbClassElement hbmClass) {
		if ( !hbmClass.getProperties().isEmpty() ) {
			log.warnf(
					"Entity mapping [%s : %s] from hbm.xml [%s]  used <properties/> construct " +
							"which is not supported in transformation; skipping",
					hbmClass.getName(),
					hbmClass.getEntityName(),
					origin
			);
		}
	}

	private void transferDiscriminatorSubclass(JaxbSubclassElement hbmSubclass, JaxbEntity entity) {
		// todo : implement
	}

	private void transferJoinedSubclass(JaxbJoinedSubclassElement hbmSubclass, JaxbEntity entity) {
		// todo : implement
	}

	private void transferUnionSubclass(JaxbUnionSubclassElement hbmSubclass, JaxbEntity entity) {
		// todo : implement
	}
	
	private JaxbCascadeType convertCascadeType(String s) {
		final JaxbCascadeType cascadeType = new JaxbCascadeType();
		
		if (! StringHelper.isEmpty( s )) {
			s = s.replaceAll( " ", "" );
			final String[] split = s.split( "," );
			for (String hbmCascade : split) {
				if (hbmCascade.equalsIgnoreCase( "all" )) {
					cascadeType.setCascadeAll( new JaxbEmptyType() );
				}
				else if (hbmCascade.equalsIgnoreCase( "persist" )) {
					cascadeType.setCascadePersist( new JaxbEmptyType() );
				}
				else if (hbmCascade.equalsIgnoreCase( "merge" )) {
					cascadeType.setCascadeMerge( new JaxbEmptyType() );
				}
				else if (hbmCascade.equalsIgnoreCase( "refresh" )) {
					cascadeType.setCascadeRefresh( new JaxbEmptyType() );
				}
				else if (hbmCascade.equalsIgnoreCase( "save-update" )) {
					// TODO
				}
				else if (hbmCascade.equalsIgnoreCase( "evict" )) {
					// TODO
				}
				else if (hbmCascade.equalsIgnoreCase( "replicate" )) {
					// TODO
				}
				else if (hbmCascade.equalsIgnoreCase( "lock" )) {
					// TODO
				}
			}
		}
		return cascadeType;
	}
	
	private FetchType convert(JaxbFetchStyleAttribute hbmFetch) {
		if (hbmFetch != null) {
			switch (hbmFetch) {
				case JOIN:
					return FetchType.EAGER;
				case SELECT:
					return FetchType.LAZY;
			}
		}
		// TODO: EAGER or LAZY?
		return FetchType.LAZY;
	}
}
