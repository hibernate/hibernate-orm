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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.FetchType;
import javax.persistence.TemporalType;
import javax.xml.bind.JAXBElement;

import org.hibernate.FlushMode;
import org.hibernate.MappingException;
import org.hibernate.annotations.FetchMode;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.source.internal.jaxb.CollectionAttribute;
import org.hibernate.metamodel.source.internal.jaxb.FetchableAttribute;
import org.hibernate.metamodel.source.internal.jaxb.JaxbAny;
import org.hibernate.metamodel.source.internal.jaxb.JaxbAttributes;
import org.hibernate.metamodel.source.internal.jaxb.JaxbBasic;
import org.hibernate.metamodel.source.internal.jaxb.JaxbCacheElement;
import org.hibernate.metamodel.source.internal.jaxb.JaxbCacheModeType;
import org.hibernate.metamodel.source.internal.jaxb.JaxbColumn;
import org.hibernate.metamodel.source.internal.jaxb.JaxbDiscriminatorColumn;
import org.hibernate.metamodel.source.internal.jaxb.JaxbElementCollection;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmbeddable;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmbeddableAttributes;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmbedded;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmbeddedId;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmptyType;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntity;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityMappings;
import org.hibernate.metamodel.source.internal.jaxb.JaxbForeignKey;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmCascadeType;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmCustomSql;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmCustomSqlCheckEnum;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmFetchProfile;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmFilter;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmFilterDef;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmIdGenerator;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmIdGeneratorDef;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmLoader;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmMultiTenancy;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmParam;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmToolingHint;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmType;
import org.hibernate.metamodel.source.internal.jaxb.JaxbHbmTypeDef;
import org.hibernate.metamodel.source.internal.jaxb.JaxbId;
import org.hibernate.metamodel.source.internal.jaxb.JaxbIdClass;
import org.hibernate.metamodel.source.internal.jaxb.JaxbInheritance;
import org.hibernate.metamodel.source.internal.jaxb.JaxbInheritanceType;
import org.hibernate.metamodel.source.internal.jaxb.JaxbJoinColumn;
import org.hibernate.metamodel.source.internal.jaxb.JaxbJoinTable;
import org.hibernate.metamodel.source.internal.jaxb.JaxbManyToMany;
import org.hibernate.metamodel.source.internal.jaxb.JaxbManyToOne;
import org.hibernate.metamodel.source.internal.jaxb.JaxbMapKeyColumn;
import org.hibernate.metamodel.source.internal.jaxb.JaxbNamedNativeQuery;
import org.hibernate.metamodel.source.internal.jaxb.JaxbNamedQuery;
import org.hibernate.metamodel.source.internal.jaxb.JaxbNaturalId;
import org.hibernate.metamodel.source.internal.jaxb.JaxbOnDeleteType;
import org.hibernate.metamodel.source.internal.jaxb.JaxbOneToMany;
import org.hibernate.metamodel.source.internal.jaxb.JaxbOneToOne;
import org.hibernate.metamodel.source.internal.jaxb.JaxbOrderColumn;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPersistenceUnitMetadata;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPrimaryKeyJoinColumn;
import org.hibernate.metamodel.source.internal.jaxb.JaxbQueryParamType;
import org.hibernate.metamodel.source.internal.jaxb.JaxbSecondaryTable;
import org.hibernate.metamodel.source.internal.jaxb.JaxbSqlResultSetMapping;
import org.hibernate.metamodel.source.internal.jaxb.JaxbSqlResultSetMappingEntityResult;
import org.hibernate.metamodel.source.internal.jaxb.JaxbSqlResultSetMappingFieldResult;
import org.hibernate.metamodel.source.internal.jaxb.JaxbSynchronizeType;
import org.hibernate.metamodel.source.internal.jaxb.JaxbTable;
import org.hibernate.metamodel.source.internal.jaxb.JaxbVersion;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbReturnPropertyElement.JaxbReturnColumn;
import org.hibernate.metamodel.spi.ClassLoaderAccess;
import org.hibernate.xml.spi.Origin;

/**
 * Transforms a JAXB binding of a hbm.xml file into a unified orm.xml representation
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class HbmXmlTransformer {
	/**
	 * Singleton access
	 */
	public static final HbmXmlTransformer INSTANCE = new HbmXmlTransformer();

	private JaxbHibernateMapping hbmXmlMapping;
	private Origin origin;
	private ClassLoaderAccess classLoaderAccess;
	
	private JaxbEntityMappings ormRoot;

	public JaxbEntityMappings transform(JaxbHibernateMapping hbmXmlMapping, Origin origin,
			ClassLoaderAccess classLoaderAccess) {
		this.hbmXmlMapping = hbmXmlMapping;
		this.origin = origin;
		this.classLoaderAccess = classLoaderAccess;

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

		ormRoot.setPackage( hbmXmlMapping.getPackage() );
		ormRoot.setSchema( hbmXmlMapping.getSchema() );
		ormRoot.setCatalog( hbmXmlMapping.getCatalog() );
		ormRoot.setAttributeAccessor( hbmXmlMapping.getDefaultAccess() );
		ormRoot.setDefaultCascade( hbmXmlMapping.getDefaultCascade() );
		ormRoot.setAutoImport( hbmXmlMapping.isAutoImport() );
		ormRoot.setDefaultLazy( hbmXmlMapping.isDefaultLazy() );

		transferToolingHints();
		transferTypeDefs();
		transferIdentifierGenerators();
		transferFilterDefs();
		transferFetchProfiles();
		transferImports();
		transferResultSetMappings();
		transferNamedQueries();
		transferNamedNativeQueries();
		transferDatabaseObjects();
		transferEntities();

		return ormRoot;
	}

	private void transferToolingHints() {
		for ( JaxbMetaElement hbmMetaElement : hbmXmlMapping.getMeta() ) {
			final JaxbHbmToolingHint toolingHint = new JaxbHbmToolingHint();
			ormRoot.getToolingHint().add( toolingHint );
			toolingHint.setName( hbmMetaElement.getName() );
			toolingHint.setInheritable( hbmMetaElement.isInheritable() );
			toolingHint.setValue( hbmMetaElement.getValue() );
		}
	}

	private void transferTypeDefs() {
		for ( JaxbTypedefElement hbmXmlTypeDef : hbmXmlMapping.getTypedef() ) {
			final JaxbHbmTypeDef typeDef = new JaxbHbmTypeDef();
			ormRoot.getTypeDef().add( typeDef );
			typeDef.setName( hbmXmlTypeDef.getName() );
			typeDef.setClazz( hbmXmlTypeDef.getClazz() );

			for ( JaxbParamElement hbmParam : hbmXmlTypeDef.getParam() ) {
				final JaxbHbmParam param = new JaxbHbmParam();
				typeDef.getParam().add( param );
				param.setName( hbmParam.getName() );
				param.setValue( hbmParam.getValue() );
			}
		}
	}

	private void transferIdentifierGenerators() {
		for ( JaxbIdentifierGeneratorElement hbmGenerator : hbmXmlMapping.getIdentifierGenerator() ) {
			final JaxbHbmIdGeneratorDef generatorDef = new JaxbHbmIdGeneratorDef();
			ormRoot.getIdentifierGeneratorDef().add( generatorDef );
			generatorDef.setName( hbmGenerator.getName() );
			generatorDef.setClazz( hbmGenerator.getClazz() );
		}
	}

	private void transferFilterDefs() {
		for ( JaxbFilterDefElement hbmFilterDef : hbmXmlMapping.getFilterDef() ) {
			JaxbHbmFilterDef filterDef = new JaxbHbmFilterDef();
			ormRoot.getFilterDef().add( filterDef );
			filterDef.setName( hbmFilterDef.getName() );

			boolean foundCondition = false;
			for ( Object content : hbmFilterDef.getContent() ) {
				if ( String.class.isInstance( content ) ) {
					String condition = (String) content;
					condition = condition.trim();
					if (! StringHelper.isEmpty( condition )) {
						foundCondition = true;
						filterDef.setCondition( condition );
					}
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

	private void transferImports() {
		for ( JaxbImportElement hbmImport : hbmXmlMapping.getImport() ) {
			final JaxbEntityMappings.JaxbImport ormImport = new JaxbEntityMappings.JaxbImport();
			ormRoot.getImport().add( ormImport );
			ormImport.setClazz( hbmImport.getClazz() );
			ormImport.setRename( hbmImport.getRename() );
		}
	}

	private void transferResultSetMappings() {
		for ( JaxbResultsetElement hbmResultSet : hbmXmlMapping.getResultset() ) {
			final JaxbSqlResultSetMapping mapping = new JaxbSqlResultSetMapping();
			mapping.setName( hbmResultSet.getName() );
			
			for (JaxbReturnElement hbmReturn : hbmResultSet.getReturn()) {
				mapping.getEntityResult().add( transferReturnElement( hbmReturn ) );
			}
			// TODO: return-scalar
			
			ormRoot.getSqlResultSetMapping().add( mapping );
		}
	}
	
	private JaxbSqlResultSetMappingEntityResult transferReturnElement(JaxbReturnElement hbmReturn) {
		
		final JaxbSqlResultSetMappingEntityResult entityResult = new JaxbSqlResultSetMappingEntityResult();
		entityResult.setEntityClass( getFqClassName( hbmReturn.getClazz() ) );
		for (JaxbReturnPropertyElement returnProperty : hbmReturn.getReturnProperty()) {
			final JaxbSqlResultSetMappingFieldResult field = new JaxbSqlResultSetMappingFieldResult();
			final List<String> columns = new ArrayList<String>();
			if (! StringHelper.isEmpty( returnProperty.getColumn() )) {
				columns.add( returnProperty.getColumn() );
			}
			for (JaxbReturnColumn returnColumn : returnProperty.getReturnColumn()) {
				columns.add( returnColumn.getName() );
			}
			if (columns.size() > 1) {
				throw new MappingException( "HBM transformation: More than one column per <return-property> not supported." );
			}
			field.setColumn( columns.get( 0 ) );
			field.setName( returnProperty.getName() );
			entityResult.getFieldResult().add( field );
		}
		return entityResult;
	}

	private void transferFetchProfiles() {
		for ( JaxbFetchProfileElement hbmFetchProfile : hbmXmlMapping.getFetchProfile() ) {
			ormRoot.getFetchProfile().add( transferFetchProfile( hbmFetchProfile ) );
		}
	}

	private JaxbHbmFetchProfile transferFetchProfile(JaxbFetchProfileElement hbmFetchProfile) {
		final JaxbHbmFetchProfile fetchProfile = new JaxbHbmFetchProfile();
		fetchProfile.setName( hbmFetchProfile.getName() );
		for ( JaxbFetchProfileElement.JaxbFetch hbmFetch : hbmFetchProfile.getFetch() ) {
			final JaxbHbmFetchProfile.JaxbFetch fetch = new JaxbHbmFetchProfile.JaxbFetch();
			fetchProfile.getFetch().add( fetch );
			fetch.setEntity( hbmFetch.getEntity() );
			fetch.setAssociation( hbmFetch.getAssociation() );
			fetch.setStyle( hbmFetch.getStyle().value() );
		}
		return fetchProfile;
	}

	private void transferNamedQueries() {
		for ( JaxbQueryElement hbmQuery : hbmXmlMapping.getQuery() ) {
			ormRoot.getNamedQuery().add( transferNamedQuery( hbmQuery, hbmQuery.getName() ) );
		}
	}
	
	private JaxbNamedQuery transferNamedQuery(JaxbQueryElement hbmQuery, String name) {
		final JaxbNamedQuery query = new JaxbNamedQuery();
		query.setName( name );
		query.setCacheable( hbmQuery.isCacheable() );
		query.setCacheMode( convert( hbmQuery.getCacheMode() ) );
		query.setCacheRegion( hbmQuery.getCacheRegion() );
		query.setComment( hbmQuery.getComment() );
		query.setFetchSize( hbmQuery.getFetchSize() );
		query.setFlushMode( convert( hbmQuery.getFlushMode() ) );
		query.setFetchSize( hbmQuery.getFetchSize() );
		query.setReadOnly( hbmQuery.isReadOnly() );
		query.setTimeout( hbmQuery.getTimeout() );

		// JaxbQueryElement#content elements can be either the query or parameters
		for ( Object content : hbmQuery.getContent() ) {
			if ( String.class.isInstance( content ) ) {
				String s = (String) content;
				s = s.trim();
				query.setQuery( s );
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

	private void transferNamedNativeQueries() {
		for ( JaxbSqlQueryElement hbmQuery : hbmXmlMapping.getSqlQuery() ) {
			ormRoot.getNamedNativeQuery().add( transferNamedNativeQuery( hbmQuery, hbmQuery.getName() ) );
		}
	}
	
	private JaxbNamedNativeQuery transferNamedNativeQuery(JaxbSqlQueryElement hbmQuery, String name) {
		final JaxbNamedNativeQuery query = new JaxbNamedNativeQuery();
		query.setName( name );
		query.setCacheable( hbmQuery.isCacheable() );
		query.setCacheMode( convert( hbmQuery.getCacheMode() ) );
		query.setCacheRegion( hbmQuery.getCacheRegion() );
		query.setComment( hbmQuery.getComment() );
		query.setFetchSize( hbmQuery.getFetchSize() );
		query.setFlushMode( convert( hbmQuery.getFlushMode() ) );
		query.setFetchSize( hbmQuery.getFetchSize() );
		query.setReadOnly( hbmQuery.isReadOnly() );
		query.setTimeout( hbmQuery.getTimeout() );

		// JaxbQueryElement#content elements can be either the query or parameters
		for ( Object content : hbmQuery.getContent() ) {
			if ( String.class.isInstance( content ) ) {
				String s = (String) content;
				s = s.trim();
				query.setQuery( s );
			}
			else if (content instanceof JAXBElement) {
				final JAXBElement element = (JAXBElement) content;
				if (element.getValue() instanceof JaxbQueryParamType) {
					final JaxbQueryParamElement hbmQueryParam = (JaxbQueryParamElement) element.getValue();
					final JaxbQueryParamType queryParam = new JaxbQueryParamType();
					query.getQueryParam().add( queryParam );
					queryParam.setName( hbmQueryParam.getName() );
					queryParam.setType( hbmQueryParam.getType() );
				}
				else if (element.getValue() instanceof JaxbReturnElement) {
					final JaxbSqlResultSetMapping mapping = new JaxbSqlResultSetMapping();
					// TODO: correct to use the query name here?
					mapping.setName( name );
					mapping.getEntityResult().add( transferReturnElement( (JaxbReturnElement) element.getValue() ) );
					ormRoot.getSqlResultSetMapping().add( mapping );
				}
				// TODO: return-scalar possible here?
			}
		}
		
		return query;
	}

	private void transferDatabaseObjects() {
		// todo : implement
	}

	private void transferEntities() {
		// thoughts...
		//		1) We only need to transfer the "extends" attribute if the model is dynamic (map mode),
		//			otherwise it will be discovered via jandex
		//		2) ?? Have abstract hbm class mappings become MappedSuperclass mappings ??

		for ( JaxbClassElement hbmClass : hbmXmlMapping.getClazz() ) {
			final JaxbEntity entity = new JaxbEntity();
			ormRoot.getEntity().add( entity );
			transferEntity( hbmClass, entity );
		}

		for ( JaxbSubclassElement hbmSubclass : hbmXmlMapping.getSubclass() ) {
			final JaxbEntity entity = new JaxbEntity();
			ormRoot.getEntity().add( entity );

			transferDiscriminatorSubclass( hbmSubclass, entity );
		}

		for ( JaxbJoinedSubclassElement hbmSubclass : hbmXmlMapping.getJoinedSubclass() ) {
			final JaxbEntity entity = new JaxbEntity();
			ormRoot.getEntity().add( entity );

			transferJoinedSubclass( hbmSubclass, entity );
		}

		for ( JaxbUnionSubclassElement hbmSubclass : hbmXmlMapping.getUnionSubclass() ) {
			final JaxbEntity entity = new JaxbEntity();
			ormRoot.getEntity().add( entity );

			transferUnionSubclass( hbmSubclass, entity );
		}

	}

	private void transferEntity(JaxbClassElement hbmClass, JaxbEntity entity) {
		transferEntityElement( hbmClass, entity );
		
		entity.setMutable( hbmClass.isMutable() );

		entity.setTable( new JaxbTable() );
		entity.getTable().setCatalog( hbmClass.getCatalog() );
		entity.getTable().setSchema( hbmClass.getSchema() );
		entity.getTable().setName( hbmClass.getTable() );
		entity.getTable().setComment( hbmClass.getComment() );
		entity.getTable().setCheck( hbmClass.getCheck() );
		entity.setSubselect( hbmClass.getSubselect() );
		
		for ( JaxbSynchronizeElement hbmSync : hbmClass.getSynchronize() ) {
			final JaxbSynchronizeType sync = new JaxbSynchronizeType();
			sync.setTable( hbmSync.getTable() );
			entity.getSynchronize().add( sync );
		}

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

		if ( !hbmClass.getTuplizer().isEmpty() ) {
			if ( hbmClass.getTuplizer().size() > 1 ) {
				throw new MappingException( "HBM transformation: More than one entity-mode per entity not supported" );
			}
			final JaxbTuplizerElement tuplizerElement = hbmClass.getTuplizer().get( 0 );
			entity.setEntityMode( tuplizerElement.getEntityMode().value() );
			entity.setTuplizer( tuplizerElement.getClazz() );
		}

		entity.setOptimisticLock( hbmClass.getOptimisticLock().value() );

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
		
		for ( JaxbQueryElement hbmQuery : hbmClass.getQuery() ) {
			entity.getNamedQuery().add( transferNamedQuery( hbmQuery, entity.getName() + "." + hbmQuery.getName() ) );
		}
		
		for ( JaxbSqlQueryElement hbmQuery : hbmClass.getSqlQuery() ) {
			entity.getNamedNativeQuery().add( transferNamedNativeQuery( hbmQuery, entity.getName() + "." + hbmQuery.getName() ) );
		}
		
		for (JaxbFilterElement hbmFilter : hbmClass.getFilter()) {
			entity.getFilter().add( convert( hbmFilter ) );
		}
		
		for (JaxbFetchProfileElement hbmFetchProfile : hbmClass.getFetchProfile()) {
			entity.getFetchProfile().add( transferFetchProfile( hbmFetchProfile ) );
		}

		transferAttributes( entity, hbmClass );
		
		for (JaxbJoinedSubclassElement hbmSubclass : hbmClass.getJoinedSubclass()) {
			entity.setInheritance( new JaxbInheritance() );
			entity.getInheritance().setStrategy( JaxbInheritanceType.JOINED );
			
			final JaxbEntity subclassEntity = new JaxbEntity();
			ormRoot.getEntity().add( subclassEntity );
			transferJoinedSubclass( hbmSubclass, subclassEntity );
		}
		
		for (JaxbUnionSubclassElement hbmSubclass : hbmClass.getUnionSubclass()) {
			entity.setInheritance( new JaxbInheritance() );
			entity.getInheritance().setStrategy( JaxbInheritanceType.UNION_SUBCLASS );
			
			final JaxbEntity subclassEntity = new JaxbEntity();
			ormRoot.getEntity().add( subclassEntity );
			transferUnionSubclass( hbmSubclass, subclassEntity );
		}
		
		for (JaxbSubclassElement hbmSubclass : hbmClass.getSubclass()) {
			final JaxbEntity subclassEntity = new JaxbEntity();
			ormRoot.getEntity().add( subclassEntity );
			transferDiscriminatorSubclass( hbmSubclass, subclassEntity );
		}
		
		for ( JaxbQueryElement hbmQuery : hbmClass.getQuery() ) {
			// Tests implied this was the case...
			final String name = hbmClass.getName() + "." + hbmQuery.getName();
			ormRoot.getNamedQuery().add( transferNamedQuery( hbmQuery, name ) );
		}
		
		for ( JaxbSqlQueryElement hbmQuery : hbmClass.getSqlQuery() ) {
			// Tests implied this was the case...
			final String name = hbmClass.getName() + "." + hbmQuery.getName();
			ormRoot.getNamedNativeQuery().add( transferNamedNativeQuery( hbmQuery, name ) );
		}
	}

	private void transferEntityElement(JaxbEntityElement hbmClass, JaxbEntity entity) {
		entity.setMetadataComplete( true );
		entity.setName( hbmClass.getEntityName() );
		entity.setClazz( hbmClass.getName() );
		entity.setAbstract( hbmClass.isAbstract() );
		entity.setLazy( hbmClass.isLazy() );
		entity.setProxy( hbmClass.getProxy() );

		entity.setBatchSize( hbmClass.getBatchSize() );

		entity.setDynamicInsert( hbmClass.isDynamicInsert() );
		entity.setDynamicUpdate( hbmClass.isDynamicUpdate() );
		entity.setSelectBeforeUpdate( hbmClass.isSelectBeforeUpdate() );

		entity.setPersister( hbmClass.getPersister() );
	}

	private void transferDiscriminatorSubclass(JaxbSubclassElement hbmSubclass, JaxbEntity subclassEntity) {
		transferEntityElement( hbmSubclass, subclassEntity );
		if (! StringHelper.isEmpty( hbmSubclass.getDiscriminatorValue() )) {
			subclassEntity.setDiscriminatorValue( hbmSubclass.getDiscriminatorValue() );
		}
		transferEntityElementAttributes( subclassEntity, hbmSubclass );
	}

	private void transferJoinedSubclass(JaxbJoinedSubclassElement hbmSubclass, JaxbEntity subclassEntity) {
		transferEntityElement( hbmSubclass, subclassEntity );
		transferEntityElementAttributes( subclassEntity, hbmSubclass );
		
		subclassEntity.setTable( new JaxbTable() );
		subclassEntity.getTable().setCatalog( hbmSubclass.getCatalog() );
		subclassEntity.getTable().setSchema( hbmSubclass.getSchema() );
		subclassEntity.getTable().setName( hbmSubclass.getTable() );
		subclassEntity.getTable().setComment( hbmSubclass.getComment() );
		subclassEntity.getTable().setCheck( hbmSubclass.getCheck() );
		
		if (hbmSubclass.getKey() != null) {
			final JaxbPrimaryKeyJoinColumn joinColumn = new JaxbPrimaryKeyJoinColumn();
			// TODO: multiple columns?
			joinColumn.setName( hbmSubclass.getKey().getColumnAttribute() );
			subclassEntity.getPrimaryKeyJoinColumn().add( joinColumn );
		}
		
		if (! hbmSubclass.getJoinedSubclass().isEmpty()) {
			subclassEntity.setInheritance( new JaxbInheritance() );
			subclassEntity.getInheritance().setStrategy( JaxbInheritanceType.JOINED );
			for (JaxbJoinedSubclassElement nestedHbmSubclass : hbmSubclass.getJoinedSubclass()) {
				final JaxbEntity nestedSubclassEntity = new JaxbEntity();
				ormRoot.getEntity().add( nestedSubclassEntity );
				transferJoinedSubclass( nestedHbmSubclass, nestedSubclassEntity );
			}
		}
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
		transferEntityElementAttributes( entity, hbmClass );

		transferIdentifier( entity, hbmClass );
		transferNaturalIdentifiers( entity, hbmClass );
		transferVersion( entity, hbmClass );
		transferTimestamp( entity, hbmClass );
		transferJoins( entity, hbmClass );
	}

	private void transferEntityElementAttributes(JaxbEntity entity, EntityElement hbmClass) {
		entity.setAttributes( new JaxbAttributes() );

		transferBasicAttributes( entity, hbmClass );
		transferEmbeddedAttributes( entity, hbmClass );
		transferOneToOneAttributes( entity, hbmClass );
		transferManyToOneAttributes( entity, hbmClass );
		transferAnyAttributes( entity, hbmClass );
		transferManyToAnyAttributes( entity, hbmClass );
		transferPrimitiveArrayAttributes( entity, hbmClass );
		transferPropertiesGrouping( entity, hbmClass );
		transferCollectionAttributes( entity, hbmClass );
	}

	private void transferIdentifier(JaxbEntity entity, JaxbClassElement hbmClass) {
		final JaxbIdElement hbmId = hbmClass.getId();
		if ( hbmId != null ) {
			// simple id
			final JaxbId id = new JaxbId();
			id.setName( hbmId.getName() );
			id.setAttributeAccessor( hbmId.getAccess() );
			
			if (hbmId.getGenerator() != null) {
				final JaxbHbmIdGenerator generator = new JaxbHbmIdGenerator();
				generator.setStrategy( hbmId.getGenerator().getClazz() );
				for (JaxbParamElement param : hbmId.getGenerator().getParam()) {
					JaxbHbmParam hbmParam = new JaxbHbmParam();
					hbmParam.setName( param.getName() );
					hbmParam.setValue( param.getValue() );
					generator.getParam().add( hbmParam );
				}
				id.setGenerator( generator );
			}
			
			if ( StringHelper.isNotEmpty( hbmId.getTypeAttribute() ) ) {
				id.setType( new JaxbHbmType() );
				id.getType().setName( hbmId.getTypeAttribute() );
			}
			else {
				if ( hbmId.getType() != null ) {
					id.setType( new JaxbHbmType() );
					id.getType().setName( hbmId.getType().getName() );
					for ( JaxbParamElement hbmParam : hbmId.getType().getParam() ) {
						final JaxbHbmParam param = new JaxbHbmParam();
						param.setName( hbmParam.getName() );
						param.setValue( hbmParam.getValue() );
						id.getType().getParam().add( param );
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
					final JaxbManyToOne manyToOne = transferManyToOneAttribute( keyManyToOne );
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
					final JaxbManyToOne manyToOne = transferManyToOneAttribute( keyManyToOne );
					entity.getAttributes().getManyToOne().add( manyToOne );
				}
			}
		}
	}

	private void transferBasicAttributes(JaxbEntity entity, EntityElement hbmClass) {
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
	
	private void transferVersion(JaxbEntity entity, JaxbClassElement hbmClass) {
//		if ( hbmClass.getOptimisticLock() == JaxbOptimisticLockAttribute.VERSION ) {
		final JaxbVersionElement hbmVersion = hbmClass.getVersion();
		if (hbmVersion != null) {
			final JaxbVersion version = new JaxbVersion();
			version.setName( hbmVersion.getName() );
			// TODO: multiple columns?
			if (! StringHelper.isEmpty( hbmVersion.getColumnAttribute() )) {
				version.setColumn( new JaxbColumn() );
				version.getColumn().setName( hbmVersion.getColumnAttribute() );
			}
			entity.getAttributes().getVersion().add( version );
		}
//		}
	}
	
	private void transferTimestamp(JaxbEntity entity, JaxbClassElement hbmClass) {
		final JaxbTimestampElement hbmTimestamp = hbmClass.getTimestamp();
		if (hbmTimestamp != null) {
			final JaxbVersion version = new JaxbVersion();
			version.setName( hbmTimestamp.getName() );
			// TODO: multiple columns?
			if (! StringHelper.isEmpty( hbmTimestamp.getColumnAttribute() )) {
				version.setColumn( new JaxbColumn() );
				version.getColumn().setName( hbmTimestamp.getColumnAttribute() );
			}
			version.setTemporal( TemporalType.TIMESTAMP );
			entity.getAttributes().getVersion().add( version );
		}
	}
	
	private void transferJoins(JaxbEntity entity, JaxbClassElement hbmClass) {
		for ( JaxbJoinElement hbmJoin : hbmClass.getJoin() ) {
			if (! hbmJoin.isInverse()) {
				final JaxbSecondaryTable secondaryTable = new JaxbSecondaryTable();
				secondaryTable.setCatalog( hbmJoin.getCatalog() );
				secondaryTable.setComment( hbmJoin.getComment() );
				secondaryTable.setName( hbmJoin.getTable() );
				secondaryTable.setSchema( hbmJoin.getSchema() );
				secondaryTable.setOptional( hbmJoin.isOptional() );
				if (hbmJoin.getKey() != null) {
					final JaxbPrimaryKeyJoinColumn joinColumn = new JaxbPrimaryKeyJoinColumn();
					// TODO: multiple columns?
					joinColumn.setName( hbmJoin.getKey().getColumnAttribute() );
					secondaryTable.getPrimaryKeyJoinColumn().add( joinColumn );
				}
				entity.getSecondaryTable().add( secondaryTable );
			}
			
			for (JaxbPropertyElement hbmProp : hbmJoin.getProperty()) {
				final JaxbBasic prop = transferBasicAttribute( hbmProp );
				for (Serializable columnOrFormula : prop.getColumnOrFormula()) {
					if (columnOrFormula instanceof JaxbColumn) {
						((JaxbColumn) columnOrFormula).setTable( hbmJoin.getTable() );
					}
				}
				entity.getAttributes().getBasic().add( prop );
			}
			for (JaxbComponentElement hbmComp : hbmJoin.getComponent()) {
				// TODO: Not sure how to handle this.  Attribute overrides?
				throw new MappingException( "HBM transformation: <component> within a <join> not yet supported." );
			}
			for (JaxbManyToOneElement hbmM2O : hbmJoin.getManyToOne()) {
				final JaxbManyToOne m2o = transferManyToOneAttribute( hbmM2O );
				final JaxbJoinTable joinTable = new JaxbJoinTable();
				joinTable.setCatalog( hbmJoin.getCatalog() );
				joinTable.setName( hbmJoin.getTable() );
				joinTable.setSchema( hbmJoin.getSchema() );
				m2o.setJoinTable( joinTable );
				entity.getAttributes().getManyToOne().add( m2o );
			}
		}
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
		
		if ( StringHelper.isNotEmpty( hbmProp.getFormulaAttribute() ) ) {
			basic.getColumnOrFormula().add( hbmProp.getFormulaAttribute() );
		}
		else if (! hbmProp.getColumn().isEmpty()) {
			for ( JaxbColumnElement hbmColumn : hbmProp.getColumn() ) {
				final JaxbColumn column = new JaxbColumn();
				transferColumn( column, hbmColumn, null, hbmProp.isInsert(), hbmProp.isUpdate() );
				basic.getColumnOrFormula().add( column );
			}
		}
		else if ( !hbmProp.getFormula().isEmpty() ) {
			for ( String formula : hbmProp.getFormula() ) {
				basic.getColumnOrFormula().add( formula );
			}
		}
		else {
			final JaxbColumn column = new JaxbColumn();
			column.setName( hbmProp.getColumnAttribute() );
			column.setLength( hbmProp.getLength() );
			column.setNullable( hbmProp.isNotNull() == null ? null : !hbmProp.isNotNull() );
			column.setUnique( hbmProp.isUnique() );
			column.setInsertable( hbmProp.isInsert() );
			column.setUpdatable( hbmProp.isUpdate() );
			basic.getColumnOrFormula().add( column );
		}
		
		return basic;
	}

	private void transferEmbeddedAttributes(JaxbEntity entity, EntityElement hbmClass) {
		for (JaxbComponentElement hbmComponent : hbmClass.getComponent()) {
			entity.getAttributes().getEmbedded().add( transferEmbeddedAttribute( hbmComponent ) );
			ormRoot.getEmbeddable().add( transferEmbeddable( entity, hbmComponent ) );
		}
	}
	
	private JaxbEmbeddable transferEmbeddable(JaxbEntity entity, JaxbComponentElement hbmComponent) {
		final JaxbEmbeddable embeddable = new JaxbEmbeddable();
		if (StringHelper.isEmpty( hbmComponent.getClazz() )) {
			// HBM allows this to be empty, so we must get the Embeddable class name with reflection.
			embeddable.setClazz( getPropertyType( entity.getClazz(), hbmComponent.getName() ).getName() );
		}
		else {
			embeddable.setClazz( hbmComponent.getClazz() );
		}
		
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
	
	private JaxbEmbeddable transferEmbeddable(JaxbEntity entity, JaxbCompositeElementElement hbmComponent) {
		final JaxbEmbeddable embeddable = new JaxbEmbeddable();
		embeddable.setClazz( hbmComponent.getClazz() );
		embeddable.setAttributes( new JaxbEmbeddableAttributes() );
		for (JaxbPropertyElement property : hbmComponent.getProperty()) {
			embeddable.getAttributes().getBasic().add( transferBasicAttribute( property ) );
		}
		for (JaxbManyToOneElement manyToOne : hbmComponent.getManyToOne()) {
			embeddable.getAttributes().getManyToOne().add( transferManyToOneAttribute( manyToOne ) );
		}
		return embeddable;
	}

	private JaxbEmbedded transferEmbeddedAttribute(JaxbComponentElement hbmComponent) {
		final JaxbEmbedded embedded = new JaxbEmbedded();
		embedded.setAttributeAccessor( hbmComponent.getAccess() );
		embedded.setName( hbmComponent.getName() );
		return embedded;
	}

	private void transferOneToOneAttributes(JaxbEntity entity, EntityElement hbmClass) {
		for (JaxbOneToOneElement hbmO2O : hbmClass.getOneToOne()) {
			entity.getAttributes().getOneToOne().add( transferOneToOneAttribute( hbmO2O ) );
		}
	}

	private JaxbOneToOne transferOneToOneAttribute(JaxbOneToOneElement hbmO2O) {
		if (!hbmO2O.getFormula().isEmpty() || !StringHelper.isEmpty( hbmO2O.getFormulaAttribute() )) {
			throw new MappingException( "HBM transformation: Formulas within one-to-ones are not yet supported." );
		}
		
		final JaxbOneToOne o2o = new JaxbOneToOne();
		o2o.setAttributeAccessor( hbmO2O.getAccess() );
		o2o.setHbmCascade( convertCascadeType( hbmO2O.getCascade() ) );
		o2o.setOrphanRemoval( isOrphanRemoval( hbmO2O.getCascade() ) );
		o2o.setForeignKey( new JaxbForeignKey() );
		o2o.getForeignKey().setName( hbmO2O.getForeignKey() );
		if (! StringHelper.isEmpty( hbmO2O.getPropertyRef() )) {
			final JaxbJoinColumn joinColumn = new JaxbJoinColumn();
			joinColumn.setReferencedColumnName( hbmO2O.getPropertyRef() );
			o2o.getJoinColumn().add( joinColumn );
		}
		o2o.setName( hbmO2O.getName() );
		if ( StringHelper.isNotEmpty( hbmO2O.getEntityName() ) ) {
			o2o.setTargetEntity( hbmO2O.getEntityName() );
		}
		else {
			o2o.setTargetEntity( hbmO2O.getClazz() );
		}

		transferFetchable( hbmO2O.getLazy(), hbmO2O.getFetch(), hbmO2O.getOuterJoin(), hbmO2O.isConstrained(), o2o );
		
		return o2o;
	}

	private void transferManyToOneAttributes(JaxbEntity entity, EntityElement hbmClass) {
		for (JaxbManyToOneElement hbmM2O : hbmClass.getManyToOne()) {
			entity.getAttributes().getManyToOne().add( transferManyToOneAttribute( hbmM2O ) );
		}
	}

	private JaxbManyToOne transferManyToOneAttribute(JaxbManyToOneElement hbmM2O) {
		if (!hbmM2O.getFormula().isEmpty() || !StringHelper.isEmpty( hbmM2O.getFormulaAttribute() )) {
			throw new MappingException( "HBM transformation: Formulas within many-to-ones are not yet supported." );
		}
		final JaxbManyToOne m2o = new JaxbManyToOne();
		m2o.setAttributeAccessor( hbmM2O.getAccess() );
		m2o.setHbmCascade( convertCascadeType( hbmM2O.getCascade() ) );
		m2o.setForeignKey( new JaxbForeignKey() );
		m2o.getForeignKey().setName( hbmM2O.getForeignKey() );
		if (! hbmM2O.getColumn().isEmpty()) {
			for ( JaxbColumnElement hbmColumn : hbmM2O.getColumn() ) {
				final JaxbJoinColumn joinColumn = new JaxbJoinColumn();
				joinColumn.setName( hbmColumn.getName() );
				joinColumn.setNullable( hbmColumn.isNotNull() == null ? null : !hbmColumn.isNotNull() );
				joinColumn.setUnique( hbmColumn.isUnique() );
				joinColumn.setInsertable( hbmM2O.isInsert() );
				joinColumn.setUpdatable( hbmM2O.isUpdate() );
				m2o.getJoinColumn().add( joinColumn );
			}
		}
		else {
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
			joinColumn.setNullable( hbmM2O.isNotNull() == null ? null : !hbmM2O.isNotNull() );
			joinColumn.setUnique( hbmM2O.isUnique() );
			joinColumn.setInsertable( hbmM2O.isInsert() );
			joinColumn.setUpdatable( hbmM2O.isUpdate() );
			m2o.getJoinColumn().add( joinColumn );
		}
		m2o.setName( hbmM2O.getName() );
		m2o.setOptional( hbmM2O.isNotNull() == null ? true : !hbmM2O.isNotNull() );
		if ( StringHelper.isNotEmpty( hbmM2O.getEntityName() ) ) {
			m2o.setTargetEntity( hbmM2O.getEntityName() );
		}
		else {
			m2o.setTargetEntity( hbmM2O.getClazz() );
		}
		transferFetchable( hbmM2O.getLazy(), hbmM2O.getFetch(), hbmM2O.getOuterJoin(), null, m2o );
		return m2o;
	}

	// TODO: Duplicates a lot of the above
	private JaxbManyToOne transferManyToOneAttribute(JaxbKeyManyToOneElement hbmM2O) {
		final JaxbManyToOne m2o = new JaxbManyToOne();
		m2o.setId( true );
		m2o.setAttributeAccessor( hbmM2O.getAccess() );
		m2o.setFetch( convert( hbmM2O.getLazy() ) );
		m2o.setForeignKey( new JaxbForeignKey() );
		m2o.getForeignKey().setName( hbmM2O.getForeignKey() );
		if (! hbmM2O.getColumn().isEmpty()) {
			for ( JaxbColumnElement hbmColumn : hbmM2O.getColumn() ) {
				final JaxbJoinColumn joinColumn = new JaxbJoinColumn();
				joinColumn.setName( hbmColumn.getName() );
				joinColumn.setNullable( hbmColumn.isNotNull() == null ? null : !hbmColumn.isNotNull() );
				joinColumn.setUnique( hbmColumn.isUnique() );
				m2o.getJoinColumn().add( joinColumn );
			}
		}
		else {
			final JaxbJoinColumn joinColumn = new JaxbJoinColumn();
			if (StringHelper.isEmpty( hbmM2O.getColumnAttribute() )) {
				// AbstractBasicBindingTests seems to imply this was the case
				joinColumn.setName( hbmM2O.getName() );
			}
			else {
				joinColumn.setName( hbmM2O.getColumnAttribute() );
			}
			m2o.getJoinColumn().add( joinColumn );
		}
		m2o.setName( hbmM2O.getName() );
		if ( StringHelper.isNotEmpty( hbmM2O.getEntityName() ) ) {
			m2o.setTargetEntity( hbmM2O.getEntityName() );
		}
		else {
			m2o.setTargetEntity( hbmM2O.getClazz() );
		}
		if (hbmM2O.getOnDelete() != null) {
			m2o.setOnDelete( convert( hbmM2O.getOnDelete() ) );
		}
		return m2o;
	}

	private void transferAnyAttributes(JaxbEntity entity, EntityElement hbmClass) {

	}

	private JaxbAny transferAnyAttribute(JaxbAnyElement hbmAny) {
		// TODO
		return new JaxbAny();
	}

	private void transferManyToAnyAttributes(JaxbEntity entity, EntityElement hbmClass) {

	}

	private void transferPrimitiveArrayAttributes(JaxbEntity entity, EntityElement hbmClass) {
		if ( !hbmClass.getPrimitiveArray().isEmpty() ) {
			throw new MappingException( "HBM transformation: Entity mapping [" + hbmClass.getName() + " : "
					+ hbmClass.getEntityName() + "] from hbm.xml [" + origin + "]  used <primitive-array/> construct " +
							"which is not supported in transformation; skipping" );
		}
	}

	private void transferPropertiesGrouping(JaxbEntity entity, EntityElement hbmClass) {
		if ( !hbmClass.getProperties().isEmpty() ) {
			throw new MappingException( "HBM transformation: Entity mapping [" + hbmClass.getName() + " : "
					+ hbmClass.getEntityName() + "] from hbm.xml [" + origin + "]  used <properties/> construct " +
							"which is not supported in transformation; skipping" );
		}
	}

	private void transferUnionSubclass(JaxbUnionSubclassElement hbmSubclass, JaxbEntity subclassEntity) {
		if (! StringHelper.isEmpty( hbmSubclass.getProxy() )) {
			// TODO
			throw new MappingException( "HBM transformation: proxy attributes not yet supported" );
		}
		transferEntityElement( hbmSubclass, subclassEntity );
		transferEntityElementAttributes( subclassEntity, hbmSubclass );
		
		subclassEntity.setTable( new JaxbTable() );
		subclassEntity.getTable().setCatalog( hbmSubclass.getCatalog() );
		subclassEntity.getTable().setSchema( hbmSubclass.getSchema() );
		subclassEntity.getTable().setName( hbmSubclass.getTable() );
		subclassEntity.getTable().setComment( hbmSubclass.getComment() );
		subclassEntity.getTable().setCheck( hbmSubclass.getCheck() );
		
		if (! hbmSubclass.getUnionSubclass().isEmpty()) {
			subclassEntity.setInheritance( new JaxbInheritance() );
			subclassEntity.getInheritance().setStrategy( JaxbInheritanceType.UNION_SUBCLASS );
			for (JaxbUnionSubclassElement nestedHbmSubclass : hbmSubclass.getUnionSubclass()) {
				final JaxbEntity nestedSubclassEntity = new JaxbEntity();
				ormRoot.getEntity().add( nestedSubclassEntity );
				transferUnionSubclass( nestedHbmSubclass, nestedSubclassEntity );
			}
		}
	}
	
	private void transferCollectionAttributes(JaxbEntity entity, EntityElement hbmClass) {
		for (JaxbSetElement hbmSet : hbmClass.getSet()) {
			transferCollectionAttribute( entity, hbmSet, "set", hbmSet.getOrderBy() );
		}
		
		for (JaxbBagElement hbmBag : hbmClass.getBag()) {
			transferCollectionAttribute( entity, hbmBag, "bag", hbmBag.getOrderBy() );
		}
		
		for (JaxbListElement hbmList : hbmClass.getList()) {
			final CollectionAttribute list = transferCollectionAttribute( entity, hbmList, "list", null );
			transferListIndex( list, hbmList.getListIndex() );
		}
		
		for (JaxbMapElement hbmMap : hbmClass.getMap()) {
			final CollectionAttribute map = transferCollectionAttribute( entity, hbmMap, "map", hbmMap.getOrderBy() );
			transferMapKey( map, hbmMap );
		}
		
		for (JaxbArrayElement hbmArray : hbmClass.getArray()) {
			final CollectionAttribute array = transferCollectionAttribute( entity, hbmArray, "array", null );
			transferListIndex( array, hbmArray.getListIndex() );
		}
	}
	
	private CollectionAttribute transferCollectionAttribute(JaxbEntity entity, PluralAttributeElement pluralAttribute,
			String collectionTypeName, String orderBy) {
		if (pluralAttribute.getBatchSize() > 0) {
			// TODO: New schema only defines batch-size at the class level, not collections.
			throw new MappingException( "HBM transformation: 'batch-size' not yet supported." );
		}
		if (! StringHelper.isEmpty( pluralAttribute.getWhere() )) {
			// TODO: New schema only defines where at the class level, not collections.
			throw new MappingException( "HBM transformation: 'where' not yet supported." );
		}
		
		CollectionAttribute collection = null;
		if (pluralAttribute.getElement() != null) {
			final JaxbElementCollection elementCollection = transferElementCollection(
					pluralAttribute, pluralAttribute.getElement(), collectionTypeName, orderBy );
			entity.getAttributes().getElementCollection().add( elementCollection );
			collection = elementCollection;
		}
		else if (pluralAttribute.getOneToMany() != null) {
			final JaxbOneToMany o2m = transferOneToManyAttribute( pluralAttribute, collectionTypeName, orderBy );
			entity.getAttributes().getOneToMany().add( o2m );
			collection = o2m;
		}
		else if (pluralAttribute.getManyToMany() != null) {
			final JaxbManyToMany m2m = transferManyToManyAttribute( pluralAttribute, collectionTypeName );
			entity.getAttributes().getManyToMany().add( m2m );
			collection = m2m;
		}
		else if (pluralAttribute.getCompositeElement() != null) {
			ormRoot.getEmbeddable().add( transferEmbeddable( entity, pluralAttribute.getCompositeElement() ) );
			
			final JaxbElementCollection elementCollection = transferElementCollection(
					pluralAttribute, pluralAttribute.getCompositeElement(), collectionTypeName, orderBy );
			entity.getAttributes().getElementCollection().add( elementCollection );
			collection = elementCollection;
		}
		
		if (collection != null) {
			for (JaxbFilterElement hbmFilter : pluralAttribute.getFilter()) {
				// TODO: How to set filter on collections in unified xml?
				throw new MappingException( "HBM transformation: Filters within collections are not yet supported." );
			}
		}
		
		return collection;
	}
	
	private void transferListIndex(CollectionAttribute list, JaxbListIndexElement hbmListIndex) {
		if (hbmListIndex != null) {
			final JaxbOrderColumn orderColumn = new JaxbOrderColumn();
			// TODO: multiple columns?
			orderColumn.setName( hbmListIndex.getColumnAttribute() );
			list.setOrderColumn( orderColumn );
		}
	}
	
	private void transferMapKey(CollectionAttribute map, JaxbMapElement pluralAttribute) {
		if (pluralAttribute.getIndex() != null) {
			final JaxbMapKeyColumn mapKey = new JaxbMapKeyColumn();
			// TODO: multiple columns?
			mapKey.setName( pluralAttribute.getIndex().getColumnAttribute() );
			map.setMapKeyColumn( mapKey );
			if ( ! StringHelper.isEmpty( pluralAttribute.getIndex().getType() ) ) {
				final JaxbHbmType type = new JaxbHbmType();
				type.setName( pluralAttribute.getIndex().getType() );
				map.setMapKeyType( type );
			}
		}
		if (pluralAttribute.getMapKey() != null) {
			if (! StringHelper.isEmpty( pluralAttribute.getMapKey().getFormulaAttribute() )) {
				throw new MappingException( "HBM transformation: Formulas within map keys are not yet supported." );
			}
			final JaxbMapKeyColumn mapKey = new JaxbMapKeyColumn();
			// TODO: multiple columns?
			mapKey.setName( pluralAttribute.getMapKey().getColumnAttribute() );
			map.setMapKeyColumn( mapKey );
			// TODO: #getType w/ attributes?
			if ( ! StringHelper.isEmpty( pluralAttribute.getMapKey().getTypeAttribute() ) ) {
				final JaxbHbmType type = new JaxbHbmType();
				type.setName( pluralAttribute.getMapKey().getTypeAttribute() );
				map.setMapKeyType( type );
			}
		}
	}
	
	private JaxbElementCollection transferElementCollection(PluralAttributeElement pluralAttribute,
			JaxbElementElement hbmElement, String collectionTypeName, String orderBy) {
		final JaxbElementCollection element = new JaxbElementCollection();
		element.setName( pluralAttribute.getName() );
		final JaxbColumn column = new JaxbColumn();
		column.setName( hbmElement.getColumnAttribute() );
		element.setColumn( column );
		final JaxbHbmType elementType = new JaxbHbmType();
		elementType.setName( hbmElement.getTypeAttribute() );
		element.setType( elementType );
		final JaxbHbmType collectionType = new JaxbHbmType();
		collectionType.setName( collectionTypeName );
		element.setCollectionType( collectionType );
		element.setOrderBy( orderBy );
		transferFetchable( pluralAttribute.getLazy(), pluralAttribute.getFetch(), pluralAttribute.getOuterJoin(), element );
		return element;
	}
	
	private JaxbElementCollection transferElementCollection(PluralAttributeElement pluralAttribute,
			JaxbCompositeElementElement hbmElement, String collectionTypeName, String orderBy) {
		final JaxbElementCollection element = new JaxbElementCollection();
		element.setName( pluralAttribute.getName() );
		final JaxbHbmType collectionType = new JaxbHbmType();
		collectionType.setName( collectionTypeName );
		element.setCollectionType( collectionType );
		element.setOrderBy( orderBy );
		transferFetchable( pluralAttribute.getLazy(), pluralAttribute.getFetch(), pluralAttribute.getOuterJoin(),
				element );
		element.setTargetClass( hbmElement.getClazz() );
		return element;
	}

	private JaxbOneToMany transferOneToManyAttribute(
			PluralAttributeElement pluralAttribute, String collectionTypeName, String orderBy) {
		final JaxbOneToManyElement hbmO2M = pluralAttribute.getOneToMany();
		final JaxbOneToMany o2m = new JaxbOneToMany();
		final JaxbHbmType collectionType = new JaxbHbmType();
		collectionType.setName( collectionTypeName );
		o2m.setCollectionType( collectionType );
		o2m.setAttributeAccessor( pluralAttribute.getAccess() );
		o2m.setHbmCascade( convertCascadeType( pluralAttribute.getCascade() ) );
		o2m.setOrphanRemoval( isOrphanRemoval( pluralAttribute.getCascade() ) );
		transferFetchable( pluralAttribute.getLazy(), pluralAttribute.getFetch(), pluralAttribute.getOuterJoin(), o2m );
		o2m.setName( pluralAttribute.getName() );
		o2m.setTargetEntity( hbmO2M.getClazz() );
		o2m.setInverse( pluralAttribute.isInverse() );
		if (pluralAttribute.getKey() != null) {
			final JaxbKeyElement hbmKey = pluralAttribute.getKey();
			if (! hbmKey.getColumn().isEmpty()) {
				for ( JaxbColumnElement hbmColumn : hbmKey.getColumn() ) {
					final JaxbJoinColumn joinColumn = new JaxbJoinColumn();
					joinColumn.setName( hbmColumn.getName() );
					joinColumn.setNullable( hbmColumn.isNotNull() == null ? null : !hbmColumn.isNotNull() );
					joinColumn.setUnique( hbmColumn.isUnique() );
					o2m.getJoinColumn().add( joinColumn );
				}
			}
			else {
				final JaxbJoinColumn joinColumn = new JaxbJoinColumn();
				joinColumn.setName( hbmKey.getColumnAttribute() );
				joinColumn.setNullable( hbmKey.isNotNull() == null ? null : !hbmKey.isNotNull() );
				joinColumn.setUnique( hbmKey.isUnique() );
				if (! StringHelper.isEmpty( hbmKey.getPropertyRef() )) {
					joinColumn.setReferencedColumnName( hbmKey.getPropertyRef() );
				}
				o2m.getJoinColumn().add( joinColumn );
			}
			if (hbmKey.getOnDelete() != null) {
				o2m.setOnDelete( convert( hbmKey.getOnDelete() ) );
			}
		}
		o2m.setOrderBy( orderBy );
		return o2m;
	}

	private JaxbManyToMany transferManyToManyAttribute(PluralAttributeElement pluralAttribute,
			String collectionTypeName) {
		final JaxbManyToManyElement hbmM2M = pluralAttribute.getManyToMany();
		
		if (!hbmM2M.getFormula().isEmpty() || !StringHelper.isEmpty( hbmM2M.getFormulaAttribute() )) {
			throw new MappingException( "HBM transformation: Formulas within many-to-manys are not yet supported." );
		}
		if (isOrphanRemoval( pluralAttribute.getCascade() )) {
			// TODO: JPA doesn't provide an orphan-removal field -- another way to do it?
			throw new MappingException( "HBM transformation: many-to-manys w/ orphan removal not yet supported." );
		}
		
		final JaxbManyToMany m2m = new JaxbManyToMany();
		final JaxbHbmType collectionType = new JaxbHbmType();
		collectionType.setName( collectionTypeName );
		m2m.setCollectionType( collectionType );
		m2m.setAttributeAccessor( pluralAttribute.getAccess() );
		m2m.setHbmCascade( convertCascadeType( pluralAttribute.getCascade() ) );
		transferFetchable( pluralAttribute.getLazy(), pluralAttribute.getFetch(), pluralAttribute.getOuterJoin(), m2m );
		m2m.setName( pluralAttribute.getName() );
		m2m.setTargetEntity( hbmM2M.getClazz() );
		m2m.setOrderBy( hbmM2M.getOrderBy() );
		m2m.setInverse( pluralAttribute.isInverse() );
		if (pluralAttribute.getKey() != null) {
			final JaxbKeyElement hbmKey = pluralAttribute.getKey();
			final String columnName = hbmKey.getColumnAttribute();
			if (m2m.isInverse()) {
				// Only set the <join-table> on the owning side.  Since we only have the column name here, we'll have
				// to resolve it later.
				m2m.setHbmKey( columnName );
				if (StringHelper.isEmpty( columnName )) {
					m2m.setHbmKey( Collection.DEFAULT_ELEMENT_COLUMN_NAME );
				}
				else {
					m2m.setHbmKey( columnName );
				}
			}
			else {
				final JaxbJoinColumn joinColumn = new JaxbJoinColumn();
				joinColumn.setName( columnName );
				final JaxbJoinColumn inverseJoinColumn = new JaxbJoinColumn();
				if (StringHelper.isEmpty( hbmM2M.getColumnAttribute() )) {
					inverseJoinColumn.setName( Collection.DEFAULT_ELEMENT_COLUMN_NAME );
				}
				else {
					inverseJoinColumn.setName( hbmM2M.getColumnAttribute() );
				}
				final JaxbJoinTable joinTable = new JaxbJoinTable();
				joinTable.getJoinColumn().add( joinColumn );
				joinTable.getInverseJoinColumn().add( inverseJoinColumn );
				if (! StringHelper.isEmpty( pluralAttribute.getTable() )) {
					joinTable.setName( pluralAttribute.getTable() );
				}
				m2m.setJoinTable( joinTable );
			}
		}
		
		return m2m;
	}
	
	// ToOne
	private void transferFetchable(JaxbLazyAttributeWithNoProxy hbmLazy, JaxbFetchStyleAttribute hbmFetch,
			JaxbOuterJoinAttribute hbmOuterJoin, Boolean constrained, FetchableAttribute fetchable) {
		FetchType laziness = FetchType.LAZY;
		FetchMode fetch = FetchMode.SELECT;
		
		if (hbmLazy != null) {
			if (hbmLazy.equals( JaxbLazyAttributeWithNoProxy.FALSE )) {
				laziness = FetchType.EAGER;
			}
			else if (hbmLazy.equals( JaxbLazyAttributeWithNoProxy.NO_PROXY )) {
				// TODO: @LazyToOne(LazyToOneOption.PROXY) or @LazyToOne(LazyToOneOption.NO_PROXY)
			}
		}
		
		// allow fetch style to override laziness, if necessary
		if (constrained != null && ! constrained) {
			// NOTE SPECIAL CASE: one-to-one constrained=false cannot be proxied, so default to join and non-lazy
			laziness = FetchType.EAGER;
			fetch = FetchMode.JOIN;
		}
		else {
			if (hbmFetch == null) {
				if (hbmOuterJoin != null && hbmOuterJoin.equals( JaxbOuterJoinAttribute.TRUE )) {
					laziness = FetchType.EAGER;
					fetch = FetchMode.JOIN;
				}
			}
			else {
				if (hbmFetch.equals( JaxbFetchStyleAttribute.JOIN )) {
					laziness = FetchType.EAGER;
					fetch = FetchMode.JOIN;
				}
			}
		}
		
		fetchable.setFetch( laziness );
		fetchable.setHbmFetchMode( fetch );
	}
	
	// ToMany
	private void transferFetchable(JaxbLazyAttributeWithExtra hbmLazy, JaxbFetchAttributeWithSubselect hbmFetch,
			JaxbOuterJoinAttribute hbmOuterJoin, FetchableAttribute fetchable) {
		FetchType laziness = FetchType.LAZY;
		FetchMode fetch = FetchMode.SELECT;
		
		if (hbmLazy != null) {
			if (hbmLazy.equals( JaxbLazyAttributeWithExtra.EXTRA )) {
				throw new MappingException( "HBM transformation: extra lazy not yet supported." );
			}
			else if (hbmLazy.equals( JaxbLazyAttributeWithExtra.FALSE )) {
				laziness = FetchType.EAGER;
			}
		}
		
		// allow fetch style to override laziness, if necessary
		if (hbmFetch == null) {
			if (hbmOuterJoin != null && hbmOuterJoin.equals( JaxbOuterJoinAttribute.TRUE )) {
				laziness = FetchType.EAGER;
				fetch = FetchMode.JOIN;
			}
		}
		else {
			if (hbmFetch.equals( JaxbFetchAttributeWithSubselect.JOIN )) {
				laziness = FetchType.EAGER;
				fetch = FetchMode.JOIN;
			}
			else if (hbmFetch.equals( JaxbFetchAttributeWithSubselect.SUBSELECT )) {
				fetch = FetchMode.SUBSELECT;
			}
		}
		
		fetchable.setFetch( laziness );
		fetchable.setHbmFetchMode( fetch );
	}
	
	// KeyManyToOne
	private FetchType convert(JaxbLazyAttribute hbmLazy) {
		// TODO: no-proxy?
		if ( hbmLazy != null || "proxy".equalsIgnoreCase( hbmLazy.value() ) ) {
			return FetchType.LAZY;
		}
		else if ( hbmLazy != null && "false".equalsIgnoreCase( hbmLazy.value() ) ) {
			return FetchType.EAGER;
		}
		else {
			// proxy is HBM default
			return FetchType.LAZY;
		}
	}
	
	private JaxbOnDeleteType convert(JaxbOnDeleteAttribute hbmOnDelete) {
		switch (hbmOnDelete) {
			case CASCADE:
				return JaxbOnDeleteType.CASCADE;
			default:
				return JaxbOnDeleteType.NO_ACTION;
		}
	}
	
	private JaxbHbmFilter convert(JaxbFilterElement hbmFilter) {
		final JaxbHbmFilter filter = new JaxbHbmFilter();
		final boolean autoAliasInjection = hbmFilter.getAutoAliasInjection() == null ? true
				: hbmFilter.getAutoAliasInjection().equalsIgnoreCase( "true" );
		filter.setAutoAliasInjection( autoAliasInjection );
		filter.setConditionAttribute( hbmFilter.getCondition() );
		filter.setName( hbmFilter.getName() );
		for (Serializable content : hbmFilter.getContent()) {
			filter.getContent().add( content );
		}
		return filter;
	}

	private JaxbCacheModeType convert(JaxbCacheModeAttribute cacheMode) {
		final String value = cacheMode == null ? null : cacheMode.value();
		if ( StringHelper.isEmpty( value ) ) {
			return JaxbCacheModeType.NORMAL;
		}

		return JaxbCacheModeType.fromValue( value );
	}

	private FlushMode convert(JaxbFlushModeAttribute flushMode) {
		final String value = flushMode == null ? null : flushMode.value();
		if ( StringHelper.isEmpty( value ) ) {
			return null;
		}

		return FlushMode.valueOf( value );
	}

	private JaxbHbmCustomSqlCheckEnum convert(JaxbCheckAttribute check) {
		if ( check == null ) {
			return null;
		}
		return JaxbHbmCustomSqlCheckEnum.valueOf( check.value() );
	}
	
	private JaxbHbmCascadeType convertCascadeType(String s) {
		final JaxbHbmCascadeType cascadeType = new JaxbHbmCascadeType();
		
		if (! StringHelper.isEmpty( s )) {
			s = s.replaceAll( " ", "" );
			s = StringHelper.toLowerCase( s );
			final String[] split = s.split( "," );
			for (String hbmCascade : split) {
				if (hbmCascade.contains( "all" )) {
					cascadeType.setCascadeAll( new JaxbEmptyType() );
				}
				if (hbmCascade.contains( "persist" )) {
					cascadeType.setCascadePersist( new JaxbEmptyType() );
				}
				if (hbmCascade.contains( "merge" )) {
					cascadeType.setCascadeMerge( new JaxbEmptyType() );
				}
				if (hbmCascade.contains( "refresh" )) {
					cascadeType.setCascadeRefresh( new JaxbEmptyType() );
				}
				if (hbmCascade.contains( "save-update" )) {
					cascadeType.setCascadeSaveUpdate( new JaxbEmptyType() );
				}
				if (hbmCascade.contains( "evict" ) || hbmCascade.contains( "detach" )) {
					cascadeType.setCascadeDetach( new JaxbEmptyType() );
				}
				if (hbmCascade.contains( "replicate" )) {
					cascadeType.setCascadeReplicate( new JaxbEmptyType() );
				}
				if (hbmCascade.contains( "lock" )) {
					cascadeType.setCascadeLock( new JaxbEmptyType() );
				}
				if (hbmCascade.contains( "delete" )) {
					cascadeType.setCascadeDelete( new JaxbEmptyType() );
				}
			}
		}
		return cascadeType;
	}
	
	private boolean isOrphanRemoval(String s) {
		if (! StringHelper.isEmpty( s )) {
			s = StringHelper.toLowerCase( s );
			return s.contains( "orphan" );
		}
		return false;
	}
	
	private String getFqClassName(String className) {
		final String defaultPackageName = ormRoot.getPackage();
		if ( StringHelper.isNotEmpty( className ) && className.indexOf( '.' ) < 0 && StringHelper.isNotEmpty( defaultPackageName ) ) {
			className = StringHelper.qualify( defaultPackageName, className );
		}
		return className;
	}
	
	private Class getClass(String className) {
		return classLoaderAccess.classForName( getFqClassName( className ) );
	}
	
	private Class getPropertyType(String className, String propertyName) {
		final Class clazz = getClass(className);
		return ReflectHelper.reflectedPropertyClass( clazz, propertyName );
	}
}
