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
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyDiscriminatorValueMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingDiscriminatorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAnyMappingKeyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCachingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCascadeTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCheckConstraintImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCustomSqlImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbDatabaseObjectImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbDatabaseObjectScopeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbDiscriminatorColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbElementCollectionImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableAttributesContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmptyTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFetchProfileImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFieldResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFilterDefImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbForeignKeyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGenericIdGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbHbmFilterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbHqlImportImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdClassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbInheritanceImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMapKeyColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedNativeQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNaturalIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOrderColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitMetadataImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAnyMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralFetchModeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPrimaryKeyJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbQueryParamTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSecondaryTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSingularAssociationAttribute;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSingularFetchModeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSqlResultSetMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSynchronizedTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTransientImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbVersionImpl;
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
 * a {@link JaxbEntityMappingsImpl} "copy" of the {@link JaxbHbmHibernateMapping}
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
	public static JaxbEntityMappingsImpl transform(JaxbHbmHibernateMapping hbmXmlMapping, Origin origin, Options options) {
		return new HbmXmlTransformer( hbmXmlMapping, origin, options ).doTransform();
	}

	public interface Options {
		UnsupportedFeatureHandling unsupportedFeatureHandling();
	}

	private final Origin origin;
	private final JaxbHbmHibernateMapping hbmXmlMapping;
	private final JaxbEntityMappingsImpl ormRoot;

	private final Options options;

	public HbmXmlTransformer(JaxbHbmHibernateMapping hbmXmlMapping, Origin origin, Options options) {
		this.origin = origin;
		this.hbmXmlMapping = hbmXmlMapping;
		this.options = options;

		this.ormRoot = new JaxbEntityMappingsImpl();
		this.ormRoot.setDescription(
				"mapping.xml document auto-generated from legacy hbm.xml format via transformation - " + origin.getName()
		);

	}

	private JaxbEntityMappingsImpl doTransform() {
		TRANSFORMATION_LOGGER.tracef(
				"Starting hbm.xml transformation - `%s`",
				origin
		);

		final JaxbPersistenceUnitMetadataImpl metadata = new JaxbPersistenceUnitMetadataImpl();
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
		handleUnsupported(
				null,
				message,
				messageArgs
		);
	}

	@FunctionalInterface
	private interface PickHandler {
		void handlePick(String message, Object... messageArgs);
	}

	private void handleUnsupported(PickHandler pickHandler, String message, Object... messageArgs) {
		switch ( options.unsupportedFeatureHandling() ) {
			case ERROR -> throw new UnsupportedOperationException(
					String.format(
							Locale.ROOT,
							message,
							messageArgs
					)
			);
			case PICK -> {
				if ( pickHandler != null ) {
					pickHandler.handlePick( message, messageArgs );
				}
			}
			case IGNORE -> TRANSFORMATION_LOGGER.debugf( message, messageArgs );
			case WARN -> TRANSFORMATION_LOGGER.warnf( message, messageArgs );
		}
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

			final JaxbGenericIdGeneratorImpl generatorDef = new JaxbGenericIdGeneratorImpl();
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

			final JaxbFilterDefImpl filterDef = new JaxbFilterDefImpl();
			ormRoot.getFilterDefinitions().add( filterDef );
			filterDef.setName( hbmFilterDef.getName() );

			boolean foundCondition = false;
			for ( Object content : hbmFilterDef.getContent() ) {
				if ( content instanceof String ) {
					final String condition = ( (String) content ).trim();
					if ( !StringHelper.isEmpty( condition ) ) {
						foundCondition = true;
						filterDef.setDefaultCondition( condition );
					}
				}
				else {
					final JaxbHbmFilterParameterType hbmFilterParam = ( (JAXBElement<JaxbHbmFilterParameterType>) content ).getValue();
					final JaxbFilterDefImpl.JaxbFilterParamImpl param = new JaxbFilterDefImpl.JaxbFilterParamImpl();
					filterDef.getFilterParams().add( param );
					param.setName( hbmFilterParam.getParameterName() );
					param.setType( hbmFilterParam.getParameterValueTypeName() );
				}
			}

			if ( !foundCondition ) {
				filterDef.setDefaultCondition( hbmFilterDef.getCondition() );
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

			final JaxbHqlImportImpl ormImport = new JaxbHqlImportImpl();
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
			final JaxbSqlResultSetMappingImpl mapping = transformResultSetMapping( null, hbmResultSet );
			ormRoot.getSqlResultSetMappings().add( mapping );
		}
	}

	private JaxbSqlResultSetMappingImpl transformResultSetMapping(
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

		final JaxbSqlResultSetMappingImpl mapping = new JaxbSqlResultSetMappingImpl();
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

	private JaxbEntityResultImpl transferEntityReturnElement(
			String resultMappingName,
			JaxbHbmNativeQueryReturnType hbmReturn) {
		final JaxbEntityResultImpl entityResult = new JaxbEntityResultImpl();
		entityResult.setEntityClass( getFullyQualifiedClassName( hbmReturn.getClazz() ) );

		for ( JaxbHbmNativeQueryPropertyReturnType propertyReturn : hbmReturn.getReturnProperty() ) {
			final JaxbFieldResultImpl field = new JaxbFieldResultImpl();
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

	private JaxbColumnResultImpl transferScalarReturnElement(
			String resultMappingName,
			JaxbHbmNativeQueryScalarReturnType hbmReturn) {
		final JaxbColumnResultImpl columnResult = new JaxbColumnResultImpl();
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

	private JaxbFetchProfileImpl transferFetchProfile(JaxbHbmFetchProfileType hbmFetchProfile) {
		JaxbLogger.JAXB_LOGGER.debugf(
				"Starting transformation of fetch-profile mapping `{}` in `{}`",
				hbmFetchProfile.getName(),
				origin
		);

		final JaxbFetchProfileImpl fetchProfile = new JaxbFetchProfileImpl();
		fetchProfile.setName( hbmFetchProfile.getName() );
		for ( JaxbHbmFetchProfileType.JaxbHbmFetch hbmFetch : hbmFetchProfile.getFetch() ) {
			final JaxbFetchProfileImpl.JaxbFetchImpl fetch = new JaxbFetchProfileImpl.JaxbFetchImpl();
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

	private JaxbNamedQueryImpl transformNamedQuery(JaxbHbmNamedQueryType hbmQuery, String name) {
		JaxbLogger.JAXB_LOGGER.debugf(
				"Starting transformation of named-query mapping `{}` in `{}`",
				name,
				origin
		);

		final JaxbNamedQueryImpl query = new JaxbNamedQueryImpl();
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
			if ( content instanceof String qryString ) {
				qryString = qryString.trim();
				query.setQuery( qryString );
			}
			else {
				@SuppressWarnings("unchecked") final JAXBElement<JaxbHbmQueryParamType> element = (JAXBElement<JaxbHbmQueryParamType>) content;
				final JaxbHbmQueryParamType hbmQueryParam = element.getValue();
				final JaxbQueryParamTypeImpl queryParam = new JaxbQueryParamTypeImpl();
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

	private JaxbNamedNativeQueryImpl transformNamedNativeQuery(JaxbHbmNamedNativeQueryType hbmQuery, String queryName) {
		JaxbLogger.JAXB_LOGGER.debugf(
				"Starting transformation of (named) query mapping `{}` in `{}`",
				queryName,
				origin
		);

		final String implicitResultSetMappingName = queryName + "-implicitResultSetMapping";

		final JaxbNamedNativeQueryImpl query = new JaxbNamedNativeQueryImpl();
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

		JaxbSqlResultSetMappingImpl implicitResultSetMapping = null;

		// JaxbQueryElement#content elements can be either the query or parameters
		for ( Object content : hbmQuery.getContent() ) {
			if ( content instanceof String qryString ) {
				qryString = qryString.trim();
				query.setQuery( qryString );
			}
			else if ( content instanceof JAXBElement ) {
				final Object element = ( (JAXBElement<?>) content ).getValue();
				if ( element instanceof JaxbHbmQueryParamType hbmQueryParam ) {
					final JaxbQueryParamTypeImpl queryParam = new JaxbQueryParamTypeImpl();
					queryParam.setName( hbmQueryParam.getName() );
					queryParam.setType( hbmQueryParam.getType() );
					query.getQueryParam().add( queryParam );
				}
				else if ( element instanceof JaxbHbmNativeQueryScalarReturnType ) {
					if ( implicitResultSetMapping == null ) {
						implicitResultSetMapping = new JaxbSqlResultSetMappingImpl();
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
						implicitResultSetMapping = new JaxbSqlResultSetMappingImpl();
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
				else if ( element instanceof JaxbHbmSynchronizeType hbmSynchronize ) {
					final JaxbSynchronizedTableImpl synchronize = new JaxbSynchronizedTableImpl();
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

			final JaxbDatabaseObjectImpl databaseObject = new JaxbDatabaseObjectImpl();
			ormRoot.getDatabaseObjects().add( databaseObject );

			databaseObject.setCreate( hbmDatabaseObject.getCreate() );
			databaseObject.setDrop( hbmDatabaseObject.getDrop() );

			if ( ! hbmDatabaseObject.getDialectScope().isEmpty() ) {
				hbmDatabaseObject.getDialectScope().forEach( (hbmScope) -> {
					final JaxbDatabaseObjectScopeImpl scope = new JaxbDatabaseObjectScopeImpl();
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
		//			otherwise it will be discovered via hibernate-models
		//		2) ?? Have abstract hbm class mappings become MappedSuperclass mappings ??

		for ( JaxbHbmRootEntityType hbmClass : hbmXmlMapping.getClazz() ) {
			final JaxbEntityImpl entity = new JaxbEntityImpl();
			ormRoot.getEntities().add( entity );
			transferRootEntity( hbmClass, entity );
		}

		for ( JaxbHbmDiscriminatorSubclassEntityType hbmSubclass : hbmXmlMapping.getSubclass() ) {
			final JaxbEntityImpl entity = new JaxbEntityImpl();
			ormRoot.getEntities().add( entity );
			transferDiscriminatorSubclass( hbmSubclass, entity );
		}

		for ( JaxbHbmJoinedSubclassEntityType hbmSubclass : hbmXmlMapping.getJoinedSubclass() ) {
			final JaxbEntityImpl entity = new JaxbEntityImpl();
			ormRoot.getEntities().add( entity );
			transferJoinedSubclass( hbmSubclass, entity );
		}

		for ( JaxbHbmUnionSubclassEntityType hbmSubclass : hbmXmlMapping.getUnionSubclass() ) {
			final JaxbEntityImpl entity = new JaxbEntityImpl();
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

	private void transferRootEntity(JaxbHbmRootEntityType hbmClass, JaxbEntityImpl entity) {
		TRANSFORMATION_LOGGER.debugf(
				"Starting transformation of root entity `%s` - `%s`",
				extractEntityName( hbmClass ),
				origin
		);

		transferBaseEntityInformation( hbmClass, entity );

		entity.setMutable( hbmClass.isMutable() );

		if ( hbmClass.getTable() != null ) {
			entity.setTable( new JaxbTableImpl() );
			transfer( hbmClass::getTable, entity.getTable()::setName );
			transfer( hbmClass::getCatalog, entity.getTable()::setCatalog );
			transfer( hbmClass::getSchema, entity.getTable()::setSchema );
			transfer( hbmClass::getComment, entity.getTable()::setComment );
			transfer( hbmClass::getCheck, (constraint) -> {
				final JaxbCheckConstraintImpl checkConstraint = new JaxbCheckConstraintImpl();
				checkConstraint.setConstraint( constraint );
				entity.getTable().getCheckConstraints().add( checkConstraint );
			} );
		}
		else {
			transfer( hbmClass::getSubselect, entity::setTableExpression );
		}

		for ( JaxbHbmSynchronizeType hbmSync : hbmClass.getSynchronize() ) {
			final JaxbSynchronizedTableImpl sync = new JaxbSynchronizedTableImpl();
			sync.setTable( hbmSync.getTable() );
			entity.getSynchronizeTables().add( sync );
		}

		if ( hbmClass.getLoader() != null ) {
			handleUnsupported(
					"<loader/> is not supported in mapping.xsd - use <sql-select/> or <hql-select/> instead: ",
					origin
			);
		}

		if ( hbmClass.getSqlInsert() != null ) {
			entity.setSqlInsert( new JaxbCustomSqlImpl() );
			entity.getSqlInsert().setValue( hbmClass.getSqlInsert().getValue() );
			entity.getSqlInsert().setResultCheck( hbmClass.getSqlInsert().getCheck() );
			entity.getSqlInsert().setValue( hbmClass.getSqlInsert().getValue() );
		}
		if ( hbmClass.getSqlUpdate() != null ) {
			entity.setSqlUpdate( new JaxbCustomSqlImpl() );
			entity.getSqlUpdate().setValue( hbmClass.getSqlUpdate().getValue() );
			entity.getSqlUpdate().setResultCheck( hbmClass.getSqlUpdate().getCheck() );
			entity.getSqlUpdate().setValue( hbmClass.getSqlUpdate().getValue() );
		}
		if ( hbmClass.getSqlDelete() != null ) {
			entity.setSqlDelete( new JaxbCustomSqlImpl() );
			entity.getSqlDelete().setValue( hbmClass.getSqlDelete().getValue() );
			entity.getSqlDelete().setResultCheck( hbmClass.getSqlDelete().getCheck() );
			entity.getSqlDelete().setValue( hbmClass.getSqlDelete().getValue() );
		}
		entity.setRowid( hbmClass.getRowid() );
		entity.setSqlRestriction( hbmClass.getWhere() );

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

		entity.setOptimisticLocking( hbmClass.getOptimisticLock() );

		entity.setDiscriminatorValue( hbmClass.getDiscriminatorValue() );
		entity.setPolymorphism( convert( hbmClass.getPolymorphism() ) );

		transferDiscriminator( hbmClass, entity );
		transferAttributes( hbmClass, entity );

		if ( hbmClass.getCache() != null ) {
			transformEntityCaching( hbmClass, entity );
		}
		
		for ( JaxbHbmNamedQueryType hbmQuery : hbmClass.getQuery() ) {
			entity.getNamedQueries().add( transformNamedQuery( hbmQuery, entity.getName() + "." + hbmQuery.getName() ) );
		}
		
		for ( JaxbHbmNamedNativeQueryType hbmQuery : hbmClass.getSqlQuery() ) {
			entity.getNamedNativeQueries().add(
					transformNamedNativeQuery(
							hbmQuery,
							entity.getName() + "." + hbmQuery.getName()
					)
			);
		}
		
		for ( JaxbHbmFilterType hbmFilter : hbmClass.getFilter()) {
			entity.getFilters().add( convert( hbmFilter ) );
		}
		
		for ( JaxbHbmFetchProfileType hbmFetchProfile : hbmClass.getFetchProfile() ) {
			entity.getFetchProfiles().add( transferFetchProfile( hbmFetchProfile ) );
		}
		
		for ( JaxbHbmJoinedSubclassEntityType hbmSubclass : hbmClass.getJoinedSubclass() ) {
			entity.setInheritance( new JaxbInheritanceImpl() );
			entity.getInheritance().setStrategy( InheritanceType.JOINED );

			final JaxbEntityImpl subclassEntity = new JaxbEntityImpl();
			ormRoot.getEntities().add( subclassEntity );
			transferJoinedSubclass( hbmSubclass, subclassEntity );
		}
		
		for (JaxbHbmUnionSubclassEntityType hbmSubclass : hbmClass.getUnionSubclass() ) {
			entity.setInheritance( new JaxbInheritanceImpl() );
			entity.getInheritance().setStrategy( InheritanceType.TABLE_PER_CLASS );

			final JaxbEntityImpl subclassEntity = new JaxbEntityImpl();
			ormRoot.getEntities().add( subclassEntity );
			transferUnionSubclass( hbmSubclass, subclassEntity );
		}
		
		for ( JaxbHbmDiscriminatorSubclassEntityType hbmSubclass : hbmClass.getSubclass() ) {
			final JaxbEntityImpl subclassEntity = new JaxbEntityImpl();
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

	private void transformEntityCaching(JaxbHbmRootEntityType hbmClass, JaxbEntityImpl entity) {
		entity.setCaching( new JaxbCachingImpl() );
		entity.getCaching().setRegion( hbmClass.getCache().getRegion() );
		entity.getCaching().setAccess( hbmClass.getCache().getUsage() );
		entity.getCaching().setIncludeLazy( convert( hbmClass.getCache().getInclude() ) );
	}

	private boolean convert(JaxbHbmCacheInclusionEnum hbmInclusion) {
		if ( hbmInclusion == null ) {
			return true;
		}

		if ( hbmInclusion == JaxbHbmCacheInclusionEnum.NON_LAZY ) {
			return false;
		}

		if ( hbmInclusion == JaxbHbmCacheInclusionEnum.ALL ) {
			return true;
		}

		throw new IllegalArgumentException( "Unrecognized cache-inclusions value : " + hbmInclusion );
	}

	@SuppressWarnings("deprecation")
	private PolymorphismType convert(JaxbHbmPolymorphismEnum polymorphism) {
		if ( polymorphism == null ) {
			return null;
		}
		return polymorphism == JaxbHbmPolymorphismEnum.EXPLICIT ? PolymorphismType.EXPLICIT : PolymorphismType.IMPLICIT;
	}

	private void transferBaseEntityInformation(JaxbHbmEntityBaseDefinition hbmClass, JaxbEntityImpl entity) {
		entity.setMetadataComplete( true );

		transfer( hbmClass::getEntityName, entity::setName );
		transfer( hbmClass::getName, entity::setClazz );

		if ( hbmClass instanceof Discriminatable discriminatable ) {
			transfer( discriminatable::getDiscriminatorValue, entity::setDiscriminatorValue );
		}

		if ( hbmClass.isAbstract() != null ) {
			// todo : handle hbm abstract as mapping abstract or as mapped-superclass?
			entity.setAbstract( hbmClass.isAbstract() );
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
					final JaxbSqlResultSetMappingImpl mapping = transformResultSetMapping( namePrefix, hbmMapping );
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

	private void transferDiscriminatorSubclass(JaxbHbmDiscriminatorSubclassEntityType hbmSubclass, JaxbEntityImpl subclassEntity) {
		TRANSFORMATION_LOGGER.debugf(
				"Starting transformation of subclass entity `%` - `%s`",
				extractEntityName( hbmSubclass ),
				origin
		);

		transferBaseEntityInformation( hbmSubclass, subclassEntity );

		transferEntityElementAttributes( hbmSubclass, subclassEntity );
	}

	private void transferJoinedSubclass(JaxbHbmJoinedSubclassEntityType hbmSubclass, JaxbEntityImpl subclassEntity) {
		TRANSFORMATION_LOGGER.debugf(
				"Starting transformation of joined-subclass entity `%` - `%s`",
				extractEntityName( hbmSubclass ),
				origin
		);

		transferBaseEntityInformation( hbmSubclass, subclassEntity );
		transferEntityElementAttributes( hbmSubclass, subclassEntity );

		subclassEntity.setTable( new JaxbTableImpl() );
		subclassEntity.getTable().setCatalog( hbmSubclass.getCatalog() );
		subclassEntity.getTable().setSchema( hbmSubclass.getSchema() );
		subclassEntity.getTable().setName( hbmSubclass.getTable() );
		subclassEntity.getTable().setComment( hbmSubclass.getComment() );
		final String hbmCheckConstraint = hbmSubclass.getCheck();
		if ( hbmCheckConstraint != null ) {
			final JaxbCheckConstraintImpl checkConstraint = new JaxbCheckConstraintImpl();
			checkConstraint.setConstraint( hbmCheckConstraint );
			subclassEntity.getTable().getCheckConstraints().add( checkConstraint );
		}

		if ( hbmSubclass.getKey() != null ) {
			final JaxbPrimaryKeyJoinColumnImpl joinColumn = new JaxbPrimaryKeyJoinColumnImpl();
			// TODO: multiple columns?
			joinColumn.setName( hbmSubclass.getKey().getColumnAttribute() );
			subclassEntity.getPrimaryKeyJoinColumns().add( joinColumn );
		}
		
		if ( !hbmSubclass.getJoinedSubclass().isEmpty() ) {
			subclassEntity.setInheritance( new JaxbInheritanceImpl() );
			subclassEntity.getInheritance().setStrategy( InheritanceType.JOINED );
			for ( JaxbHbmJoinedSubclassEntityType nestedHbmSubclass : hbmSubclass.getJoinedSubclass() ) {
				final JaxbEntityImpl nestedSubclassEntity = new JaxbEntityImpl();
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

	private void transferDiscriminator(final JaxbHbmRootEntityType hbmClass, final JaxbEntityImpl entity) {
		if ( hbmClass.getDiscriminator() == null ) {
			return;
		}

		if ( isNotEmpty( hbmClass.getDiscriminator().getColumnAttribute() ) ) {
			entity.setDiscriminatorColumn( new JaxbDiscriminatorColumnImpl() );
			entity.getDiscriminatorColumn().setName( hbmClass.getDiscriminator().getColumnAttribute() );
		}
		else if ( StringHelper.isEmpty( hbmClass.getDiscriminator().getFormulaAttribute() ) ) {
			entity.setDiscriminatorFormula( hbmClass.getDiscriminator().getFormulaAttribute() );
		}
		else if ( StringHelper.isEmpty( hbmClass.getDiscriminator().getFormula() ) ) {
			entity.setDiscriminatorFormula( hbmClass.getDiscriminator().getFormulaAttribute().trim() );
		}
		else {
			entity.setDiscriminatorColumn( new JaxbDiscriminatorColumnImpl() );
			entity.getDiscriminatorColumn().setName( hbmClass.getDiscriminator().getColumn().getName() );
			entity.getDiscriminatorColumn().setColumnDefinition( hbmClass.getDiscriminator().getColumn().getSqlType() );
			entity.getDiscriminatorColumn().setLength( hbmClass.getDiscriminator().getColumn().getLength() );
			entity.getDiscriminatorColumn().setForceSelection( hbmClass.getDiscriminator().isForce() );
		}
	}

	private void transferAttributes(JaxbHbmRootEntityType source, JaxbEntityImpl target) {
		transferEntityElementAttributes( source, target );

		transferIdentifier( source, target );
		transferNaturalIdentifiers( source, target );
		transferVersion( source, target );
		transferTimestamp( source, target );

		transferJoins( source, target );
	}


	private void transferEntityElementAttributes(EntityInfo hbmClass, JaxbEntityImpl entity) {
		entity.setAttributes( new JaxbAttributesContainerImpl() );
		transferAttributes( hbmClass.getAttributes(), entity.getAttributes() );
	}

	private void transferAttributes(List hbmAttributeMappings, JaxbAttributesContainer attributes) {
		for ( Object hbmAttributeMapping : hbmAttributeMappings ) {
			if ( hbmAttributeMapping instanceof JaxbHbmBasicAttributeType basic ) {
				attributes.getBasicAttributes().add( transformBasicAttribute( basic ) );
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmCompositeAttributeType hbmComponent ) {
				ormRoot.getEmbeddables().add( convertEmbeddable( hbmComponent ) );
				attributes.getEmbeddedAttributes().add( transformEmbedded( hbmComponent ) );
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmPropertiesType hbmProperties ) {
				transferAttributes( hbmProperties.getAttributes(), attributes );
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmDynamicComponentType ) {
				final String name = ( (JaxbHbmDynamicComponentType) hbmAttributeMapping ).getName();
				handleUnsupported(
						"<dynamic-component/> mappings not supported for transformation [name=%s]",
						name
				);
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmOneToOneType o2o ) {
				transferOneToOne( o2o, attributes );
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmManyToOneType m2o ) {
				attributes.getManyToOneAttributes().add( transformManyToOne( m2o ) );
			}
			else if ( hbmAttributeMapping instanceof JaxbHbmAnyAssociationType any ) {
				attributes.getAnyMappingAttributes().add( transformAnyAttribute( any ) );

			}
			else if ( hbmAttributeMapping instanceof PluralAttributeInfo pluralAttributeInfo ) {
				if ( pluralAttributeInfo.getElement() != null
						|| pluralAttributeInfo.getCompositeElement() != null ) {
					attributes.getElementCollectionAttributes().add( transformElementCollection( pluralAttributeInfo ) );
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
				else if ( pluralAttributeInfo.getManyToAny() != null ) {
					attributes.getPluralAnyMappingAttributes().add( transformManyToAnyCollection( pluralAttributeInfo ) );
				}
				else {
					throw new UnsupportedOperationException( "Unexpected node type - " + hbmCollection );
				}
			}
		}
	}

	private JaxbBasicImpl transformBasicAttribute(final JaxbHbmBasicAttributeType hbmProp) {
		final JaxbBasicImpl basic = new JaxbBasicImpl();
		transferBasicAttribute( hbmProp, basic );
		return basic;
	}

	private void transferBasicAttribute(JaxbHbmBasicAttributeType hbmProp, JaxbBasicImpl basic) {
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

	private JaxbEmbeddableImpl convertEmbeddable(JaxbHbmCompositeAttributeType hbmComponent) {
		final JaxbEmbeddableImpl embeddable = new JaxbEmbeddableImpl();
		embeddable.setMetadataComplete( true );
		embeddable.setClazz( determineEmbeddableName( hbmComponent ) );

		embeddable.setAttributes( new JaxbEmbeddableAttributesContainerImpl() );
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

	private JaxbEmbeddedImpl transformEmbedded(JaxbHbmCompositeAttributeType hbmComponent) {
		final JaxbEmbeddedImpl embedded = new JaxbEmbeddedImpl();
		embedded.setName( hbmComponent.getName() );
		embedded.setAttributeAccessor( hbmComponent.getAccess() );
		return embedded;
	}

	private void transferOneToOne(JaxbHbmOneToOneType hbmOneToOne, JaxbAttributesContainer attributes) {
		if ( !hbmOneToOne.getFormula().isEmpty() || !StringHelper.isEmpty( hbmOneToOne.getFormulaAttribute() ) ) {
			handleUnsupported(
					"Transformation of formulas within one-to-ones are not supported - `%s`",
					origin
			);
			return;
		}

		final JaxbOneToOneImpl oneToOne = new JaxbOneToOneImpl();
		oneToOne.setAttributeAccessor( hbmOneToOne.getAccess() );
		oneToOne.setCascade( convertCascadeType( hbmOneToOne.getCascade() ) );
		oneToOne.setOrphanRemoval( isOrphanRemoval( hbmOneToOne.getCascade() ) );
		oneToOne.setForeignKey( new JaxbForeignKeyImpl() );
		oneToOne.getForeignKey().setName( hbmOneToOne.getForeignKey() );
		if (! StringHelper.isEmpty( hbmOneToOne.getPropertyRef() ) ) {
			final JaxbJoinColumnImpl joinColumn = new JaxbJoinColumnImpl();
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

	private JaxbManyToOneImpl transformManyToOne(final JaxbHbmManyToOneType hbmNode) {
		if ( hbmNode.getNotFound() != JaxbHbmNotFoundEnum.EXCEPTION ) {
			handleUnsupported( "`not-found` not supported for transformation" );
		}

		final JaxbManyToOneImpl m2o = new JaxbManyToOneImpl();
		m2o.setAttributeAccessor( hbmNode.getAccess() );
		m2o.setCascade( convertCascadeType( hbmNode.getCascade() ) );
		m2o.setForeignKeys( new JaxbForeignKeyImpl() );
		m2o.getForeignKeys().setName( hbmNode.getForeignKey() );

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
						m2o.getJoinColumns().add( ( (TargetColumnAdapterJaxbJoinColumn) column ).getTargetColumn() );
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


	private JaxbAnyMappingImpl transformAnyAttribute(JaxbHbmAnyAssociationType source) {
		final JaxbAnyMappingImpl target = new JaxbAnyMappingImpl();

		target.setName( source.getName() );
		target.setAttributeAccessor( source.getAccess() );
		target.setOptimisticLock( source.isOptimisticLock() );

		// todo : cascade
		// todo : discriminator column
		// todo : key column

		target.setDiscriminator( new JaxbAnyMappingDiscriminatorImpl() );
		source.getMetaValue().forEach( (sourceMapping) -> {
			final JaxbAnyDiscriminatorValueMappingImpl mapping = new JaxbAnyDiscriminatorValueMappingImpl();
			mapping.setDiscriminatorValue( sourceMapping.getValue() );
			mapping.setCorrespondingEntityName( sourceMapping.getClazz() );
			target.getDiscriminator().getValueMappings().add( mapping );
		} );

		target.setKey( new JaxbAnyMappingKeyImpl() );

		return target;
	}

	private JaxbElementCollectionImpl transformElementCollection(final PluralAttributeInfo source) {
		final JaxbElementCollectionImpl target = new JaxbElementCollectionImpl();
		transferCollectionTable( source, target );
		transferCollectionBasicInfo( source, target );

		if ( source instanceof JaxbHbmMapType ) {
			transferMapKey( (JaxbHbmMapType) source, target );
		}

		if ( source.getElement() != null ) {
			transferColumnsAndFormulas(
					new ColumnAndFormulaSource() {
						@Override
						public String getColumnAttribute() {
							return source.getElement().getColumnAttribute();
						}

						@Override
						public String getFormulaAttribute() {
							return source.getElement().getFormulaAttribute();
						}

						@Override
						public List<Serializable> getColumnOrFormula() {
							return source.getElement().getColumnOrFormula();
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
		else {
			target.setTargetClass( source.getCompositeElement().getClazz() );

			// todo : account for same embeddable used multiple times
			final JaxbEmbeddableImpl embeddedable = new JaxbEmbeddableImpl();
			embeddedable.setClazz( source.getCompositeElement().getClazz() );
			embeddedable.setAttributes( new JaxbEmbeddableAttributesContainerImpl() );
			transferAttributes(
					source.getCompositeElement().getAttributes(),
					embeddedable.getAttributes()
			);
			ormRoot.getEmbeddables().add( embeddedable );
		}

		return target;
	}

	private void transferCollectionTable(
			final PluralAttributeInfo source,
			final JaxbElementCollectionImpl target) {
		target.setCollectionTable( new JaxbCollectionTableImpl() );

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
						return new ArrayList<>( source.getKey().getColumn() );
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


	private void transferCollectionBasicInfo(PluralAttributeInfo source, JaxbPluralAttribute target) {
		target.setName( source.getName() );
		target.setAttributeAccessor( source.getAccess() );
		target.setFetchMode( convert( source.getFetch() ) );

		if ( source instanceof JaxbHbmSetType set ) {
			target.setSort( set.getSort() );
			target.setOrderBy( set.getOrderBy() );
		}
		else if ( source instanceof JaxbHbmMapType map ) {
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
			JaxbPluralAttribute target) {
		final JaxbOrderColumnImpl orderColumn = new JaxbOrderColumnImpl();
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

	private void transferMapKey(JaxbHbmMapType source, JaxbPluralAttribute target) {
		if ( source.getIndex() != null ) {
			final JaxbMapKeyColumnImpl mapKey = new JaxbMapKeyColumnImpl();
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

			final JaxbMapKeyColumnImpl mapKey = new JaxbMapKeyColumnImpl();
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

	private JaxbPluralFetchModeImpl convert(JaxbHbmFetchStyleWithSubselectEnum fetch) {
		if ( fetch != null ) {
			return switch ( fetch ) {
				case SELECT -> JaxbPluralFetchModeImpl.SELECT;
				case JOIN -> JaxbPluralFetchModeImpl.JOIN;
				case SUBSELECT -> JaxbPluralFetchModeImpl.SUBSELECT;
			};
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

	private void transferIdentifier(JaxbHbmRootEntityType source, JaxbEntityImpl target) {
		if ( source.getId() != null ) {
			target.getAttributes().getIdAttributes().add( convertSimpleId( source.getId() ) );
		}
		else {
			final JaxbHbmCompositeIdType hbmCompositeId = source.getCompositeId();
			assert hbmCompositeId != null;

			final boolean isAggregate;
			if ( isNotEmpty( hbmCompositeId.getClazz() ) ) {
				// we have  <composite-id class="XYZ">.
				// user explicitly said the class is an "IdClass"
				isAggregate = !hbmCompositeId.isMapped();
			}
			else {
				// there was no class specified, can only be non-aggregated
				isAggregate = false;
			}

			if ( isAggregate ) {
				target.getAttributes().setEmbeddedIdAttribute( new JaxbEmbeddedIdImpl() );
				target.getAttributes().getEmbeddedIdAttribute().setName( hbmCompositeId.getName() );
				target.getAttributes().getEmbeddedIdAttribute().setAttributeAccessor( hbmCompositeId.getAccess() );

				final JaxbEmbeddableImpl embeddable = new JaxbEmbeddableImpl();
				embeddable.setClazz( hbmCompositeId.getClazz() );
				embeddable.setAttributes( new JaxbEmbeddableAttributesContainerImpl() );
				for ( Object hbmCompositeAttribute : hbmCompositeId.getKeyPropertyOrKeyManyToOne() ) {
					if ( hbmCompositeAttribute instanceof JaxbHbmCompositeKeyBasicAttributeType keyProp ) {
						final JaxbBasicImpl basic = new JaxbBasicImpl();
						basic.setName( keyProp.getName() );
						basic.setAttributeAccessor( keyProp.getAccess() );
						if ( isNotEmpty( keyProp.getColumnAttribute() ) ) {
							final JaxbColumnImpl column = new JaxbColumnImpl();
							column.setName( keyProp.getColumnAttribute() );
							basic.setColumn( column );
						}
						else {
							for ( JaxbHbmColumnType hbmColumn : keyProp.getColumn() ) {
								final JaxbColumnImpl column = new JaxbColumnImpl();
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
						final JaxbManyToOneImpl manyToOne = transferManyToOneAttribute( keyManyToOne );
						embeddable.getAttributes().getManyToOneAttributes().add( manyToOne );
					}
				}
				ormRoot.getEmbeddables().add( embeddable );
			}
			else {
				final JaxbIdClassImpl idClass = new JaxbIdClassImpl();
				idClass.setClazz( hbmCompositeId.getClazz() );
				target.setIdClass( idClass );
				for ( Object hbmCompositeAttribute : hbmCompositeId.getKeyPropertyOrKeyManyToOne() ) {
					if ( hbmCompositeAttribute instanceof JaxbHbmCompositeKeyBasicAttributeType keyProp ) {
						final JaxbIdImpl id = new JaxbIdImpl();
						id.setName( keyProp.getName() );
						id.setAttributeAccessor( keyProp.getAccess() );
						if ( isNotEmpty( keyProp.getColumnAttribute() ) ) {
							final JaxbColumnImpl column = new JaxbColumnImpl();
							column.setName( keyProp.getColumnAttribute() );
							id.setColumn( column );
						}
						else {
							if ( keyProp.getColumn().size() == 1 ) {
								id.setColumn( new JaxbColumnImpl() );
								transferColumn(
										new SourceColumnAdapterJaxbHbmColumnType( keyProp.getColumn().get( 0 ) ),
										new TargetColumnAdapterJaxbColumn( id.getColumn(), ColumnDefaultsInsertableNonUpdateableImpl.INSTANCE )
								);
							}
						}
						target.getAttributes().getIdAttributes().add( id );
					}
					else {
						final JaxbHbmCompositeKeyManyToOneType keyManyToOne = (JaxbHbmCompositeKeyManyToOneType) hbmCompositeAttribute;
						final JaxbManyToOneImpl manyToOne = transferManyToOneAttribute( keyManyToOne );
						target.getAttributes().getManyToOneAttributes().add( manyToOne );
					}
				}
			}
		}
	}


	private JaxbIdImpl convertSimpleId(JaxbHbmSimpleIdType source) {
		final JaxbIdImpl target = new JaxbIdImpl();
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
			target.setColumn( new JaxbColumnImpl() );
			target.getColumn().setName( source.getColumnAttribute() );
		}
		else {
			if ( source.getColumn() != null ) {
				if ( source.getColumn().size() == 1 ) {
					target.setColumn( new JaxbColumnImpl() );
					transferColumn(
							new SourceColumnAdapterJaxbHbmColumnType( source.getColumn().get( 0 ) ),
							new TargetColumnAdapterJaxbColumn( target.getColumn(), ColumnDefaultsInsertableNonUpdateableImpl.INSTANCE )
					);
				}
			}
		}

		return target;
	}


	private void transferNaturalIdentifiers(JaxbHbmRootEntityType source, JaxbEntityImpl target) {
		if ( source.getNaturalId() == null ) {
			return;
		}

		final JaxbNaturalIdImpl naturalId = new JaxbNaturalIdImpl();
		transferAttributes(
				source.getNaturalId().getAttributes(),
				new JaxbAttributesContainer() {
					@Override
					public List<JaxbBasicImpl> getBasicAttributes() {
						return naturalId.getBasicAttributes();
					}

					@Override
					public List<JaxbEmbeddedImpl> getEmbeddedAttributes() {
						return naturalId.getEmbeddedAttributes();
					}

					@Override
					public List<JaxbOneToOneImpl> getOneToOneAttributes() {
						return null;
					}

					@Override
					public List<JaxbManyToOneImpl> getManyToOneAttributes() {
						return null;
					}

					@Override
					public List<JaxbAnyMappingImpl> getAnyMappingAttributes() {
						return null;
					}

					@Override
					public List<JaxbElementCollectionImpl> getElementCollectionAttributes() {
						return null;
					}

					@Override
					public List<JaxbOneToManyImpl> getOneToManyAttributes() {
						return null;
					}

					@Override
					public List<JaxbManyToManyImpl> getManyToManyAttributes() {
						return null;
					}

					@Override
					public List<JaxbPluralAnyMappingImpl> getPluralAnyMappingAttributes() {
						return null;
					}

					@Override
					public List<JaxbTransientImpl> getTransients() {
						return null;
					}
				}
		);

		naturalId.setMutable( source.getNaturalId().isMutable() );
		target.getAttributes().setNaturalId( naturalId );
	}

	private void transferVersion(JaxbHbmRootEntityType source, JaxbEntityImpl target) {
		final JaxbHbmVersionAttributeType hbmVersion = source.getVersion();
		if ( hbmVersion != null ) {
			final JaxbVersionImpl version = new JaxbVersionImpl();
			version.setName( hbmVersion.getName() );
			if ( isNotEmpty( hbmVersion.getColumnAttribute() ) ) {
				version.setColumn( new JaxbColumnImpl() );
				version.getColumn().setName( hbmVersion.getColumnAttribute() );
			}
			target.getAttributes().getVersion().add( version );
		}
	}

	private void transferTimestamp(JaxbHbmRootEntityType source, JaxbEntityImpl target) {
		final JaxbHbmTimestampAttributeType hbmTimestamp = source.getTimestamp();
		if ( hbmTimestamp != null ) {
			final JaxbVersionImpl version = new JaxbVersionImpl();
			version.setName( hbmTimestamp.getName() );
			// TODO: multiple columns?
			if ( isNotEmpty( hbmTimestamp.getColumnAttribute() ) ) {
				version.setColumn( new JaxbColumnImpl() );
				version.getColumn().setName( hbmTimestamp.getColumnAttribute() );
			}
			//noinspection deprecation
			version.setTemporal( TemporalType.TIMESTAMP );
			target.getAttributes().getVersion().add( version );
		}
	}

	private void transferJoins(JaxbHbmRootEntityType source, JaxbEntityImpl target) {
		for ( JaxbHbmSecondaryTableType hbmJoin : source.getJoin() ) {
			if ( !hbmJoin.isInverse() ) {
				final JaxbSecondaryTableImpl secondaryTable = new JaxbSecondaryTableImpl();
				secondaryTable.setCatalog( hbmJoin.getCatalog() );
				secondaryTable.setComment( hbmJoin.getComment() );
				secondaryTable.setName( hbmJoin.getTable() );
				secondaryTable.setSchema( hbmJoin.getSchema() );
				secondaryTable.setOptional( hbmJoin.isOptional() );
				if ( hbmJoin.getKey() != null ) {
					final JaxbPrimaryKeyJoinColumnImpl joinColumn = new JaxbPrimaryKeyJoinColumnImpl();
					joinColumn.setName( hbmJoin.getKey().getColumnAttribute() );
					secondaryTable.getPrimaryKeyJoinColumn().add( joinColumn );
				}
				target.getSecondaryTables().add( secondaryTable );
			}
			
			for ( Serializable attributeMapping : hbmJoin.getAttributes() ) {
				if ( attributeMapping instanceof JaxbHbmBasicAttributeType ) {
					final JaxbBasicImpl prop = transformBasicAttribute( (JaxbHbmBasicAttributeType) attributeMapping );
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

	private JaxbManyToOneImpl transferManyToOneAttribute(JaxbHbmCompositeKeyManyToOneType hbmM2O) {
		final JaxbManyToOneImpl m2o = new JaxbManyToOneImpl();
		m2o.setId( true );
		m2o.setName( hbmM2O.getName() );
		m2o.setAttributeAccessor( hbmM2O.getAccess() );
		m2o.setFetch( convert( hbmM2O.getLazy() ) );
		m2o.setForeignKeys( new JaxbForeignKeyImpl() );
		m2o.getForeignKeys().setName( hbmM2O.getForeignKey() );
		if ( !hbmM2O.getColumn().isEmpty() ) {
			for ( JaxbHbmColumnType hbmColumn : hbmM2O.getColumn() ) {
				final JaxbJoinColumnImpl joinColumn = new JaxbJoinColumnImpl();
				joinColumn.setName( hbmColumn.getName() );
				joinColumn.setNullable( hbmColumn.isNotNull() == null ? null : !hbmColumn.isNotNull() );
				joinColumn.setUnique( hbmColumn.isUnique() );
				m2o.getJoinColumns().add( joinColumn );
			}
		}
		else {
			final JaxbJoinColumnImpl joinColumn = new JaxbJoinColumnImpl();
			if ( StringHelper.isEmpty( hbmM2O.getColumnAttribute() )) {
				// AbstractBasicBindingTests seems to imply this was the case
				joinColumn.setName( hbmM2O.getName() );
			}
			else {
				joinColumn.setName( hbmM2O.getColumnAttribute() );
			}
			m2o.getJoinColumns().add( joinColumn );
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

	private void transferUnionSubclass(JaxbHbmUnionSubclassEntityType hbmSubclass, JaxbEntityImpl subclassEntity) {
		TRANSFORMATION_LOGGER.debugf(
				"Starting transformation of union-subclass entity `%` - `%s`",
				extractEntityName( hbmSubclass ),
				origin
		);

		subclassEntity.setProxy( hbmSubclass.getProxy() );
		transferBaseEntityInformation( hbmSubclass, subclassEntity );
		transferEntityElementAttributes( hbmSubclass, subclassEntity );

		subclassEntity.setTable( new JaxbTableImpl() );
		subclassEntity.getTable().setCatalog( hbmSubclass.getCatalog() );
		subclassEntity.getTable().setSchema( hbmSubclass.getSchema() );
		subclassEntity.getTable().setName( hbmSubclass.getTable() );
		subclassEntity.getTable().setComment( hbmSubclass.getComment() );
		final String hbmCheck = hbmSubclass.getCheck();
		if ( hbmCheck != null ) {
			final JaxbCheckConstraintImpl checkConstraint = new JaxbCheckConstraintImpl();
			checkConstraint.setConstraint( hbmCheck );
			subclassEntity.getTable().getCheckConstraints().add( checkConstraint );
		}

		if ( !hbmSubclass.getUnionSubclass().isEmpty() ) {
			subclassEntity.setInheritance( new JaxbInheritanceImpl() );
			subclassEntity.getInheritance().setStrategy( InheritanceType.TABLE_PER_CLASS );
			for ( JaxbHbmUnionSubclassEntityType nestedHbmSubclass : hbmSubclass.getUnionSubclass() ) {
				final JaxbEntityImpl nestedSubclassEntity = new JaxbEntityImpl();
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
			JaxbSingularAssociationAttribute fetchable) {
		FetchType laziness = FetchType.LAZY;
		JaxbSingularFetchModeImpl fetch = JaxbSingularFetchModeImpl.SELECT;
		
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
			fetch = JaxbSingularFetchModeImpl.JOIN;
		}
		else {
			if (hbmFetch == null) {
				if (hbmOuterJoin != null && hbmOuterJoin.equals( JaxbHbmOuterJoinEnum.TRUE ) ) {
					laziness = FetchType.EAGER;
					fetch = JaxbSingularFetchModeImpl.JOIN;
				}
			}
			else {
				if (hbmFetch.equals( JaxbHbmFetchStyleEnum.JOIN ) ) {
					laziness = FetchType.EAGER;
					fetch = JaxbSingularFetchModeImpl.JOIN;
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
			JaxbPluralAttribute fetchable) {
		FetchType laziness = FetchType.LAZY;
		JaxbPluralFetchModeImpl fetch = JaxbPluralFetchModeImpl.SELECT;
		
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
				fetch = JaxbPluralFetchModeImpl.JOIN;
			}
		}
		else {
			if (hbmFetch.equals( JaxbHbmFetchStyleWithSubselectEnum.JOIN ) ) {
				laziness = FetchType.EAGER;
				fetch = JaxbPluralFetchModeImpl.JOIN;
			}
			else if (hbmFetch.equals( JaxbHbmFetchStyleWithSubselectEnum.SUBSELECT ) ) {
				fetch = JaxbPluralFetchModeImpl.SUBSELECT;
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

	private JaxbHbmFilterImpl convert(JaxbHbmFilterType hbmFilter) {
		final JaxbHbmFilterImpl filter = new JaxbHbmFilterImpl();
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
				final JaxbHbmFilterImpl.JaxbAliasesImpl aliasMapping = new JaxbHbmFilterImpl.JaxbAliasesImpl();
				aliasMapping.setAlias( hbmAliasMapping.getAlias() );
				aliasMapping.setEntity( hbmAliasMapping.getEntity() );
				aliasMapping.setTable( hbmAliasMapping.getTable() );
				filter.getAliases().add( aliasMapping );
			}
		}

		return filter;
	}

	private JaxbCascadeTypeImpl convertCascadeType(String s) {
		final JaxbCascadeTypeImpl cascadeType = new JaxbCascadeTypeImpl();

		if ( isNotEmpty( s ) ) {
			s = s.toLowerCase( Locale.ROOT ).replaceAll( " ", "" );
			final String[] split = s.split( "," );
			for ( String hbmCascade : split ) {
				if ( hbmCascade.contains( "all" ) ) {
					cascadeType.setCascadeAll( new JaxbEmptyTypeImpl() );
				}
				if ( hbmCascade.contains( "persist" ) ) {
					cascadeType.setCascadePersist( new JaxbEmptyTypeImpl() );
				}
				if (hbmCascade.contains( "merge" ) ) {
					cascadeType.setCascadeMerge( new JaxbEmptyTypeImpl() );
				}
				if (hbmCascade.contains( "refresh" ) ) {
					cascadeType.setCascadeRefresh( new JaxbEmptyTypeImpl() );
				}
				if (hbmCascade.contains( "save-update" ) ) {
					cascadeType.setCascadeMerge( new JaxbEmptyTypeImpl() );
				}
				if (hbmCascade.contains( "evict" ) || hbmCascade.contains( "detach" ) ) {
					cascadeType.setCascadeDetach( new JaxbEmptyTypeImpl() );
				}
				if (hbmCascade.contains( "replicate" ) ) {
					cascadeType.setCascadeReplicate( new JaxbEmptyTypeImpl() );
				}
				if (hbmCascade.contains( "lock" ) ) {
					cascadeType.setCascadeLock( new JaxbEmptyTypeImpl() );
				}
				if (hbmCascade.contains( "delete" ) ) {
					cascadeType.setCascadeRemove( new JaxbEmptyTypeImpl() );
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
