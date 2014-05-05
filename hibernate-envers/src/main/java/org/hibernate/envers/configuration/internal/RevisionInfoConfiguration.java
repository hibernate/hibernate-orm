/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.configuration.internal;

import java.sql.Date;
import java.util.Collection;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.configuration.internal.metadata.AuditTableData;
import org.hibernate.envers.configuration.internal.metadata.MetadataTools;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.event.spi.EnversDotNames;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.revisioninfo.DefaultRevisionInfoGenerator;
import org.hibernate.envers.internal.revisioninfo.DefaultTrackingModifiedEntitiesRevisionInfoGenerator;
import org.hibernate.envers.internal.revisioninfo.ModifiedEntityNamesReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoGenerator;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.internal.tools.MutableBoolean;
import org.hibernate.envers.internal.tools.Tools;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.AdditionalJaxbRootProducer.AdditionalJaxbRootProducerContext;
import org.hibernate.metamodel.spi.InFlightMetadataCollector;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.SetBinding;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class RevisionInfoConfiguration {
	private String revisionInfoEntityName;
	private PropertyData revisionInfoIdData;
	private PropertyData revisionInfoTimestampData;
	private PropertyData modifiedEntityNamesData;
	private Type revisionInfoTimestampType;
	private GlobalConfiguration globalCfg;

	private String revisionPropType;
	private String revisionPropSqlType;

	public RevisionInfoConfiguration(GlobalConfiguration globalCfg) {
		this.globalCfg = globalCfg;
		if ( globalCfg.isUseRevisionEntityWithNativeId() ) {
			revisionInfoEntityName = "org.hibernate.envers.DefaultRevisionEntity";
		}
		else {
			revisionInfoEntityName = "org.hibernate.envers.enhanced.SequenceIdRevisionEntity";
		}
		revisionInfoIdData = new PropertyData( "id", "id", "field", null );
		revisionInfoTimestampData = new PropertyData( "timestamp", "timestamp", "field", null );
		modifiedEntityNamesData = new PropertyData( "modifiedEntityNames", "modifiedEntityNames", "field", null );
		revisionInfoTimestampType = new LongType();

		revisionPropType = "integer";
	}

	private Document generateDefaultRevisionInfoXmlMapping() {
		final Document document = DocumentFactory.getInstance().createDocument();

		final Element classMapping = MetadataTools.createEntity(
				document,
				new AuditTableData( null, null, globalCfg.getDefaultSchemaName(), globalCfg.getDefaultCatalogName() ),
				null,
				null
		);

		classMapping.addAttribute( "name", revisionInfoEntityName );
		classMapping.addAttribute( "table", "REVINFO" );

		final Element idProperty = MetadataTools.addNativelyGeneratedId(
				classMapping,
				revisionInfoIdData.getName(),
				revisionPropType,
				globalCfg.isUseRevisionEntityWithNativeId(),
				"REV"
		);

		final Element timestampProperty = MetadataTools.addProperty(
				classMapping,
				revisionInfoTimestampData.getName(),
				revisionInfoTimestampType.getName(),
				true,
				false
		);
		MetadataTools.addColumn( timestampProperty, "REVTSTMP", null, null, null, null, null, null, false );

		if ( globalCfg.isTrackEntitiesChangedInRevision() ) {
			generateEntityNamesTrackingTableMapping(
					classMapping,
					"modifiedEntityNames",
					globalCfg.getDefaultSchemaName(),
					globalCfg.getDefaultCatalogName(),
					"REVCHANGES",
					"REV",
					"ENTITYNAME",
					"string"
			);
		}

		return document;
	}

	/**
	 * Generates mapping that represents a set of primitive types.<br />
	 * <code>
	 * &lt;set name="propertyName" table="joinTableName" schema="joinTableSchema" catalog="joinTableCatalog"
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cascade="persist, delete" lazy="false" fetch="join"&gt;<br />
	 * &nbsp;&nbsp;&nbsp;&lt;key column="joinTablePrimaryKeyColumnName" /&gt;<br />
	 * &nbsp;&nbsp;&nbsp;&lt;element type="joinTableValueColumnType"&gt;<br />
	 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;column name="joinTableValueColumnName" /&gt;<br />
	 * &nbsp;&nbsp;&nbsp;&lt;/element&gt;<br />
	 * &lt;/set&gt;
	 * </code>
	 */
	private void generateEntityNamesTrackingTableMapping(
			Element classMapping,
			String propertyName,
			String joinTableSchema,
			String joinTableCatalog,
			String joinTableName,
			String joinTablePrimaryKeyColumnName,
			String joinTableValueColumnName,
			String joinTableValueColumnType) {
		final Element set = classMapping.addElement( "set" );
		set.addAttribute( "name", propertyName );
		set.addAttribute( "table", joinTableName );
		set.addAttribute( "schema", joinTableSchema );
		set.addAttribute( "catalog", joinTableCatalog );
		set.addAttribute( "cascade", "persist, delete" );
		set.addAttribute( "fetch", "join" );
		set.addAttribute( "lazy", "false" );
		final Element key = set.addElement( "key" );
		key.addAttribute( "column", joinTablePrimaryKeyColumnName );
		final Element element = set.addElement( "element" );
		element.addAttribute( "type", joinTableValueColumnType );
		final Element column = element.addElement( "column" );
		column.addAttribute( "name", joinTableValueColumnName );
	}

	private Element generateRevisionInfoRelationMapping() {
		final Document document = DocumentFactory.getInstance().createDocument();
		final Element revRelMapping = document.addElement( "key-many-to-one" );
		// TODO: this does not belong here; does it belong somewhere else????
		//revRelMapping.addAttribute( "type", revisionPropType );
		revRelMapping.addAttribute( "entity-name", revisionInfoEntityName );

		if ( revisionPropSqlType != null ) {
			// Putting a fake name to make Hibernate happy. It will be replaced later anyway.
			MetadataTools.addColumn( revRelMapping, "*", null, null, null, revisionPropSqlType, null, null, false );
		}

		return revRelMapping;
	}

	private void searchForRevisionNumberCfg(
			ClassInfo revisionInfoEntityClassInfo,
			EntityBinding revisionInfoEntityBinding,
			AdditionalJaxbRootProducerContext context,
			MutableBoolean revisionNumberFound) {
		for ( AnnotationInstance annotation : context.getJandexIndex().getAnnotations( EnversDotNames.REVISION_NUMBER ) ) {
			final AnnotationTarget annotationTarget = annotation.target();
			if ( !( annotationTarget instanceof FieldInfo || annotationTarget instanceof MethodInfo ) ) {
				throw new MappingException( "@RevisionNumber is applicable only to fields or properties." );
			}
			if ( Tools.isFieldOrPropertyOfClass(
					annotationTarget,
					revisionInfoEntityClassInfo,
					context.getJandexIndex() ) ) {
				if ( revisionNumberFound.isSet() ) {
					throw new MappingException( "Only one property may be annotated with @RevisionNumber." );
				}

				final String revisionNumberProperty = JandexHelper.getPropertyName( annotationTarget );
				final AttributeBinding revisionNumberAttribute = revisionInfoEntityBinding.locateAttributeBinding(
						revisionNumberProperty
				);
				HibernateTypeDescriptor revisionNumberType = revisionNumberAttribute.getHibernateTypeDescriptor();
				// TODO: Check whether it is required to verify HibernateTypeDescriptor#getJavaTypeName()?
				if ( revisionNumberType.getResolvedTypeMapping() instanceof IntegerType ) {
					revisionPropType = "integer";
				}
				else if ( revisionNumberType.getResolvedTypeMapping() instanceof LongType ) {
					// The default is integer.
					revisionPropType = "long";
				}
				else {
					throw new MappingException(
							"Field annotated with @RevisionNumber must be of type int, Integer, long or Long."
					);
				}

				revisionInfoIdData = new PropertyData(
						revisionNumberProperty,
						revisionNumberProperty,
						revisionNumberAttribute.getPropertyAccessorName(),
						null
				);
				revisionNumberFound.set();


				// Getting the @Column definition of the revision number property, to later use that info to
				// generate the same mapping for the relation from an audit table's revision number to the
				// revision entity revision number.
				final AnnotationInstance jpaColumnAnnotation = JandexHelper.getSingleAnnotation(
						JandexHelper.getMemberAnnotations(
								revisionInfoEntityClassInfo,
								revisionNumberProperty,
								context.getServiceRegistry()
						),
						JPADotNames.COLUMN
				);
				if ( jpaColumnAnnotation != null ) {
					final ClassLoaderService classLoaderService =
							context.getServiceRegistry().getService( ClassLoaderService.class );
					revisionPropSqlType = JandexHelper.getValue(
							jpaColumnAnnotation,
							"columnDefinition",
							String.class,
							classLoaderService
					);
				}
			}
		}
	}

	private void searchForRevisionTimestampCfg(
			ClassInfo revisionInfoEntityClassInfo,
			EntityBinding revisionInfoEntityBinding,
			AdditionalJaxbRootProducerContext context,
			MutableBoolean revisionTimestampFound) {
		final IndexView jandexIndex = context.getJandexIndex();
		for ( AnnotationInstance annotation : jandexIndex.getAnnotations( EnversDotNames.REVISION_TIMESTAMP ) ) {
			AnnotationTarget annotationTarget = annotation.target();
			if ( !( annotationTarget instanceof FieldInfo || annotationTarget instanceof MethodInfo ) ) {
				throw new MappingException( "@RevisionTimestamp is applicable only to fields or properties." );
			}
			if ( Tools.isFieldOrPropertyOfClass( annotationTarget, revisionInfoEntityClassInfo, jandexIndex ) ) {
				if ( revisionTimestampFound.isSet() ) {
					throw new MappingException( "Only one property may be annotated with @RevisionTimestamp." );
				}

				final String revisionTimestampProperty = JandexHelper.getPropertyName( annotationTarget );
				final AttributeBinding revisionTimestampAttribute = revisionInfoEntityBinding.locateAttributeBinding(
						revisionTimestampProperty
				);
				HibernateTypeDescriptor revisionTimestampType = revisionTimestampAttribute.getHibernateTypeDescriptor();
				final String revisionTimestampClassName = revisionTimestampType.getJavaTypeDescriptor().getName().toString();
				if ( Long.TYPE.getName().equals( revisionTimestampClassName ) ||
						Long.class.getName().equals( revisionTimestampClassName ) ||
						java.util.Date.class.getName().equals( revisionTimestampClassName ) ||
						Date.class.getName().equals( revisionTimestampClassName) ) {
					revisionInfoTimestampData = new PropertyData(
							revisionTimestampProperty,
							revisionTimestampProperty,
							revisionTimestampAttribute.getPropertyAccessorName(),
							null
					);
					revisionTimestampFound.set();
				}
				else {
					throw new MappingException(
							"Field annotated with @RevisionTimestamp must be of type long, Long, java.util.Date or java.sql.Date."
					);
				}
			}
		}
	}

	private void searchForModifiedEntityNamesCfg(
			ClassInfo revisionInfoEntityClassInfo,
			EntityBinding revisionInfoEntityBinding,
			AdditionalJaxbRootProducerContext context,
			MutableBoolean modifiedEntityNamesFound) {
		final IndexView jandexIndex = context.getJandexIndex();
		for ( AnnotationInstance annotation : jandexIndex.getAnnotations( EnversDotNames.MODIFIED_ENTITY_NAMES ) ) {
			AnnotationTarget annotationTarget = annotation.target();
			if ( !( annotationTarget instanceof FieldInfo || annotationTarget instanceof MethodInfo ) ) {
				throw new MappingException( "@ModifiedEntityNames is applicable only to fields or properties." );
			}
			if ( Tools.isFieldOrPropertyOfClass( annotationTarget, revisionInfoEntityClassInfo, jandexIndex ) ) {
				if ( modifiedEntityNamesFound.isSet() ) {
					throw new MappingException( "Only one property may be annotated with @ModifiedEntityNames." );
				}

				final String modifiedEntityNamesProperty = JandexHelper.getPropertyName( annotationTarget );
				final AttributeBinding modifiedEntityNamesAttribute = revisionInfoEntityBinding.locateAttributeBinding(
						modifiedEntityNamesProperty
				);

				if ( modifiedEntityNamesAttribute instanceof SetBinding ) {
					final SetBinding collectionBinding = (SetBinding) modifiedEntityNamesAttribute;
					final String elementType =
							collectionBinding
									.getPluralAttributeElementBinding()
									.getHibernateTypeDescriptor()
									.getJavaTypeDescriptor().getName().toString();
					if ( String.class.getName().equals( elementType ) ) {
						modifiedEntityNamesData = new PropertyData(
								modifiedEntityNamesProperty,
								modifiedEntityNamesProperty,
								modifiedEntityNamesAttribute.getPropertyAccessorName(),
								null
						);
						modifiedEntityNamesFound.set();
					}
				}

				if ( !modifiedEntityNamesFound.isSet() ) {
					throw new MappingException(
							"Field annotated with @ModifiedEntityNames must be of Set<String> type."
					);
				}
			}
		}
	}

	private void searchForRevisionInfoCfg(
			ClassInfo revisionInfoEntityClassInfo,
			EntityBinding revisionInfoEntityBinding,
			AdditionalJaxbRootProducerContext context,
			MutableBoolean revisionNumberFound,
			MutableBoolean revisionTimestampFound,
			MutableBoolean modifiedEntityNamesFound) {
		searchForRevisionNumberCfg( revisionInfoEntityClassInfo, revisionInfoEntityBinding, context, revisionNumberFound );
		searchForRevisionTimestampCfg(
				revisionInfoEntityClassInfo,
				revisionInfoEntityBinding,
				context,
				revisionTimestampFound
		);
		searchForModifiedEntityNamesCfg(
				revisionInfoEntityClassInfo,
				revisionInfoEntityBinding,
				context,
				modifiedEntityNamesFound
		);
	}

	public RevisionInfoConfigurationResult configure(
			InFlightMetadataCollector metadataCollector,
			AdditionalJaxbRootProducerContext context) {

		final ClassLoaderService classLoaderService = context.getServiceRegistry().getService( ClassLoaderService.class );

		boolean revisionEntityFound = false;
		RevisionInfoGenerator revisionInfoGenerator = null;
		Class<?> revisionInfoEntityClass = null;

		// Locate @RevisionEntity provided by user and validate its mapping.
		Collection<AnnotationInstance> revisionEntityAnnotations = context.getJandexIndex().getAnnotations(
				EnversDotNames.REVISION_ENTITY
		);
		if ( revisionEntityAnnotations.size() > 1 ) {
			throw new MappingException( "Only one entity may be annotated with @RevisionEntity." );
		}
		if ( revisionEntityAnnotations.size() == 1 ) {
			final AnnotationInstance revisionEntityAnnotation = revisionEntityAnnotations.iterator().next();
			final ClassInfo revisionInfoEntityClassInfo = (ClassInfo) revisionEntityAnnotation.target();

			// TODO: Get rid of revisionInfoEntityClass (don't want to load the class).
			revisionInfoEntityClass = classLoaderService.classForName( revisionInfoEntityClassInfo.name().toString() );

			// TODO: get entity name from @Entity
			revisionInfoEntityName = revisionInfoEntityClass.getName();

			final EntityBinding revisionInfoEntityBinding = metadataCollector.getEntityBinding(
					revisionInfoEntityName
			);

			if ( revisionInfoEntityClassInfo.annotations().containsKey( EnversDotNames.AUDITED ) ) {
				throw new MappingException( "An entity annotated with @RevisionEntity cannot be audited." );
			}

			MutableBoolean revisionNumberFound = new MutableBoolean();
			MutableBoolean revisionTimestampFound = new MutableBoolean();
			MutableBoolean modifiedEntityNamesFound = new MutableBoolean();

			searchForRevisionInfoCfg(
					revisionInfoEntityClassInfo,
					revisionInfoEntityBinding,
					context,
					revisionNumberFound,
					revisionTimestampFound,
					modifiedEntityNamesFound
			);

			if ( !revisionNumberFound.isSet() ) {
				throw new MappingException(
						"An entity annotated with @RevisionEntity must have a field annotated with @RevisionNumber!"
				);
			}

			if ( !revisionTimestampFound.isSet() ) {
				throw new MappingException(
						"An entity annotated with @RevisionEntity must have a field annotated with @RevisionTimestamp!"
				);
			}

			Class<? extends RevisionListener> revisionListenerClass = getRevisionListenerClass(
					classLoaderService,
					revisionEntityAnnotation
			);
			revisionInfoTimestampType =
					revisionInfoEntityBinding.locateAttributeBinding( revisionInfoTimestampData.getName() )
							.getHibernateTypeDescriptor()
							.getResolvedTypeMapping();
			if ( globalCfg.isTrackEntitiesChangedInRevision()
					|| ( globalCfg.isUseRevisionEntityWithNativeId() && DefaultTrackingModifiedEntitiesRevisionEntity.class.isAssignableFrom( revisionInfoEntityClass ) )
					|| ( !globalCfg.isUseRevisionEntityWithNativeId() && SequenceIdTrackingModifiedEntitiesRevisionEntity.class.isAssignableFrom( revisionInfoEntityClass ) )
					|| modifiedEntityNamesFound.isSet() ) {
				// If tracking modified entities parameter is enabled, custom revision info entity is a subtype
				// of DefaultTrackingModifiedEntitiesRevisionEntity class, or @ModifiedEntityNames annotation is used.
				revisionInfoGenerator = new DefaultTrackingModifiedEntitiesRevisionInfoGenerator(
						revisionInfoEntityBinding.getEntity().getName(), revisionInfoEntityClass,
						revisionListenerClass, revisionInfoTimestampData, isTimestampAsDate(), modifiedEntityNamesData
				);
				globalCfg.setTrackEntitiesChangedInRevision( true );
			}
			else {
				revisionInfoGenerator = new DefaultRevisionInfoGenerator(
						revisionInfoEntityBinding.getEntity().getName(), revisionInfoEntityClass,
						revisionListenerClass, revisionInfoTimestampData, isTimestampAsDate()
				);
			}
		}

		// In case of a custom revision info generator, the mapping will be null.
		Document revisionInfoXmlMapping = null;

		if ( revisionInfoGenerator == null ) {
			final Class<? extends RevisionListener> revisionListenerClass = getRevisionListenerClass( classLoaderService, null );
			if ( globalCfg.isTrackEntitiesChangedInRevision() ) {
				revisionInfoEntityClass = globalCfg.isUseRevisionEntityWithNativeId() ?
						DefaultTrackingModifiedEntitiesRevisionEntity.class :
						SequenceIdTrackingModifiedEntitiesRevisionEntity.class;
				revisionInfoEntityName = revisionInfoEntityClass.getName();
				revisionInfoGenerator = new DefaultTrackingModifiedEntitiesRevisionInfoGenerator(
						revisionInfoEntityName, revisionInfoEntityClass,
						revisionListenerClass, revisionInfoTimestampData, isTimestampAsDate(), modifiedEntityNamesData
				);
			}
			else {
				revisionInfoEntityClass = globalCfg.isUseRevisionEntityWithNativeId() ?
						DefaultRevisionEntity.class :
						SequenceIdRevisionEntity.class;
				revisionInfoGenerator = new DefaultRevisionInfoGenerator(
						revisionInfoEntityName, revisionInfoEntityClass,
						revisionListenerClass, revisionInfoTimestampData, isTimestampAsDate()
				);
			}
			revisionInfoXmlMapping = generateDefaultRevisionInfoXmlMapping();
		}

		return new RevisionInfoConfigurationResult(
				revisionInfoGenerator, revisionInfoXmlMapping,
				new RevisionInfoQueryCreator(
						revisionInfoEntityName, revisionInfoIdData.getName(),
						revisionInfoTimestampData.getName(), isTimestampAsDate()
				),
				generateRevisionInfoRelationMapping(),
				new RevisionInfoNumberReader( revisionInfoEntityClass, revisionInfoIdData ),
				globalCfg.isTrackEntitiesChangedInRevision() ? new ModifiedEntityNamesReader(
						revisionInfoEntityClass,
						modifiedEntityNamesData
				)
						: null,
				revisionInfoEntityName, revisionInfoEntityClass, revisionInfoTimestampData
		);
	}

	private boolean isTimestampAsDate() {
		final String typename = revisionInfoTimestampType.getName();
		return "date".equals( typename ) || "time".equals( typename ) || "timestamp".equals( typename );
	}

	/**
	 * Method takes into consideration {@code org.hibernate.envers.revision_listener} parameter and custom
	 * {@link RevisionEntity} annotation.
	 * @param classLoaderService Class loading service.
	 * @param revisionEntityAnnotation User defined @RevisionEntity annotation, or {@code null} if none.
	 * @return Revision listener.
	 */
	private Class<? extends RevisionListener> getRevisionListenerClass(ClassLoaderService classLoaderService,
																	   AnnotationInstance revisionEntityAnnotation) {
		if ( globalCfg.getRevisionListenerClass() != null ) {
			return globalCfg.getRevisionListenerClass();
		}
		if ( revisionEntityAnnotation != null && revisionEntityAnnotation.value() != null ) {
			// User provided revision listener implementation in @RevisionEntity mapping.
			return classLoaderService.classForName( revisionEntityAnnotation.value().asString() );
		}
		return RevisionListener.class;
	}
}
