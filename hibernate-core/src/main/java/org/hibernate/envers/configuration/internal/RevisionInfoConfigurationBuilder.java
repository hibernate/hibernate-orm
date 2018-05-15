/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;
import javax.persistence.Column;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ClassLoadingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.hibernate.envers.boot.spi.AuditMetadataBuilderImplementor;
import org.hibernate.envers.boot.spi.AuditMetadataBuildingOptions;
import org.hibernate.envers.configuration.internal.metadata.AuditTableData;
import org.hibernate.envers.configuration.internal.metadata.MetadataTools;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.internal.EnversMessageLogger;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.RevisionTimestampData;
import org.hibernate.envers.internal.revisioninfo.DefaultRevisionInfoGenerator;
import org.hibernate.envers.internal.revisioninfo.DefaultTrackingModifiedEntitiesRevisionInfoGenerator;
import org.hibernate.envers.internal.revisioninfo.ModifiedEntityNamesReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoGenerator;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.internal.revisioninfo.RevisionTimestampValueResolver;
import org.hibernate.envers.internal.tools.MutableBoolean;
import org.hibernate.internal.util.xml.XMLHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.type.descriptor.java.internal.LongJavaDescriptor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import org.jboss.logging.Logger;

import org.dom4j.Document;
import org.dom4j.Element;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public class RevisionInfoConfigurationBuilder {
	private static final EnversMessageLogger LOG = Logger.getMessageLogger(
			EnversMessageLogger.class,
			RevisionInfoConfigurationBuilder.class.getName()
	);

	private final MetadataImplementor metadata;
	private final ReflectionManager reflectionManager;
	private final XMLHelper xmlHelper;
	private final AuditMetadataBuilderImplementor auditMetadataBuilder;
	private final AuditMetadataBuildingOptions options;

	private String revisionInfoEntityName;
	private PropertyData revisionInfoIdData;
	private RevisionTimestampData revisionInfoTimestampData;
	private PropertyData modifiedEntityNamesData;

	private BasicType revisionPropType;
	private String revisionPropColumnDefinition;

	public RevisionInfoConfigurationBuilder(
			InFlightMetadataCollector metadata,
			AuditMetadataBuilderImplementor auditMetadataBuilder) {
		this.metadata = metadata;
		this.reflectionManager = metadata.getBootstrapContext().getReflectionManager();
		this.options = auditMetadataBuilder.getAuditMetadataBuildingOptions();
		this.auditMetadataBuilder = auditMetadataBuilder;
		this.xmlHelper = new XMLHelper( options.getServiceRegistry().getService( ClassLoaderService.class ) );

		if ( options.isUseRevisionEntityWithNativeIdEnabled() ) {
			revisionInfoEntityName = "org.hibernate.envers.DefaultRevisionEntity";
		}
		else {
			revisionInfoEntityName = "org.hibernate.envers.enhanced.SequenceIdRevisionEntity";
		}

		revisionInfoIdData = new PropertyData( "id", "id", "field" );
		revisionInfoTimestampData = new RevisionTimestampData( "timestamp", "timestamp", "field", LongJavaDescriptor.INSTANCE );
		modifiedEntityNamesData = new PropertyData( "modifiedEntityNames", "modifiedEntityNames", "field" );
		revisionPropType = StandardSpiBasicTypes.INTEGER;
	}

	public RevisionInfoConfiguration build() {
		boolean revisionEntityFound = false;
		RevisionInfoGenerator revisionInfoGenerator = null;
		Class<?> revisionInfoClass = null;
		RevisionTimestampValueResolver timestampValueResolver = null;

		for ( PersistentClass persistentClass : metadata.getEntityBindings() ) {
			// Ensure we're in POJO, not dynamic model, mapping.
			if (persistentClass.getClassName() != null) {
				XClass clazz;
				try {
					clazz = reflectionManager.classForName( persistentClass.getClassName() );
				}
				catch ( ClassLoadingException e ) {
					// todo: log a warning?
					continue;
				}

				final RevisionEntity revisionEntity = clazz.getAnnotation( RevisionEntity.class );
				if ( revisionEntity != null ) {
					if (revisionEntityFound) {
						throw new MappingException("Only one entity may be annotated with @RevisionEntity!");
					}

					// Checking if custom revision entity isn't audited
					if ( clazz.getAnnotation( Audited.class ) != null ) {
						throw new MappingException("An entity annotated with @RevisionEntity cannot be audited!");
					}

					revisionEntityFound = true;

					final MutableBoolean revisionNumberFound = new MutableBoolean();
					final MutableBoolean revisionTimestampFound = new MutableBoolean();
					final MutableBoolean modifiedEntityNamesFound = new MutableBoolean();

					searchForRevisionInfoCfg(
							clazz,
							persistentClass,
							reflectionManager,
							revisionNumberFound,
							revisionTimestampFound,
							modifiedEntityNamesFound
					);

					if (!revisionNumberFound.isSet()) {
						throw new MappingException(
								"An entity annotated with @RevisionEntity must have a field annotated " +
										"with @RevisionNumber!"
						);
					}

					if (!revisionTimestampFound.isSet()) {
						throw new MappingException(
								"An entity annotated with @RevisionEntity must have a field annotated " +
										"with @RevisionTimestamp!"
						);
					}

					revisionInfoEntityName = persistentClass.getEntityName();
					revisionInfoClass = persistentClass.getMappedClass();
					final Class<? extends RevisionListener> revisionListenerClass = getRevisionListenerClass( revisionEntity.value() );

					if ( options.isTrackEntitiesChangedInRevisionEnabled()
							|| ( options.isUseRevisionEntityWithNativeIdEnabled() && DefaultTrackingModifiedEntitiesRevisionEntity.class
							.isAssignableFrom( revisionInfoClass ) )
							|| ( !options.isUseRevisionEntityWithNativeIdEnabled() && SequenceIdTrackingModifiedEntitiesRevisionEntity.class
							.isAssignableFrom( revisionInfoClass ) )
							|| modifiedEntityNamesFound.isSet() ) {

						timestampValueResolver = new RevisionTimestampValueResolver(
								revisionInfoClass,
								revisionInfoTimestampData,
								metadata.getMetadataBuildingOptions().getServiceRegistry()
						);

						// If tracking modified entities parameter is enabled, custom revision info entity is a subtype
						// of DefaultTrackingModifiedEntitiesRevisionEntity class, or @ModifiedEntityNames annotation is used.
						revisionInfoGenerator = new DefaultTrackingModifiedEntitiesRevisionInfoGenerator(
								revisionInfoEntityName,
								revisionInfoClass,
								revisionListenerClass,
								timestampValueResolver,
								modifiedEntityNamesData,
								metadata.getMetadataBuildingOptions().getServiceRegistry()
						);

						auditMetadataBuilder.applyTrackEntitiesChangedInRevision( true );
					}
					else {

						timestampValueResolver = new RevisionTimestampValueResolver(
								revisionInfoClass,
								revisionInfoTimestampData,
								metadata.getMetadataBuildingOptions().getServiceRegistry()
						);

						revisionInfoGenerator = new DefaultRevisionInfoGenerator(
								revisionInfoEntityName,
								revisionInfoClass,
								revisionListenerClass,
								timestampValueResolver
						);
					}
				}
			}
		}

		// In case of a custom revision info generator, the mapping will be null.
		Document revisionInfoXmlMapping = null;

		final Class<? extends RevisionListener> revisionListenerClass = getRevisionListenerClass( RevisionListener.class );

		if ( revisionInfoGenerator == null ) {
			if ( options.isTrackEntitiesChangedInRevisionEnabled() ) {
				revisionInfoClass = options.isUseRevisionEntityWithNativeIdEnabled()
						? DefaultTrackingModifiedEntitiesRevisionEntity.class
						: SequenceIdTrackingModifiedEntitiesRevisionEntity.class;
				revisionInfoEntityName = revisionInfoClass.getName();

				timestampValueResolver = new RevisionTimestampValueResolver(
						revisionInfoClass,
						revisionInfoTimestampData,
						metadata.getMetadataBuildingOptions().getServiceRegistry()
				);

				revisionInfoGenerator = new DefaultTrackingModifiedEntitiesRevisionInfoGenerator(
						revisionInfoEntityName,
						revisionInfoClass,
						revisionListenerClass,
						timestampValueResolver,
						modifiedEntityNamesData,
						metadata.getMetadataBuildingOptions().getServiceRegistry()
				);
			}
			else {
				revisionInfoClass = options.isUseRevisionEntityWithNativeIdEnabled()
						? DefaultRevisionEntity.class
						: SequenceIdRevisionEntity.class;

				timestampValueResolver = new RevisionTimestampValueResolver(
						revisionInfoClass,
						revisionInfoTimestampData,
						metadata.getMetadataBuildingOptions().getServiceRegistry()
				);


				revisionInfoGenerator = new DefaultRevisionInfoGenerator(
						revisionInfoEntityName,
						revisionInfoClass,
						revisionListenerClass,
						timestampValueResolver
				);
			}
			revisionInfoXmlMapping = generateDefaultRevisionInfoXmlMapping();
		}

		auditMetadataBuilder.applyRevisionInfoEntityName( revisionInfoEntityName );

		return new RevisionInfoConfiguration(
				revisionInfoGenerator, revisionInfoXmlMapping,
				new RevisionInfoQueryCreator(
						revisionInfoEntityName,
						revisionInfoIdData.getName(),
						timestampValueResolver
				),
				generateRevisionInfoRelationMapping(),
				new RevisionInfoNumberReader( revisionInfoClass, revisionInfoIdData, metadata.getMetadataBuildingOptions().getServiceRegistry() ),
				options.isTrackEntitiesChangedInRevisionEnabled()
						? new ModifiedEntityNamesReader( revisionInfoClass, modifiedEntityNamesData, metadata.getMetadataBuildingOptions().getServiceRegistry() )
						: null,
				revisionInfoEntityName, revisionInfoClass, revisionInfoTimestampData
		);
	}

	private void searchForRevisionInfoCfgInProperties(
			XClass clazz,
			PersistentClass persistentClass,
			ReflectionManager reflectionManager,
			MutableBoolean revisionNumberFound,
			MutableBoolean revisionTimestampFound,
			MutableBoolean modifiedEntityNamesFound,
			String accessType) {
		for ( XProperty property : clazz.getDeclaredProperties( accessType ) ) {
			final RevisionNumber revisionNumber = property.getAnnotation( RevisionNumber.class );
			final RevisionTimestamp revisionTimestamp = property.getAnnotation( RevisionTimestamp.class );
			final ModifiedEntityNames modifiedEntityNames = property.getAnnotation( ModifiedEntityNames.class );

			if ( revisionNumber != null ) {
				if ( revisionNumberFound.isSet() ) {
					throw new MappingException( "Only one property may be annotated with @RevisionNumber!" );
				}

				final XClass revisionNumberClass = property.getType();
				if ( reflectionManager.equals( revisionNumberClass, Integer.class ) ||
						reflectionManager.equals( revisionNumberClass, Integer.TYPE ) ) {
					revisionInfoIdData = new PropertyData( property.getName(), property.getName(), accessType );
					revisionNumberFound.set();
				}
				else if ( reflectionManager.equals( revisionNumberClass, Long.class ) ||
						reflectionManager.equals( revisionNumberClass, Long.TYPE ) ) {
					revisionInfoIdData = new PropertyData( property.getName(), property.getName(), accessType );
					revisionNumberFound.set();

					// The default is integer
					revisionPropType = StandardSpiBasicTypes.LONG;
				}
				else {
					throw new MappingException(
							"The field annotated with @RevisionNumber must be of type " +
									"int, Integer, long or Long"
					);
				}

				// Getting the @Column definition of the revision number property, to later use that info to
				// generate the same mapping for the relation from an audit table's revision number to the
				// revision entity revision number.
				final Column revisionPropColumn = property.getAnnotation( Column.class );
				if ( revisionPropColumn != null ) {
					revisionPropColumnDefinition = revisionPropColumn.columnDefinition();
				}
			}

			if ( revisionTimestamp != null ) {
				if ( revisionTimestampFound.isSet() ) {
					throw new MappingException( "Only one property may be annotated with @RevisionTimestamp!" );
				}

				if ( isValidTimestampProperty( property.getType(), reflectionManager ) ) {
					final org.hibernate.mapping.Property value = persistentClass.getProperty( property.getName() );
					revisionInfoTimestampData = new RevisionTimestampData(
							property.getName(),
							property.getName(),
							accessType,
							value.getValueMapping().getJavaTypeMapping().getJavaTypeDescriptor()
					);
					revisionTimestampFound.set();
				}
				else {
					throw new MappingException(
							"The field annotated with @RevisionTimestamp must be of type " +
									"long, Long, LocalDateTime, java.util.Date or java.sql.Date"
					);
				}
			}

			if ( modifiedEntityNames != null ) {
				if ( modifiedEntityNamesFound.isSet() ) {
					throw new MappingException( "Only one property may be annotated with @ModifiedEntityNames!" );
				}
				final XClass modifiedEntityNamesClass = property.getType();
				if ( reflectionManager.equals( modifiedEntityNamesClass, Set.class ) &&
						reflectionManager.equals( property.getElementClass(), String.class ) ) {
					modifiedEntityNamesData = new PropertyData(
							property.getName(),
							property.getName(),
							accessType
					);
					modifiedEntityNamesFound.set();
				}
				else {
					throw new MappingException(
							"The field annotated with @ModifiedEntityNames must be of Set<String> type."
					);
				}
			}
		}
	}

	private void searchForRevisionInfoCfg(
			XClass clazz,
			PersistentClass persistentClass,
			ReflectionManager reflectionManager,
			MutableBoolean revisionNumberFound,
			MutableBoolean revisionTimestampFound,
			MutableBoolean modifiedEntityNamesFound) {
		final XClass superclazz = clazz.getSuperclass();
		if ( !"java.lang.Object".equals( superclazz.getName() ) ) {
			searchForRevisionInfoCfg(
					superclazz,
					persistentClass,
					reflectionManager,
					revisionNumberFound,
					revisionTimestampFound,
					modifiedEntityNamesFound
			);
		}

		searchForRevisionInfoCfgInProperties(
				clazz,
				persistentClass,
				reflectionManager,
				revisionNumberFound,
				revisionTimestampFound,
				modifiedEntityNamesFound,
				"field"
		);
		searchForRevisionInfoCfgInProperties(
				clazz,
				persistentClass,
				reflectionManager,
				revisionNumberFound,
				revisionTimestampFound,
				modifiedEntityNamesFound,
				"property"
		);
	}

	private boolean isValidTimestampProperty(XClass revisionTimestampClass, ReflectionManager reflectionManager) {
		return reflectionManager.equals( revisionTimestampClass, Long.class )
				|| reflectionManager.equals( revisionTimestampClass, Long.TYPE )
				|| reflectionManager.equals( revisionTimestampClass, Date.class )
				|| reflectionManager.equals( revisionTimestampClass, java.sql.Date.class )
				|| reflectionManager.equals( revisionTimestampClass, LocalDateTime.class );
	}

	/**
	 * @param defaultListener Revision listener that shall be applied if {@code org.hibernate.envers.revision_listener}
	 * parameter has not been set.
	 *
	 * @return Revision listener.
	 */
	private Class<? extends RevisionListener> getRevisionListenerClass(Class<? extends RevisionListener> defaultListener) {
		if ( options.getRevisionListenerClass() != null ) {
			return options.getRevisionListenerClass();
		}
		return defaultListener;
	}

	private Document generateDefaultRevisionInfoXmlMapping() {
		final Document document = xmlHelper.getDocumentFactory().createDocument();

		final Element classMapping = MetadataTools.createEntity(
				document,
				new AuditTableData( null, null, options.getDefaultSchemaName(), options.getDefaultCatalogName() ),
				null,
				null
		);

		classMapping.addAttribute( "name", revisionInfoEntityName );
		classMapping.addAttribute( "table", "REVINFO" );

		final Element idProperty = MetadataTools.addNativelyGeneratedId(
				classMapping,
				revisionInfoIdData.getName(),
				revisionPropType.getJavaTypeDescriptor().getTypeName(),
				options.isUseRevisionEntityWithNativeIdEnabled()
		);
		MetadataTools.addColumn( idProperty, "REV", null, null, null, null, null, null, false );

		final Element timestampProperty = MetadataTools.addProperty(
				classMapping,
				revisionInfoTimestampData.getName(),
				revisionInfoTimestampData.getJavaTypeDescriptor().getTypeName(),
				true,
				false
		);
		MetadataTools.addColumn( timestampProperty, "REVTSTMP", null, null, null, null, null, null, false );

		if ( options.isTrackEntitiesChangedInRevisionEnabled() ) {
			generateEntityNamesTrackingTableMapping(
					classMapping,
					"modifiedEntityNames",
					options.getDefaultSchemaName(),
					options.getDefaultCatalogName(),
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
		final Document document = xmlHelper.getDocumentFactory().createDocument();
		final Element revRelMapping = document.addElement( "key-many-to-one" );
		revRelMapping.addAttribute( "type", getBasicTypeSqlType( revisionPropType ) );
		revRelMapping.addAttribute( "class", revisionInfoEntityName );

		if ( revisionPropColumnDefinition != null ) {
			// Putting a fake name to make Hibernate happy. It will be replaced later anyway.
			MetadataTools.addColumn( revRelMapping, "*", null, null, null, revisionPropColumnDefinition, null, null, false );
		}

		return revRelMapping;
	}

	private String getBasicTypeSqlType(BasicType basicType) {
		final int sqlType = basicType.getSqlTypeDescriptor().getJdbcTypeCode();
		return metadata.getDatabase().getDialect().getTypeName( sqlType );
	}
}
