/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.hbm.transform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.PolymorphismType;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.JaxbLogger;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.hbm.spi.Discriminatable;
import org.hibernate.boot.jaxb.hbm.spi.EntityInfo;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAnyAssociationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAuxiliaryDatabaseObjectType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCacheInclusionEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmClassRenameType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeKeyManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDiscriminatorSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDynamicComponentType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmEntityBaseDefinition;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchProfileType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchStyleEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchStyleWithSubselectEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterAliasMappingType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterDefinitionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterParameterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIdBagCollectionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIdentifierGeneratorDefinitionType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIndexType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmJoinedSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmLazyEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmLazyWithExtraEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmLazyWithNoProxyEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmListIndexType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmListType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryPropertyReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNotFoundEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOnDeleteEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToManyCollectionElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOuterJoinEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPolymorphismEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPrimitiveArrayType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPropertiesType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmQueryParamType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmResultSetMappingType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSecondaryTableType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSetType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSimpleIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSynchronizeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTimestampAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmUnionSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmVersionAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.PluralAttributeInfo;
import org.hibernate.boot.jaxb.hbm.spi.ResultSetMappingContainer;
import org.hibernate.boot.jaxb.hbm.spi.ToolingHintContainer;
import org.hibernate.boot.jaxb.mapping.AttributesContainer;
import org.hibernate.boot.jaxb.mapping.CollectionAttribute;
import org.hibernate.boot.jaxb.mapping.JaxbAttributes;
import org.hibernate.boot.jaxb.mapping.JaxbBasic;
import org.hibernate.boot.jaxb.mapping.JaxbCacheInclusionType;
import org.hibernate.boot.jaxb.mapping.JaxbCaching;
import org.hibernate.boot.jaxb.mapping.JaxbCascadeType;
import org.hibernate.boot.jaxb.mapping.JaxbCollectionTable;
import org.hibernate.boot.jaxb.mapping.JaxbColumn;
import org.hibernate.boot.jaxb.mapping.JaxbColumnResult;
import org.hibernate.boot.jaxb.mapping.JaxbCustomLoader;
import org.hibernate.boot.jaxb.mapping.JaxbCustomSql;
import org.hibernate.boot.jaxb.mapping.JaxbDatabaseObject;
import org.hibernate.boot.jaxb.mapping.JaxbDatabaseObjectScope;
import org.hibernate.boot.jaxb.mapping.JaxbDiscriminatorColumn;
import org.hibernate.boot.jaxb.mapping.JaxbElementCollection;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddable;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddableAttributes;
import org.hibernate.boot.jaxb.mapping.JaxbEmbedded;
import org.hibernate.boot.jaxb.mapping.JaxbEmbeddedId;
import org.hibernate.boot.jaxb.mapping.JaxbEmptyType;
import org.hibernate.boot.jaxb.mapping.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.JaxbEntityMappings;
import org.hibernate.boot.jaxb.mapping.JaxbEntityResult;
import org.hibernate.boot.jaxb.mapping.JaxbFetchProfile;
import org.hibernate.boot.jaxb.mapping.JaxbFieldResult;
import org.hibernate.boot.jaxb.mapping.JaxbFilterDef;
import org.hibernate.boot.jaxb.mapping.JaxbForeignKey;
import org.hibernate.boot.jaxb.mapping.JaxbGenericIdGenerator;
import org.hibernate.boot.jaxb.mapping.JaxbHbmAnyDiscriminator;
import org.hibernate.boot.jaxb.mapping.JaxbHbmAnyDiscriminatorValueMapping;
import org.hibernate.boot.jaxb.mapping.JaxbHbmAnyKey;
import org.hibernate.boot.jaxb.mapping.JaxbHbmAnyMapping;
import org.hibernate.boot.jaxb.mapping.JaxbHbmFilter;
import org.hibernate.boot.jaxb.mapping.JaxbHbmManyToAny;
import org.hibernate.boot.jaxb.mapping.JaxbHqlImport;
import org.hibernate.boot.jaxb.mapping.JaxbId;
import org.hibernate.boot.jaxb.mapping.JaxbIdClass;
import org.hibernate.boot.jaxb.mapping.JaxbInheritance;
import org.hibernate.boot.jaxb.mapping.JaxbJoinColumn;
import org.hibernate.boot.jaxb.mapping.JaxbManyToMany;
import org.hibernate.boot.jaxb.mapping.JaxbManyToOne;
import org.hibernate.boot.jaxb.mapping.JaxbMapKeyColumn;
import org.hibernate.boot.jaxb.mapping.JaxbNamedNativeQuery;
import org.hibernate.boot.jaxb.mapping.JaxbNamedQuery;
import org.hibernate.boot.jaxb.mapping.JaxbNaturalId;
import org.hibernate.boot.jaxb.mapping.JaxbOneToMany;
import org.hibernate.boot.jaxb.mapping.JaxbOneToOne;
import org.hibernate.boot.jaxb.mapping.JaxbOrderColumn;
import org.hibernate.boot.jaxb.mapping.JaxbPersistenceUnitMetadata;
import org.hibernate.boot.jaxb.mapping.JaxbPluralFetchMode;
import org.hibernate.boot.jaxb.mapping.JaxbPrimaryKeyJoinColumn;
import org.hibernate.boot.jaxb.mapping.JaxbQueryParamType;
import org.hibernate.boot.jaxb.mapping.JaxbSecondaryTable;
import org.hibernate.boot.jaxb.mapping.JaxbSingularFetchMode;
import org.hibernate.boot.jaxb.mapping.JaxbSqlResultSetMapping;
import org.hibernate.boot.jaxb.mapping.JaxbSynchronizedTable;
import org.hibernate.boot.jaxb.mapping.JaxbTable;
import org.hibernate.boot.jaxb.mapping.JaxbTransient;
import org.hibernate.boot.jaxb.mapping.JaxbVersion;
import org.hibernate.boot.jaxb.mapping.ToOneAttribute;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;

import org.jboss.logging.Logger;

import jakarta.persistence.FetchType;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.JAXBElement;

import static org.hibernate.boot.jaxb.hbm.transform.HbmTransformationLogging.TRANSFORMATION_LOGGER;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * Transforms a JAXB binding of a hbm.xml file into a unified orm.xml representation
 *
 * @author Steve Ebersole
 * @author Brett Meyer
 *
 * @implNote This transformation happens on the JAXB model level creating
 * a {@link JaxbEntityMappings} "copy" of the {@link JaxbHbmHibernateMapping}
 * representation
 */
public class HbmXmlTransformer {
	/**
	 * Main entry into hbm.xml transformation
	 *
	 * @param hbmXmlMapping The hbm.xml mapping to be transformed
	 * @param origin The origin of the hbm.xml mapping
	 * @return The transformed representation
	 */
	public static JaxbEntityMappings transform(JaxbHbmHibernateMapping hbmXmlMapping, Origin origin, Options options) {
		return new HbmXmlTransformer( hbmXmlMapping, origin, options ).doTransform();
	}

	public interface Options {
		UnsupportedFeatureHandling unsupportedFeatureHandling();
	}

	private final Origin origin;
	private final JaxbHbmHibernateMapping hbmXmlMapping;
	private final JaxbEntityMappings ormRoot;

	private final Options options;

	public HbmXmlTransformer(JaxbHbmHibernateMapping hbmXmlMapping, Origin origin, Options options) {
		this.origin = origin;
		this.hbmXmlMapping = hbmXmlMapping;
		this.options = options;

		this.ormRoot = new JaxbEntityMappings();
		this.ormRoot.setDescription(
				"mapping.xml document auto-generated from legacy hbm.xml format via transformation - " + origin.getName()
		);

	}

	private JaxbEntityMappings doTransform() {
		TRANSFORMATION_LOGGER.tracef(
				"Starting hbm.xml transformation - `%s`",
				origin
		);

		final JaxbPersistenceUnitMetadata metadata = new JaxbPersistenceUnitMetadata();
		ormRoot.setPersistenceUnitMetadata( metadata );

		transfer( hbmXmlMapping::getPackage, ormRoot::setPackage );
		transfer( hbmXmlMapping::getCatalog, ormRoot::setCatalog );
		transfer( hbmXmlMapping::getSchema, ormRoot::setSchema );
		transfer( hbmXmlMapping::getDefaultAccess, ormRoot::setAttributeAccessor );
		transfer( hbmXmlMapping::getDefaultCascade, ormRoot::setDefaultCascade );
		transfer( hbmXmlMapping::isDefaultLazy, ormRoot::setDefaultLazy );

		transferIdentifierGenerators();
		transferTypeDefs();
		transferFilterDefinitions();
		transferImports();
		transferEntities();
		transferResultSetMappings();
		transferNamedQueries();
		transferNamedNativeQueries();
		transferFetchProfiles();
		transferDatabaseObjects();

		return ormRoot;
	}

	private <T> void transfer(Supplier<T> source, Consumer<T> target) {
		final T value = source.get();
		if ( value != null ) {
			target.accept( value );
		}
	}

	private void handleUnsupportedContent(String description) {
		handleUnsupported(
				"Transformation of hbm.xml `%s` encountered unsupported content : %s",
				origin.toString(),
				description
		);
	}

	private void handleUnsupported(String message, Object... messageArgs) {
		if ( options.unsupportedFeatureHandling() == UnsupportedFeatureHandling.ERROR ) {
			throw new UnsupportedOperationException(
					String.format(
							Locale.ROOT,
							message,
							messageArgs
					)
			);
		}

		final Logger.Level logLevel = options.unsupportedFeatureHandling() == UnsupportedFeatureHandling.WARN
				? Logger.Level.WARN
				: Logger.Level.DEBUG;
		//noinspection deprecation
		TRANSFORMATION_LOGGER.log(
				logLevel,
				message,
				messageArgs
		);
	}

	private void transferTypeDefs() {
		if ( hbmXmlMapping.getTypedef().isEmpty() ) {
			return;
		}

		handleUnsupported(
				"Transformation of type-def mapping not supported - `%s`",
				origin
		);
	}

	private void transferIdentifierGenerators() {
		if ( hbmXmlMapping.getIdentifierGenerator().isEmpty() ) {
			return;
		}

		JaxbLogger.JAXB_LOGGER.tracef(
				"Starting transformation of identifier-generator mappings in `%s`",
				origin
		);

		for ( JaxbHbmIdentifierGeneratorDefinitionType hbmGenerator : hbmXmlMapping.getIdentifierGenerator() ) {
			JaxbLogger.JAXB_LOGGER.debugf(
					"Starting transformation of identifier-generator mapping `%s` - `%s`",
					hbmGenerator.getName(),
					origin
			);

			final JaxbGenericIdGenerator generatorDef = new JaxbGenericIdGenerator();
			ormRoot.getGenericGenerators().add( generatorDef );
			generatorDef.setName( hbmGenerator.getName() );
			generatorDef.setClazz( hbmGenerator.getClazz() );
		}
	}

	@SuppressWarnings("unchecked")
	private void transferFilterDefinitions() {
		if ( hbmXmlMapping.getFilterDef().isEmpty() ) {
			return;
		}

		JaxbLogger.JAXB_LOGGER.tracef(
				"Starting transformation of filter-def mappings in `%s`",
				origin
		);

		for ( JaxbHbmFilterDefinitionType hbmFilterDef : hbmXmlMapping.getFilterDef() ) {
			JaxbLogger.JAXB_LOGGER.debugf(
					"Starting transformation of filter-def mapping `%s` - `%s`",
					hbmFilterDef.getName(),
					origin
			);

			final JaxbFilterDef filterDef = new JaxbFilterDef();
			ormRoot.getFilterDefinitions().add( filterDef );
			filterDef.setName( hbmFilterDef.getName() );

			boolean foundCondition = false;
			for ( Object content : hbmFilterDef.getContent() ) {
				if ( content instanceof String ) {
					final String condition = ( (String) content ).trim();
					if (! StringHelper.isEmpty( condition )) {
						foundCondition = true;
						filterDef.setCondition( condition );
					}
				}
				else {
					final JaxbHbmFilterParameterType hbmFilterParam = ( (JAXBElement<JaxbHbmFilterParameterType>) content ).getValue();
					final JaxbFilterDef.JaxbFilterParam param = new JaxbFilterDef.JaxbFilterParam();
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
		if ( hbmXmlMapping.getImport().isEmpty() ) {
			return;
		}

		JaxbLogger.JAXB_LOGGER.tracef(
				"Starting transformation of import mappings - `%s`",
				origin
		);

		for ( JaxbHbmClassRenameType hbmImport : hbmXmlMapping.getImport() ) {
			JaxbLogger.JAXB_LOGGER.debugf(
					"Starting transformation of import `%s` -> `%s` - `%s`",
					hbmImport.getClazz(),
					hbmImport.getRename(),
					origin
			);

			final JaxbHqlImport ormImport = new JaxbHqlImport();
			ormRoot.getHqlImports().add( ormImport );
			ormImport.setClazz( hbmImport.getClazz() );
			ormImport.setRename( hbmImport.getRename() );
		}
	}

	private void transferResultSetMappings() {
		if ( hbmXmlMapping.getResultset().isEmpty() ) {
			return;
		}

		JaxbLogger.JAXB_LOGGER.tracef(
				"Starting transformation of resultset mappings - `%s`",
				origin
		);

		for ( JaxbHbmResultSetMappingType hbmResultSet : hbmXmlMapping.getResultset() ) {
			final JaxbSqlResultSetMapping mapping = transformResultSetMapping( null, hbmResultSet );
			ormRoot.getSqlResultSetMappings().add( mapping );
		}
	}

	private JaxbSqlResultSetMapping transformResultSetMapping(
			String namePrefix,
			JaxbHbmResultSetMappingType hbmResultSet) {
		final String resultMappingName = namePrefix == null
				? hbmResultSet.getName()
				: namePrefix + "." + hbmResultSet.getName();

		JaxbLogger.JAXB_LOGGER.debugf(
				"Starting transformation of resultset mapping `{}` in `{}`",
				resultMappingName,
				origin
		);

		final JaxbSqlResultSetMapping mapping = new JaxbSqlResultSetMapping();
		mapping.setName( resultMappingName );
		mapping.setDescription( "SQL ResultSet mapping - " + resultMappingName );

		for ( Serializable hbmReturn : hbmResultSet.getValueMappingSources() ) {
			if ( hbmReturn instanceof JaxbHbmNativeQueryReturnType ) {
				mapping.getEntityResult().add(
						transferEntityReturnElement(
								resultMappingName,
								(JaxbHbmNativeQueryReturnType) hbmReturn
						)
				);
			}
			else if ( hbmReturn instanceof JaxbHbmNativeQueryScalarReturnType ) {
				mapping.getColumnResult().add(
						transferScalarReturnElement(
								resultMappingName,
								(JaxbHbmNativeQueryScalarReturnType) hbmReturn
						)
				);
			}
			else if ( hbmReturn instanceof JaxbHbmNativeQueryJoinReturnType ) {
				handleUnsupportedContent(
						String.format(
								"SQL ResultSet mapping [name=%s] contained a <return-join/> element, " +
										"which is not supported for transformation",
								resultMappingName
						)
				);
			}
			else if ( hbmReturn instanceof JaxbHbmNativeQueryCollectionLoadReturnType ) {
				handleUnsupportedContent(
						String.format(
								"SQL ResultSet mapping [name=%s] contained a <collection-load/> element, " +
										"which is not supported for transformation",
								resultMappingName
						)
				);
			}
			else {
				// should never happen thanks to XSD
				handleUnsupportedContent(
						String.format(
								"SQL ResultSet mapping [name=%s] contained an unexpected element type",
								resultMappingName
						)
				);
			}
		}
		return mapping;
	}

	private JaxbEntityResult transferEntityReturnElement(
			String resultMappingName,
			JaxbHbmNativeQueryReturnType hbmReturn) {
		final JaxbEntityResult entityResult = new JaxbEntityResult();
		entityResult.setEntityClass( getFullyQualifiedClassName( hbmReturn.getClazz() ) );

		for ( JaxbHbmNativeQueryPropertyReturnType propertyReturn : hbmReturn.getReturnProperty() ) {
			final JaxbFieldResult field = new JaxbFieldResult();
			final List<String> columns = new ArrayList<>();
			if ( !StringHelper.isEmpty( propertyReturn.getColumn() ) ) {
				columns.add( propertyReturn.getColumn() );
			}

			for ( JaxbHbmNativeQueryPropertyReturnType.JaxbHbmReturnColumn returnColumn : propertyReturn.getReturnColumn() ) {
				columns.add( returnColumn.getName() );
			}

			if ( columns.size() > 1 ) {
				handleUnsupportedContent(
						String.format(
								"SQL ResultSet mapping [name=%s] contained a <return-property name='%s'/> element " +
										"declaring multiple 1 column mapping, which is not supported for transformation;" +
										"skipping that return-property mapping",
								resultMappingName,
								propertyReturn.getName()
						)
				);
				continue;
			}

			field.setColumn( columns.get( 0 ) );
			field.setName( propertyReturn.getName() );
			entityResult.getFieldResult().add( field );
		}
		return entityResult;
	}

	private JaxbColumnResult transferScalarReturnElement(
			String resultMappingName,
			JaxbHbmNativeQueryScalarReturnType hbmReturn) {
		final JaxbColumnResult columnResult = new JaxbColumnResult();
		columnResult.setName( hbmReturn.getColumn() );
		columnResult.setClazz( hbmReturn.getType() );
		handleUnsupportedContent(
				String.format(
						"SQL ResultSet mapping [name=%s] contained a <return-scalar column='%s'/> element; " +
								"transforming type->class likely requires manual adjustment",
						resultMappingName,
						hbmReturn.getColumn()
				)
		);
		return columnResult;
	}

	private void transferFetchProfiles() {
		if ( hbmXmlMapping.getFetchProfile().isEmpty() ) {
			return;
		}

		JaxbLogger.JAXB_LOGGER.tracef(
				"Starting transformation of fetch-profile mappings in `{}`",
				origin
		);

		for ( JaxbHbmFetchProfileType hbmFetchProfile : hbmXmlMapping.getFetchProfile() ) {
			ormRoot.getFetchProfiles().add( transferFetchProfile( hbmFetchProfile ) );
		}
	}

	private JaxbFetchProfile transferFetchProfile(JaxbHbmFetchProfileType hbmFetchProfile) {
		JaxbLogger.JAXB_LOGGER.debugf(
				"Starting transformation of fetch-profile mapping `{}` in `{}`",
				hbmFetchProfile.getName(),
				origin
		);

		final JaxbFetchProfile fetchProfile = new JaxbFetchProfile();
		fetchProfile.setName( hbmFetchProfile.getName() );
		for ( JaxbHbmFetchProfileType.JaxbHbmFetch hbmFetch : hbmFetchProfile.getFetch() ) {
			final JaxbFetchProfile.JaxbFetch fetch = new JaxbFetchProfile.JaxbFetch();
			fetchProfile.getFetch().add( fetch );
			fetch.setEntity( hbmFetch.getEntity() );
			fetch.setAssociation( hbmFetch.getAssociation() );
			fetch.setStyle( hbmFetch.getStyle().value() );
		}
		return fetchProfile;
	}

	private void transferNamedQueries() {
		if ( hbmXmlMapping.getQuery().isEmpty() ) {
			return;
		}

		JaxbLogger.JAXB_LOGGER.tracef(
				"Starting transformation of named-query mappings in `{}`",
				origin
		);

		for ( JaxbHbmNamedQueryType hbmQuery : hbmXmlMapping.getQuery() ) {
			ormRoot.getNamedQueries().add( transformNamedQuery( hbmQuery, hbmQuery.getName() ) );
		}
	}

	private JaxbNamedQuery transformNamedQuery(JaxbHbmNamedQueryType hbmQuery, String name) {
		JaxbLogger.JAXB_LOGGER.debugf(
				"Starting transformation of named-query mapping `{}` in `{}`",
				name,
				origin
		);

		final JaxbNamedQuery query = new JaxbNamedQuery();
		query.setName( name );
		query.setCacheable( hbmQuery.isCacheable() );
		query.setCacheMode( hbmQuery.getCacheMode() );
		query.setCacheRegion( hbmQuery.getCacheRegion() );
		query.setComment( hbmQuery.getComment() );
		query.setFetchSize( hbmQuery.getFetchSize() );
		query.setFlushMode( hbmQuery.getFlushMode() );
		query.setFetchSize( hbmQuery.getFetchSize() );
		query.setReadOnly( hbmQuery.isReadOnly() );
		query.setTimeout( hbmQuery.getTimeout() );

		for ( Object content : hbmQuery.getContent() ) {
			if ( content instanceof String ) {
				String s = (String) content;
				s = s.trim();
				query.setQuery( s );
			}
			else {
				@SuppressWarnings("unchecked") final JAXBElement<JaxbHbmQueryParamType> element = (JAXBElement<JaxbHbmQueryParamType>) content;
				final JaxbHbmQueryParamType hbmQueryParam = element.getValue();
				final JaxbQueryParamType queryParam = new JaxbQueryParamType();
				query.getQueryParam().add( queryParam );
				queryParam.setName( hbmQueryParam.getName() );
				queryParam.setType( hbmQueryParam.getType() );
			}
		}
		
		return query;
	}

	private void transferNamedNativeQueries() {
		if ( hbmXmlMapping.getSqlQuery().isEmpty() ) {
			return;
		}

		JaxbLogger.JAXB_LOGGER.tracef(
				"Starting transformation of (named) query mappings in `{}`",
				origin
		);

		for ( JaxbHbmNamedNativeQueryType hbmQuery : hbmXmlMapping.getSqlQuery() ) {
			ormRoot.getNamedNativeQueries().add( transformNamedNativeQuery( hbmQuery, hbmQuery.getName() ) );
		}
	}

	private JaxbNamedNativeQuery transformNamedNativeQuery(JaxbHbmNamedNativeQueryType hbmQuery, String queryName) {
		JaxbLogger.JAXB_LOGGER.debugf(
				"Starting transformation of (named) query mapping `{}` in `{}`",
				queryName,
				origin
		);

		final String implicitResultSetMappingName = queryName + "-implicitResultSetMapping";

		final JaxbNamedNativeQuery query = new JaxbNamedNativeQuery();
		query.setName( queryName );
		query.setCacheable( hbmQuery.isCacheable() );
		query.setCacheMode( hbmQuery.getCacheMode() );
		query.setCacheRegion( hbmQuery.getCacheRegion() );
		query.setComment( hbmQuery.getComment() );
		query.setFetchSize( hbmQuery.getFetchSize() );
		query.setFlushMode( hbmQuery.getFlushMode() );
		query.setFetchSize( hbmQuery.getFetchSize() );
		query.setReadOnly( hbmQuery.isReadOnly() );
		query.setTimeout( hbmQuery.getTimeout() );

		JaxbSqlResultSetMapping implicitResultSetMapping = null;

		// JaxbQueryElement#content elements can be either the query or parameters
		for ( Object content : hbmQuery.getContent() ) {
			if ( content instanceof String ) {
				String s = (String) content;
				s = s.trim();
				query.setQuery( s );
			}
			else if ( content instanceof JAXBElement ) {
				final Object element = ( (JAXBElement<?>) content ).getValue();
				if ( element instanceof JaxbHbmQueryParamType ) {
					final JaxbHbmQueryParamType hbmQueryParam = (JaxbHbmQueryParamType) element;
					final JaxbQueryParamType queryParam = new JaxbQueryParamType();
					queryParam.setName( hbmQueryParam.getName() );
					queryParam.setType( hbmQueryParam.getType() );
					query.getQueryParam().add( queryParam );
				}
				else if ( element instanceof JaxbHbmNativeQueryScalarReturnType ) {
					if ( implicitResultSetMapping == null ) {
						implicitResultSetMapping = new JaxbSqlResultSetMapping();
						implicitResultSetMapping.setName( implicitResultSetMappingName );
						implicitResultSetMapping.setDescription(
								String.format(
										Locale.ROOT,
										"ResultSet mapping implicitly created for named native query `%s` during hbm.xml transformation",
										queryName
								)
						);
						ormRoot.getSqlResultSetMappings().add( implicitResultSetMapping );
					}
					implicitResultSetMapping.getColumnResult().add(
							transferScalarReturnElement(
									implicitResultSetMappingName,
									(JaxbHbmNativeQueryScalarReturnType) element
							)
					);
				}
				else if ( element instanceof JaxbHbmNativeQueryReturnType ) {
					if ( implicitResultSetMapping == null ) {
						implicitResultSetMapping = new JaxbSqlResultSetMapping();
						implicitResultSetMapping.setName( implicitResultSetMappingName );
						implicitResultSetMapping.setDescription(
								String.format(
										Locale.ROOT,
										"ResultSet mapping implicitly created for named native query `%s` during hbm.xml transformation",
										queryName
								)
						);
						ormRoot.getSqlResultSetMappings().add( implicitResultSetMapping );
					}
					implicitResultSetMapping.getEntityResult().add(
							transferEntityReturnElement(
									implicitResultSetMappingName,
									(JaxbHbmNativeQueryReturnType) element
							)
					);
				}
				else if ( element instanceof JaxbHbmNativeQueryCollectionLoadReturnType ) {
					handleUnsupportedContent(
							String.format(
									"Named native query [name=%s] contained a <collection-load/> element, " +
											"which is not supported for transformation",
									queryName
							)
					);
				}
				else if ( element instanceof JaxbHbmNativeQueryJoinReturnType ) {
					handleUnsupportedContent(
							String.format(
									"Named native query [name=%s] contained a <return-join/> element, " +
											"which is not supported for transformation",
									queryName
							)
					);
				}
				else if ( element instanceof JaxbHbmSynchronizeType ) {
					final JaxbHbmSynchronizeType hbmSynchronize = (JaxbHbmSynchronizeType) element;
					final JaxbSynchronizedTable synchronize = new JaxbSynchronizedTable();
					synchronize.setTable( hbmSynchronize.getTable() );
					query.getSynchronizations().add( synchronize );
				}
				else {
					// should never happen thanks to XSD
					handleUnsupportedContent(
							String.format(
									"Named native query [name=%s] contained an unexpected element type",
									queryName
							)
					);
				}
			}
		}
		
		return query;
	}

	private void transferDatabaseObjects() {
		if ( hbmXmlMapping.getDatabaseObject().isEmpty() ) {
			return;
		}

		JaxbLogger.JAXB_LOGGER.tracef(
				"Starting transformation of database-object mappings in `{}`",
				origin
		);


		for ( JaxbHbmAuxiliaryDatabaseObjectType hbmDatabaseObject : hbmXmlMapping.getDatabaseObject() ) {
			// NOTE: database-object does not define a name nor a good "identifier" for logging (exportable)

			final JaxbDatabaseObject databaseObject = new JaxbDatabaseObject();
			ormRoot.getDatabaseObjects().add( databaseObject );

			databaseObject.setCreate( hbmDatabaseObject.getCreate() );
			databaseObject.setDrop( hbmDatabaseObject.getDrop() );

			if ( ! hbmDatabaseObject.getDialectScope().isEmpty() ) {
				hbmDatabaseObject.getDialectScope().forEach( (hbmScope) -> {
					final JaxbDatabaseObjectScope scope = new JaxbDatabaseObjectScope();
					databaseObject.getDialectScopes().add( scope );

					scope.setName( hbmScope.getName() );
					// hbm.xml does not define min/max versions for its dialect-scope type
				} );
			}
		}
	}

	private void transferEntities() {
		// thoughts...
		//		1) We only need to transfer the "extends" attribute if the model is dynamic (map mode),
		//			otherwise it will be discovered via jandex
		//		2) ?? Have abstract hbm class mappings become MappedSuperclass mappings ??

		for ( JaxbHbmRootEntityType hbmClass : hbmXmlMapping.getClazz() ) {
			final JaxbEntity entity = new JaxbEntity();
			ormRoot.getEntities().add( entity );
			transferRootEntity( hbmClass, entity );
		}

		for ( JaxbHbmDiscriminatorSubclassEntityType hbmSubclass : hbmXmlMapping.getSubclass() ) {
			final JaxbEntity entity = new JaxbEntity();
			ormRoot.getEntities().add( entity );
			transferDiscriminatorSubclass( hbmSubclass, entity );
		}

		for ( JaxbHbmJoinedSubclassEntityType hbmSubclass : hbmXmlMapping.getJoinedSubclass() ) {
			final JaxbEntity entity = new JaxbEntity();
			ormRoot.getEntities().add( entity );
			transferJoinedSubclass( hbmSubclass, entity );
		}

		for ( JaxbHbmUnionSubclassEntityType hbmSubclass : hbmXmlMapping.getUnionSubclass() ) {
			final JaxbEntity entity = new JaxbEntity();
			ormRoot.getEntities().add( entity );
			transferUnionSubclass( hbmSubclass, entity );
		}

	}

	private String extractEntityName(EntityInfo entityInfo) {
		if ( entityInfo.getEntityName() != null ) {
			return entityInfo.getEntityName();
		}
		return entityInfo.getName();
	}

	private void transferRootEntity(JaxbHbmRootEntityType hbmClass, JaxbEntity entity) {
		TRANSFORMATION_LOGGER.debugf(
				"Starting transformation of root entity `%s` - `%s`",
				extractEntityName( hbmClass ),
				origin
		);

		transferBaseEntityInformation( hbmClass, entity );

		entity.setMutable( hbmClass.isMutable() );

		if ( hbmClass.getTable() != null ) {
			entity.setTable( new JaxbTable() );
			transfer( hbmClass::getTable, entity.getTable()::setName );
			transfer( hbmClass::getCatalog, entity.getTable()::setCatalog );
			transfer( hbmClass::getSchema, entity.getTable()::setSchema );
			transfer( hbmClass::getComment, entity.getTable()::setComment );
			transfer( hbmClass::getCheck, entity.getTable()::setCheck );
		}
		else {
			transfer( hbmClass::getSubselect, entity::setTableExpression );
		}

		for ( JaxbHbmSynchronizeType hbmSync : hbmClass.getSynchronize() ) {
			final JaxbSynchronizedTable sync = new JaxbSynchronizedTable();
			sync.setTable( hbmSync.getTable() );
			entity.getSynchronize().add( sync );
		}

		if ( hbmClass.getLoader() != null ) {
			entity.setLoader( new JaxbCustomLoader() );
			entity.getLoader().setQueryRef( hbmClass.getLoader().getQueryRef() );
		}
		if ( hbmClass.getSqlInsert() != null ) {
			entity.setSqlInsert( new JaxbCustomSql() );
			entity.getSqlInsert().setValue( hbmClass.getSqlInsert().getValue() );
			entity.getSqlInsert().setCheck( hbmClass.getSqlInsert().getCheck() );
			entity.getSqlInsert().setValue( hbmClass.getSqlInsert().getValue() );
		}
		if ( hbmClass.getSqlUpdate() != null ) {
			entity.setSqlUpdate( new JaxbCustomSql() );
			entity.getSqlUpdate().setValue( hbmClass.getSqlUpdate().getValue() );
			entity.getSqlUpdate().setCheck( hbmClass.getSqlUpdate().getCheck() );
			entity.getSqlUpdate().setValue( hbmClass.getSqlUpdate().getValue() );
		}
		if ( hbmClass.getSqlDelete() != null ) {
			entity.setSqlDelete( new JaxbCustomSql() );
			entity.getSqlDelete().setValue( hbmClass.getSqlDelete().getValue() );
			entity.getSqlDelete().setCheck( hbmClass.getSqlDelete().getCheck() );
			entity.getSqlDelete().setValue( hbmClass.getSqlDelete().getValue() );
		}
		entity.setRowid( hbmClass.getRowid() );
		entity.setWhere( hbmClass.getWhere() );

		if ( !hbmClass.getTuplizer().isEmpty() ) {
			if ( options.unsupportedFeatureHandling() == UnsupportedFeatureHandling.ERROR ) {
				throw new MappingException( "HBM transformation: Tuplizer not supported", origin );
			}

			TRANSFORMATION_LOGGER.logf(
					options.unsupportedFeatureHandling() == UnsupportedFeatureHandling.WARN
							? Logger.Level.WARN
							: Logger.Level.DEBUG,
					"Transformation of <tuplizer/> is not supported - `%s`",
					origin
			);

			return;
		}

		entity.setOptimisticLock( hbmClass.getOptimisticLock() );

		entity.setDiscriminatorValue( hbmClass.getDiscriminatorValue() );
		entity.setPolymorphism( convert( hbmClass.getPolymorphism() ) );

		transferDiscriminator( hbmClass, entity );
		transferAttributes( hbmClass, entity );

		if ( hbmClass.getCache() != null ) {
			transformEntityCaching( hbmClass, entity );
		}
		
		for ( JaxbHbmNamedQueryType hbmQuery : hbmClass.getQuery() ) {
			entity.getNamedQuery().add( transformNamedQuery( hbmQuery, entity.getName() + "." + hbmQuery.getName() ) );
		}
		
		for ( JaxbHbmNamedNativeQueryType hbmQuery : hbmClass.getSqlQuery() ) {
			entity.getNamedNativeQuery().add(
					transformNamedNativeQuery(
							hbmQuery,
							entity.getName() + "." + hbmQuery.getName()
					)
			);
		}
		
		for ( JaxbHbmFilterType hbmFilter : hbmClass.getFilter()) {
			entity.getFilter().add( convert( hbmFilter ) );
		}
		
		for ( JaxbHbmFetchProfileType hbmFetchProfile : hbmClass.getFetchProfile() ) {
			entity.getFetchProfile().add( transferFetchProfile( hbmFetchProfile ) );
		}
		
		for ( JaxbHbmJoinedSubclassEntityType hbmSubclass : hbmClass.getJoinedSubclass() ) {
			entity.setInheritance( new JaxbInheritance() );
			entity.getInheritance().setStrategy( InheritanceType.JOINED );

			final JaxbEntity subclassEntity = new JaxbEntity();
			ormRoot.getEntities().add( subclassEntity );
			transferJoinedSubclass( hbmSubclass, subclassEntity );
		}
		
		for (JaxbHbmUnionSubclassEntityType hbmSubclass : hbmClass.getUnionSubclass() ) {
			entity.setInheritance( new JaxbInheritance() );
			entity.getInheritance().setStrategy( InheritanceType.TABLE_PER_CLASS );

			final JaxbEntity subclassEntity = new JaxbEntity();
			ormRoot.getEntities().add( subclassEntity );
			transferUnionSubclass( hbmSubclass, subclassEntity );
		}
		
		for ( JaxbHbmDiscriminatorSubclassEntityType hbmSubclass : hbmClass.getSubclass() ) {
			final JaxbEntity subclassEntity = new JaxbEntity();
			ormRoot.getEntities().add( subclassEntity );
			transferDiscriminatorSubclass( hbmSubclass, subclassEntity );
		}
		
		for ( JaxbHbmNamedQueryType hbmQuery : hbmClass.getQuery() ) {
			// Tests implied this was the case...
			final String name = hbmClass.getName() + "." + hbmQuery.getName();
			ormRoot.getNamedQueries().add( transformNamedQuery( hbmQuery, name ) );
		}

		for ( JaxbHbmNamedNativeQueryType hbmQuery : hbmClass.getSqlQuery() ) {
			// Tests implied this was the case...
			final String name = hbmClass.getName() + "." + hbmQuery.getName();
			ormRoot.getNamedNativeQueries().add( transformNamedNativeQuery( hbmQuery, name ) );
		}
	}

	private void transformEntityCaching(JaxbHbmRootEntityType hbmClass, JaxbEntity entity) {
		entity.setCaching( new JaxbCaching() );
		entity.getCaching().setRegion( hbmClass.getCache().getRegion() );
		entity.getCaching().setAccess( hbmClass.getCache().getUsage() );
		entity.getCaching().setInclude( convert( hbmClass.getCache().getInclude() ) );
	}

	private JaxbCacheInclusionType convert(JaxbHbmCacheInclusionEnum hbmInclusion) {
		if ( hbmInclusion == null ) {
			return null;
		}

		if ( hbmInclusion == JaxbHbmCacheInclusionEnum.NON_LAZY ) {
			return JaxbCacheInclusionType.NON_LAZY;
		}

		if ( hbmInclusion == JaxbHbmCacheInclusionEnum.ALL ) {
			return JaxbCacheInclusionType.ALL;
		}

		throw new IllegalArgumentException( "Unrecognized cache-inclusions value : " + hbmInclusion );
	}

	private PolymorphismType convert(JaxbHbmPolymorphismEnum polymorphism) {
		if ( polymorphism == null ) {
			return null;
		}
		return polymorphism == JaxbHbmPolymorphismEnum.EXPLICIT ? PolymorphismType.EXPLICIT : PolymorphismType.IMPLICIT;
	}

	private void transferBaseEntityInformation(JaxbHbmEntityBaseDefinition hbmClass, JaxbEntity entity) {
		entity.setMetadataComplete( true );

		transfer( hbmClass::getEntityName, entity::setName );
		transfer( hbmClass::getName, entity::setClazz );

		if ( hbmClass instanceof Discriminatable ) {
			final Discriminatable discriminatable = (Discriminatable) hbmClass;
			transfer( discriminatable::getDiscriminatorValue, entity::setDiscriminatorValue );
		}

		// todo (6.1) : what to do with abstract? add abstract attribute to mapping xsd, or handle as mapped-superclass?
		if ( hbmClass.isAbstract() != null ) {
			handleUnsupported(
					"Transformation of abstract entity mappings is not supported : `%s` - `%s`",
					extractEntityName( hbmClass ),
					origin
			);
			return;
		}

		if ( hbmClass.getPersister() != null ) {
			handleUnsupported(
					"Transforming <persister/> mappings not supported - `%s` in `%s`",
					entity.getName(),
					origin
			);
			return;
		}

		transfer( hbmClass::isLazy, entity::setLazy );
		transfer( hbmClass::getProxy, entity::setProxy );

		transfer( hbmClass::getBatchSize, entity::setBatchSize );

		transfer( hbmClass::isDynamicInsert, entity::setDynamicInsert );
		transfer( hbmClass::isDynamicUpdate, entity::setDynamicUpdate );
		transfer( hbmClass::isSelectBeforeUpdate, entity::setSelectBeforeUpdate );

		transferToolingHints( hbmClass );
		transferResultSetMappings( entity.getName(), hbmClass );
	}

	private void transferResultSetMappings(String namePrefix, ResultSetMappingContainer container) {
		final List<JaxbHbmResultSetMappingType> resultSetMappings = container.getResultset();
		resultSetMappings.forEach( (hbmMapping) -> {
					final JaxbSqlResultSetMapping mapping = transformResultSetMapping( namePrefix, hbmMapping );
					ormRoot.getSqlResultSetMappings().add( mapping );
		} );
	}

	private void transferToolingHints(ToolingHintContainer container) {
		if ( CollectionHelper.isNotEmpty( container.getToolingHints() ) ) {
			handleUnsupported(
					"Transformation of <meta/> (tooling hint) is not supported - `%s`",
					origin
			);
		}
	}

	private void transferDiscriminatorSubclass(JaxbHbmDiscriminatorSubclassEntityType hbmSubclass, JaxbEntity subclassEntity) {
		TRANSFORMATION_LOGGER.debugf(
				"Starting transformation of subclass entity `%` - `%s`",
				extractEntityName( hbmSubclass ),
				origin
		);

		transferBaseEntityInformation( hbmSubclass, subclassEntity );

		transferEntityElementAttributes( hbmSubclass, subclassEntity );
	}

	private void transferJoinedSubclass(JaxbHbmJoinedSubclassEntityType hbmSubclass, JaxbEntity subclassEntity) {
		TRANSFORMATION_LOGGER.debugf(
				"Starting transformation of joined-subclass entity `%` - `%s`",
				extractEntityName( hbmSubclass ),
				origin
		);

		transferBaseEntityInformation( hbmSubclass, subclassEntity );
		transferEntityElementAttributes( hbmSubclass, subclassEntity );

		subclassEntity.setTable( new JaxbTable() );
		subclassEntity.getTable().setCatalog( hbmSubclass.getCatalog() );
		subclassEntity.getTable().setSchema( hbmSubclass.getSchema() );
		subclassEntity.getTable().setName( hbmSubclass.getTable() );
		subclassEntity.getTable().setComment( hbmSubclass.getComment() );
		subclassEntity.getTable().setCheck( hbmSubclass.getCheck() );
		
		if ( hbmSubclass.getKey() != null ) {
			final JaxbPrimaryKeyJoinColumn joinColumn = new JaxbPrimaryKeyJoinColumn();
			// TODO: multiple columns?
			joinColumn.setName( hbmSubclass.getKey().getColumnAttribute() );
			subclassEntity.getPrimaryKeyJoinColumn().add( joinColumn );
		}
		
		if ( !hbmSubclass.getJoinedSubclass().isEmpty() ) {
			subclassEntity.setInheritance( new JaxbInheritance() );
			subclassEntity.getInheritance().setStrategy( InheritanceType.JOINED );
			for ( JaxbHbmJoinedSubclassEntityType nestedHbmSubclass : hbmSubclass.getJoinedSubclass() ) {
				final JaxbEntity nestedSubclassEntity = new JaxbEntity();
				ormRoot.getEntities().add( nestedSubclassEntity );
				transferJoinedSubclass( nestedHbmSubclass, nestedSubclassEntity );
			}
		}
	}

	private void transferColumnsAndFormulas(
			ColumnAndFormulaSource source,
			ColumnAndFormulaTarget target,
			ColumnDefaults columnDefaults,
			String tableName) {
		if ( isNotEmpty( source.getFormulaAttribute() ) ) {
			target.addFormula( source.getFormulaAttribute() );
		}
		else if ( isNotEmpty( source.getColumnAttribute() ) ) {
			final TargetColumnAdapter column = target.makeColumnAdapter( columnDefaults );
			column.setName( source.getColumnAttribute() );
			column.setTable( tableName );
			target.addColumn( column );
		}
		else {
			for ( Serializable columnOrFormula : source.getColumnOrFormula() ) {
				if ( columnOrFormula instanceof String ) {
					target.addFormula( (String) columnOrFormula );
				}
				else {
					final JaxbHbmColumnType hbmColumn = (JaxbHbmColumnType) columnOrFormula;
					final TargetColumnAdapter column = target.makeColumnAdapter( columnDefaults );
					column.setTable( tableName );
					transferColumn( source.wrap( hbmColumn ), column );
					target.addColumn( column );
				}
			}
		}
	}

	private void transferColumn(
			SourceColumnAdapter source,
			TargetColumnAdapter target) {
		target.setName( source.getName() );

		target.setNullable( invert( source.isNotNull() ) );
		target.setUnique( source.isUnique() );

		target.setLength( source.getLength() );
		target.setScale( source.getScale() );
		target.setPrecision( source.getPrecision() );

		target.setComment( source.getComment() );

		target.setCheck( source.getCheck() );
		target.setDefault( source.getDefault() );

		target.setColumnDefinition( source.getSqlType() );

		target.setRead( source.getRead() );
		target.setWrite( source.getWrite() );

	}

	private void transferColumn(
			SourceColumnAdapter source,
			TargetColumnAdapter target,
			String tableName,
			ColumnDefaults columnDefaults) {
		target.setName( source.getName() );
		target.setTable( tableName );

		target.setNullable( invert( source.isNotNull(), columnDefaults.isNullable() ) );

		if ( source.getLength() != null ) {
			target.setLength( source.getLength() );
		}
		else {
			target.setLength( columnDefaults.getLength() );
		}

		if ( source.getScale() != null ) {
			target.setScale( source.getScale() );
		}
		else {
			target.setScale( columnDefaults.getScale() );
		}

		if ( source.getPrecision() != null ) {
			target.setPrecision( source.getPrecision() );
		}
		else {
			target.setPrecision( columnDefaults.getPrecision() );
		}

		if ( source.isUnique() != null ) {
			target.setUnique( source.isUnique() );
		}
		else {
			target.setUnique( columnDefaults.isUnique() );
		}

		target.setInsertable( columnDefaults.isInsertable() );
		target.setUpdatable( columnDefaults.isUpdateable() );

		target.setComment( source.getComment() );

		target.setCheck( source.getCheck() );
		target.setDefault( source.getDefault() );

		target.setColumnDefinition( source.getSqlType() );

		target.setRead( source.getRead() );
		target.setWrite( source.getWrite() );
	}

	private void transferDiscriminator(final JaxbHbmRootEntityType hbmClass, final JaxbEntity entity) {
		if ( hbmClass.getDiscriminator() == null ) {
			return;
		}

		if ( isNotEmpty( hbmClass.getDiscriminator().getColumnAttribute() ) ) {
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
			entity.getDiscriminatorColumn().setForceSelection( hbmClass.getDiscriminator().isForce() );
		}
	}

	private void transferAttributes(JaxbHbmRootEntityType source, JaxbEntity target) {
		transferEntityElementAttributes( source, target );

		transferIdentifier( source, target );
		transferNaturalIdentifiers( source, target );
		transferVersion( source, target );
		transferTimestamp( source, target );

		transferJoins( source, target );
	}


	private void transferEntityElementAttributes(EntityInfo hbmClass, JaxbEntity entity) {
		entity.setAttributes( new JaxbAttributes() );
		transferAttributes( hbmClass.getAttributes(), entity.getAttributes() );
	}

	private void transferAttributes(List hbmAttributeMappings, AttributesContainer attributes) {
		for ( Object hbmAttributeMapping : hbmAttributeMappings ) {
			if ( hbmAttributeMapping instanceof JaxbHbmBasicAttributeType ) {
				final JaxbHbmBasicAttributeType basic = (JaxbHbmBasicAttributeType) hbmAttributeMapping;
				attributes.getBasicAttributes().add( transformBasicAttribute( basic ) );
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmCompositeAttributeType ) {
				final JaxbHbmCompositeAttributeType hbmComponent = (JaxbHbmCompositeAttributeType) hbmAttributeMapping;
				ormRoot.getEmbeddables().add( convertEmbeddable( hbmComponent ) );
				attributes.getEmbeddedAttributes().add( transformEmbedded( hbmComponent ) );
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmPropertiesType ) {
				final JaxbHbmPropertiesType hbmProperties = (JaxbHbmPropertiesType) hbmAttributeMapping;
				transferAttributes( hbmProperties.getAttributes(), attributes );
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmDynamicComponentType ) {
				final String name = ( (JaxbHbmDynamicComponentType) hbmAttributeMapping ).getName();
				handleUnsupported(
						"<dynamic-component/> mappings not supported for transformation [name=%s]",
						name
				);
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmOneToOneType ) {
				final JaxbHbmOneToOneType o2o = (JaxbHbmOneToOneType) hbmAttributeMapping;
				transferOneToOne( o2o, attributes );
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmManyToOneType ) {
				final JaxbHbmManyToOneType m2o = (JaxbHbmManyToOneType) hbmAttributeMapping;
				attributes.getManyToOneAttributes().add( transformManyToOne( m2o ) );
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmAnyAssociationType ) {
				final JaxbHbmAnyAssociationType any = (JaxbHbmAnyAssociationType) hbmAttributeMapping;
				attributes.getDiscriminatedAssociations().add( transformAnyAttribute( any ) );

			}
			else if ( hbmAttributeMapping instanceof PluralAttributeInfo ) {
				final PluralAttributeInfo hbmCollection = (PluralAttributeInfo) hbmAttributeMapping;
				final CollectionAttribute target;

				if ( hbmCollection.getElement() != null
						|| hbmCollection.getCompositeElement() != null ) {
					target = new JaxbElementCollection();
					if ( hbmCollection.getElement() != null ) {
						transferElementInfo( hbmCollection, hbmCollection.getElement(), (JaxbElementCollection) target );
					}
					else {
						transferElementInfo( hbmCollection, hbmCollection.getCompositeElement(), (JaxbElementCollection) target );
					}
					attributes.getElementCollectionAttributes().add( (JaxbElementCollection) target );
				}
				else if ( hbmCollection.getOneToMany() != null ) {
					target = new JaxbOneToMany();
					transferOneToManyInfo( hbmCollection, hbmCollection.getOneToMany(), (JaxbOneToMany) target );
					attributes.getOneToManyAttributes().add( (JaxbOneToMany) target );
				}
				else if ( hbmCollection.getManyToMany() != null ) {
					target = new JaxbManyToMany();
					transferManyToManyInfo( hbmCollection, hbmCollection.getManyToMany(), (JaxbManyToMany) target );
					attributes.getManyToManyAttributes().add( (JaxbManyToMany) target );
				}
				else if ( hbmCollection.getManyToAny() != null ) {
					handleUnsupported( "<many-to-any/> not supported for transformation" );
				}
				else {
					throw new UnsupportedOperationException( "Unexpected node type - " + hbmCollection );
				}
			}
		}
	}

	private JaxbBasic transformBasicAttribute(final JaxbHbmBasicAttributeType hbmProp) {
		final JaxbBasic basic = new JaxbBasic();
		transferBasicAttribute( hbmProp, basic );
		return basic;
	}

	private void transferBasicAttribute(JaxbHbmBasicAttributeType hbmProp, JaxbBasic basic) {
		basic.setName( hbmProp.getName() );
		basic.setOptional( hbmProp.isNotNull() == null || !hbmProp.isNotNull() );
		basic.setFetch( FetchType.EAGER );
		basic.setAttributeAccessor( hbmProp.getAccess() );
		basic.setOptimisticLock( hbmProp.isOptimisticLock() );

//		if ( isNotEmpty( hbmProp.getTypeAttribute() ) ) {
//			basic.setType( new JaxbHbmType() );
//			basic.getType().setName( hbmProp.getTypeAttribute() );
//		}
//		else {
//			if ( hbmProp.getType() != null ) {
//				basic.setType( new JaxbHbmType() );
//				basic.getType().setName( hbmProp.getType().getName() );
//				for ( JaxbHbmConfigParameterType hbmParam : hbmProp.getType().getConfigParameters() ) {
//					final JaxbHbmParam param = new JaxbHbmParam();
//					param.setName( hbmParam.getName() );
//					param.setValue( hbmParam.getValue() );
//					basic.getType().getParam().add( param );
//				}
//			}
//		}

		transferColumnsAndFormulas(
				new ColumnAndFormulaSource() {
					@Override
					public String getColumnAttribute() {
						return hbmProp.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return hbmProp.getFormulaAttribute();
					}

					@Override
					public List<Serializable> getColumnOrFormula() {
						return hbmProp.getColumnOrFormula();
					}

					@Override
					public SourceColumnAdapter wrap(Serializable column) {
						return new SourceColumnAdapterJaxbHbmColumnType( (JaxbHbmColumnType) column );
					}
				},
				new ColumnAndFormulaTarget() {
					@Override
					public TargetColumnAdapter makeColumnAdapter(ColumnDefaults columnDefaults) {
						return new TargetColumnAdapterJaxbColumn( columnDefaults );
					}

					@Override
					public void addColumn(TargetColumnAdapter column) {
						basic.setColumn( ( (TargetColumnAdapterJaxbColumn) column ).getTargetColumn() );
					}

					@Override
					public void addFormula(String formula) {
						basic.setFormula( formula );
					}
				},
				new ColumnDefaults() {
					@Override
					public Boolean isNullable() {
						return invert( hbmProp.isNotNull() );
					}

					@Override
					public Integer getLength() {
						return hbmProp.getLength();
					}

					@Override
					public Integer getScale() {
						return isNotEmpty( hbmProp.getScale() )
								? Integer.parseInt( hbmProp.getScale() )
								: null;
					}

					@Override
					public Integer getPrecision() {
						return isNotEmpty( hbmProp.getPrecision() )
								? Integer.parseInt( hbmProp.getPrecision() )
								: null;
					}

					@Override
					public Boolean isUnique() {
						return hbmProp.isUnique();
					}

					@Override
					public Boolean isInsertable() {
						return hbmProp.isInsert();
					}

					@Override
					public Boolean isUpdateable() {
						return hbmProp.isUpdate();
					}
				},
				// todo : need to push the table name down into this method to pass along
				// todo : need to push the table name down into this method to pass along
				null
		);
	}

	private JaxbEmbeddable convertEmbeddable(JaxbHbmCompositeAttributeType hbmComponent) {
		final JaxbEmbeddable embeddable = new JaxbEmbeddable();
		embeddable.setMetadataComplete( true );
		embeddable.setClazz( determineEmbeddableName( hbmComponent ) );

		embeddable.setAttributes( new JaxbEmbeddableAttributes() );
		transferAttributes( hbmComponent.getAttributes(), embeddable.getAttributes() );

		return embeddable;
	}

	private int counter = 1;

	private String determineEmbeddableName(JaxbHbmCompositeAttributeType hbmComponent) {
		if ( StringHelper.isNotEmpty( hbmComponent.getClazz() ) ) {
			return hbmComponent.getClazz();
		}
		return hbmComponent.getName() + "_" + counter++;
	}

	private String determineEmbeddableName(JaxbHbmCompositeCollectionElementType hbmComponent, PluralAttributeInfo hbmCollection) {
		if ( StringHelper.isNotEmpty( hbmComponent.getClazz() ) ) {
			return hbmComponent.getClazz();
		}
		return hbmCollection.getName() + "_" + counter++;
	}

	private JaxbEmbedded transformEmbedded(JaxbHbmCompositeAttributeType hbmComponent) {
		final JaxbEmbedded embedded = new JaxbEmbedded();
		embedded.setName( hbmComponent.getName() );
		embedded.setAttributeAccessor( hbmComponent.getAccess() );
		return embedded;
	}

	private void transferOneToOne(JaxbHbmOneToOneType hbmOneToOne, AttributesContainer attributes) {
		if ( !hbmOneToOne.getFormula().isEmpty() || !StringHelper.isEmpty( hbmOneToOne.getFormulaAttribute() ) ) {
			handleUnsupported(
					"Transformation of formulas within one-to-ones are not supported - `%s`",
					origin
			);
			return;
		}

		final JaxbOneToOne oneToOne = new JaxbOneToOne();
		oneToOne.setAttributeAccessor( hbmOneToOne.getAccess() );
		oneToOne.setCascade( convertCascadeType( hbmOneToOne.getCascade() ) );
		oneToOne.setOrphanRemoval( isOrphanRemoval( hbmOneToOne.getCascade() ) );
		oneToOne.setForeignKey( new JaxbForeignKey() );
		oneToOne.getForeignKey().setName( hbmOneToOne.getForeignKey() );
		if (! StringHelper.isEmpty( hbmOneToOne.getPropertyRef() ) ) {
			final JaxbJoinColumn joinColumn = new JaxbJoinColumn();
			joinColumn.setReferencedColumnName( hbmOneToOne.getPropertyRef() );
			oneToOne.getJoinColumn().add( joinColumn );
		}
		oneToOne.setName( hbmOneToOne.getName() );
		if ( isNotEmpty( hbmOneToOne.getEntityName() ) ) {
			oneToOne.setTargetEntity( hbmOneToOne.getEntityName() );
		}
		else {
			oneToOne.setTargetEntity( hbmOneToOne.getClazz() );
		}

		transferFetchable( hbmOneToOne.getLazy(), hbmOneToOne.getFetch(), hbmOneToOne.getOuterJoin(), hbmOneToOne.isConstrained(), oneToOne );

		attributes.getOneToOneAttributes().add( oneToOne );
	}

	private JaxbManyToOne transformManyToOne(final JaxbHbmManyToOneType hbmNode) {
		if ( hbmNode.getNotFound() != JaxbHbmNotFoundEnum.EXCEPTION ) {
			handleUnsupported( "`not-found` not supported for transformation" );
		}

		final JaxbManyToOne m2o = new JaxbManyToOne();
		m2o.setAttributeAccessor( hbmNode.getAccess() );
		m2o.setCascade( convertCascadeType( hbmNode.getCascade() ) );
		m2o.setForeignKey( new JaxbForeignKey() );
		m2o.getForeignKey().setName( hbmNode.getForeignKey() );

		transferColumnsAndFormulas(
				new ColumnAndFormulaSource() {
					@Override
					public String getColumnAttribute() {
						return hbmNode.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return hbmNode.getFormulaAttribute();
					}

					@Override
					public List<Serializable> getColumnOrFormula() {
						return hbmNode.getColumnOrFormula();
					}

					@Override
					public SourceColumnAdapter wrap(Serializable column) {
						return new SourceColumnAdapterJaxbHbmColumnType( (JaxbHbmColumnType) column );
					}
				},
				new ColumnAndFormulaTarget() {
					@Override
					public TargetColumnAdapter makeColumnAdapter(ColumnDefaults columnDefaults) {
						return new TargetColumnAdapterJaxbJoinColumn( columnDefaults );
					}

					@Override
					public void addColumn(TargetColumnAdapter column) {
						m2o.getJoinColumn().add( ( (TargetColumnAdapterJaxbJoinColumn) column ).getTargetColumn() );
					}

					@Override
					public void addFormula(String formula) {
						handleUnsupportedContent(
								"<many-to-one/> [name=" + hbmNode.getName() + "] specified formula [" + formula +
										"] which is not supported for transformation; skipping"
						);
					}
				},
				ColumnDefaultsBasicImpl.INSTANCE,
				null
		);

		m2o.setName( hbmNode.getName() );
		m2o.setOptional( hbmNode.isNotNull() == null || !hbmNode.isNotNull() );
		if ( isNotEmpty( hbmNode.getEntityName() ) ) {
			m2o.setTargetEntity( hbmNode.getEntityName() );
		}
		else {
			m2o.setTargetEntity( hbmNode.getClazz() );
		}
		transferFetchable( hbmNode.getLazy(), hbmNode.getFetch(), hbmNode.getOuterJoin(), null, m2o );
		return m2o;
	}


	private JaxbHbmAnyMapping transformAnyAttribute(JaxbHbmAnyAssociationType source) {
		final JaxbHbmAnyMapping target = new JaxbHbmAnyMapping();

		target.setName( source.getName() );
		target.setAttributeAccessor( source.getAccess() );
		target.setOptimisticLock( source.isOptimisticLock() );

		// todo : cascade
		// todo : discriminator column
		// todo : key column

		target.setDiscriminator( new JaxbHbmAnyDiscriminator() );
		source.getMetaValue().forEach( (sourceMapping) -> {
			final JaxbHbmAnyDiscriminatorValueMapping mapping = new JaxbHbmAnyDiscriminatorValueMapping();
			mapping.setDiscriminatorValue( sourceMapping.getValue() );
			mapping.setCorrespondingEntityName( sourceMapping.getClazz() );
			target.getDiscriminator().getValueMappings().add( mapping );
		} );

		target.setKey( new JaxbHbmAnyKey() );

		return target;
	}


	private void transferCollectionTable(
			final PluralAttributeInfo source,
			final JaxbElementCollection target) {
		target.setCollectionTable( new JaxbCollectionTable() );

		if ( isNotEmpty( source.getTable() ) ) {
			target.getCollectionTable().setName( source.getTable() );
			target.getCollectionTable().setCatalog( source.getCatalog() );
			target.getCollectionTable().setSchema( source.getSchema() );
		}

		transferColumnsAndFormulas(
				new ColumnAndFormulaSource() {
					@Override
					public String getColumnAttribute() {
						return source.getKey().getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return null;
					}

					@Override
					public List<Serializable> getColumnOrFormula() {
						return new ArrayList<Serializable>( source.getKey().getColumn() );
					}

					@Override
					public SourceColumnAdapter wrap(Serializable column) {
						return new SourceColumnAdapterJaxbHbmColumnType( (JaxbHbmColumnType) column );
					}
				},
				new ColumnAndFormulaTarget() {
					@Override
					public TargetColumnAdapter makeColumnAdapter(ColumnDefaults columnDefaults) {
						return new TargetColumnAdapterJaxbJoinColumn( columnDefaults );
					}

					@Override
					public void addColumn(TargetColumnAdapter column) {

					}

					@Override
					public void addFormula(String formula) {
						handleUnsupportedContent(
								"formula as part of element-collection key is not supported for transformation; skipping"
						);
					}
				},
				ColumnDefaultsBasicImpl.INSTANCE,
				source.getTable()

		);

		if ( isNotEmpty( source.getKey().getPropertyRef() ) ) {
			handleUnsupportedContent(
					"Foreign-key (<key/>) for persistent collection (name=" + source.getName() +
							") specified property-ref which is not supported for transformation; " +
							"transformed <join-column/> will need manual adjustment of referenced-column-name"
			);
		}
	}


	private void transferCollectionBasicInfo(PluralAttributeInfo source, CollectionAttribute target) {
		target.setName( source.getName() );
		target.setAttributeAccessor( source.getAccess() );
		target.setFetchMode( convert( source.getFetch() ) );

		if ( source instanceof JaxbHbmSetType ) {
			final JaxbHbmSetType set = (JaxbHbmSetType) source;
			target.setSort( set.getSort() );
			target.setOrderBy( set.getOrderBy() );
		}
		else if ( source instanceof JaxbHbmMapType ) {
			final JaxbHbmMapType map = (JaxbHbmMapType) source;
			target.setSort( map.getSort() );
			target.setOrderBy( map.getOrderBy() );

			transferMapKey( (JaxbHbmMapType) source, target );
		}
		else if ( source instanceof JaxbHbmIdBagCollectionType ) {
			handleUnsupported( "collection-id is not supported for transformation" );
		}
		else if ( source instanceof JaxbHbmListType ) {
			transferListIndex(
					( (JaxbHbmListType) source ).getIndex(),
					( (JaxbHbmListType) source ).getListIndex(),
					target
			);
		}
		else if ( source instanceof JaxbHbmArrayType ) {
			transferListIndex(
					( (JaxbHbmArrayType) source ).getIndex(),
					( (JaxbHbmArrayType) source ).getListIndex(),
					target
			);
		}
		else if ( source instanceof JaxbHbmPrimitiveArrayType ) {
			transferListIndex(
					( (JaxbHbmPrimitiveArrayType) source ).getIndex(),
					( (JaxbHbmPrimitiveArrayType) source ).getListIndex(),
					target
			);
		}
	}

	private void transferListIndex(
			JaxbHbmIndexType index,
			JaxbHbmListIndexType listIndex,
			CollectionAttribute target) {
		final JaxbOrderColumn orderColumn = new JaxbOrderColumn();
		target.setOrderColumn( orderColumn );

		if ( index != null ) {
			// todo : base on order-column
			if ( isNotEmpty( index.getColumnAttribute() ) ) {
				orderColumn.setName( index.getColumnAttribute() );
			}
			else if ( index.getColumn().size() == 1 ) {
				final JaxbHbmColumnType hbmColumn = index.getColumn().get( 0 );
				orderColumn.setName( hbmColumn.getName() );
				orderColumn.setNullable( invert( hbmColumn.isNotNull() ) );
				orderColumn.setColumnDefinition( hbmColumn.getSqlType() );
			}
		}
		else if ( listIndex != null ) {
			// todo : base on order-column
			if ( isNotEmpty( listIndex.getColumnAttribute() ) ) {
				orderColumn.setName( listIndex.getColumnAttribute() );
			}
			else if ( listIndex.getColumn() != null ) {
				orderColumn.setName( listIndex.getColumn().getName() );
				orderColumn.setNullable( invert( listIndex.getColumn().isNotNull() ) );
				orderColumn.setColumnDefinition( listIndex.getColumn().getSqlType() );
			}
		}
	}

	private void transferMapKey(JaxbHbmMapType source, CollectionAttribute target) {
		if ( source.getIndex() != null ) {
			final JaxbMapKeyColumn mapKey = new JaxbMapKeyColumn();
			// TODO: multiple columns?
			mapKey.setName( source.getIndex().getColumnAttribute() );
			target.setMapKeyColumn( mapKey );
		}
		else if ( source.getMapKey() != null ) {
			if ( ! StringHelper.isEmpty( source.getMapKey().getFormulaAttribute() ) ) {
				handleUnsupported(
						"Transformation of formulas within map-keys are not supported - `%s`",
						origin
				);
				return;
			}

			final JaxbMapKeyColumn mapKey = new JaxbMapKeyColumn();
			mapKey.setName( source.getMapKey().getColumnAttribute() );
			target.setMapKeyColumn( mapKey );
		}
	}

	private Boolean invert(Boolean value) {
		return invert( value, null );
	}

	private Boolean invert(Boolean value, Boolean defaultValue) {
		if ( value == null ) {
			return defaultValue;
		}
		return !value;
	}

	private JaxbPluralFetchMode convert(JaxbHbmFetchStyleWithSubselectEnum fetch) {
		if ( fetch != null ) {
			switch ( fetch ) {
				case SELECT: {
					return JaxbPluralFetchMode.SELECT;
				}
				case JOIN: {
					return JaxbPluralFetchMode.JOIN;
				}
				case SUBSELECT: {
					return JaxbPluralFetchMode.SUBSELECT;
				}
			}
		}

		return null;
	}


	private void transferElementInfo(
			PluralAttributeInfo hbmCollection,
			JaxbHbmBasicCollectionElementType element,
			JaxbElementCollection target) {
		transferCollectionBasicInfo( hbmCollection, target );
		transferCollectionTable( hbmCollection, target );

		transferColumnsAndFormulas(
				new ColumnAndFormulaSource() {
					@Override
					public String getColumnAttribute() {
						return element.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return element.getFormulaAttribute();
					}

					@Override
					public List<Serializable> getColumnOrFormula() {
						return element.getColumnOrFormula();
					}

					@Override
					public SourceColumnAdapter wrap(Serializable column) {
						return new SourceColumnAdapterJaxbHbmColumnType( (JaxbHbmColumnType) column );
					}
				},
				new ColumnAndFormulaTarget() {
					@Override
					public TargetColumnAdapter makeColumnAdapter(ColumnDefaults columnDefaults) {
						return new TargetColumnAdapterJaxbColumn( columnDefaults );
					}

					@Override
					public void addColumn(TargetColumnAdapter column) {
						target.setColumn( ( (TargetColumnAdapterJaxbColumn) column ).getTargetColumn() );
					}

					@Override
					public void addFormula(String formula) {
						target.setFormula( formula );
					}
				},
				ColumnDefaultsBasicImpl.INSTANCE,
				null
		);
	}

	private void transferElementInfo(
			PluralAttributeInfo hbmCollection,
			JaxbHbmCompositeCollectionElementType compositeElement,
			JaxbElementCollection target) {
		transferCollectionBasicInfo( hbmCollection, target );
		transferCollectionTable( hbmCollection, target );

		final String embeddableName = determineEmbeddableName( compositeElement, hbmCollection );

		target.setTargetClass( embeddableName );

		// todo : account for same embeddable used multiple times
		final JaxbEmbeddable embeddable = new JaxbEmbeddable();
		embeddable.setClazz( embeddableName );
		embeddable.setAttributes( new JaxbEmbeddableAttributes() );
		transferAttributes(
				compositeElement.getAttributes(),
				embeddable.getAttributes()
		);
		ormRoot.getEmbeddables().add( embeddable );
	}

	private void transferOneToManyInfo(
			PluralAttributeInfo hbmAttributeInfo,
			JaxbHbmOneToManyCollectionElementType hbmOneToMany,
			JaxbOneToMany target) {
		if ( StringHelper.isNotEmpty( hbmAttributeInfo.getCollectionType() ) ) {
			handleUnsupported( "Collection-type is not supported for transformation" );
		}
		if ( CollectionHelper.isNotEmpty( hbmAttributeInfo.getFilter() ) ) {
			handleUnsupported( "Filters are not supported for transformation" );
		}
		if ( StringHelper.isNotEmpty( hbmAttributeInfo.getWhere() ) ) {
			handleUnsupported( "SQL restrictions are not supported for transformation" );
		}
		if ( hbmAttributeInfo.getSqlInsert() != null ) {
			handleUnsupported( "<sql-insert/> not supported for transformation" );
		}
		if ( hbmAttributeInfo.getSqlUpdate() != null ) {
			handleUnsupported( "<sql-update/> not supported for transformation" );
		}
		if ( hbmAttributeInfo.getSqlDelete() != null ) {
			handleUnsupported( "<sql-delete/> not supported for transformation" );
		}
		if ( hbmAttributeInfo.getSqlDeleteAll() != null ) {
			handleUnsupported( "<sql-delete-all/> not supported for transformation" );
		}

		if ( hbmOneToMany.isEmbedXml() != null ) {
			handleUnsupported( "`embed-xml` not supported for transformation" );
		}
		if ( !(hbmOneToMany.getNode() == null || hbmOneToMany.getNode().isBlank() ) ) {
			handleUnsupported( "`node` not supported for transformation" );
		}
		if ( hbmOneToMany.getNotFound() != JaxbHbmNotFoundEnum.EXCEPTION ) {
			handleUnsupported( "`not-found` not supported for transformation" );
		}

		transferCollectionBasicInfo( hbmAttributeInfo, target );
		target.setTargetEntity( StringHelper.isNotEmpty( hbmOneToMany.getClazz() ) ? hbmOneToMany.getClazz() : hbmOneToMany.getEntityName() );

		// columns + formulas --> do we need similar for lists, sets, etc?
		// ~~> hbmListNode.getElement()
		//transferCollectionTable( source, oneToMany )

		final JaxbHbmKeyType key = hbmAttributeInfo.getKey();
		if ( key != null ) {
			target.setForeignKey( new JaxbForeignKey() );
			if ( StringHelper.isNotEmpty( key.getForeignKey() ) ) {
				target.getForeignKey().setName( key.getForeignKey() );
			}
//			oneToMany.setCollectionId( ? );
//			oneToMany.setMappedBy( ?? );
//			oneToMany.setOnDelete( ?? );
		}

		target.setOrphanRemoval( isOrphanRemoval( hbmAttributeInfo.getCascade() ) );
		target.setCascade( convertCascadeType( hbmAttributeInfo.getCascade() ) );
	}

	private void transferManyToManyInfo(
			PluralAttributeInfo hbmCollection,
			JaxbHbmManyToManyCollectionElementType manyToMany,
			JaxbManyToMany target) {
		if ( StringHelper.isNotEmpty( hbmCollection.getCollectionType() ) ) {
			handleUnsupported( "Collection-type is not supported for transformation" );
		}
		if ( CollectionHelper.isNotEmpty( hbmCollection.getFilter() ) ) {
			handleUnsupported( "Filters are not supported for transformation" );
		}
		if ( StringHelper.isNotEmpty( hbmCollection.getWhere() ) ) {
			handleUnsupported( "SQL restrictions are not supported for transformation" );
		}
		if ( hbmCollection.getSqlInsert() != null ) {
			handleUnsupported( "<sql-insert/> not supported for transformation" );
		}
		if ( hbmCollection.getSqlUpdate() != null ) {
			handleUnsupported( "<sql-update/> not supported for transformation" );
		}
		if ( hbmCollection.getSqlDelete() != null ) {
			handleUnsupported( "<sql-delete/> not supported for transformation" );
		}
		if ( hbmCollection.getSqlDeleteAll() != null ) {
			handleUnsupported( "<sql-delete-all/> not supported for transformation" );
		}

		if ( manyToMany.isEmbedXml() != null ) {
			handleUnsupported( "`embed-xml` not supported for transformation" );
		}
		if ( StringHelper.isNotEmpty( manyToMany.getNode() ) ) {
			handleUnsupported( "`node` not supported for transformation" );
		}
		if ( manyToMany.getNotFound() != JaxbHbmNotFoundEnum.EXCEPTION ) {
			handleUnsupported( "`not-found` not supported for transformation" );
		}

		transferCollectionBasicInfo( hbmCollection, target );
		target.setTargetEntity( StringHelper.isNotEmpty( manyToMany.getClazz() ) ? manyToMany.getClazz() : manyToMany.getEntityName() );
	}

	private void transferIdentifier(JaxbHbmRootEntityType source, JaxbEntity target) {
		if ( source.getId() != null ) {
			target.getAttributes().getId().add( convertSimpleId( source.getId() ) );
		}
		else {
			final JaxbHbmCompositeIdType hbmCompositeId = source.getCompositeId();
			assert hbmCompositeId != null;

			final boolean isAggregate;
			if ( isNotEmpty( hbmCompositeId.getClazz() ) ) {
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
				target.getAttributes().setEmbeddedId( new JaxbEmbeddedId() );
				target.getAttributes().getEmbeddedId().setName( hbmCompositeId.getName() );
				target.getAttributes().getEmbeddedId().setAttributeAccessor( hbmCompositeId.getAccess() );

				final JaxbEmbeddable embeddable = new JaxbEmbeddable();
				embeddable.setClazz( hbmCompositeId.getClazz() );
				embeddable.setAttributes( new JaxbEmbeddableAttributes() );
				for ( Object hbmCompositeAttribute : hbmCompositeId.getKeyPropertyOrKeyManyToOne() ) {
					if ( hbmCompositeAttribute instanceof JaxbHbmCompositeKeyBasicAttributeType ) {
						final JaxbHbmCompositeKeyBasicAttributeType keyProp = (JaxbHbmCompositeKeyBasicAttributeType) hbmCompositeAttribute;
						final JaxbBasic basic = new JaxbBasic();
						basic.setName( keyProp.getName() );
						basic.setAttributeAccessor( keyProp.getAccess() );
						if ( isNotEmpty( keyProp.getColumnAttribute() ) ) {
							final JaxbColumn column = new JaxbColumn();
							column.setName( keyProp.getColumnAttribute() );
							basic.setColumn( column );
						}
						else {
							for ( JaxbHbmColumnType hbmColumn : keyProp.getColumn() ) {
								final JaxbColumn column = new JaxbColumn();
								transferColumn(
										new SourceColumnAdapterJaxbHbmColumnType( hbmColumn ),
										new TargetColumnAdapterJaxbColumn( column, ColumnDefaultsInsertableNonUpdateableImpl.INSTANCE )
								);
								basic.setColumn( column );
							}
						}
						embeddable.getAttributes().getBasicAttributes().add( basic );
					}
					else {
						final JaxbHbmCompositeKeyManyToOneType keyManyToOne = (JaxbHbmCompositeKeyManyToOneType) hbmCompositeAttribute;
						final JaxbManyToOne manyToOne = transferManyToOneAttribute( keyManyToOne );
						embeddable.getAttributes().getManyToOneAttributes().add( manyToOne );
					}
				}
				ormRoot.getEmbeddables().add( embeddable );
			}
			else {
				final JaxbIdClass idClass = new JaxbIdClass();
				idClass.setClazz( hbmCompositeId.getClazz() );
				target.setIdClass( idClass );
				for ( Object hbmCompositeAttribute : hbmCompositeId.getKeyPropertyOrKeyManyToOne() ) {
					if ( hbmCompositeAttribute instanceof JaxbHbmCompositeKeyBasicAttributeType ) {
						final JaxbHbmCompositeKeyBasicAttributeType keyProp = (JaxbHbmCompositeKeyBasicAttributeType) hbmCompositeAttribute;
						final JaxbId id = new JaxbId();
						id.setName( keyProp.getName() );
						id.setAttributeAccessor( keyProp.getAccess() );
						if ( isNotEmpty( keyProp.getColumnAttribute() ) ) {
							final JaxbColumn column = new JaxbColumn();
							column.setName( keyProp.getColumnAttribute() );
							id.setColumn( column );
						}
						else {
							if ( keyProp.getColumn().size() == 1 ) {
								id.setColumn( new JaxbColumn() );
								transferColumn(
										new SourceColumnAdapterJaxbHbmColumnType( keyProp.getColumn().get( 0 ) ),
										new TargetColumnAdapterJaxbColumn( id.getColumn(), ColumnDefaultsInsertableNonUpdateableImpl.INSTANCE )
								);
							}
						}
						target.getAttributes().getId().add( id );
					}
					else {
						final JaxbHbmCompositeKeyManyToOneType keyManyToOne = (JaxbHbmCompositeKeyManyToOneType) hbmCompositeAttribute;
						final JaxbManyToOne manyToOne = transferManyToOneAttribute( keyManyToOne );
						target.getAttributes().getManyToOneAttributes().add( manyToOne );
					}
				}
			}
		}
	}


	private JaxbId convertSimpleId(JaxbHbmSimpleIdType source) {
		final JaxbId target = new JaxbId();
		target.setName( source.getName() );
		target.setAttributeAccessor( source.getAccess() );

//		// this depends on how we want to model "generic generators" in the mapping xsd.  this might
//		// mean "inline" like we do in hbm.xml or using separate generator declarations like orm.xml
//		if ( source.getGenerator() != null ) {
//			final JaxbGenericIdGenerator generator = new JaxbGenericIdGenerator();
//			generator.setStrategy( source.getGenerator().getClazz() );
//			for ( JaxbHbmConfigParameterType param : source.getGenerator().getConfigParameters() ) {
//				JaxbHbmParam hbmParam = new JaxbHbmParam();
//				hbmParam.setName( param.getName() );
//				hbmParam.setValue( param.getValue() );
//				generator.getParam().add( hbmParam );
//			}
//			target.getGeneratedValue().setGenerator( generator );
//		}

//		if ( isNotEmpty( source.getTypeAttribute() ) ) {
//			target.setType( new JaxbHbmType() );
//			target.getType().setName( source.getTypeAttribute() );
//		}
//		else {
//			if ( source.getType() != null ) {
//				target.setType( new JaxbHbmType() );
//				target.getType().setName( source.getType().getName() );
//				for ( JaxbHbmConfigParameterType hbmParam : source.getType().getConfigParameters() ) {
//					final JaxbHbmParam param = new JaxbHbmParam();
//					param.setName( hbmParam.getName() );
//					param.setValue( hbmParam.getValue() );
//					target.getType().getParam().add( param );
//				}
//			}
//		}

		target.setUnsavedValue( source.getUnsavedValue() );

		if ( isNotEmpty( source.getColumnAttribute() ) ) {
			target.setColumn( new JaxbColumn() );
			target.getColumn().setName( source.getColumnAttribute() );
		}
		else {
			if ( source.getColumn() != null ) {
				if ( source.getColumn().size() == 1 ) {
					target.setColumn( new JaxbColumn() );
					transferColumn(
							new SourceColumnAdapterJaxbHbmColumnType( source.getColumn().get( 0 ) ),
							new TargetColumnAdapterJaxbColumn( target.getColumn(), ColumnDefaultsInsertableNonUpdateableImpl.INSTANCE )
					);
				}
			}
		}

		return target;
	}


	private void transferNaturalIdentifiers(JaxbHbmRootEntityType source, JaxbEntity target) {
		if ( source.getNaturalId() == null ) {
			return;
		}

		final JaxbNaturalId naturalId = new JaxbNaturalId();
		transferAttributes(
				source.getNaturalId().getAttributes(),
				new AttributesContainer() {
					@Override
					public List<JaxbBasic> getBasicAttributes() {
						return naturalId.getBasic();
					}

					@Override
					public List<JaxbEmbedded> getEmbeddedAttributes() {
						return naturalId.getEmbedded();
					}

					@Override
					public List<JaxbOneToOne> getOneToOneAttributes() {
						return null;
					}

					@Override
					public List<JaxbManyToOne> getManyToOneAttributes() {
						return null;
					}

					@Override
					public List<JaxbHbmAnyMapping> getDiscriminatedAssociations() {
						return null;
					}

					@Override
					public List<JaxbElementCollection> getElementCollectionAttributes() {
						return null;
					}

					@Override
					public List<JaxbOneToMany> getOneToManyAttributes() {
						return null;
					}

					@Override
					public List<JaxbManyToMany> getManyToManyAttributes() {
						return null;
					}

					@Override
					public List<JaxbHbmManyToAny> getPluralDiscriminatedAssociations() {
						return null;
					}

					@Override
					public List<JaxbTransient> getTransients() {
						return null;
					}
				}
		);

		naturalId.setMutable( source.getNaturalId().isMutable() );
		target.getAttributes().setNaturalId( naturalId );
	}

	private void transferVersion(JaxbHbmRootEntityType source, JaxbEntity target) {
		final JaxbHbmVersionAttributeType hbmVersion = source.getVersion();
		if ( hbmVersion != null ) {
			final JaxbVersion version = new JaxbVersion();
			version.setName( hbmVersion.getName() );
			if ( isNotEmpty( hbmVersion.getColumnAttribute() ) ) {
				version.setColumn( new JaxbColumn() );
				version.getColumn().setName( hbmVersion.getColumnAttribute() );
			}
			target.getAttributes().getVersion().add( version );
		}
	}

	private void transferTimestamp(JaxbHbmRootEntityType source, JaxbEntity target) {
		final JaxbHbmTimestampAttributeType hbmTimestamp = source.getTimestamp();
		if ( hbmTimestamp != null ) {
			final JaxbVersion version = new JaxbVersion();
			version.setName( hbmTimestamp.getName() );
			// TODO: multiple columns?
			if ( isNotEmpty( hbmTimestamp.getColumnAttribute() ) ) {
				version.setColumn( new JaxbColumn() );
				version.getColumn().setName( hbmTimestamp.getColumnAttribute() );
			}
			version.setTemporal( TemporalType.TIMESTAMP );
			target.getAttributes().getVersion().add( version );
		}
	}

	private void transferJoins(JaxbHbmRootEntityType source, JaxbEntity target) {
		for ( JaxbHbmSecondaryTableType hbmJoin : source.getJoin() ) {
			if ( !hbmJoin.isInverse() ) {
				final JaxbSecondaryTable secondaryTable = new JaxbSecondaryTable();
				secondaryTable.setCatalog( hbmJoin.getCatalog() );
				secondaryTable.setComment( hbmJoin.getComment() );
				secondaryTable.setName( hbmJoin.getTable() );
				secondaryTable.setSchema( hbmJoin.getSchema() );
				secondaryTable.setOptional( hbmJoin.isOptional() );
				if ( hbmJoin.getKey() != null ) {
					final JaxbPrimaryKeyJoinColumn joinColumn = new JaxbPrimaryKeyJoinColumn();
					joinColumn.setName( hbmJoin.getKey().getColumnAttribute() );
					secondaryTable.getPrimaryKeyJoinColumn().add( joinColumn );
				}
				target.getSecondaryTable().add( secondaryTable );
			}
			
			for ( Serializable attributeMapping : hbmJoin.getAttributes() ) {
				if ( attributeMapping instanceof JaxbHbmBasicAttributeType ) {
					final JaxbBasic prop = transformBasicAttribute( (JaxbHbmBasicAttributeType) attributeMapping );
					if ( prop.getColumn() != null ) {
						prop.getColumn().setTable( hbmJoin.getTable() );
					}
					target.getAttributes().getBasicAttributes().add( prop );
				}
				else if ( attributeMapping instanceof JaxbHbmCompositeAttributeType ) {
					throw new MappingException(
							"transformation of <component/> as part of <join/> (secondary-table) not yet implemented",
							origin
					);
				}
				else if ( attributeMapping instanceof JaxbHbmManyToOneType ) {
					throw new MappingException(
							"transformation of <many-to-one/> as part of <join/> (secondary-table) not yet implemented",
							origin
					);
				}
				else if ( attributeMapping instanceof JaxbHbmAnyAssociationType ) {
					throw new MappingException(
							"transformation of <any/> as part of <join/> (secondary-table) not yet implemented",
							origin
					);
				}
				else if ( attributeMapping instanceof JaxbHbmDynamicComponentType ) {
					handleUnsupportedContent(
							"<dynamic-component/> mappings not supported; skipping"
					);
				}
			}
		}
	}

	private JaxbManyToOne transferManyToOneAttribute(JaxbHbmCompositeKeyManyToOneType hbmM2O) {
		final JaxbManyToOne m2o = new JaxbManyToOne();
		m2o.setId( true );
		m2o.setName( hbmM2O.getName() );
		m2o.setAttributeAccessor( hbmM2O.getAccess() );
		m2o.setFetch( convert( hbmM2O.getLazy() ) );
		m2o.setForeignKey( new JaxbForeignKey() );
		m2o.getForeignKey().setName( hbmM2O.getForeignKey() );
		if ( !hbmM2O.getColumn().isEmpty() ) {
			for ( JaxbHbmColumnType hbmColumn : hbmM2O.getColumn() ) {
				final JaxbJoinColumn joinColumn = new JaxbJoinColumn();
				joinColumn.setName( hbmColumn.getName() );
				joinColumn.setNullable( hbmColumn.isNotNull() == null ? null : !hbmColumn.isNotNull() );
				joinColumn.setUnique( hbmColumn.isUnique() );
				m2o.getJoinColumn().add( joinColumn );
			}
		}
		else {
			final JaxbJoinColumn joinColumn = new JaxbJoinColumn();
			if ( StringHelper.isEmpty( hbmM2O.getColumnAttribute() )) {
				// AbstractBasicBindingTests seems to imply this was the case
				joinColumn.setName( hbmM2O.getName() );
			}
			else {
				joinColumn.setName( hbmM2O.getColumnAttribute() );
			}
			m2o.getJoinColumn().add( joinColumn );
		}

		if ( isNotEmpty( hbmM2O.getEntityName() ) ) {
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

	private void transferUnionSubclass(JaxbHbmUnionSubclassEntityType hbmSubclass, JaxbEntity subclassEntity) {
		TRANSFORMATION_LOGGER.debugf(
				"Starting transformation of union-subclass entity `%` - `%s`",
				extractEntityName( hbmSubclass ),
				origin
		);

		subclassEntity.setProxy( hbmSubclass.getProxy() );
		transferBaseEntityInformation( hbmSubclass, subclassEntity );
		transferEntityElementAttributes( hbmSubclass, subclassEntity );

		subclassEntity.setTable( new JaxbTable() );
		subclassEntity.getTable().setCatalog( hbmSubclass.getCatalog() );
		subclassEntity.getTable().setSchema( hbmSubclass.getSchema() );
		subclassEntity.getTable().setName( hbmSubclass.getTable() );
		subclassEntity.getTable().setComment( hbmSubclass.getComment() );
		subclassEntity.getTable().setCheck( hbmSubclass.getCheck() );
		
		if ( !hbmSubclass.getUnionSubclass().isEmpty() ) {
			subclassEntity.setInheritance( new JaxbInheritance() );
			subclassEntity.getInheritance().setStrategy( InheritanceType.TABLE_PER_CLASS );
			for ( JaxbHbmUnionSubclassEntityType nestedHbmSubclass : hbmSubclass.getUnionSubclass() ) {
				final JaxbEntity nestedSubclassEntity = new JaxbEntity();
				ormRoot.getEntities().add( nestedSubclassEntity );
				transferUnionSubclass( nestedHbmSubclass, nestedSubclassEntity );
			}
		}
	}

	
	// ToOne
	private void transferFetchable(
			JaxbHbmLazyWithNoProxyEnum hbmLazy,
			JaxbHbmFetchStyleEnum hbmFetch,
			JaxbHbmOuterJoinEnum hbmOuterJoin,
			Boolean constrained,
			ToOneAttribute fetchable) {
		FetchType laziness = FetchType.LAZY;
		JaxbSingularFetchMode fetch = JaxbSingularFetchMode.SELECT;
		
		if (hbmLazy != null) {
			if (hbmLazy.equals( JaxbHbmLazyWithNoProxyEnum.FALSE )) {
				laziness = FetchType.EAGER;
			}
			else if (hbmLazy.equals( JaxbHbmLazyWithNoProxyEnum.NO_PROXY )) {
				// TODO: @LazyToOne(LazyToOneOption.PROXY) or @LazyToOne(LazyToOneOption.NO_PROXY)
			}
		}
		
		// allow fetch style to override laziness, if necessary
		if (constrained != null && ! constrained) {
			// NOTE SPECIAL CASE: one-to-one constrained=false cannot be proxied, so default to join and non-lazy
			laziness = FetchType.EAGER;
			fetch = JaxbSingularFetchMode.JOIN;
		}
		else {
			if (hbmFetch == null) {
				if (hbmOuterJoin != null && hbmOuterJoin.equals( JaxbHbmOuterJoinEnum.TRUE ) ) {
					laziness = FetchType.EAGER;
					fetch = JaxbSingularFetchMode.JOIN;
				}
			}
			else {
				if (hbmFetch.equals( JaxbHbmFetchStyleEnum.JOIN ) ) {
					laziness = FetchType.EAGER;
					fetch = JaxbSingularFetchMode.JOIN;
				}
			}
		}
		
		fetchable.setFetch( laziness );
		fetchable.setFetchMode( fetch );
	}
	
	// ToMany
	private void transferFetchable(
			JaxbHbmLazyWithExtraEnum hbmLazy,
			JaxbHbmFetchStyleWithSubselectEnum hbmFetch,
			JaxbHbmOuterJoinEnum hbmOuterJoin,
			CollectionAttribute fetchable) {
		FetchType laziness = FetchType.LAZY;
		JaxbPluralFetchMode fetch = JaxbPluralFetchMode.SELECT;
		
		if (hbmLazy != null) {
			if (hbmLazy.equals( JaxbHbmLazyWithExtraEnum.EXTRA )) {
				throw new MappingException( "HBM transformation: extra lazy not yet supported.", origin );
			}
			else if (hbmLazy.equals( JaxbHbmLazyWithExtraEnum.FALSE )) {
				laziness = FetchType.EAGER;
			}
		}
		
		// allow fetch style to override laziness, if necessary
		if (hbmFetch == null) {
			if (hbmOuterJoin != null && hbmOuterJoin.equals( JaxbHbmOuterJoinEnum.TRUE ) ) {
				laziness = FetchType.EAGER;
				fetch = JaxbPluralFetchMode.JOIN;
			}
		}
		else {
			if (hbmFetch.equals( JaxbHbmFetchStyleWithSubselectEnum.JOIN ) ) {
				laziness = FetchType.EAGER;
				fetch = JaxbPluralFetchMode.JOIN;
			}
			else if (hbmFetch.equals( JaxbHbmFetchStyleWithSubselectEnum.SUBSELECT ) ) {
				fetch = JaxbPluralFetchMode.SUBSELECT;
			}
		}
		
		fetchable.setFetch( laziness );
		fetchable.setFetchMode( fetch );
	}
	
	// KeyManyToOne
	private FetchType convert(JaxbHbmLazyEnum hbmLazy) {
		if ( hbmLazy != null && "false".equalsIgnoreCase( hbmLazy.value() ) ) {
			return FetchType.EAGER;
		}
		else {
			// proxy is HBM default
			return FetchType.LAZY;
		}
	}

	private OnDeleteAction convert(JaxbHbmOnDeleteEnum hbmOnDelete) {
		return hbmOnDelete == JaxbHbmOnDeleteEnum.CASCADE ? OnDeleteAction.CASCADE : OnDeleteAction.NO_ACTION;
	}

	private JaxbHbmFilter convert(JaxbHbmFilterType hbmFilter) {
		final JaxbHbmFilter filter = new JaxbHbmFilter();
		filter.setName( hbmFilter.getName() );

		final boolean shouldAutoInjectAliases = hbmFilter.getAutoAliasInjection() == null
				|| hbmFilter.getAutoAliasInjection().equalsIgnoreCase( "true" );

		filter.setAutoAliasInjection( shouldAutoInjectAliases );
		filter.setCondition( hbmFilter.getCondition() );

		for ( Serializable content : hbmFilter.getContent() ) {
			if ( content instanceof String ) {
				filter.setCondition( (String) content );
			}
			else {
				final JaxbHbmFilterAliasMappingType hbmAliasMapping = (JaxbHbmFilterAliasMappingType) content;
				final JaxbHbmFilter.JaxbAliases aliasMapping = new JaxbHbmFilter.JaxbAliases();
				aliasMapping.setAlias( hbmAliasMapping.getAlias() );
				aliasMapping.setEntity( hbmAliasMapping.getEntity() );
				aliasMapping.setTable( hbmAliasMapping.getTable() );
				filter.getContent().add( aliasMapping );
			}
		}

		return filter;
	}

	private JaxbCascadeType convertCascadeType(String s) {
		final JaxbCascadeType cascadeType = new JaxbCascadeType();

		if ( isNotEmpty( s ) ) {
			s = s.toLowerCase( Locale.ROOT ).replaceAll( " ", "" );
			final String[] split = s.split( "," );
			for ( String hbmCascade : split ) {
				if ( hbmCascade.contains( "all" ) ) {
					cascadeType.setCascadeAll( new JaxbEmptyType() );
				}
				if ( hbmCascade.contains( "persist" ) ) {
					cascadeType.setCascadePersist( new JaxbEmptyType() );
				}
				if (hbmCascade.contains( "merge" ) ) {
					cascadeType.setCascadeMerge( new JaxbEmptyType() );
				}
				if (hbmCascade.contains( "refresh" ) ) {
					cascadeType.setCascadeRefresh( new JaxbEmptyType() );
				}
				if (hbmCascade.contains( "save-update" ) ) {
					cascadeType.setCascadeSaveUpdate( new JaxbEmptyType() );
				}
				if (hbmCascade.contains( "evict" ) || hbmCascade.contains( "detach" ) ) {
					cascadeType.setCascadeDetach( new JaxbEmptyType() );
				}
				if (hbmCascade.contains( "replicate" ) ) {
					cascadeType.setCascadeReplicate( new JaxbEmptyType() );
				}
				if (hbmCascade.contains( "lock" ) ) {
					cascadeType.setCascadeLock( new JaxbEmptyType() );
				}
				if (hbmCascade.contains( "delete" ) ) {
					cascadeType.setCascadeDelete( new JaxbEmptyType() );
				}
			}
		}
		return cascadeType;
	}
	
	private boolean isOrphanRemoval(String s) {
		return isNotEmpty( s )
				&& s.toLowerCase( Locale.ROOT ).contains( "orphan" );
	}
	
	private String getFullyQualifiedClassName(String className) {
		// todo : right now we do both, we set the package into the XML and qualify the names; pick one...
		//		1) pass the names through as-is and set the package into the XML; the orm.xml reader
		//			would apply the package as needed
		//		2) qualify the name that we write into the XML, but the do not set the package into the XML;
		//			if going this route, would be better to leverage the normal hierarchical lookup for package
		// 			names which would mean passing along MappingDefaults (or maybe even the full "binding context")

		final String defaultPackageName = ormRoot.getPackage();
		if ( isNotEmpty( className )
				&& className.indexOf( '.' ) < 0
				&& isNotEmpty( defaultPackageName ) ) {
			className = StringHelper.qualify( defaultPackageName, className );
		}
		return className;
	}

}
