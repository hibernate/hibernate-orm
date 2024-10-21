/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration.internal;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import jakarta.persistence.Column;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.envers.boot.model.Attribute;
import org.hibernate.envers.boot.model.BasicAttribute;
import org.hibernate.envers.boot.model.ManyToOneAttribute;
import org.hibernate.envers.boot.model.RootPersistentEntity;
import org.hibernate.envers.boot.model.SetAttribute;
import org.hibernate.envers.boot.model.SimpleIdentifier;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.envers.configuration.internal.metadata.AuditTableData;
import org.hibernate.envers.enhanced.OrderedSequenceGenerator;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.entities.RevisionTimestampData;
import org.hibernate.envers.internal.entities.mappings.DefaultRevisionEntityImpl;
import org.hibernate.envers.internal.entities.mappings.DefaultTrackingModifiedEntitiesRevisionEntityImpl;
import org.hibernate.envers.internal.entities.mappings.enhanced.SequenceIdRevisionEntityImpl;
import org.hibernate.envers.internal.entities.mappings.enhanced.SequenceIdTrackingModifiedEntitiesRevisionEntityImpl;
import org.hibernate.envers.internal.revisioninfo.DefaultRevisionInfoGenerator;
import org.hibernate.envers.internal.revisioninfo.DefaultTrackingModifiedEntitiesRevisionInfoGenerator;
import org.hibernate.envers.internal.revisioninfo.ModifiedEntityNamesReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoGenerator;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.internal.revisioninfo.RevisionTimestampValueResolver;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public class RevisionInfoConfiguration {

	// todo: should these defaults also come in from configuration; there are some overlaps.
	private static final String DEFAULT_REVISION_ENTITY_TABLE_NAME = "REVINFO";
	private static final String DEFAULT_REVISION_SEQUENCE_NAME = "REVISION_GENERATOR";
	private static final String DEFAULT_REVISION_SEQUENCE_TABLE_NAME = "REVISION_GENERATOR";
	private static final String DEFAULT_REVISION_FIELD_NAME = "REV";
	private static final String DEFAULT_REVISION_TIMESTAMP_FIELD_NAME = "REVTSTMP";
	private static final String DEFAULT_REVCHANGES_TABLE_NAME = "REVCHANGES";
	private static final String DEFAULT_REVCHANGES_ENTITY_COLUMN_NAME = "ENTITYNAME";

	private final Configuration configuration;
	private final RevisionInfoGenerator revisionInfoGenerator;
	private final RevisionInfoNumberReader revisionInfoNumberReader;
	private final RevisionInfoQueryCreator revisionInfoQueryCreator;
	private final ModifiedEntityNamesReader modifiedEntityNamesReader;
	private final String revisionInfoEntityName;
	private final PropertyData revisionInfoTimestampData;
	private final String revisionInfoTimestampTypeName;
	private final String revisionPropType;
	private final String revisionPropSqlType;
	private final String revisionInfoIdName;
	private final Class<?> revisionInfoClass;
	private final boolean useDefaultRevisionInfoMapping;

	public RevisionInfoConfiguration(Configuration config, MetadataImplementor metadata, ReflectionManager reflectionManager) {
		this.configuration = config;

		// Generate the resolver metadata
		RevisionEntityResolver resolver = new RevisionEntityResolver( metadata, reflectionManager );

		// initialize attributes from resolver
		this.revisionInfoClass = resolver.revisionInfoClass;
		this.revisionInfoEntityName = resolver.revisionInfoEntityName;
		this.revisionPropType = resolver.revisionPropType;
		this.revisionPropSqlType = resolver.revisionPropSqlType;
		this.revisionInfoTimestampData = resolver.revisionInfoTimestampData;
		this.revisionInfoTimestampTypeName = resolver.revisionInfoTimestampTypeName;
		this.revisionInfoIdName = resolver.revisionInfoIdData.getName();
		this.useDefaultRevisionInfoMapping = resolver.useDefaultRevisionInfoMapping;

		revisionInfoGenerator = resolver.revisionInfoGenerator;

		revisionInfoNumberReader = new RevisionInfoNumberReader(
				resolver.revisionInfoClass,
				resolver.revisionInfoIdData,
				metadata.getMetadataBuildingOptions().getServiceRegistry(),
				revisionInfoGenerator
		);

		revisionInfoQueryCreator = new RevisionInfoQueryCreator(
				resolver.revisionInfoEntityName,
				resolver.revisionInfoIdData.getName(),
				resolver.timestampValueResolver
		);

		if ( configuration.isTrackEntitiesChanged() ) {
			modifiedEntityNamesReader = new ModifiedEntityNamesReader(
					resolver.revisionInfoClass,
					resolver.modifiedEntityNamesData,
					metadata.getMetadataBuildingOptions().getServiceRegistry()
			);
		}
		else {
			modifiedEntityNamesReader = null;
		}
	}

	public String getRevisionInfoEntityName() {
		return revisionInfoEntityName;
	}

	public String getRevisionInfoPropertyType() {
		return revisionPropType;
	}

	public Class<?> getRevisionInfoClass() {
		return revisionInfoClass;
	}

	public PropertyData getRevisionInfoTimestampData() {
		return revisionInfoTimestampData;
	}

	public RevisionInfoGenerator getRevisionInfoGenerator() {
		return revisionInfoGenerator;
	}

	public RevisionInfoQueryCreator getRevisionInfoQueryCreator() {
		return revisionInfoQueryCreator;
	}

	public RevisionInfoNumberReader getRevisionInfoNumberReader() {
		return revisionInfoNumberReader;
	}

	public ModifiedEntityNamesReader getModifiedEntityNamesReader() {
		return modifiedEntityNamesReader;
	}

	public RootPersistentEntity getRevisionInfoMapping() {
		return useDefaultRevisionInfoMapping ? generateDefaultRevisionInfoMapping( revisionInfoIdName ) : null;
	}
	
	public Attribute getRevisionInfoRelationMapping() {
		final ManyToOneAttribute attribute = new ManyToOneAttribute(
				configuration.getRevisionFieldName(),
				revisionPropType,
				true,
				false,
				true,
				revisionInfoEntityName
		);

		attribute.setOnDelete( configuration.isCascadeDeleteRevision() ? "cascade" : null );

		attribute.addColumn(
				new org.hibernate.envers.boot.model.Column(
						configuration.getRevisionFieldName(),
						null,
						null,
						null,
						revisionPropSqlType,
						null,
						null
				)
		);

		return attribute;		
	}

	private RootPersistentEntity generateDefaultRevisionInfoMapping(String revisionInfoIdName) {
		RootPersistentEntity mapping = new RootPersistentEntity(
				new AuditTableData( null, null, configuration.getDefaultSchemaName(), configuration.getDefaultCatalogName() ),
				revisionInfoClass,
				revisionInfoEntityName,
				DEFAULT_REVISION_ENTITY_TABLE_NAME
		);

		final SimpleIdentifier identifier = new SimpleIdentifier( revisionInfoIdName, revisionPropType );
		if ( configuration.isNativeIdEnabled() ) {
			identifier.setGeneratorClass( "native" );
		}
		else {
			identifier.setGeneratorClass( OrderedSequenceGenerator.class.getName() );
			identifier.setParameter( "sequence_name", DEFAULT_REVISION_SEQUENCE_NAME );
			identifier.setParameter( "table_name", DEFAULT_REVISION_SEQUENCE_TABLE_NAME );
			identifier.setParameter( "initial_value", "1" );
			identifier.setParameter( "increment_size", "1" );
			if ( configuration.isRevisionSequenceNoCache() ) {
				identifier.setParameter( "nocache", "true" );
			}
		}

		identifier.addColumn( createColumn( DEFAULT_REVISION_FIELD_NAME, null ) );
		mapping.setIdentifier( identifier );

		BasicAttribute timestampAttribute = new BasicAttribute(
				revisionInfoTimestampData.getName(),
				revisionInfoTimestampTypeName,
				true,
				false
		);

		timestampAttribute.addColumn( createColumn( DEFAULT_REVISION_TIMESTAMP_FIELD_NAME, null ) );
		mapping.addAttribute( timestampAttribute );

		if ( configuration.isTrackEntitiesChanged() ) {
			final String schema = configuration.getDefaultSchemaName();
			final String catalog = configuration.getDefaultCatalogName();

			final SetAttribute set = new SetAttribute( "modifiedEntityNames", DEFAULT_REVCHANGES_TABLE_NAME, schema, catalog );
			set.setCascade( "persist, delete" );
			set.setFetch( "join" );
			set.setLazy( "false" );
			set.setKeyColumn( "REV" );
			set.setElementType( "string" );
			set.setColumnName( DEFAULT_REVCHANGES_ENTITY_COLUMN_NAME );
			mapping.addAttribute( set );
		}

		return mapping;
	}

	private org.hibernate.envers.boot.model.Column createColumn(String name, String type) {
		return new org.hibernate.envers.boot.model.Column( name, null, null, null, type, null, null );
	}

	private RevisionTimestampValueResolver createRevisionTimestampResolver(
			Class<?> revisionInfoClass,
			PropertyData revisionInfoTimestampData,
			String typeName,
			ServiceRegistry serviceRegistry) {
		return new RevisionTimestampValueResolver(
				revisionInfoClass,
				new RevisionTimestampData(
						revisionInfoTimestampData.getName(),
						revisionInfoTimestampData.getBeanName(),
						revisionInfoTimestampData.getAccessType(),
						typeName
				),
				serviceRegistry
		);
	}

	private class RevisionEntityResolver {

		private final MetadataImplementor metadata;
		private final ReflectionManager reflectionManager;

		private boolean revisionEntityFound;
		private boolean revisionNumberFound;
		private boolean revisionTimestampFound;
		private boolean modifiedEntityNamesFound;
		private String revisionInfoEntityName;
		private Class<?> revisionInfoClass;
		private Class<? extends RevisionListener> revisionListenerClass;
		private RevisionInfoGenerator revisionInfoGenerator;
		private boolean useDefaultRevisionInfoMapping;
		private PropertyData revisionInfoIdData;
		private PropertyData revisionInfoTimestampData;
		private PropertyData modifiedEntityNamesData;
		private String revisionInfoTimestampTypeName;
		private String revisionPropType;
		private String revisionPropSqlType;
		private RevisionTimestampValueResolver timestampValueResolver;

		public RevisionEntityResolver(MetadataImplementor metadata, ReflectionManager reflectionManager) {
			this.metadata = metadata;
			this.reflectionManager = reflectionManager;
			this.revisionInfoIdData = createPropertyData( "id", "field" );
			this.revisionInfoTimestampData = createPropertyData( "timestamp", "field" );
			this.modifiedEntityNamesData = createPropertyData( "modifiedEntityNames", "field" );
			this.revisionInfoTimestampTypeName = "long";
			this.revisionPropType = "integer";

			// automatically initiates a revision entity search over metadata sources
			locateRevisionEntityMapping();
		}

		private void locateRevisionEntityMapping() {
			for ( PersistentClass persistentClass : metadata.getEntityBindings() ) {
				// Only process POJO models, not dynamic models
				if ( persistentClass.getClassName() == null ) {
					continue;
				}

				XClass clazz = reflectionManager.toXClass( persistentClass.getMappedClass() );
				final RevisionEntity revisionEntity = clazz.getAnnotation( RevisionEntity.class );
				if ( revisionEntity == null ) {
					// not annotated, skip
					continue;
				}

				if ( revisionEntityFound ) {
					throw new EnversMappingException( "Only one entity can be annotated with @RevisionEntity" );
				}

				// Verify that the revision entity isn't audited
				if ( clazz.getAnnotation( Audited.class ) != null ) {
					throw new EnversMappingException( "The @RevisionEntity entity cannot be audited" );
				}

				revisionEntityFound = true;

				resolveConfiguration( clazz );

				if ( !revisionNumberFound || !revisionTimestampFound ) {
					// A revision number and timestamp fields must be annotated or the revision entity mapping
					// is to be considered in error and a mapping exception should be thrown.
					throw new EnversMappingException(
							String.format(
									Locale.ENGLISH,
									"An entity annotated with @RevisionEntity must have a field annotated with %s",
									!revisionNumberFound ? "@RevisionNumber" : "@RevisionTimestamp"
							)
					);
				}

				revisionInfoEntityName = persistentClass.getEntityName();
				revisionInfoClass = persistentClass.getMappedClass();
				revisionListenerClass = getRevisionListenerClass( revisionEntity.value() );

				final Property timestampProperty = persistentClass.getProperty( revisionInfoTimestampData.getName() );
				revisionInfoTimestampTypeName = timestampProperty.getType().getName();

				timestampValueResolver = createRevisionTimestampResolver(
						revisionInfoClass,
						revisionInfoTimestampData,
						revisionInfoTimestampTypeName,
						metadata.getMetadataBuildingOptions().getServiceRegistry()
				);

				if ( useEntityTrackingRevisionEntity( revisionInfoClass ) ) {
					// If tracking modified entities is enabled, custom revision info entity is a subtype
					// of DefaultTrackingModifiedEntitiesRevisionEntity class or @ModifiedEntityNames was used
					revisionInfoGenerator = new DefaultTrackingModifiedEntitiesRevisionInfoGenerator(
							revisionInfoEntityName,
							revisionInfoClass,
							revisionListenerClass,
							timestampValueResolver,
							modifiedEntityNamesData,
							metadata.getMetadataBuildingOptions().getServiceRegistry()
					);
					configuration.setTrackEntitiesChanged( true );
				}
				else {
					revisionInfoGenerator = new DefaultRevisionInfoGenerator(
							revisionInfoEntityName,
							revisionInfoClass,
							revisionListenerClass,
							timestampValueResolver,
							metadata.getMetadataBuildingOptions().getServiceRegistry()
					);
				}
			}

			if ( revisionInfoGenerator == null ) {

				revisionListenerClass = getRevisionListenerClass( RevisionListener.class );

				if ( configuration.isTrackEntitiesChanged() ) {
					revisionInfoClass = configuration.isNativeIdEnabled()
							? DefaultTrackingModifiedEntitiesRevisionEntityImpl.class
							: SequenceIdTrackingModifiedEntitiesRevisionEntityImpl.class;
				}
				else {
					revisionInfoClass = configuration.isNativeIdEnabled()
							? DefaultRevisionEntityImpl.class
							: SequenceIdRevisionEntityImpl.class;
				}
				revisionInfoEntityName = revisionInfoClass.getName();

				timestampValueResolver = createRevisionTimestampResolver(
						revisionInfoClass,
						revisionInfoTimestampData,
						revisionInfoTimestampTypeName,
						metadata.getMetadataBuildingOptions().getServiceRegistry()
				);

				if ( configuration.isTrackEntitiesChanged() ) {
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
					revisionInfoGenerator = new DefaultRevisionInfoGenerator(
							revisionInfoEntityName,
							revisionInfoClass,
							revisionListenerClass,
							timestampValueResolver,
							metadata.getMetadataBuildingOptions().getServiceRegistry()
					);
				}

				useDefaultRevisionInfoMapping = true;
			}
		}

		private boolean useEntityTrackingRevisionEntity(Class<?> clazz) {
			return configuration.isTrackEntitiesChanged()
					|| ( configuration.isNativeIdEnabled() && DefaultTrackingModifiedEntitiesRevisionEntity.class.isAssignableFrom( clazz ) )
					|| ( !configuration.isNativeIdEnabled() && SequenceIdTrackingModifiedEntitiesRevisionEntity.class.isAssignableFrom( clazz ) )
					|| modifiedEntityNamesFound;
		}

		private void resolveConfiguration(XClass clazz) {
			final XClass superclazz = clazz.getSuperclass();
			if ( !Object.class.getName().equals( superclazz.getName() ) ) {
				// traverse to the top of the entity hierarchy
				resolveConfiguration( superclazz );
			}
			resolveConfigurationFromProperties( clazz, "field" );
			resolveConfigurationFromProperties( clazz, "property" );
		}

		private void resolveConfigurationFromProperties(XClass clazz, String accessType) {
			for ( XProperty property : clazz.getDeclaredProperties( accessType ) ) {
				final RevisionNumber revisionNumber = property.getAnnotation( RevisionNumber.class );
				if ( revisionNumber != null ) {
					resolveRevisionNumberFromProperty( property, accessType );
				}

				final RevisionTimestamp revisionTimestamp = property.getAnnotation( RevisionTimestamp.class );
				if ( revisionTimestamp != null ) {
					resolveRevisionTimestampFromProperty( property, accessType );
				}

				final ModifiedEntityNames modifiedEntityNames = property.getAnnotation( ModifiedEntityNames.class );
				if ( modifiedEntityNames != null ) {
					resolveModifiedEntityNamesFromProperty( property, accessType );
				}
			}
		}

		private void resolveRevisionNumberFromProperty(XProperty property, String accessType) {
			if ( revisionNumberFound ) {
				throw new EnversMappingException( "Only one property can be defined with @RevisionNumber" );
			}

			final XClass propertyType = property.getType();
			if ( isAnyType( propertyType, Integer.class, Integer.TYPE ) ) {
				revisionInfoIdData = createPropertyData( property, accessType );
				revisionNumberFound = true;
			}
			else if ( isAnyType( propertyType, Long.class, Long.TYPE ) ) {
				revisionInfoIdData = createPropertyData( property, accessType );
				revisionPropType = "long";
				revisionNumberFound = true;
			}
			else {
				throwUnexpectedAnnotatedType( property, RevisionNumber.class, "int, Integer, long, or Long" );
			}

			// Getting the @Column definition of the revision number property, to later use that information
			// to generate the same mapping for the relation from an audit table's revision number to the
			// revision entity's revision number field.
			final Column column = property.getAnnotation( Column.class );
			if ( column != null ) {
				revisionPropSqlType = column.columnDefinition();
			}
		}
		
		private void resolveRevisionTimestampFromProperty(XProperty property, String accessType) {
			if ( revisionTimestampFound ) {
				throw new EnversMappingException( "Only one property can be defined with @RevisionTimestamp" );
			}

			final XClass propertyType = property.getType();
			if ( isAnyType( propertyType, Long.class, Long.TYPE, Date.class, LocalDateTime.class, Instant.class, java.sql.Date.class ) ) {
				revisionInfoTimestampData = createPropertyData( property, accessType );
				revisionTimestampFound = true;
			}
			else {
				throwUnexpectedAnnotatedType( property, RevisionTimestamp.class, "long, Long, Date, LocalDateTime, Instant, or java.sql.Date" );
			}
		}
		
		private void resolveModifiedEntityNamesFromProperty(XProperty property, String accessType) {
			if ( modifiedEntityNamesFound ) {
				throw new EnversMappingException( "Only one property can be defined with @ModifiedEntityNames" );
			}

			final XClass propertyType = property.getType();
			if ( isAnyType( propertyType, Set.class ) ) {
				final XClass elementType = property.getElementClass();
				if ( isAnyType( elementType, String.class ) ) {
					modifiedEntityNamesData = createPropertyData( property, accessType );
					modifiedEntityNamesFound = true;
					return;
				}
			}

			throwUnexpectedAnnotatedType( property, ModifiedEntityNames.class, "Set<String>" );
		}

		private PropertyData createPropertyData(XProperty property, String accessType) {
			return createPropertyData( property.getName(), accessType );
		}

		private PropertyData createPropertyData(String name, String accessType) {
			return new PropertyData( name, name, accessType );
		}

		private boolean isAnyType(XClass clazz, Class<?>... types) {
			for ( Class<?> type : types ) {
				if ( isType( clazz, type ) ) {
					return true;
				}
			}
			return false;
		}

		private boolean isType(XClass clazz, Class<?> type) {
			return reflectionManager.equals( clazz, type );
		}

		private Class<? extends RevisionListener> getRevisionListenerClass(Class<? extends RevisionListener> defaultListener) {
			if ( configuration.getRevisionListenerClass() != null ) {
				return configuration.getRevisionListenerClass();
			}
			return defaultListener;
		}
		
		private void throwUnexpectedAnnotatedType(XProperty property, Class<?> annotation, String allowedTypes) {
			throw new EnversMappingException(
					String.format(
							Locale.ENGLISH,
							"The field '%s' annotated with '@%s' must be of type: %s",
							property.getName(),
							annotation.getName(),
							allowedTypes
					)
			);
		}
	}
}
