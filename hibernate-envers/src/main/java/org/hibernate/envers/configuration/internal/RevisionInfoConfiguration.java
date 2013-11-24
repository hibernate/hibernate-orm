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

import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import javax.persistence.Column;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.Audited;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.DefaultTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.hibernate.envers.configuration.internal.metadata.AuditTableData;
import org.hibernate.envers.configuration.internal.metadata.MetadataTools;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;
import org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionEntity;
import org.hibernate.envers.internal.entities.PropertyData;
import org.hibernate.envers.internal.revisioninfo.DefaultRevisionInfoGenerator;
import org.hibernate.envers.internal.revisioninfo.DefaultTrackingModifiedEntitiesRevisionInfoGenerator;
import org.hibernate.envers.internal.revisioninfo.ModifiedEntityNamesReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoGenerator;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.internal.tools.MutableBoolean;
import org.hibernate.internal.util.xml.XMLHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;

import org.dom4j.Document;
import org.dom4j.Element;

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
		final Document document = XMLHelper.getDocumentFactory().createDocument();

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
				globalCfg.isUseRevisionEntityWithNativeId()
		);
		MetadataTools.addColumn( idProperty, "REV", null, null, null, null, null, null, false );

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
		final Document document = XMLHelper.getDocumentFactory().createDocument();
		final Element revRelMapping = document.addElement( "key-many-to-one" );
		revRelMapping.addAttribute( "type", revisionPropType );
		revRelMapping.addAttribute( "class", revisionInfoEntityName );

		if ( revisionPropSqlType != null ) {
			// Putting a fake name to make Hibernate happy. It will be replaced later anyway.
			MetadataTools.addColumn( revRelMapping, "*", null, null, null, revisionPropSqlType, null, null, false );
		}

		return revRelMapping;
	}

	private void searchForRevisionInfoCfgInProperties(
			XClass clazz,
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
					revisionInfoIdData = new PropertyData( property.getName(), property.getName(), accessType, null );
					revisionNumberFound.set();
				}
				else if ( reflectionManager.equals( revisionNumberClass, Long.class ) ||
						reflectionManager.equals( revisionNumberClass, Long.TYPE ) ) {
					revisionInfoIdData = new PropertyData( property.getName(), property.getName(), accessType, null );
					revisionNumberFound.set();

					// The default is integer
					revisionPropType = "long";
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
					revisionPropSqlType = revisionPropColumn.columnDefinition();
				}
			}

			if ( revisionTimestamp != null ) {
				if ( revisionTimestampFound.isSet() ) {
					throw new MappingException( "Only one property may be annotated with @RevisionTimestamp!" );
				}

				final XClass revisionTimestampClass = property.getType();
				if ( reflectionManager.equals( revisionTimestampClass, Long.class ) ||
						reflectionManager.equals( revisionTimestampClass, Long.TYPE ) ||
						reflectionManager.equals( revisionTimestampClass, Date.class ) ||
						reflectionManager.equals( revisionTimestampClass, java.sql.Date.class ) ) {
					revisionInfoTimestampData = new PropertyData(
							property.getName(),
							property.getName(),
							accessType,
							null
					);
					revisionTimestampFound.set();
				}
				else {
					throw new MappingException(
							"The field annotated with @RevisionTimestamp must be of type " +
									"long, Long, java.util.Date or java.sql.Date"
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
							accessType,
							null
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
			XClass clazz, ReflectionManager reflectionManager,
			MutableBoolean revisionNumberFound, MutableBoolean revisionTimestampFound,
			MutableBoolean modifiedEntityNamesFound) {
		final XClass superclazz = clazz.getSuperclass();
		if ( !"java.lang.Object".equals( superclazz.getName() ) ) {
			searchForRevisionInfoCfg(
					superclazz,
					reflectionManager,
					revisionNumberFound,
					revisionTimestampFound,
					modifiedEntityNamesFound
			);
		}

		searchForRevisionInfoCfgInProperties(
				clazz, reflectionManager, revisionNumberFound, revisionTimestampFound,
				modifiedEntityNamesFound, "field"
		);
		searchForRevisionInfoCfgInProperties(
				clazz, reflectionManager, revisionNumberFound, revisionTimestampFound,
				modifiedEntityNamesFound, "property"
		);
	}

	public RevisionInfoConfigurationResult configure(Configuration cfg, ReflectionManager reflectionManager) {
		boolean revisionEntityFound = false;
		RevisionInfoGenerator revisionInfoGenerator = null;
		Class<?> revisionInfoClass = null;

		final Iterator<PersistentClass> classes = cfg.getClassMappings();
		while ( classes.hasNext() ) {
			PersistentClass pc = classes.next();
			XClass clazz;
			try {
				clazz = reflectionManager.classForName( pc.getClassName(), this.getClass() );
			}
			catch (ClassNotFoundException e) {
				throw new MappingException( e );
			}

			final RevisionEntity revisionEntity = clazz.getAnnotation( RevisionEntity.class );
			if ( revisionEntity != null ) {
				if ( revisionEntityFound ) {
					throw new MappingException( "Only one entity may be annotated with @RevisionEntity!" );
				}

				// Checking if custom revision entity isn't audited
				if ( clazz.getAnnotation( Audited.class ) != null ) {
					throw new MappingException( "An entity annotated with @RevisionEntity cannot be audited!" );
				}

				revisionEntityFound = true;

				final MutableBoolean revisionNumberFound = new MutableBoolean();
				final MutableBoolean revisionTimestampFound = new MutableBoolean();
				final MutableBoolean modifiedEntityNamesFound = new MutableBoolean();

				searchForRevisionInfoCfg(
						clazz,
						reflectionManager,
						revisionNumberFound,
						revisionTimestampFound,
						modifiedEntityNamesFound
				);

				if ( !revisionNumberFound.isSet() ) {
					throw new MappingException(
							"An entity annotated with @RevisionEntity must have a field annotated " +
									"with @RevisionNumber!"
					);
				}

				if ( !revisionTimestampFound.isSet() ) {
					throw new MappingException(
							"An entity annotated with @RevisionEntity must have a field annotated " +
									"with @RevisionTimestamp!"
					);
				}

				revisionInfoEntityName = pc.getEntityName();
				revisionInfoClass = pc.getMappedClass();
				final Class<? extends RevisionListener> revisionListenerClass = getRevisionListenerClass( revisionEntity.value() );
				revisionInfoTimestampType = pc.getProperty( revisionInfoTimestampData.getName() ).getType();
				if ( globalCfg.isTrackEntitiesChangedInRevision()
						|| (globalCfg.isUseRevisionEntityWithNativeId() && DefaultTrackingModifiedEntitiesRevisionEntity.class
						.isAssignableFrom( revisionInfoClass ))
						|| (!globalCfg.isUseRevisionEntityWithNativeId() && SequenceIdTrackingModifiedEntitiesRevisionEntity.class
						.isAssignableFrom( revisionInfoClass ))
						|| modifiedEntityNamesFound.isSet() ) {
					// If tracking modified entities parameter is enabled, custom revision info entity is a subtype
					// of DefaultTrackingModifiedEntitiesRevisionEntity class, or @ModifiedEntityNames annotation is used.
					revisionInfoGenerator = new DefaultTrackingModifiedEntitiesRevisionInfoGenerator(
							revisionInfoEntityName,
							revisionInfoClass, revisionListenerClass, revisionInfoTimestampData, isTimestampAsDate(),
							modifiedEntityNamesData
					);
					globalCfg.setTrackEntitiesChangedInRevision( true );
				}
				else {
					revisionInfoGenerator = new DefaultRevisionInfoGenerator(
							revisionInfoEntityName, revisionInfoClass,
							revisionListenerClass, revisionInfoTimestampData, isTimestampAsDate()
					);
				}
			}
		}

		// In case of a custom revision info generator, the mapping will be null.
		Document revisionInfoXmlMapping = null;

		final Class<? extends RevisionListener> revisionListenerClass = getRevisionListenerClass( RevisionListener.class );

		if ( revisionInfoGenerator == null ) {
			if ( globalCfg.isTrackEntitiesChangedInRevision() ) {
				revisionInfoClass = globalCfg.isUseRevisionEntityWithNativeId() ?
						DefaultTrackingModifiedEntitiesRevisionEntity.class
						:
						SequenceIdTrackingModifiedEntitiesRevisionEntity.class;
				revisionInfoEntityName = revisionInfoClass.getName();
				revisionInfoGenerator = new DefaultTrackingModifiedEntitiesRevisionInfoGenerator(
						revisionInfoEntityName, revisionInfoClass,
						revisionListenerClass, revisionInfoTimestampData, isTimestampAsDate(), modifiedEntityNamesData
				);
			}
			else {
				revisionInfoClass = globalCfg.isUseRevisionEntityWithNativeId() ? DefaultRevisionEntity.class
						: SequenceIdRevisionEntity.class;
				revisionInfoGenerator = new DefaultRevisionInfoGenerator(
						revisionInfoEntityName, revisionInfoClass,
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
				new RevisionInfoNumberReader( revisionInfoClass, revisionInfoIdData ),
				globalCfg.isTrackEntitiesChangedInRevision() ? new ModifiedEntityNamesReader(
						revisionInfoClass,
						modifiedEntityNamesData
				)
						: null,
				revisionInfoEntityName, revisionInfoClass, revisionInfoTimestampData
		);
	}

	private boolean isTimestampAsDate() {
		final String typename = revisionInfoTimestampType.getName();
		return "date".equals( typename ) || "time".equals( typename ) || "timestamp".equals( typename );
	}

	/**
	 * @param defaultListener Revision listener that shall be applied if {@code org.hibernate.envers.revision_listener}
	 * parameter has not been set.
	 *
	 * @return Revision listener.
	 */
	private Class<? extends RevisionListener> getRevisionListenerClass(Class<? extends RevisionListener> defaultListener) {
		if ( globalCfg.getRevisionListenerClass() != null ) {
			return globalCfg.getRevisionListenerClass();
		}
		return defaultListener;
	}
}
