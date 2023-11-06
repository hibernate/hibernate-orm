/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.hibernate.annotations.common.reflection.AnnotationReader;
import org.hibernate.annotations.common.reflection.ReflectionUtil;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAssociationOverrideImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributeOverrideImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbAttributesContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbBasicImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCachingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCascadeTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbCollectionTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbColumnResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConfigurationParameterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConstructorResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConvertImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbDiscriminatorColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbElementCollectionImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddedImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmptyTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntity;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenersImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityOrMappedSuperclass;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFieldResultImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbGeneratedValueImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdClassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbIndexImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbInheritanceImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinTableCapable;
import org.hibernate.boot.jaxb.mapping.spi.JaxbJoinTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLifecycleCallbackContainer;
import org.hibernate.boot.jaxb.mapping.spi.JaxbLobImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManagedType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManyToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMapKeyClassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMapKeyColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMapKeyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMapKeyJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedAttributeNodeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedEntityGraphImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedNativeQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedStoredProcedureQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedSubgraphImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToManyImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOneToOneImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbOrderColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPrimaryKeyJoinColumnImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbQueryHintImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSecondaryTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSequenceGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSqlResultSetMappingImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbStoredProcedureParameterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSynchronizedTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTenantIdImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUniqueConstraintImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUserTypeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbUuidGeneratorImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbVersionImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jpa.AvailableHints;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AssociationOverrides;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Convert;
import jakarta.persistence.Converts;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EntityResult;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ExcludeDefaultListeners;
import jakarta.persistence.ExcludeSuperclassListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.FieldResult;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.SecondaryTables;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import static org.hibernate.boot.model.internal.HCANNHelper.hasAnnotation;
import static org.hibernate.boot.model.internal.PropertyMappingElementCollector.JAXB_TRANSIENT_NAME;
import static org.hibernate.boot.model.internal.PropertyMappingElementCollector.PERSISTENT_ATTRIBUTE_NAME;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * Encapsulates the overriding of Java annotations from an EJB 3.0 descriptor (orm.xml, ...).
 *
 * @author Paolo Perrotta
 * @author Davide Marchignoli
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@SuppressWarnings({ "unchecked", "ToArrayCallWithZeroLengthArrayArgument" })
public class JPAXMLOverriddenAnnotationReader implements AnnotationReader {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( JPAXMLOverriddenAnnotationReader.class );

	private static final String SCHEMA_VALIDATION = "Activate schema validation for more information";
	private static final String WORD_SEPARATOR = "-";

	private enum PropertyType {
		PROPERTY,
		FIELD,
		METHOD
	}

	private static final Map<Class<? extends Annotation>, String> annotationToXml;

	static {
		annotationToXml = new HashMap<>();
		annotationToXml.put( Entity.class, "entity" );
		annotationToXml.put( MappedSuperclass.class, "mapped-superclass" );
		annotationToXml.put( Embeddable.class, "embeddable" );
		annotationToXml.put( Table.class, "table" );
		annotationToXml.put( SecondaryTable.class, "secondary-table" );
		annotationToXml.put( SecondaryTables.class, "secondary-table" );
		annotationToXml.put( PrimaryKeyJoinColumn.class, "primary-key-join-column" );
		annotationToXml.put( PrimaryKeyJoinColumns.class, "primary-key-join-column" );
		annotationToXml.put( IdClass.class, "id-class" );
		annotationToXml.put( Inheritance.class, "inheritance" );
		annotationToXml.put( DiscriminatorValue.class, "discriminator-value" );
		annotationToXml.put( DiscriminatorColumn.class, "discriminator-column" );
		annotationToXml.put( SequenceGenerator.class, "sequence-generator" );
		annotationToXml.put( TableGenerator.class, "table-generator" );
		annotationToXml.put( NamedEntityGraph.class, "named-entity-graph" );
		annotationToXml.put( NamedEntityGraphs.class, "named-entity-graph" );
		annotationToXml.put( NamedQuery.class, "named-query" );
		annotationToXml.put( NamedQueries.class, "named-query" );
		annotationToXml.put( NamedNativeQuery.class, "named-native-query" );
		annotationToXml.put( NamedNativeQueries.class, "named-native-query" );
		annotationToXml.put( NamedStoredProcedureQuery.class, "named-stored-procedure-query" );
		annotationToXml.put( NamedStoredProcedureQueries.class, "named-stored-procedure-query" );
		annotationToXml.put( SqlResultSetMapping.class, "sql-result-set-mapping" );
		annotationToXml.put( SqlResultSetMappings.class, "sql-result-set-mapping" );
		annotationToXml.put( ExcludeDefaultListeners.class, "exclude-default-listeners" );
		annotationToXml.put( ExcludeSuperclassListeners.class, "exclude-superclass-listeners" );
//		annotationToXml.put( AccessType.class, "access" );
		// FIXME: adding same annotation as a key multiple times:
		annotationToXml.put( AttributeOverride.class, "attribute-override" );
		annotationToXml.put( AttributeOverrides.class, "attribute-override" );
		annotationToXml.put( AttributeOverride.class, "association-override" );
		annotationToXml.put( AttributeOverrides.class, "association-override" );
		annotationToXml.put( AttributeOverride.class, "map-key-attribute-override" );
		annotationToXml.put( AttributeOverrides.class, "map-key-attribute-override" );
		annotationToXml.put( Id.class, "id" );
		annotationToXml.put( EmbeddedId.class, "embedded-id" );
		annotationToXml.put( GeneratedValue.class, "generated-value" );
		annotationToXml.put( Column.class, "column" );
		annotationToXml.put( Columns.class, "column" );
		annotationToXml.put( Temporal.class, "temporal" );
		annotationToXml.put( Lob.class, "lob" );
		annotationToXml.put( Enumerated.class, "enumerated" );
		annotationToXml.put( Version.class, "version" );
		annotationToXml.put( Transient.class, "transient" );
		annotationToXml.put( Basic.class, "basic" );
		annotationToXml.put( Embedded.class, "embedded" );
		annotationToXml.put( ManyToOne.class, "many-to-one" );
		annotationToXml.put( OneToOne.class, "one-to-one" );
		annotationToXml.put( OneToMany.class, "one-to-many" );
		annotationToXml.put( ManyToMany.class, "many-to-many" );
		annotationToXml.put( Any.class, "any" );
		annotationToXml.put( ManyToAny.class, "many-to-any" );
		annotationToXml.put( JoinTable.class, "join-table" );
		annotationToXml.put( JoinColumn.class, "join-column" );
		annotationToXml.put( JoinColumns.class, "join-column" );
		annotationToXml.put( MapKey.class, "map-key" );
		annotationToXml.put( OrderBy.class, "order-by" );
		annotationToXml.put( EntityListeners.class, "entity-listeners" );
		annotationToXml.put( PrePersist.class, "pre-persist" );
		annotationToXml.put( PreRemove.class, "pre-remove" );
		annotationToXml.put( PreUpdate.class, "pre-update" );
		annotationToXml.put( PostPersist.class, "post-persist" );
		annotationToXml.put( PostRemove.class, "post-remove" );
		annotationToXml.put( PostUpdate.class, "post-update" );
		annotationToXml.put( PostLoad.class, "post-load" );
		annotationToXml.put( CollectionTable.class, "collection-table" );
		annotationToXml.put( MapKeyClass.class, "map-key-class" );
		annotationToXml.put( MapKeyTemporal.class, "map-key-temporal" );
		annotationToXml.put( MapKeyEnumerated.class, "map-key-enumerated" );
		annotationToXml.put( MapKeyColumn.class, "map-key-column" );
		annotationToXml.put( MapKeyJoinColumn.class, "map-key-join-column" );
		annotationToXml.put( MapKeyJoinColumns.class, "map-key-join-column" );
		annotationToXml.put( OrderColumn.class, "order-column" );
		annotationToXml.put( Cacheable.class, "cacheable" );
		annotationToXml.put( Index.class, "index" );
		annotationToXml.put( ForeignKey.class, "foreign-key" );
		annotationToXml.put( Convert.class, "convert" );
		annotationToXml.put( Converts.class, "convert" );
		annotationToXml.put( ConstructorResult.class, "constructor-result" );
		annotationToXml.put( Type.class, "type" );
		annotationToXml.put( JdbcTypeCode.class, "jdbc-type-code" );
		annotationToXml.put( UuidGenerator.class, "uuid-generator" );
	}

	private final XMLContext xmlContext;
	private final ClassLoaderAccess classLoaderAccess;
	private final AnnotatedElement element;
	private final String className;
	private final String propertyName;
	private final PropertyType propertyType;
	private transient Annotation[] annotations;
	private transient Map<Class, Annotation> annotationsMap;
	private transient PropertyMappingElementCollector elementsForProperty;
	private AccessibleObject mirroredAttribute;

	JPAXMLOverriddenAnnotationReader(
			AnnotatedElement el,
			XMLContext xmlContext,
			ClassLoaderAccess classLoaderAccess) {
		this.element = el;
		this.xmlContext = xmlContext;
		this.classLoaderAccess = classLoaderAccess;

		if ( el instanceof Class ) {
			Class clazz = (Class) el;
			className = clazz.getName();
			propertyName = null;
			propertyType = null;
		}
		else if ( el instanceof Field ) {
			Field field = (Field) el;
			className = field.getDeclaringClass().getName();
			propertyName = field.getName();
			propertyType = PropertyType.FIELD;
			String expectedGetter = "get" + Character.toUpperCase( propertyName.charAt( 0 ) ) + propertyName.substring(
					1
			);
			try {
				mirroredAttribute = field.getDeclaringClass().getDeclaredMethod( expectedGetter );
			}
			catch (NoSuchMethodException e) {
				//no method
			}
		}
		else if ( el instanceof Method ) {
			Method method = (Method) el;
			className = method.getDeclaringClass().getName();
			String methodName = method.getName();

			// YUCK!  The null here is the 'boundType', we'd rather get the TypeEnvironment()
			if ( ReflectionUtil.isProperty( method, null, PersistentAttributeFilter.INSTANCE ) ) {
				if ( methodName.startsWith( "get" ) ) {
					propertyName = Introspector.decapitalize( methodName.substring( "get".length() ) );
				}
				else if ( methodName.startsWith( "is" ) ) {
					propertyName = Introspector.decapitalize( methodName.substring( "is".length() ) );
				}
				else {
					throw new RuntimeException( "Method " + methodName + " is not a property getter" );
				}
				propertyType = PropertyType.PROPERTY;
				try {
					mirroredAttribute = method.getDeclaringClass().getDeclaredField( propertyName );
				}
				catch (NoSuchFieldException e) {
					//no method
				}
			}
			else {
				propertyName = methodName;
				propertyType = PropertyType.METHOD;
			}
		}
		else {
			className = null;
			propertyName = null;
			propertyType = null;
		}
	}

	// For tests only
	public JPAXMLOverriddenAnnotationReader(
			AnnotatedElement el,
			XMLContext xmlContext,
			BootstrapContext bootstrapContext) {
		this( el, xmlContext, bootstrapContext.getClassLoaderAccess() );
	}

	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		initAnnotations();
		return (T) annotationsMap.get( annotationType );
	}

	public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
		initAnnotations();
		return annotationsMap.containsKey( annotationType );
	}

	public Annotation[] getAnnotations() {
		initAnnotations();
		return annotations;
	}

	/*
	 * The idea is to create annotation proxies for the xml configuration elements. Using this proxy annotations together
	 * with the {@link JPAMetadataProvider} allows to handle xml configuration the same way as annotation configuration.
	 */
	private void initAnnotations() {
		if ( annotations == null ) {
			// We don't want the global catalog and schema here: they are applied much later,
			// when SQL gets rendered.
			XMLContext.Default defaults = xmlContext.getDefaultWithoutGlobalCatalogAndSchema( className );
			if ( className != null && propertyName == null ) {
				//is a class
				final JaxbManagedType managedTypeOverride = xmlContext.getManagedTypeOverride( className );
				Annotation[] annotations = getPhysicalAnnotations();
				List<Annotation> annotationList = new ArrayList<>( annotations.length + 5 );
				annotationsMap = new HashMap<>( annotations.length + 5 );
				for ( Annotation annotation : annotations ) {
					if ( !annotationToXml.containsKey( annotation.annotationType() ) ) {
						//unknown annotations are left over
						annotationList.add( annotation );
					}
				}
				addIfNotNull( annotationList, getEntity( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getMappedSuperclass( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getEmbeddable( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getTable( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getSecondaryTables( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getPrimaryKeyJoinColumns( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getIdClass( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getCacheable( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getCaching( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getInheritance( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getDiscriminatorValue( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getDiscriminatorColumn( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getSequenceGenerator( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getTableGenerator( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getNamedQueries( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getNamedNativeQueries( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getNamedStoredProcedureQueries( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getNamedEntityGraphs( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getSqlResultSetMappings( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getExcludeDefaultListeners( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getExcludeSuperclassListeners( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getAccessType( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getAttributeOverrides( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getAssociationOverrides( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getEntityListeners( managedTypeOverride, defaults ) );
				addIfNotNull( annotationList, getConverts( managedTypeOverride, defaults ) );

				this.annotations = annotationList.toArray( new Annotation[0] );
				for ( Annotation ann : this.annotations ) {
					annotationsMap.put( ann.annotationType(), ann );
				}
				checkForOrphanProperties( managedTypeOverride );
			}
			else if ( className != null ) { //&& propertyName != null ) { //always true but less confusing
				final JaxbManagedType managedTypeOverride = xmlContext.getManagedTypeOverride( className );
				final JaxbEntityListenerImpl entityListenerOverride = xmlContext.getEntityListenerOverride( className );
				Annotation[] annotations = getPhysicalAnnotations();
				List<Annotation> annotationList = new ArrayList<>( annotations.length + 5 );
				annotationsMap = new HashMap<>( annotations.length + 5 );
				for ( Annotation annotation : annotations ) {
					if ( !annotationToXml.containsKey( annotation.annotationType() ) ) {
						//unknown annotations are left over
						annotationList.add( annotation );
					}
				}
				preCalculateElementsForProperty( managedTypeOverride, entityListenerOverride );
				Transient transientAnn = getTransient( defaults );
				if ( transientAnn != null ) {
					annotationList.add( transientAnn );
				}
				else {
					if ( defaults.canUseJavaAnnotations() ) {
						Annotation annotation = getPhysicalAnnotation( Access.class );
						addIfNotNull( annotationList, annotation );
					}
					getId( annotationList, defaults );
					getEmbeddedId( annotationList, defaults );
					getEmbedded( annotationList, defaults );
					getBasic( annotationList, defaults );
					getVersion( annotationList, defaults );
					getManyToOne( annotationList, defaults );
					getOneToOne( annotationList, defaults );
					getOneToMany( annotationList, defaults );
					getManyToMany( annotationList, defaults );
					getAny( annotationList, defaults );
					getManyToAny( annotationList, defaults );
					getElementCollection( annotationList, defaults );
					addIfNotNull( annotationList, getSequenceGenerator( elementsForProperty, defaults ) );
					addIfNotNull( annotationList, getTableGenerator( elementsForProperty, defaults ) );
					addIfNotNull( annotationList, getConvertsForAttribute( elementsForProperty, defaults ) );
				}
				processEventAnnotations( annotationList, defaults );
				//FIXME use annotationsMap rather than annotationList this will be faster since the annotation type is usually known at put() time
				this.annotations = annotationList.toArray( new Annotation[0] );
				for ( Annotation ann : this.annotations ) {
					annotationsMap.put( ann.annotationType(), ann );
				}
			}
			else {
				this.annotations = getPhysicalAnnotations();
				annotationsMap = new HashMap<>( annotations.length + 5 );
				for ( Annotation ann : this.annotations ) {
					annotationsMap.put( ann.annotationType(), ann );
				}
			}
		}
	}

	private Annotation getConvertsForAttribute(PropertyMappingElementCollector elementsForProperty, XMLContext.Default defaults) {
		final Map<String, Convert> convertAnnotationsMap = new HashMap<>();

		for ( JaxbBasicImpl element : elementsForProperty.getBasic() ) {
			JaxbConvertImpl convert = element.getConvert();
			if ( convert != null ) {
				applyXmlDefinedConverts( Collections.singletonList( convert ), defaults, null,
						convertAnnotationsMap );
			}
		}
		for ( JaxbEmbeddedImpl element : elementsForProperty.getEmbedded() ) {
			applyXmlDefinedConverts( element.getConvert(), defaults, propertyName, convertAnnotationsMap );
		}
		for ( JaxbElementCollectionImpl element : elementsForProperty.getElementCollection() ) {
			applyXmlDefinedConverts( element.getConvert(), defaults, propertyName, convertAnnotationsMap );
		}

		// NOTE : per section 12.2.3.16 of the spec <convert/> is additive, although only if "metadata-complete" is not
		// specified in the XML

		if ( defaults.canUseJavaAnnotations() ) {
			// todo : note sure how to best handle attributeNamePrefix here
			applyPhysicalConvertAnnotations( propertyName, convertAnnotationsMap );
		}

		if ( !convertAnnotationsMap.isEmpty() ) {
			final AnnotationDescriptor groupingDescriptor = new AnnotationDescriptor( Converts.class );
			groupingDescriptor.setValue(
					"value",
					convertAnnotationsMap.values().toArray( new Convert[0] )
			);
			return AnnotationFactory.create( groupingDescriptor );
		}

		return null;
	}

	private Converts getConverts(JaxbManagedType root, XMLContext.Default defaults) {
		// NOTE : we use a map here to make sure that an xml and annotation referring to the same attribute
		// properly overrides.  Bit sparse, but easy...
		final Map<String, Convert> convertAnnotationsMap = new HashMap<>();

		if ( root instanceof JaxbEntityImpl ) {
			applyXmlDefinedConverts( ( (JaxbEntityImpl) root ).getConverts(), defaults, null, convertAnnotationsMap );
		}

		// NOTE : per section 12.2.3.16 of the spec <convert/> is additive, although only if "metadata-complete" is not
		// specified in the XML

		if ( defaults.canUseJavaAnnotations() ) {
			applyPhysicalConvertAnnotations( null, convertAnnotationsMap );
		}

		if ( !convertAnnotationsMap.isEmpty() ) {
			final AnnotationDescriptor groupingDescriptor = new AnnotationDescriptor( Converts.class );
			groupingDescriptor.setValue(
					"value",
					convertAnnotationsMap.values().toArray( new Convert[0] )
			);
			return AnnotationFactory.create( groupingDescriptor );
		}

		return null;
	}

	private void applyXmlDefinedConverts(
			List<JaxbConvertImpl> elements,
			XMLContext.Default defaults,
			String attributeNamePrefix,
			Map<String, Convert> convertAnnotationsMap) {
		for ( JaxbConvertImpl convertElement : elements ) {
			final AnnotationDescriptor convertAnnotationDescriptor = new AnnotationDescriptor( Convert.class );
			copyAttribute( convertAnnotationDescriptor, "attribute-name", convertElement.getAttributeName(), false );
			copyAttribute( convertAnnotationDescriptor, "disable-conversion", convertElement.isDisableConversion(), false );

			final String converter = convertElement.getConverter();
			if ( converter != null ) {
				final String converterClassName = XMLContext.buildSafeClassName(
						converter,
						defaults
				);
				try {
					final Class<?> converterClass = classLoaderAccess.classForName( converterClassName );
					convertAnnotationDescriptor.setValue( "converter", converterClass );
				}
				catch (ClassLoadingException e) {
					throw new AnnotationException( "Unable to find specified converter class id-class: " + converterClassName, e );
				}
			}
			final Convert convertAnnotation = AnnotationFactory.create( convertAnnotationDescriptor );
			final String qualifiedAttributeName = qualifyConverterAttributeName(
					attributeNamePrefix,
					convertAnnotation.attributeName()
			);
			convertAnnotationsMap.put( qualifiedAttributeName, convertAnnotation );
		}

	}

	private String qualifyConverterAttributeName(String attributeNamePrefix, String specifiedAttributeName) {
		String qualifiedAttributeName;
		if ( isNotEmpty( specifiedAttributeName ) ) {
			if ( isNotEmpty( attributeNamePrefix ) ) {
				qualifiedAttributeName = attributeNamePrefix + '.' + specifiedAttributeName;
			}
			else {
				qualifiedAttributeName = specifiedAttributeName;
			}
		}
		else {
			qualifiedAttributeName = "";
		}
		return qualifiedAttributeName;
	}

	private void applyPhysicalConvertAnnotations(
			String attributeNamePrefix,
			Map<String, Convert> convertAnnotationsMap) {
		final Convert physicalAnnotation = getPhysicalAnnotation( Convert.class );
		if ( physicalAnnotation != null ) {
			// only add if no XML element named a converter for this attribute
			final String qualifiedAttributeName = qualifyConverterAttributeName( attributeNamePrefix, physicalAnnotation.attributeName() );
			if ( !convertAnnotationsMap.containsKey( qualifiedAttributeName ) ) {
				convertAnnotationsMap.put( qualifiedAttributeName, physicalAnnotation );
			}
		}
		final Converts physicalGroupingAnnotation = getPhysicalAnnotation( Converts.class );
		if ( physicalGroupingAnnotation != null ) {
			for ( Convert convertAnnotation : physicalGroupingAnnotation.value() ) {
				// again, only add if no XML element named a converter for this attribute
				final String qualifiedAttributeName = qualifyConverterAttributeName( attributeNamePrefix, convertAnnotation.attributeName() );
				if ( !convertAnnotationsMap.containsKey( qualifiedAttributeName ) ) {
					convertAnnotationsMap.put( qualifiedAttributeName, convertAnnotation );
				}
			}
		}
	}

	private void checkForOrphanProperties(JaxbManagedType root) {
		Class<?> clazz;
		try {
			clazz = classLoaderAccess.classForName( className );
		}
		catch (ClassLoadingException e) {
			//a primitive type most likely
			return;
		}
		JaxbAttributesContainer container = root != null ? root.getAttributes() : null;
		//put entity.attributes elements
		if ( container != null ) {
			//precompute the list of properties
			//TODO is it really useful...
			Set<String> properties = new HashSet<>();
			for ( Field field : clazz.getFields() ) {
				properties.add( field.getName() );
			}
			for ( Method method : clazz.getMethods() ) {
				String name = method.getName();
				if ( name.startsWith( "get" ) ) {
					properties.add( Introspector.decapitalize( name.substring( "get".length() ) ) );
				}
				else if ( name.startsWith( "is" ) ) {
					properties.add( Introspector.decapitalize( name.substring( "is".length() ) ) );
				}
			}
			if ( container instanceof JaxbAttributesContainerImpl jaxbAttributes ) {
				checkForOrphanProperties( jaxbAttributes.getIdAttributes(), properties, PERSISTENT_ATTRIBUTE_NAME );
				checkForOrphanProperties( jaxbAttributes.getEmbeddedIdAttribute(), properties, PERSISTENT_ATTRIBUTE_NAME );
				checkForOrphanProperties( jaxbAttributes.getVersion(), properties, PERSISTENT_ATTRIBUTE_NAME );
			}
			checkForOrphanProperties( container.getBasicAttributes(), properties, PERSISTENT_ATTRIBUTE_NAME );
			checkForOrphanProperties( container.getManyToOneAttributes(), properties, PERSISTENT_ATTRIBUTE_NAME );
			checkForOrphanProperties( container.getOneToManyAttributes(), properties, PERSISTENT_ATTRIBUTE_NAME );
			checkForOrphanProperties( container.getOneToOneAttributes(), properties, PERSISTENT_ATTRIBUTE_NAME );
			checkForOrphanProperties( container.getManyToManyAttributes(), properties, PERSISTENT_ATTRIBUTE_NAME );
			checkForOrphanProperties( container.getElementCollectionAttributes(), properties, PERSISTENT_ATTRIBUTE_NAME );
			checkForOrphanProperties( container.getEmbeddedAttributes(), properties, PERSISTENT_ATTRIBUTE_NAME );
			checkForOrphanProperties( container.getTransients(), properties, JAXB_TRANSIENT_NAME );
		}
	}

	private <T> void checkForOrphanProperties(
			List<T> elements,
			Set<String> properties,
			Function<? super T, String> nameGetter) {
		for ( T element : elements ) {
			checkForOrphanProperties( element, properties, nameGetter );
		}
	}

	private <T> void checkForOrphanProperties(
			T element,
			Set<String> properties,
			Function<? super T, String> nameGetter) {
		if ( element == null ) {
			return;
		}
		String propertyName = nameGetter.apply( element );
		if ( !properties.contains( propertyName ) ) {
			LOG.propertyNotFound( StringHelper.qualify( className, propertyName ) );
		}
	}

	/**
	 * Adds {@code annotation} to the list (only if it's not null) and then returns it.
	 *
	 * @param annotationList The list of annotations.
	 * @param annotation The annotation to add to the list.
	 * @return The annotation which was added to the list or {@code null}.
	 */
	private void addIfNotNull(List<Annotation> annotationList, Annotation annotation) {
		if ( annotation != null ) {
			annotationList.add( annotation );
		}
	}

	private Annotation getTableGenerator(PropertyMappingElementCollector elementsForProperty, XMLContext.Default defaults) {
		for ( JaxbIdImpl element : elementsForProperty.getId() ) {
			JaxbTableGeneratorImpl subelement = element.getTableGenerator();
			if ( subelement != null ) {
				return buildTableGeneratorAnnotation( subelement, defaults );
			}
		}
		if ( elementsForProperty.isEmpty() && defaults.canUseJavaAnnotations() ) {
			return getPhysicalAnnotation( TableGenerator.class );
		}
		else {
			return null;
		}
	}

	private Annotation getSequenceGenerator(PropertyMappingElementCollector elementsForProperty, XMLContext.Default defaults) {
		for ( JaxbIdImpl element : elementsForProperty.getId() ) {
			JaxbSequenceGeneratorImpl subelement = element.getSequenceGenerator();
			if ( subelement != null ) {
				return buildSequenceGeneratorAnnotation( subelement );
			}
		}
		if ( elementsForProperty.isEmpty() && defaults.canUseJavaAnnotations() ) {
			return getPhysicalAnnotation( SequenceGenerator.class );
		}
		else {
			return null;
		}
	}

	private void processEventAnnotations(List<Annotation> annotationList, XMLContext.Default defaults) {
		boolean eventElement = false;
		if ( !elementsForProperty.getPrePersist().isEmpty() ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( PrePersist.class );
			annotationList.add( AnnotationFactory.create( ad ) );
			eventElement = true;
		}
		else if ( !elementsForProperty.getPreRemove().isEmpty() ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( PreRemove.class );
			annotationList.add( AnnotationFactory.create( ad ) );
			eventElement = true;
		}
		else if ( !elementsForProperty.getPreUpdate().isEmpty() ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( PreUpdate.class );
			annotationList.add( AnnotationFactory.create( ad ) );
			eventElement = true;
		}
		else if ( !elementsForProperty.getPostPersist().isEmpty() ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( PostPersist.class );
			annotationList.add( AnnotationFactory.create( ad ) );
			eventElement = true;
		}
		else if ( !elementsForProperty.getPostRemove().isEmpty() ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( PostRemove.class );
			annotationList.add( AnnotationFactory.create( ad ) );
			eventElement = true;
		}
		else if ( !elementsForProperty.getPostUpdate().isEmpty() ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( PostUpdate.class );
			annotationList.add( AnnotationFactory.create( ad ) );
			eventElement = true;
		}
		else if ( !elementsForProperty.getPostLoad().isEmpty() ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( PostLoad.class );
			annotationList.add( AnnotationFactory.create( ad ) );
			eventElement = true;
		}
		if ( !eventElement && defaults.canUseJavaAnnotations() ) {
			Annotation ann = getPhysicalAnnotation( PrePersist.class );
			addIfNotNull( annotationList, ann );
			ann = getPhysicalAnnotation( PreRemove.class );
			addIfNotNull( annotationList, ann );
			ann = getPhysicalAnnotation( PreUpdate.class );
			addIfNotNull( annotationList, ann );
			ann = getPhysicalAnnotation( PostPersist.class );
			addIfNotNull( annotationList, ann );
			ann = getPhysicalAnnotation( PostRemove.class );
			addIfNotNull( annotationList, ann );
			ann = getPhysicalAnnotation( PostUpdate.class );
			addIfNotNull( annotationList, ann );
			ann = getPhysicalAnnotation( PostLoad.class );
			addIfNotNull( annotationList, ann );
		}
	}

	private EntityListeners getEntityListeners(JaxbManagedType root, XMLContext.Default defaults) {
		JaxbEntityListenersImpl element = root instanceof JaxbEntityOrMappedSuperclass ? ( (JaxbEntityOrMappedSuperclass) root ).getEntityListeners() : null;
		if ( element != null ) {
			final List<Class<?>> entityListenerClasses = new ArrayList<>();
			for ( JaxbEntityListenerImpl subelement : element.getEntityListener() ) {
				String className = subelement.getClazz();
				try {
					entityListenerClasses.add(
							classLoaderAccess.classForName(
									XMLContext.buildSafeClassName( className, defaults )
							)
					);
				}
				catch (ClassLoadingException e) {
					throw new AnnotationException(
							"Unable to find class: " + className, e
					);
				}
			}
			AnnotationDescriptor ad = new AnnotationDescriptor( EntityListeners.class );
			ad.setValue( "value", entityListenerClasses.toArray( new Class[ 0 ] ) );
			return AnnotationFactory.create( ad );
		}
		else if ( defaults.canUseJavaAnnotations() ) {
			return getPhysicalAnnotation( EntityListeners.class );
		}
		else {
			return null;
		}
	}

	private JoinTable overridesDefaultsInJoinTable(Annotation annotation, XMLContext.Default defaults) {
		//no element but might have some default or some annotation
		boolean defaultToJoinTable = !( isPhysicalAnnotationPresent( JoinColumn.class )
				|| isPhysicalAnnotationPresent( JoinColumns.class ) );
		final Class<? extends Annotation> annotationClass = annotation.annotationType();
		defaultToJoinTable = defaultToJoinTable &&
				( ( annotationClass == ManyToMany.class && isEmpty( ( (ManyToMany) annotation ).mappedBy() ) )
						|| ( annotationClass == OneToMany.class && isEmpty( ( (OneToMany) annotation ).mappedBy() ) )
						|| ( annotationClass == ElementCollection.class )
				);
		final Class<JoinTable> annotationType = JoinTable.class;
		if ( defaultToJoinTable
				&& ( isNotEmpty( defaults.getCatalog() )
				|| isNotEmpty( defaults.getSchema() ) ) ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( annotationType );
			if ( defaults.canUseJavaAnnotations() ) {
				JoinTable table = getPhysicalAnnotation( annotationType );
				if ( table != null ) {
					ad.setValue( "name", table.name() );
					ad.setValue( "schema", table.schema() );
					ad.setValue( "catalog", table.catalog() );
					ad.setValue( "uniqueConstraints", table.uniqueConstraints() );
					ad.setValue( "joinColumns", table.joinColumns() );
					ad.setValue( "inverseJoinColumns", table.inverseJoinColumns() );
				}
			}
			if ( isEmpty( (String) ad.valueOf( "schema" ) )
					&& isNotEmpty( defaults.getSchema() ) ) {
				ad.setValue( "schema", defaults.getSchema() );
			}
			if ( isEmpty( (String) ad.valueOf( "catalog" ) )
					&& isNotEmpty( defaults.getCatalog() ) ) {
				ad.setValue( "catalog", defaults.getCatalog() );
			}
			return AnnotationFactory.create( ad );
		}
		else if ( defaults.canUseJavaAnnotations() ) {
			return getPhysicalAnnotation( annotationType );
		}
		else {
			return null;
		}
	}

	private Annotation overridesDefaultCascadePersist(Annotation annotation, XMLContext.Default defaults) {
		if ( Boolean.TRUE.equals( defaults.getCascadePersist() ) ) {
			final Class<? extends Annotation> annotationType = annotation.annotationType();

			if ( annotationType == ManyToOne.class ) {
				ManyToOne manyToOne = (ManyToOne) annotation;
				List<CascadeType> cascades = new ArrayList<>( Arrays.asList( manyToOne.cascade() ) );
				if ( !cascades.contains( CascadeType.ALL ) && !cascades.contains( CascadeType.PERSIST ) ) {
					cascades.add( CascadeType.PERSIST );
				}
				else {
					return annotation;
				}

				AnnotationDescriptor ad = new AnnotationDescriptor( annotationType );
				ad.setValue( "cascade", cascades.toArray( new CascadeType[] {} ) );
				ad.setValue( "targetEntity", manyToOne.targetEntity() );
				ad.setValue( "fetch", manyToOne.fetch() );
				ad.setValue( "optional", manyToOne.optional() );

				return AnnotationFactory.create( ad );
			}
			else if ( annotationType == OneToOne.class ) {
				OneToOne oneToOne = (OneToOne) annotation;
				List<CascadeType> cascades = new ArrayList<>( Arrays.asList( oneToOne.cascade() ) );
				if ( !cascades.contains( CascadeType.ALL ) && !cascades.contains( CascadeType.PERSIST ) ) {
					cascades.add( CascadeType.PERSIST );
				}
				else {
					return annotation;
				}

				AnnotationDescriptor ad = new AnnotationDescriptor( annotationType );
				ad.setValue( "cascade", cascades.toArray( new CascadeType[] {} ) );
				ad.setValue( "targetEntity", oneToOne.targetEntity() );
				ad.setValue( "fetch", oneToOne.fetch() );
				ad.setValue( "optional", oneToOne.optional() );
				ad.setValue( "mappedBy", oneToOne.mappedBy() );
				ad.setValue( "orphanRemoval", oneToOne.orphanRemoval() );

				return AnnotationFactory.create( ad );
			}
		}
		return annotation;
	}

	private void getJoinTable(
			List<Annotation> annotationList,
			JaxbJoinTableCapable associationAttribute,
			XMLContext.Default defaults) {
		addIfNotNull( annotationList, buildJoinTable( associationAttribute.getJoinTable(), defaults ) );
	}

	/*
	 * no partial overriding possible
	 */
	private JoinTable buildJoinTable(JaxbJoinTableImpl subelement, XMLContext.Default defaults) {
		final Class<JoinTable> annotationType = JoinTable.class;
		if ( subelement == null ) {
			return null;
		}
		//ignore java annotation, an element is defined
		AnnotationDescriptor annotation = new AnnotationDescriptor( annotationType );
		copyAttribute( annotation, "name", subelement.getName(), false );
		copyAttribute( annotation, "catalog", subelement.getCatalog(), false );
		if ( isNotEmpty( defaults.getCatalog() )
				&& isEmpty( (String) annotation.valueOf( "catalog" ) ) ) {
			annotation.setValue( "catalog", defaults.getCatalog() );
		}
		copyAttribute( annotation, "schema", subelement.getSchema(), false );
		if ( isNotEmpty( defaults.getSchema() )
				&& isEmpty( (String) annotation.valueOf( "schema" ) ) ) {
			annotation.setValue( "schema", defaults.getSchema() );
		}
		buildUniqueConstraints( annotation, subelement.getUniqueConstraint() );
		buildIndex( annotation, subelement.getIndex() );
		annotation.setValue( "joinColumns", getJoinColumns( subelement.getJoinColumn(), false ) );
		annotation.setValue( "inverseJoinColumns", getJoinColumns( subelement.getInverseJoinColumn(), true ) );
		return AnnotationFactory.create( annotation );
	}

	private void getOneToMany(List<Annotation> annotationList, XMLContext.Default defaults) {
		Class<OneToMany> annotationType = OneToMany.class;
		List<JaxbOneToManyImpl> elements = elementsForProperty.getOneToMany();
		for ( JaxbOneToManyImpl element : elements ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( annotationType );
			addTargetClass( element.getTargetEntity(), ad, "target-entity", defaults );
			getFetchType( ad, element.getFetch() );
			getCascades( ad, element.getCascade(), defaults );
			getJoinTable( annotationList, element, defaults );
			buildJoinColumns( annotationList, element.getJoinColumn() );
			copyAttribute( ad, "orphan-removal", element.isOrphanRemoval(), false );
			copyAttribute( ad, "mapped-by", element.getMappedBy(), false );
			annotationList.add( AnnotationFactory.create( ad ) );

			getOrderBy( annotationList, element.getOrderBy() );
			getMapKey( annotationList, element.getMapKey() );
			getMapKeyClass( annotationList, element.getMapKeyClass(), defaults );
			getMapKeyColumn( annotationList, element.getMapKeyColumn() );
			getOrderColumn( annotationList, element.getOrderColumn() );
			getMapKeyTemporal( annotationList, element.getMapKeyTemporal() );
			getMapKeyEnumerated( annotationList, element.getMapKeyEnumerated() );
			Annotation annotation = getMapKeyAttributeOverrides( element.getMapKeyAttributeOverride(), defaults );
			addIfNotNull( annotationList, annotation );
			getMapKeyJoinColumns( annotationList, element.getMapKeyJoinColumn() );
			getAccessType( annotationList, element.getAccess() );
		}
		afterGetAssociation( annotationType, annotationList, defaults );
	}

	/**
	 * As per section 12.2 of the JPA 2.0 specification, the association
	 * subelements (many-to-one, one-to-many, one-to-one, many-to-many,
	 * element-collection) completely override the mapping for the specified
	 * field or property.  Thus, any methods which might in some contexts merge
	 * with annotations must not do so in this context.
	 *
	 * @see #getElementCollection(List, XMLContext.Default)
	 */
	private void getOneToOne(List<Annotation> annotationList, XMLContext.Default defaults) {
		Class<OneToOne> annotationType = OneToOne.class;
		List<JaxbOneToOneImpl> elements = elementsForProperty.getOneToOne();
		for ( JaxbOneToOneImpl element : elements ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( annotationType );
			addTargetClass( element.getTargetEntity(), ad, "target-entity", defaults );
			getFetchType( ad, element.getFetch() );
			getCascades( ad, element.getCascade(), defaults );
			getJoinTable( annotationList, element, defaults );
			buildJoinColumns( annotationList, element.getJoinColumn() );
			Annotation annotation = getPrimaryKeyJoinColumns( element.getPrimaryKeyJoinColumn(), defaults, false );
			addIfNotNull( annotationList, annotation );
			copyAttribute( ad, "optional", element.isOptional(), false );
			copyAttribute( ad, "orphan-removal", element.isOrphanRemoval(), false );
			copyAttribute( ad, "mapped-by", element.getMappedBy(), false );
			annotationList.add( AnnotationFactory.create( ad ) );

			getAssociationId( annotationList, element.isId() );
			getMapsId( annotationList, element.getMapsId() );
			getAccessType( annotationList, element.getAccess() );
		}
		afterGetAssociation( annotationType, annotationList, defaults );
	}

	/**
	 * @see #getOneToOne(List, XMLContext.Default)
	 * @see #getElementCollection(List, XMLContext.Default)
	 */
	private void getManyToOne(List<Annotation> annotationList, XMLContext.Default defaults) {
		Class<ManyToOne> annotationType = ManyToOne.class;
		List<JaxbManyToOneImpl> elements = elementsForProperty.getManyToOne();
		for ( JaxbManyToOneImpl element : elements ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( annotationType );
			addTargetClass( element.getTargetEntity(), ad, "target-entity", defaults );
			getFetchType( ad, element.getFetch() );
			getCascades( ad, element.getCascade(), defaults );
			getJoinTable( annotationList, element, defaults );
			buildJoinColumns( annotationList, element.getJoinColumn() );
			copyAttribute( ad, "optional", element.isOptional(), false );
			annotationList.add( AnnotationFactory.create( ad ) );

			getAssociationId( annotationList, element.isId() );
			getMapsId( annotationList, element.getMapsId() );
			getAccessType( annotationList, element.getAccess() );
		}
		afterGetAssociation( annotationType, annotationList, defaults );
	}

	/**
	 * @see #getOneToOne(List, XMLContext.Default)
	 * @see #getElementCollection(List, XMLContext.Default)
	 */
	private void getManyToMany(List<Annotation> annotationList, XMLContext.Default defaults) {
		Class<ManyToMany> annotationType = ManyToMany.class;
		List<JaxbManyToManyImpl> elements = elementsForProperty.getManyToMany();
		for ( JaxbManyToManyImpl element : elements ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( annotationType );
			addTargetClass( element.getTargetEntity(), ad, "target-entity", defaults );
			getFetchType( ad, element.getFetch() );
			getCascades( ad, element.getCascade(), defaults );
			getJoinTable( annotationList, element, defaults );
			copyAttribute( ad, "mapped-by", element.getMappedBy(), false );
			annotationList.add( AnnotationFactory.create( ad ) );

			getOrderBy( annotationList, element.getOrderBy() );
			getMapKey( annotationList, element.getMapKey() );
			getMapKeyClass( annotationList, element.getMapKeyClass(), defaults );
			getMapKeyColumn( annotationList, element.getMapKeyColumn() );
			getOrderColumn( annotationList, element.getOrderColumn() );
			getMapKeyTemporal( annotationList, element.getMapKeyTemporal() );
			getMapKeyEnumerated( annotationList, element.getMapKeyEnumerated() );
			Annotation annotation = getMapKeyAttributeOverrides( element.getMapKeyAttributeOverride(), defaults );
			addIfNotNull( annotationList, annotation );
			getMapKeyJoinColumns( annotationList, element.getMapKeyJoinColumn() );
			getAccessType( annotationList, element.getAccess() );
		}
		afterGetAssociation( annotationType, annotationList, defaults );
	}

	private void getAny(List<Annotation> annotationList, XMLContext.Default defaults) {
		// No support for "any" in JPA's orm.xml; we will just use the "physical" annotations.
		// TODO HHH-10176 We should allow "any" associations, but the JPA XSD doesn't allow that. We would need our own XSD.
		afterGetAssociation( Any.class, annotationList, defaults );
	}

	private void getManyToAny(List<Annotation> annotationList, XMLContext.Default defaults) {
		// No support for "many-to-any" in JPA's orm.xml; we will just use the annotations.
		// TODO HHH-10176 We should allow "many-to-any" associations, but the JPA XSD doesn't allow that. We would need our own XSD.
		afterGetAssociation( ManyToAny.class, annotationList, defaults );
	}

	private void afterGetAssociation(
			Class<? extends Annotation> annotationType,
			List<Annotation> annotationList,
			XMLContext.Default defaults) {
		if ( elementsForProperty.isEmpty() && defaults.canUseJavaAnnotations() ) {
			Annotation annotation = getPhysicalAnnotation( annotationType );
			if ( annotation != null ) {
				annotation = overridesDefaultCascadePersist( annotation, defaults );
				annotationList.add( annotation );
				annotation = overridesDefaultsInJoinTable( annotation, defaults );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( JoinColumn.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( JoinColumns.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( PrimaryKeyJoinColumn.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( PrimaryKeyJoinColumns.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( MapKey.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( OrderBy.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AttributeOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AttributeOverrides.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AssociationOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AssociationOverrides.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( Lob.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( Enumerated.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( Temporal.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( Column.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( Columns.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( MapKeyClass.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( MapKeyTemporal.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( MapKeyEnumerated.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( MapKeyColumn.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( MapKeyJoinColumn.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( MapKeyJoinColumns.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( OrderColumn.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( Cascade.class );
				addIfNotNull( annotationList, annotation );
			}
			else if ( isPhysicalAnnotationPresent( ElementCollection.class ) ) { //JPA2
				annotation = overridesDefaultsInJoinTable( getPhysicalAnnotation( ElementCollection.class ), defaults );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( MapKey.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( OrderBy.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AttributeOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AttributeOverrides.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AssociationOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AssociationOverrides.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( Lob.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( Enumerated.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( Temporal.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( Column.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( OrderColumn.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( MapKeyClass.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( MapKeyTemporal.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( MapKeyEnumerated.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( MapKeyColumn.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( MapKeyJoinColumn.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( MapKeyJoinColumns.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( CollectionTable.class );
				addIfNotNull( annotationList, annotation );
			}
		}
	}

	private void getMapKeyJoinColumns(List<Annotation> annotationList, List<JaxbMapKeyJoinColumnImpl> elements) {
		MapKeyJoinColumn[] joinColumns = buildMapKeyJoinColumns( elements );
		if ( joinColumns.length > 0 ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( MapKeyJoinColumns.class );
			ad.setValue( "value", joinColumns );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private MapKeyJoinColumn[] buildMapKeyJoinColumns(List<JaxbMapKeyJoinColumnImpl> elements) {
		List<MapKeyJoinColumn> joinColumns = new ArrayList<>();
		if ( elements != null ) {
			for ( JaxbMapKeyJoinColumnImpl element : elements ) {
				AnnotationDescriptor column = new AnnotationDescriptor( MapKeyJoinColumn.class );
				copyAttribute( column, "name", element.getName(), false );
				copyAttribute( column, "referenced-column-name", element.getReferencedColumnName(), false );
				copyAttribute( column, "unique", element.isUnique(), false );
				copyAttribute( column, "nullable", element.isNullable(), false );
				copyAttribute( column, "insertable", element.isInsertable(), false );
				copyAttribute( column, "updatable", element.isUpdatable(), false );
				copyAttribute( column, "column-definition", element.getColumnDefinition(), false );
				copyAttribute( column, "table", element.getTable(), false );
				joinColumns.add( AnnotationFactory.create( column ) );
			}
		}
		return joinColumns.toArray( new MapKeyJoinColumn[ 0 ] );
	}

	private AttributeOverrides getMapKeyAttributeOverrides(List<JaxbAttributeOverrideImpl> elements, XMLContext.Default defaults) {
		List<AttributeOverride> attributes = buildAttributeOverrides( elements, "map-key-attribute-override" );
		return mergeAttributeOverrides( defaults, attributes, false );
	}

	private Cache getCaching(JaxbManagedType root, XMLContext.Default defaults) {
		if ( root instanceof JaxbEntityImpl jaxbEntity ) {
			final JaxbCachingImpl caching = jaxbEntity.getCaching();
			if ( caching != null ) {
				final AnnotationDescriptor ad = new AnnotationDescriptor( Cache.class );
				ad.setValue( "usage", CacheConcurrencyStrategy.fromAccessType( caching.getAccess() ) );
				ad.setValue( "region", caching.getRegion() );
				ad.setValue( "include", caching.getInclude().value() );
				return AnnotationFactory.create( ad );
			}
		}

		if ( defaults.canUseJavaAnnotations() ) {
			return getPhysicalAnnotation( Cache.class );
		}
		else {
			return null;
		}
	}

	private Cacheable getCacheable(JaxbManagedType root, XMLContext.Default defaults) {
		if ( root instanceof JaxbEntityImpl jaxbRootEntity ) {
			Boolean attValue = jaxbRootEntity.isCacheable();
			if ( attValue != null ) {
				AnnotationDescriptor ad = new AnnotationDescriptor( Cacheable.class );
				ad.setValue( "value", attValue );
				return AnnotationFactory.create( ad );
			}
		}
		if ( defaults.canUseJavaAnnotations() ) {
			return getPhysicalAnnotation( Cacheable.class );
		}
		else {
			return null;
		}
	}

	/**
	 * Adds a @MapKeyEnumerated annotation to the specified annotationList if the specified element
	 * contains a map-key-enumerated sub-element. This should only be the case for
	 * element-collection, many-to-many, or one-to-many associations.
	 */
	private void getMapKeyEnumerated(List<Annotation> annotationList, EnumType enumType) {
		if ( enumType != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( MapKeyEnumerated.class );
			ad.setValue( "value", enumType );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	/**
	 * Adds a @MapKeyTemporal annotation to the specified annotationList if the specified element
	 * contains a map-key-temporal sub-element. This should only be the case for element-collection,
	 * many-to-many, or one-to-many associations.
	 */
	private void getMapKeyTemporal(List<Annotation> annotationList, TemporalType temporalType) {
		if ( temporalType != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( MapKeyTemporal.class );
			ad.setValue( "value", temporalType );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	/**
	 * Adds an @OrderColumn annotation to the specified annotationList if the specified element
	 * contains an order-column sub-element. This should only be the case for element-collection,
	 * many-to-many, or one-to-many associations.
	 */
	private void getOrderColumn(List<Annotation> annotationList, JaxbOrderColumnImpl element) {
		if ( element != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( OrderColumn.class );
			copyAttribute( ad, "name", element.getName(), false );
			copyAttribute( ad, "nullable", element.isNullable(), false );
			copyAttribute( ad, "insertable", element.isInsertable(), false );
			copyAttribute( ad, "updatable", element.isUpdatable(), false );
			copyAttribute( ad, "column-definition", element.getColumnDefinition(), false );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	/**
	 * Adds a @MapsId annotation to the specified annotationList if the specified element has the
	 * maps-id attribute set. This should only be the case for many-to-one or one-to-one
	 * associations.
	 */
	private void getMapsId(List<Annotation> annotationList, String mapsId) {
		if ( mapsId != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( MapsId.class );
			ad.setValue( "value", mapsId );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	/**
	 * Adds an @Id annotation to the specified annotationList if the specified element has the id
	 * attribute set to true. This should only be the case for many-to-one or one-to-one
	 * associations.
	 */
	private void getAssociationId(List<Annotation> annotationList, Boolean isId) {
		if ( Boolean.TRUE.equals( isId ) ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( Id.class );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void addTargetClass(String className, AnnotationDescriptor ad, String nodeName, XMLContext.Default defaults) {
		if ( className != null ) {
			Class<?> clazz;
			try {
				clazz = classLoaderAccess.classForName( XMLContext.buildSafeClassName( className, defaults ) );
			}
			catch (ClassLoadingException e) {
				throw new AnnotationException( "Unable to find " + nodeName + ": " + className, e );
			}
			ad.setValue( getJavaAttributeNameFromXMLOne( nodeName ), clazz );
		}
	}

	/**
	 * As per sections 12.2.3.23.9, 12.2.4.8.9 and 12.2.5.3.6 of the JPA 2.0
	 * specification, the element-collection subelement completely overrides the
	 * mapping for the specified field or property.  Thus, any methods which
	 * might in some contexts merge with annotations must not do so in this
	 * context.
	 */
	private void getElementCollection(List<Annotation> annotationList, XMLContext.Default defaults) {
		for ( JaxbElementCollectionImpl element : elementsForProperty.getElementCollection() ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( ElementCollection.class );
			addTargetClass( element.getTargetClass(), ad, "target-class", defaults );
			getFetchType( ad, element.getFetch() );
			getOrderBy( annotationList, element.getOrderBy() );
			getOrderColumn( annotationList, element.getOrderColumn() );
			getMapKey( annotationList, element.getMapKey() );
			getMapKeyClass( annotationList, element.getMapKeyClass(), defaults );
			getMapKeyTemporal( annotationList, element.getMapKeyTemporal() );
			getMapKeyEnumerated( annotationList, element.getMapKeyEnumerated() );
			getMapKeyColumn( annotationList, element.getMapKeyColumn() );
			getMapKeyJoinColumns( annotationList, element.getMapKeyJoinColumn() );
			Annotation annotation = getColumn( element.getColumn(), false, "element-collection" );
			addIfNotNull( annotationList, annotation );
			getTemporal( annotationList, element.getTemporal() );
			getEnumerated( annotationList, element.getEnumerated() );
			getLob( annotationList, element.getLob() );
			//Both map-key-attribute-overrides and attribute-overrides
			//translate into AttributeOverride annotations, which need
			//need to be wrapped in the same AttributeOverrides annotation.
			List<AttributeOverride> attributes = new ArrayList<>();
			attributes.addAll( buildAttributeOverrides( element.getMapKeyAttributeOverride(), "map-key-attribute-override" ) );
			attributes.addAll( buildAttributeOverrides( element.getAttributeOverride(), "attribute-override" ) );
			annotation = mergeAttributeOverrides( defaults, attributes, false );
			addIfNotNull( annotationList, annotation );
			annotation = getAssociationOverrides( element.getAssociationOverride(), defaults, false );
			addIfNotNull( annotationList, annotation );
			getCollectionTable( annotationList, element.getCollectionTable(), defaults );
			annotationList.add( AnnotationFactory.create( ad ) );
			getAccessType( annotationList, element.getAccess() );
		}
	}

	private void getOrderBy(List<Annotation> annotationList, String orderBy) {
		if ( orderBy != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( OrderBy.class );
			ad.setValue( "value", orderBy );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getMapKey(List<Annotation> annotationList, JaxbMapKeyImpl element) {
		if ( element != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( MapKey.class );
			copyAttribute( ad, "name", element.getName(), false );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getMapKeyColumn(List<Annotation> annotationList, JaxbMapKeyColumnImpl element) {
		if ( element != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( MapKeyColumn.class );
			copyAttribute( ad, "name", element.getName(), false );
			copyAttribute( ad, "unique", element.isUnique(), false );
			copyAttribute( ad, "nullable", element.isNullable(), false );
			copyAttribute( ad, "insertable", element.isInsertable(), false );
			copyAttribute( ad, "updatable", element.isUpdatable(), false );
			copyAttribute( ad, "column-definition", element.getColumnDefinition(), false );
			copyAttribute( ad, "table", element.getTable(), false );
			copyAttribute( ad, "length", element.getLength(), false );
			copyAttribute( ad, "precision", element.getPrecision(), false );
			copyAttribute( ad, "scale", element.getScale(), false );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getMapKeyClass(List<Annotation> annotationList, JaxbMapKeyClassImpl element, XMLContext.Default defaults) {
		String nodeName = "map-key-class";
		if ( element != null ) {
			String mapKeyClassName = element.getClazz();
			AnnotationDescriptor ad = new AnnotationDescriptor( MapKeyClass.class );
			if ( isNotEmpty( mapKeyClassName ) ) {
				Class<?> clazz;
				try {
					clazz = classLoaderAccess.classForName(
							XMLContext.buildSafeClassName( mapKeyClassName, defaults )
					);
				}
				catch (ClassLoadingException e) {
					throw new AnnotationException(
							"Unable to find " + nodeName + ": " + mapKeyClassName, e
					);
				}
				ad.setValue( "value", clazz );
			}
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getCollectionTable(List<Annotation> annotationList, JaxbCollectionTableImpl element, XMLContext.Default defaults) {
		if ( element != null ) {
			AnnotationDescriptor annotation = new AnnotationDescriptor( CollectionTable.class );
			copyAttribute( annotation, "name", element.getName(), false );
			copyAttribute( annotation, "catalog", element.getCatalog(), false );
			if ( isNotEmpty( defaults.getCatalog() )
					&& isEmpty( (String) annotation.valueOf( "catalog" ) ) ) {
				annotation.setValue( "catalog", defaults.getCatalog() );
			}
			copyAttribute( annotation, "schema", element.getSchema(), false );
			if ( isNotEmpty( defaults.getSchema() )
					&& isEmpty( (String) annotation.valueOf( "schema" ) ) ) {
				annotation.setValue( "schema", defaults.getSchema() );
			}
			JoinColumn[] joinColumns = getJoinColumns( element.getJoinColumn(), false );
			if ( joinColumns.length > 0 ) {
				annotation.setValue( "joinColumns", joinColumns );
			}
			buildUniqueConstraints( annotation, element.getUniqueConstraint() );
			buildIndex( annotation, element.getIndex() );
			annotationList.add( AnnotationFactory.create( annotation ) );
		}
	}

	private void buildJoinColumns(List<Annotation> annotationList, List<JaxbJoinColumnImpl> elements) {
		JoinColumn[] joinColumns = getJoinColumns( elements, false );
		if ( joinColumns.length > 0 ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( JoinColumns.class );
			ad.setValue( "value", joinColumns );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getCascades(AnnotationDescriptor ad, JaxbCascadeTypeImpl element, XMLContext.Default defaults) {
		List<CascadeType> cascades = new ArrayList<>();
		if ( element != null ) {
			if ( element.getCascadeAll() != null ) {
				cascades.add( CascadeType.ALL );
			}
			if ( element.getCascadePersist() != null ) {
				cascades.add( CascadeType.PERSIST );
			}
			if ( element.getCascadeMerge() != null ) {
				cascades.add( CascadeType.MERGE );
			}
			if ( element.getCascadeRemove() != null ) {
				cascades.add( CascadeType.REMOVE );
			}
			if ( element.getCascadeRefresh() != null ) {
				cascades.add( CascadeType.REFRESH );
			}
			if ( element.getCascadeDetach() != null ) {
				cascades.add( CascadeType.DETACH );
			}
		}
		if ( Boolean.TRUE.equals( defaults.getCascadePersist() )
				&& !cascades.contains( CascadeType.ALL ) && !cascades.contains( CascadeType.PERSIST ) ) {
			cascades.add( CascadeType.PERSIST );
		}
		if ( cascades.size() > 0 ) {
			ad.setValue( "cascade", cascades.toArray( new CascadeType[0] ) );
		}
	}

	private void getEmbedded(List<Annotation> annotationList, XMLContext.Default defaults) {
		for ( JaxbEmbeddedImpl element : elementsForProperty.getEmbedded() ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( Embedded.class );
			annotationList.add( AnnotationFactory.create( ad ) );
			Annotation annotation = getAttributeOverrides( element.getAttributeOverride(), defaults, false );
			addIfNotNull( annotationList, annotation );
			annotation = getAssociationOverrides( element.getAssociationOverride(), defaults, false );
			addIfNotNull( annotationList, annotation );
			getAccessType( annotationList, element.getAccess() );
		}
		if ( elementsForProperty.isEmpty() && defaults.canUseJavaAnnotations() ) {
			Annotation annotation = getPhysicalAnnotation( Embedded.class );
			if ( annotation != null ) {
				annotationList.add( annotation );
				annotation = getPhysicalAnnotation( AttributeOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AttributeOverrides.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AssociationOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AssociationOverrides.class );
				addIfNotNull( annotationList, annotation );
			}
		}
	}

	private Transient getTransient(XMLContext.Default defaults) {
		if ( !elementsForProperty.getTransient().isEmpty() ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( Transient.class );
			return AnnotationFactory.create( ad );
		}
		if ( elementsForProperty.isEmpty() && defaults.canUseJavaAnnotations() ) {
			return getPhysicalAnnotation( Transient.class );
		}
		else {
			return null;
		}
	}

	private void getVersion(List<Annotation> annotationList, XMLContext.Default defaults) {
		for ( JaxbVersionImpl element : elementsForProperty.getVersion() ) {
			Annotation annotation = buildColumns( element.getColumn(), "version" );
			addIfNotNull( annotationList, annotation );
			getTemporal( annotationList, element.getTemporal() );
			AnnotationDescriptor basic = new AnnotationDescriptor( Version.class );
			annotationList.add( AnnotationFactory.create( basic ) );
			getAccessType( annotationList, element.getAccess() );
		}
		if ( elementsForProperty.isEmpty() && defaults.canUseJavaAnnotations() ) {
			//we have nothing, so Java annotations might occur
			Annotation annotation = getPhysicalAnnotation( Version.class );
			if ( annotation != null ) {
				annotationList.add( annotation );
				annotation = getPhysicalAnnotation( Column.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( Columns.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( Temporal.class );
				addIfNotNull( annotationList, annotation );
			}
		}
	}

	private void getBasic(List<Annotation> annotationList, XMLContext.Default defaults) {
		for ( JaxbBasicImpl element : elementsForProperty.getBasic() ) {
			Annotation annotation = buildColumns( element.getColumn(), "basic" );
			addIfNotNull( annotationList, annotation );
			getAccessType( annotationList, element.getAccess() );
			getTemporal( annotationList, element.getTemporal() );
			getLob( annotationList, element.getLob() );
			getEnumerated( annotationList, element.getEnumerated() );
			AnnotationDescriptor basic = new AnnotationDescriptor( Basic.class );
			getFetchType( basic, element.getFetch() );
			copyAttribute( basic, "optional", element.isOptional(), false );
			annotationList.add( AnnotationFactory.create( basic ) );
			getType( annotationList, element.getType() );
			getJdbcTypeCode( annotationList, element.getJdbcTypeCode() );
			getTenantId( annotationList, element );
		}
		if ( elementsForProperty.isEmpty() && defaults.canUseJavaAnnotations() ) {
			//no annotation presence constraint, basic is the default
			Annotation annotation = getPhysicalAnnotation( Basic.class );
			addIfNotNull( annotationList, annotation );
			annotation = getPhysicalAnnotation( Lob.class );
			addIfNotNull( annotationList, annotation );
			annotation = getPhysicalAnnotation( Enumerated.class );
			addIfNotNull( annotationList, annotation );
			annotation = getPhysicalAnnotation( Temporal.class );
			addIfNotNull( annotationList, annotation );
			annotation = getPhysicalAnnotation( Column.class );
			addIfNotNull( annotationList, annotation );
			annotation = getPhysicalAnnotation( Columns.class );
			addIfNotNull( annotationList, annotation );
			annotation = getPhysicalAnnotation( AttributeOverride.class );
			addIfNotNull( annotationList, annotation );
			annotation = getPhysicalAnnotation( AttributeOverrides.class );
			addIfNotNull( annotationList, annotation );
			annotation = getPhysicalAnnotation( AssociationOverride.class );
			addIfNotNull( annotationList, annotation );
			annotation = getPhysicalAnnotation( AssociationOverrides.class );
			addIfNotNull( annotationList, annotation );
			annotation = getPhysicalAnnotation( Type.class );
			addIfNotNull( annotationList, annotation );
			annotation = getPhysicalAnnotation( JdbcTypeCode.class );
			addIfNotNull( annotationList, annotation );
		}
	}

	private void getTenantId(List<Annotation> annotationList, JaxbBasicImpl element) {
		if ( element instanceof JaxbTenantIdImpl ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( TenantId.class );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getType(List<Annotation> annotationList, JaxbUserTypeImpl type) {
		if ( type != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( Type.class );
			ad.setValue( "value", classLoaderAccess.classForName( type.getValue() ) );
			Parameter[] parameters = new Parameter[type.getParameters().size()];
			for ( int i = 0; i < parameters.length; i++ ) {
				JaxbConfigurationParameterImpl parameter = type.getParameters().get( i );
				AnnotationDescriptor param = new AnnotationDescriptor( Parameter.class );
				param.setValue( "name", parameter.getName() );
				param.setValue( "value", parameter.getValue() );
				parameters[i] = AnnotationFactory.create( param );
			}
			ad.setValue( "parameters", parameters );

			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getUuidGenerator(List<Annotation> annotationList, JaxbUuidGeneratorImpl uuidGenerator) {
		if ( uuidGenerator != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( UuidGenerator.class );
			ad.setValue( "style", uuidGenerator.getStyle() );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getJdbcTypeCode(List<Annotation> annotationList, Integer jdbcTypeCode) {
		if ( jdbcTypeCode != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( JdbcTypeCode.class );
			ad.setValue( "value", jdbcTypeCode );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getEnumerated(List<Annotation> annotationList, EnumType enumType) {
		if ( enumType != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( Enumerated.class );
			ad.setValue( "value", enumType );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getLob(List<Annotation> annotationList, JaxbLobImpl element) {
		if ( element != null ) {
			annotationList.add( AnnotationFactory.create( new AnnotationDescriptor( Lob.class ) ) );
		}
	}

	private void getFetchType(AnnotationDescriptor descriptor, FetchType type) {
		if ( type != null ) {
			descriptor.setValue( "fetch", type );
		}
	}

	private void getEmbeddedId(List<Annotation> annotationList, XMLContext.Default defaults) {
		for ( JaxbEmbeddedIdImpl element : elementsForProperty.getEmbeddedId() ) {
			if ( isProcessingId( defaults ) ) {
				Annotation annotation = getAttributeOverrides( element.getAttributeOverride(), defaults, false );
				addIfNotNull( annotationList, annotation );
				// TODO HHH-10176 We should allow association overrides here, but the JPA XSD doesn't allow that. We would need our own XSD.
				AnnotationDescriptor ad = new AnnotationDescriptor( EmbeddedId.class );
				annotationList.add( AnnotationFactory.create( ad ) );
				getAccessType( annotationList, element.getAccess() );
			}
		}
		if ( elementsForProperty.isEmpty() && defaults.canUseJavaAnnotations() ) {
			Annotation annotation = getPhysicalAnnotation( EmbeddedId.class );
			if ( annotation != null ) {
				annotationList.add( annotation );
				annotation = getPhysicalAnnotation( Column.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( Columns.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( GeneratedValue.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( Temporal.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( TableGenerator.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( SequenceGenerator.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AttributeOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AttributeOverrides.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AssociationOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AssociationOverrides.class );
				addIfNotNull( annotationList, annotation );
			}
		}
	}

	private void preCalculateElementsForProperty(JaxbManagedType managedType, JaxbEntityListenerImpl entityListener) {
		elementsForProperty = new PropertyMappingElementCollector( propertyName );
		JaxbAttributesContainer attributes = managedType == null ? null : managedType.getAttributes();
		//put entity.attributes elements
		if ( attributes != null ) {
			elementsForProperty.collectPersistentAttributesIfMatching( attributes );
		}
		//add pre-* etc from entity and pure entity listener classes
		if ( managedType instanceof JaxbLifecycleCallbackContainer ) {
			elementsForProperty.collectLifecycleCallbacksIfMatching( (JaxbLifecycleCallbackContainer) managedType );
		}
		if ( entityListener != null ) {
			elementsForProperty.collectLifecycleCallbacksIfMatching( entityListener );
		}
		elementsForProperty.collectTenantIdIfMatching( managedType );
	}

	private void getId(List<Annotation> annotationList, XMLContext.Default defaults) {
		for ( JaxbIdImpl element : elementsForProperty.getId() ) {
			boolean processId = isProcessingId( defaults );
			if ( processId ) {
				Annotation annotation = buildColumns( element.getColumn(), "id" );
				addIfNotNull( annotationList, annotation );
				annotation = buildGeneratedValue( element.getGeneratedValue() );
				addIfNotNull( annotationList, annotation );
				getTemporal( annotationList, element.getTemporal() );
				//FIXME: fix the priority of xml over java for generator names
				annotation = getTableGenerator( element.getTableGenerator(), defaults );
				addIfNotNull( annotationList, annotation );
				annotation = getSequenceGenerator( element.getSequenceGenerator(), defaults );
				addIfNotNull( annotationList, annotation );
				AnnotationDescriptor id = new AnnotationDescriptor( Id.class );
				annotationList.add( AnnotationFactory.create( id ) );
				getAccessType( annotationList, element.getAccess() );
				getType( annotationList, element.getType() );
				getJdbcTypeCode( annotationList, element.getJdbcTypeCode() );
				getUuidGenerator( annotationList, element.getUuidGenerator() );
			}
		}
		if ( elementsForProperty.isEmpty() && defaults.canUseJavaAnnotations() ) {
			Annotation annotation = getPhysicalAnnotation( Id.class );
			if ( annotation != null ) {
				annotationList.add( annotation );
				annotation = getPhysicalAnnotation( Column.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( Columns.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( GeneratedValue.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( Temporal.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( TableGenerator.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( SequenceGenerator.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AttributeOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AttributeOverrides.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AssociationOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getPhysicalAnnotation( AssociationOverrides.class );
				addIfNotNull( annotationList, annotation );
			}
		}
	}

	private boolean isProcessingId(XMLContext.Default defaults) {
		boolean isExplicit = defaults.getAccess() != null;
		boolean correctAccess = ( PropertyType.PROPERTY.equals( propertyType ) && AccessType.PROPERTY.equals( defaults.getAccess() ) )
				|| ( PropertyType.FIELD.equals( propertyType ) && AccessType.FIELD.equals( defaults.getAccess() ) );
		boolean hasId = defaults.canUseJavaAnnotations()
				&& ( isPhysicalAnnotationPresent( Id.class ) || isPhysicalAnnotationPresent( EmbeddedId.class ) );
		boolean mirrorAttributeIsId = defaults.canUseJavaAnnotations()
				&& hasAnnotation( mirroredAttribute, Id.class, EmbeddedId.class );
		boolean propertyIsDefault = PropertyType.PROPERTY.equals( propertyType )
				&& !mirrorAttributeIsId;
		return correctAccess || ( !isExplicit && hasId ) || ( !isExplicit && propertyIsDefault );
	}

	private Columns buildColumns(JaxbColumnImpl element, String nodeName) {
		if ( element == null ) {
			return null;
		}
		List<Column> columns = new ArrayList<>( 1 );
		columns.add( getColumn( element, false, nodeName ) );
		AnnotationDescriptor columnsDescr = new AnnotationDescriptor( Columns.class );
		columnsDescr.setValue( "columns", columns.toArray( new Column[0] ) );
		return AnnotationFactory.create( columnsDescr );
	}

	private Columns buildColumns(List<JaxbColumnImpl> elements, String nodeName) {
		List<Column> columns = new ArrayList<>( elements.size() );
		for ( JaxbColumnImpl element : elements ) {
			columns.add( getColumn( element, false, nodeName ) );
		}
		if ( columns.size() > 0 ) {
			AnnotationDescriptor columnsDescr = new AnnotationDescriptor( Columns.class );
			columnsDescr.setValue( "columns", columns.toArray( new Column[ columns.size() ] ) );
			return AnnotationFactory.create( columnsDescr );
		}
		else {
			return null;
		}
	}

	private GeneratedValue buildGeneratedValue(JaxbGeneratedValueImpl element) {
		if ( element != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( GeneratedValue.class );
			GenerationType strategy = element.getStrategy();
			if ( strategy != null ) {
				ad.setValue( "strategy", strategy );
			}
			copyAttribute( ad, "generator", element.getGenerator(), false );
			return AnnotationFactory.create( ad );
		}
		else {
			return null;
		}
	}

	private void getTemporal(List<Annotation> annotationList, TemporalType type) {
		if ( type != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( Temporal.class );
			ad.setValue( "value", type );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getAccessType(List<Annotation> annotationList, AccessType type) {
		if ( element == null ) {
			return;
		}
		if ( type != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( Access.class );

			if ( ( AccessType.PROPERTY.equals( type ) && this.element instanceof Method ) ||
					( AccessType.FIELD.equals( type ) && this.element instanceof Field ) ) {
				return;
			}

			ad.setValue( "value", type );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private AssociationOverrides getAssociationOverrides(JaxbManagedType root, XMLContext.Default defaults) {
		return getAssociationOverrides(
				root instanceof JaxbEntityImpl ? ( (JaxbEntityImpl) root ).getAssociationOverrides() : Collections.emptyList(),
				defaults, true
		);
	}

	/**
	 * @param mergeWithAnnotations Whether to use Java annotations for this
	 * 		element, if present and not disabled by the XMLContext defaults.
	 * 		In some contexts (such as an element-collection mapping) merging
	 */
	private AssociationOverrides getAssociationOverrides(
			List<JaxbAssociationOverrideImpl> elements,
			XMLContext.Default defaults,
			boolean mergeWithAnnotations) {
		List<AssociationOverride> attributes = buildAssociationOverrides( elements, defaults );
		if ( mergeWithAnnotations && defaults.canUseJavaAnnotations() ) {
			AssociationOverride annotation = getPhysicalAnnotation( AssociationOverride.class );
			addAssociationOverrideIfNeeded( annotation, attributes );
			AssociationOverrides annotations = getPhysicalAnnotation( AssociationOverrides.class );
			if ( annotations != null ) {
				for ( AssociationOverride current : annotations.value() ) {
					addAssociationOverrideIfNeeded( current, attributes );
				}
			}
		}
		if ( attributes.size() > 0 ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( AssociationOverrides.class );
			ad.setValue( "value", attributes.toArray( new AssociationOverride[0] ) );
			return AnnotationFactory.create( ad );
		}
		else {
			return null;
		}
	}

	private List<AssociationOverride> buildAssociationOverrides(List<JaxbAssociationOverrideImpl> elements, XMLContext.Default defaults) {
		List<AssociationOverride> overrides = new ArrayList<>();
		if ( elements != null && elements.size() > 0 ) {
			for ( JaxbAssociationOverrideImpl current : elements ) {
				AnnotationDescriptor override = new AnnotationDescriptor( AssociationOverride.class );
				copyAttribute( override, "name", current.getName(), true );
				override.setValue( "joinColumns", getJoinColumns( current.getJoinColumn(), false ) );
				JoinTable joinTable = buildJoinTable( current.getJoinTable(), defaults );
				if ( joinTable != null ) {
					override.setValue( "joinTable", joinTable );
				}
				overrides.add( AnnotationFactory.create( override ) );
			}
		}
		return overrides;
	}

	private JoinColumn[] getJoinColumns(List<JaxbJoinColumnImpl> subelements, boolean isInverse) {
		List<JoinColumn> joinColumns = new ArrayList<>();
		if ( subelements != null ) {
			for ( JaxbJoinColumnImpl subelement : subelements ) {
				AnnotationDescriptor column = new AnnotationDescriptor( JoinColumn.class );
				copyAttribute( column, "name", subelement.getName(), false );
				copyAttribute( column, "referenced-column-name", subelement.getReferencedColumnName(), false );
				copyAttribute( column, "unique", subelement.isUnique(), false );
				copyAttribute( column, "nullable", subelement.isNullable(), false );
				copyAttribute( column, "insertable", subelement.isInsertable(), false );
				copyAttribute( column, "updatable", subelement.isUpdatable(), false );
				copyAttribute( column, "column-definition", subelement.getColumnDefinition(), false );
				copyAttribute( column, "table", subelement.getTable(), false );
				joinColumns.add( AnnotationFactory.create( column ) );
			}
		}
		return joinColumns.toArray( new JoinColumn[0] );
	}

	private void addAssociationOverrideIfNeeded(AssociationOverride annotation, List<AssociationOverride> overrides) {
		if ( annotation != null ) {
			String overrideName = annotation.name();
			boolean present = false;
			for ( AssociationOverride current : overrides ) {
				if ( current.name().equals( overrideName ) ) {
					present = true;
					break;
				}
			}
			if ( !present ) {
				overrides.add( annotation );
			}
		}
	}

	private AttributeOverrides getAttributeOverrides(JaxbManagedType root, XMLContext.Default defaults) {
		return getAttributeOverrides(
				root instanceof JaxbEntityImpl ? ( (JaxbEntityImpl) root ).getAttributeOverrides() : Collections.emptyList(),
				defaults, true
		);
	}

	/**
	 * @param mergeWithAnnotations Whether to use Java annotations for this
	 * 		element, if present and not disabled by the XMLContext defaults.
	 * 		In some contexts (such as an association mapping) merging with
	 * 		annotations is never allowed.
	 */
	private AttributeOverrides getAttributeOverrides(
			List<JaxbAttributeOverrideImpl> elements,
			XMLContext.Default defaults,
			boolean mergeWithAnnotations) {
		List<AttributeOverride> attributes = buildAttributeOverrides( elements, "attribute-override" );
		return mergeAttributeOverrides( defaults, attributes, mergeWithAnnotations );
	}

	/**
	 * @param mergeWithAnnotations Whether to use Java annotations for this
	 * 		element, if present and not disabled by the XMLContext defaults.
	 * 		In some contexts (such as an association mapping) merging with
	 * 		annotations is never allowed.
	 */
	private AttributeOverrides mergeAttributeOverrides(XMLContext.Default defaults, List<AttributeOverride> attributes, boolean mergeWithAnnotations) {
		if ( mergeWithAnnotations && defaults.canUseJavaAnnotations() ) {
			AttributeOverride annotation = getPhysicalAnnotation( AttributeOverride.class );
			addAttributeOverrideIfNeeded( annotation, attributes );
			AttributeOverrides annotations = getPhysicalAnnotation( AttributeOverrides.class );
			if ( annotations != null ) {
				for ( AttributeOverride current : annotations.value() ) {
					addAttributeOverrideIfNeeded( current, attributes );
				}
			}
		}
		if ( attributes.size() > 0 ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( AttributeOverrides.class );
			ad.setValue( "value", attributes.toArray( new AttributeOverride[ attributes.size() ] ) );
			return AnnotationFactory.create( ad );
		}
		else {
			return null;
		}
	}

	private List<AttributeOverride> buildAttributeOverrides(List<JaxbAttributeOverrideImpl> subelements, String nodeName) {
		List<AttributeOverride> overrides = new ArrayList<>();
		if ( subelements != null && subelements.size() > 0 ) {
			for ( JaxbAttributeOverrideImpl current : subelements ) {
				AnnotationDescriptor override = new AnnotationDescriptor( AttributeOverride.class );
				copyAttribute( override, "name", current.getName(), true );
				JaxbColumnImpl column = current.getColumn();
				override.setValue( "column", getColumn( column, true, nodeName ) );
				overrides.add( AnnotationFactory.create( override ) );
			}
		}
		return overrides;
	}

	private Column getColumn(JaxbColumnImpl element, boolean isMandatory, String nodeName) {
		if ( element != null ) {
			AnnotationDescriptor column = new AnnotationDescriptor( Column.class );
			copyAttribute( column, "name", element.getName(), false );
			copyAttribute( column, "unique", element.isUnique(), false );
			copyAttribute( column, "nullable", element.isNullable(), false );
			copyAttribute( column, "insertable", element.isInsertable(), false );
			copyAttribute( column, "updatable", element.isUpdatable(), false );
			copyAttribute( column, "column-definition", element.getColumnDefinition(), false );
			copyAttribute( column, "table", element.getTable(), false );
			copyAttribute( column, "length", element.getLength(), false );
			copyAttribute( column, "precision", element.getPrecision(), false );
			copyAttribute( column, "scale", element.getScale(), false );
			return AnnotationFactory.create( column );
		}
		else {
			if ( isMandatory ) {
				throw new AnnotationException( nodeName + ".column is mandatory. " + SCHEMA_VALIDATION );
			}
			return null;
		}
	}

	private void addAttributeOverrideIfNeeded(AttributeOverride annotation, List<AttributeOverride> overrides) {
		if ( annotation != null ) {
			String overrideName = annotation.name();
			boolean present = false;
			for ( AttributeOverride current : overrides ) {
				if ( current.name().equals( overrideName ) ) {
					present = true;
					break;
				}
			}
			if ( !present ) {
				overrides.add( annotation );
			}
		}
	}

	private Access getAccessType(JaxbManagedType root, XMLContext.Default defaults) {
		AccessType access = root == null ? null : root.getAccess();
		if ( access != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( Access.class );
			ad.setValue( "value", access );
			return AnnotationFactory.create( ad );
		}
		else if ( defaults.canUseJavaAnnotations() && isPhysicalAnnotationPresent( Access.class ) ) {
			return getPhysicalAnnotation( Access.class );
		}
		else if ( defaults.getAccess() != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( Access.class );
			ad.setValue( "value", defaults.getAccess() );
			return AnnotationFactory.create( ad );
		}
		else {
			return null;
		}
	}

	private ExcludeSuperclassListeners getExcludeSuperclassListeners(JaxbManagedType root, XMLContext.Default defaults) {
		return (ExcludeSuperclassListeners) getMarkerAnnotation(
				ExcludeSuperclassListeners.class,
				root instanceof JaxbEntityOrMappedSuperclass
						? ( (JaxbEntityOrMappedSuperclass) root ).getExcludeSuperclassListeners()
						: null,
				defaults
		);
	}

	private ExcludeDefaultListeners getExcludeDefaultListeners(JaxbManagedType root, XMLContext.Default defaults) {
		return (ExcludeDefaultListeners) getMarkerAnnotation(
				ExcludeDefaultListeners.class,
				root instanceof JaxbEntityOrMappedSuperclass
						? ( (JaxbEntityOrMappedSuperclass) root ).getExcludeDefaultListeners()
						: null,
				defaults
		);
	}

	private Annotation getMarkerAnnotation(
			Class<? extends Annotation> clazz,
			JaxbEmptyTypeImpl element,
			XMLContext.Default defaults) {
		if ( element != null ) {
			return AnnotationFactory.create( new AnnotationDescriptor( clazz ) );
		}
		else if ( defaults.canUseJavaAnnotations() ) {
			//TODO wonder whether it should be excluded so that user can undone it
			return getPhysicalAnnotation( clazz );
		}
		else {
			return null;
		}
	}

	private SqlResultSetMappings getSqlResultSetMappings(JaxbManagedType root, XMLContext.Default defaults) {
		List<SqlResultSetMapping> results = root instanceof JaxbEntityImpl
				? buildSqlResultSetMappings( ( (JaxbEntityImpl) root ).getSqlResultSetMappings(), defaults, classLoaderAccess )
				: new ArrayList<>();
		if ( defaults.canUseJavaAnnotations() ) {
			SqlResultSetMapping annotation = getPhysicalAnnotation( SqlResultSetMapping.class );
			addSqlResultsetMappingIfNeeded( annotation, results );
			SqlResultSetMappings annotations = getPhysicalAnnotation( SqlResultSetMappings.class );
			if ( annotations != null ) {
				for ( SqlResultSetMapping current : annotations.value() ) {
					addSqlResultsetMappingIfNeeded( current, results );
				}
			}
		}
		if ( results.size() > 0 ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( SqlResultSetMappings.class );
			ad.setValue( "value", results.toArray( new SqlResultSetMapping[ results.size() ] ) );
			return AnnotationFactory.create( ad );
		}
		else {
			return null;
		}
	}

	public static List<NamedEntityGraph> buildNamedEntityGraph(
			List<JaxbNamedEntityGraphImpl> elements,
			XMLContext.Default defaults,
			ClassLoaderAccess classLoaderAccess) {
		List<NamedEntityGraph> namedEntityGraphList = new ArrayList<>();
		for ( JaxbNamedEntityGraphImpl element : elements ) {
			AnnotationDescriptor ann = new AnnotationDescriptor( NamedEntityGraph.class );
			copyAttribute( ann, "name", element.getName(), false );
			copyAttribute( ann, "include-all-attributes", element.isIncludeAllAttributes(), false );
			bindNamedAttributeNodes( element.getNamedAttributeNode(), ann );

			bindNamedSubgraph( defaults, ann, "subgraphs", element.getSubgraph(), classLoaderAccess );
			bindNamedSubgraph( defaults, ann, "subclassSubgraphs", element.getSubclassSubgraph(), classLoaderAccess );
			namedEntityGraphList.add( AnnotationFactory.create( ann ) );
		}
		return namedEntityGraphList;
	}

	private static void bindNamedSubgraph(
			XMLContext.Default defaults,
			AnnotationDescriptor ann,
			String annotationAttributeName,
			List<JaxbNamedSubgraphImpl> subgraphNodes,
			ClassLoaderAccess classLoaderAccess) {
		List<NamedSubgraph> annSubgraphNodes = new ArrayList<>();
		for ( JaxbNamedSubgraphImpl subgraphNode : subgraphNodes ) {
			AnnotationDescriptor annSubgraphNode = new AnnotationDescriptor( NamedSubgraph.class );
			copyAttribute( annSubgraphNode, "name", subgraphNode.getName(), true );
			String clazzName = subgraphNode.getClazz();
			final Class<?> clazz;
			try {
				clazz = classLoaderAccess.classForName(
						XMLContext.buildSafeClassName( clazzName, defaults )
				);
			}
			catch (ClassLoadingException e) {
				throw new AnnotationException( "Unable to find entity-class: " + clazzName, e );
			}
			annSubgraphNode.setValue( "type", clazz );
			bindNamedAttributeNodes( subgraphNode.getNamedAttributeNode(), annSubgraphNode );
			annSubgraphNodes.add( AnnotationFactory.create( annSubgraphNode ) );
		}

		ann.setValue( annotationAttributeName, annSubgraphNodes.toArray( new NamedSubgraph[ annSubgraphNodes.size() ] ) );
	}

	private static void bindNamedAttributeNodes(List<JaxbNamedAttributeNodeImpl> elements, AnnotationDescriptor ann) {
		List<NamedAttributeNode> annNamedAttributeNodes = new ArrayList<>();
		for ( JaxbNamedAttributeNodeImpl element : elements ) {
			AnnotationDescriptor annNamedAttributeNode = new AnnotationDescriptor( NamedAttributeNode.class );
			copyAttribute( annNamedAttributeNode, "value", "name", element.getName(), true );
			copyAttribute( annNamedAttributeNode, "subgraph", element.getSubgraph(), false );
			copyAttribute( annNamedAttributeNode, "key-subgraph", element.getKeySubgraph(), false );
			annNamedAttributeNodes.add( AnnotationFactory.create( annNamedAttributeNode ) );
		}
		ann.setValue( "attributeNodes", annNamedAttributeNodes.toArray( new NamedAttributeNode[ 0 ] ) );
	}

	public static List<NamedStoredProcedureQuery> buildNamedStoreProcedureQueries(
			List<JaxbNamedStoredProcedureQueryImpl> elements,
			XMLContext.Default defaults,
			ClassLoaderAccess classLoaderAccess) {
		List<NamedStoredProcedureQuery> namedStoredProcedureQueries = new ArrayList<>();
		for ( JaxbNamedStoredProcedureQueryImpl element : elements ) {
			AnnotationDescriptor ann = new AnnotationDescriptor( NamedStoredProcedureQuery.class );
			copyAttribute( ann, "name", element.getName(), true );
			copyAttribute( ann, "procedure-name", element.getProcedureName(), true );

			List<StoredProcedureParameter> storedProcedureParameters = new ArrayList<>();

			for ( JaxbStoredProcedureParameterImpl parameterElement : element.getParameter() ) {
				AnnotationDescriptor parameterDescriptor = new AnnotationDescriptor( StoredProcedureParameter.class );
				copyAttribute( parameterDescriptor, "name", parameterElement.getName(), false );
				ParameterMode modeValue = parameterElement.getMode();
				if ( modeValue == null ) {
					parameterDescriptor.setValue( "mode", ParameterMode.IN );
				}
				else {
					parameterDescriptor.setValue( "mode", modeValue );
				}
				String clazzName = parameterElement.getClazz();
				Class<?> clazz;
				try {
					clazz = classLoaderAccess.classForName(
							XMLContext.buildSafeClassName( clazzName, defaults )
					);
				}
				catch (ClassLoadingException e) {
					throw new AnnotationException( "Unable to find entity-class: " + clazzName, e );
				}
				parameterDescriptor.setValue( "type", clazz );
				storedProcedureParameters.add( AnnotationFactory.create( parameterDescriptor ) );
			}

			ann.setValue(
					"parameters",
					storedProcedureParameters.toArray( new StoredProcedureParameter[ 0 ] )
			);

			final List<Class<?>> returnClasses = new ArrayList<>();
			for ( String clazzName : element.getResultClasses() ) {
				final Class<?> clazz;
				try {
					clazz = classLoaderAccess.classForName(
							XMLContext.buildSafeClassName( clazzName, defaults )
					);
				}
				catch (ClassLoadingException e) {
					throw new AnnotationException( "Unable to find entity-class: " + clazzName, e );
				}
				returnClasses.add( clazz );
			}
			ann.setValue( "resultClasses", returnClasses.toArray( new Class[ 0 ] ) );


			ann.setValue( "resultSetMappings", element.getResultSetMappings().toArray( new String[ 0 ] ) );
			buildQueryHints( element.getHints(), ann, Collections.emptyMap() );
			namedStoredProcedureQueries.add( AnnotationFactory.create( ann ) );
		}
		return namedStoredProcedureQueries;

	}

	public static List<SqlResultSetMapping> buildSqlResultSetMappings(
			List<JaxbSqlResultSetMappingImpl> elements,
			XMLContext.Default defaults,
			ClassLoaderAccess classLoaderAccess) {
		final List<SqlResultSetMapping> builtResultSetMappings = new ArrayList<>();

		// iterate over each <sql-result-set-mapping/> element
		for ( JaxbSqlResultSetMappingImpl resultSetMappingElement : elements ) {
			final AnnotationDescriptor resultSetMappingAnnotation = new AnnotationDescriptor( SqlResultSetMapping.class );
			copyAttribute( resultSetMappingAnnotation, "name", resultSetMappingElement.getName(), true );

			// iterate over the <sql-result-set-mapping/> sub-elements, which should include:
			//		* <entity-result/>
			//		* <column-result/>
			//		* <constructor-result/>

			List<EntityResult> entityResultAnnotations = null;
			List<ColumnResult> columnResultAnnotations = null;
			List<ConstructorResult> constructorResultAnnotations = null;

			for ( JaxbEntityResultImpl resultElement : resultSetMappingElement.getEntityResult() ) {
				if ( entityResultAnnotations == null ) {
					entityResultAnnotations = new ArrayList<>();
				}
				// process the <entity-result/>
				entityResultAnnotations.add( buildEntityResult( resultElement, defaults, classLoaderAccess ) );
			}
			for ( JaxbColumnResultImpl resultElement : resultSetMappingElement.getColumnResult() ) {
				if ( columnResultAnnotations == null ) {
					columnResultAnnotations = new ArrayList<>();
				}
				columnResultAnnotations.add( buildColumnResult( resultElement, defaults, classLoaderAccess ) );
			}
			for ( JaxbConstructorResultImpl resultElement : resultSetMappingElement.getConstructorResult() ) {
				if ( constructorResultAnnotations == null ) {
					constructorResultAnnotations = new ArrayList<>();
				}
				constructorResultAnnotations.add( buildConstructorResult( resultElement, defaults, classLoaderAccess ) );
			}

			if ( entityResultAnnotations != null && !entityResultAnnotations.isEmpty() ) {
				resultSetMappingAnnotation.setValue(
						"entities",
						entityResultAnnotations.toArray( new EntityResult[ entityResultAnnotations.size() ] )
				);
			}
			if ( columnResultAnnotations != null && !columnResultAnnotations.isEmpty() ) {
				resultSetMappingAnnotation.setValue(
						"columns",
						columnResultAnnotations.toArray( new ColumnResult[ columnResultAnnotations.size() ] )
				);
			}
			if ( constructorResultAnnotations != null && !constructorResultAnnotations.isEmpty() ) {
				resultSetMappingAnnotation.setValue(
						"classes",
						constructorResultAnnotations.toArray( new ConstructorResult[ constructorResultAnnotations.size() ] )
				);
			}

			builtResultSetMappings.add( AnnotationFactory.create( resultSetMappingAnnotation ) );
		}

		return builtResultSetMappings;
	}

	private static EntityResult buildEntityResult(
			JaxbEntityResultImpl entityResultElement,
			XMLContext.Default defaults,
			ClassLoaderAccess classLoaderAccess) {
		final AnnotationDescriptor entityResultDescriptor = new AnnotationDescriptor( EntityResult.class );

		final Class<?> entityClass = resolveClassReference( entityResultElement.getEntityClass(), defaults, classLoaderAccess );
		entityResultDescriptor.setValue( "entityClass", entityClass );

		copyAttribute( entityResultDescriptor, "discriminator-column", entityResultElement.getDiscriminatorColumn(), false );

		// process the <field-result/> sub-elements
		List<FieldResult> fieldResultAnnotations = new ArrayList<>();
		for ( JaxbFieldResultImpl fieldResult : entityResultElement.getFieldResult() ) {
			AnnotationDescriptor fieldResultDescriptor = new AnnotationDescriptor( FieldResult.class );
			copyAttribute( fieldResultDescriptor, "name", fieldResult.getName(), true );
			copyAttribute( fieldResultDescriptor, "column", fieldResult.getColumn(), true );
			fieldResultAnnotations.add( AnnotationFactory.create( fieldResultDescriptor ) );
		}
		entityResultDescriptor.setValue(
				"fields",
				fieldResultAnnotations.toArray( new FieldResult[ fieldResultAnnotations.size() ] )
		);
		return AnnotationFactory.create( entityResultDescriptor );
	}

	private static Class<?> resolveClassReference(
			String className,
			XMLContext.Default defaults,
			ClassLoaderAccess classLoaderAccess) {
		if ( className == null ) {
			throw new AnnotationException( "<entity-result> without entity-class. " + SCHEMA_VALIDATION );
		}
		try {
			return classLoaderAccess.classForName(
					XMLContext.buildSafeClassName( className, defaults )
			);
		}
		catch (ClassLoadingException e) {
			throw new AnnotationException( "Unable to find specified class: " + className, e );
		}
	}

	private static ColumnResult buildColumnResult(
			JaxbColumnResultImpl columnResultElement,
			XMLContext.Default defaults,
			ClassLoaderAccess classLoaderAccess) {
		AnnotationDescriptor columnResultDescriptor = new AnnotationDescriptor( ColumnResult.class );
		copyAttribute( columnResultDescriptor, "name", columnResultElement.getName(), true );
		final String columnTypeName = columnResultElement.getClazz();
		if ( isNotEmpty( columnTypeName ) ) {
			columnResultDescriptor.setValue( "type", resolveClassReference( columnTypeName, defaults, classLoaderAccess ) );
		}
		return AnnotationFactory.create( columnResultDescriptor );
	}

	private static ConstructorResult buildConstructorResult(
			JaxbConstructorResultImpl constructorResultElement,
			XMLContext.Default defaults,
			ClassLoaderAccess classLoaderAccess) {
		AnnotationDescriptor constructorResultDescriptor = new AnnotationDescriptor( ConstructorResult.class );

		final Class<?> entityClass = resolveClassReference( constructorResultElement.getTargetClass(), defaults, classLoaderAccess );
		constructorResultDescriptor.setValue( "targetClass", entityClass );

		List<ColumnResult> columnResultAnnotations = new ArrayList<>();
		for ( JaxbColumnResultImpl columnResultElement : constructorResultElement.getColumn() ) {
			columnResultAnnotations.add( buildColumnResult( columnResultElement, defaults, classLoaderAccess ) );
		}
		constructorResultDescriptor.setValue(
				"columns",
				columnResultAnnotations.toArray( new ColumnResult[ columnResultAnnotations.size() ] )
		);

		return AnnotationFactory.create( constructorResultDescriptor );
	}

	private void addSqlResultsetMappingIfNeeded(SqlResultSetMapping annotation, List<SqlResultSetMapping> resultsets) {
		if ( annotation != null ) {
			String resultsetName = annotation.name();
			boolean present = false;
			for ( SqlResultSetMapping current : resultsets ) {
				if ( current.name().equals( resultsetName ) ) {
					present = true;
					break;
				}
			}
			if ( !present ) {
				resultsets.add( annotation );
			}
		}
	}

	private NamedQueries getNamedQueries(JaxbManagedType root, XMLContext.Default defaults) {
		//TODO avoid the Proxy Creation (@NamedQueries) when possible
		List<NamedQuery> queries = root instanceof JaxbEntityImpl
				? buildNamedQueries( ( (JaxbEntityImpl) root ).getNamedQueries(), defaults, classLoaderAccess )
				: new ArrayList<>();
		if ( defaults.canUseJavaAnnotations() ) {
			NamedQuery annotation = getPhysicalAnnotation( NamedQuery.class );
			addNamedQueryIfNeeded( annotation, queries );
			NamedQueries annotations = getPhysicalAnnotation( NamedQueries.class );
			if ( annotations != null ) {
				for ( NamedQuery current : annotations.value() ) {
					addNamedQueryIfNeeded( current, queries );
				}
			}
		}
		if ( queries.size() > 0 ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( NamedQueries.class );
			ad.setValue( "value", queries.toArray( new NamedQuery[queries.size()] ) );
			return AnnotationFactory.create( ad );
		}
		else {
			return null;
		}
	}

	private void addNamedQueryIfNeeded(NamedQuery annotation, List<NamedQuery> queries) {
		if ( annotation != null ) {
			String queryName = annotation.name();
			boolean present = false;
			for ( NamedQuery current : queries ) {
				if ( current.name().equals( queryName ) ) {
					present = true;
					break;
				}
			}
			if ( !present ) {
				queries.add( annotation );
			}
		}
	}

	private NamedEntityGraphs getNamedEntityGraphs(JaxbManagedType root, XMLContext.Default defaults) {
		List<NamedEntityGraph> queries = root instanceof JaxbEntityImpl
				? buildNamedEntityGraph( ( (JaxbEntityImpl) root ).getNamedEntityGraphs(), defaults, classLoaderAccess )
				: new ArrayList<>();
		if ( defaults.canUseJavaAnnotations() ) {
			NamedEntityGraph annotation = getPhysicalAnnotation( NamedEntityGraph.class );
			addNamedEntityGraphIfNeeded( annotation, queries );
			NamedEntityGraphs annotations = getPhysicalAnnotation( NamedEntityGraphs.class );
			if ( annotations != null ) {
				for ( NamedEntityGraph current : annotations.value() ) {
					addNamedEntityGraphIfNeeded( current, queries );
				}
			}
		}
		if ( queries.size() > 0 ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( NamedEntityGraphs.class );
			ad.setValue( "value", queries.toArray( new NamedEntityGraph[queries.size()] ) );
			return AnnotationFactory.create( ad );
		}
		else {
			return null;
		}
	}

	private void addNamedEntityGraphIfNeeded(NamedEntityGraph annotation, List<NamedEntityGraph> queries) {
		if ( annotation != null ) {
			String queryName = annotation.name();
			boolean present = false;
			for ( NamedEntityGraph current : queries ) {
				if ( current.name().equals( queryName ) ) {
					present = true;
					break;
				}
			}
			if ( !present ) {
				queries.add( annotation );
			}
		}

	}

	private NamedStoredProcedureQueries getNamedStoredProcedureQueries(JaxbManagedType root, XMLContext.Default defaults) {
		List<NamedStoredProcedureQuery> queries = root instanceof JaxbEntityImpl
				? buildNamedStoreProcedureQueries( ( (JaxbEntityImpl) root ).getNamedStoredProcedureQueries(), defaults, classLoaderAccess )
				: new ArrayList<>();
		if ( defaults.canUseJavaAnnotations() ) {
			NamedStoredProcedureQuery annotation = getPhysicalAnnotation( NamedStoredProcedureQuery.class );
			addNamedStoredProcedureQueryIfNeeded( annotation, queries );
			NamedStoredProcedureQueries annotations = getPhysicalAnnotation( NamedStoredProcedureQueries.class );
			if ( annotations != null ) {
				for ( NamedStoredProcedureQuery current : annotations.value() ) {
					addNamedStoredProcedureQueryIfNeeded( current, queries );
				}
			}
		}
		if ( queries.size() > 0 ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( NamedStoredProcedureQueries.class );
			ad.setValue( "value", queries.toArray( new NamedStoredProcedureQuery[queries.size()] ) );
			return AnnotationFactory.create( ad );
		}
		else {
			return null;
		}
	}

	private void addNamedStoredProcedureQueryIfNeeded(NamedStoredProcedureQuery annotation, List<NamedStoredProcedureQuery> queries) {
		if ( annotation != null ) {
			String queryName = annotation.name();
			boolean present = false;
			for ( NamedStoredProcedureQuery current : queries ) {
				if ( current.name().equals( queryName ) ) {
					present = true;
					break;
				}
			}
			if ( !present ) {
				queries.add( annotation );
			}
		}
	}


	private NamedNativeQueries getNamedNativeQueries(
			JaxbManagedType root,
			XMLContext.Default defaults) {
		List<NamedNativeQuery> queries = root instanceof JaxbEntityImpl
				? buildNamedNativeQueries( ( (JaxbEntityImpl) root ).getNamedNativeQueries(), defaults, classLoaderAccess )
				: new ArrayList<>();
		if ( defaults.canUseJavaAnnotations() ) {
			NamedNativeQuery annotation = getPhysicalAnnotation( NamedNativeQuery.class );
			addNamedNativeQueryIfNeeded( annotation, queries );
			NamedNativeQueries annotations = getPhysicalAnnotation( NamedNativeQueries.class );
			if ( annotations != null ) {
				for ( NamedNativeQuery current : annotations.value() ) {
					addNamedNativeQueryIfNeeded( current, queries );
				}
			}
		}
		if ( queries.size() > 0 ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( NamedNativeQueries.class );
			ad.setValue( "value", queries.toArray( new NamedNativeQuery[queries.size()] ) );
			return AnnotationFactory.create( ad );
		}
		else {
			return null;
		}
	}

	private void addNamedNativeQueryIfNeeded(NamedNativeQuery annotation, List<NamedNativeQuery> queries) {
		if ( annotation != null ) {
			String queryName = annotation.name();
			boolean present = false;
			for ( NamedNativeQuery current : queries ) {
				if ( current.name().equals( queryName ) ) {
					present = true;
					break;
				}
			}
			if ( !present ) {
				queries.add( annotation );
			}
		}
	}

	private static void buildQueryHints(
			List<JaxbQueryHintImpl> elements,
			AnnotationDescriptor ann,
			Map<String, String> additionalHints) {
		List<QueryHint> queryHints = new ArrayList<>( elements.size() + additionalHints.size() );
		for ( Map.Entry<String, String> entry : additionalHints.entrySet() ) {
			AnnotationDescriptor hintDescriptor = new AnnotationDescriptor( QueryHint.class );
			hintDescriptor.setValue( "name", entry.getKey() );
			hintDescriptor.setValue( "value", entry.getValue() );
			queryHints.add( AnnotationFactory.create( hintDescriptor ) );
		}
		for ( JaxbQueryHintImpl hint : elements ) {
			String value = hint.getName();
			if ( value == null ) {
				throw new AnnotationException( "<hint> without name. " + SCHEMA_VALIDATION );
			}
			if ( additionalHints.containsKey( value ) ) {
				continue;
			}
			AnnotationDescriptor hintDescriptor = new AnnotationDescriptor( QueryHint.class );
			hintDescriptor.setValue( "name", value );
			value = hint.getValue();
			if ( value == null ) {
				throw new AnnotationException( "<hint> without value. " + SCHEMA_VALIDATION );
			}
			hintDescriptor.setValue( "value", value );
			queryHints.add( AnnotationFactory.create( hintDescriptor ) );
		}
		ann.setValue( "hints", queryHints.toArray( new QueryHint[queryHints.size()] ) );
	}

	public static List<NamedQuery> buildNamedQueries(
			List<JaxbNamedQueryImpl> elements,
			XMLContext.Default defaults,
			ClassLoaderAccess classLoaderAccess) {
		List<NamedQuery> namedQueries = new ArrayList<>();
		for ( JaxbNamedQueryImpl element : elements ) {
			AnnotationDescriptor ann = new AnnotationDescriptor( NamedQuery.class );
			copyAttribute( ann, "name", element.getName(), false );
			copyAttribute( ann, "query", element.getQuery(), true );
			Map<String, String> additionalHints = new HashMap<>();
			addHint( additionalHints, AvailableHints.HINT_CACHEABLE, element.isCacheable() );
			addHint( additionalHints, AvailableHints.HINT_CACHE_MODE, element.getCacheMode() );
			addHint( additionalHints, AvailableHints.HINT_CACHE_REGION, element.getCacheRegion() );
			addHint( additionalHints, AvailableHints.HINT_COMMENT, element.getComment() );
			addHint( additionalHints, AvailableHints.HINT_FETCH_SIZE, element.getFetchSize() );
			addHint( additionalHints, AvailableHints.HINT_FLUSH_MODE, element.getFlushMode() );
			addHint( additionalHints, AvailableHints.HINT_TIMEOUT, element.getTimeout() );
			buildQueryHints( element.getHints(), ann, additionalHints );
			copyAttribute( ann, "lock-mode", element.getLockMode(), false );
			namedQueries.add( AnnotationFactory.create( ann ) );
		}
		return namedQueries;
	}

	public static List<NamedNativeQuery> buildNamedNativeQueries(
			List<JaxbNamedNativeQueryImpl> elements,
			XMLContext.Default defaults,
			ClassLoaderAccess classLoaderAccess) {
		List<NamedNativeQuery> namedQueries = new ArrayList<>();
		for ( JaxbNamedNativeQueryImpl element : elements ) {
			AnnotationDescriptor ann = new AnnotationDescriptor( NamedNativeQuery.class );
			copyAttribute( ann, "name", element.getName(), false );
			copyAttribute( ann, "query", element.getQuery(), true );
			Map<String, String> additionalHints = new HashMap<>();
			addHint( additionalHints, AvailableHints.HINT_CACHEABLE, element.isCacheable() );
			addHint( additionalHints, AvailableHints.HINT_CACHE_MODE, element.getCacheMode() );
			addHint( additionalHints, AvailableHints.HINT_CACHE_REGION, element.getCacheRegion() );
			addHint( additionalHints, AvailableHints.HINT_COMMENT, element.getComment() );
			addHint( additionalHints, AvailableHints.HINT_FETCH_SIZE, element.getFetchSize() );
			addHint( additionalHints, AvailableHints.HINT_FLUSH_MODE, element.getFlushMode() );
			addHint( additionalHints, AvailableHints.HINT_TIMEOUT, element.getTimeout() );
			addSynchronizationsHint( additionalHints, element.getSynchronizations() );
			buildQueryHints( element.getHints(), ann, additionalHints );
			String clazzName = element.getResultClass();
			if ( isNotEmpty( clazzName ) ) {
				Class clazz;
				try {
					clazz = classLoaderAccess.classForName(
							XMLContext.buildSafeClassName( clazzName, defaults )
					);
				}
				catch (ClassLoadingException e) {
					throw new AnnotationException( "Unable to find entity-class: " + clazzName, e );
				}
				ann.setValue( "resultClass", clazz );
			}
			copyAttribute( ann, "result-set-mapping", element.getResultSetMapping(), false );
			namedQueries.add( AnnotationFactory.create( ann ) );
		}
		return namedQueries;
	}

	private static void addHint(Map<String, String> hints, String hint, Object value) {
		if ( value != null ) {
			hints.put( hint, value.toString() );
		}
	}

	private static void addSynchronizationsHint(
			Map<String, String> hints,
			List<JaxbSynchronizedTableImpl> synchronizations) {
		if ( synchronizations.isEmpty() ) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append( synchronizations.get( 0 ).getTable() );
		for ( int i = 1; i < synchronizations.size(); i++ ) {
			sb.append( ' ' );
			sb.append( synchronizations.get( i ).getTable() );
		}
		hints.put( AvailableHints.HINT_NATIVE_SPACES, sb.toString() );
	}

	private TableGenerator getTableGenerator(JaxbManagedType root, XMLContext.Default defaults) {
		return getTableGenerator(
				root instanceof JaxbEntityImpl ? ( (JaxbEntityImpl) root ).getTableGenerator() : null,
				defaults
		);
	}

	private TableGenerator getTableGenerator(JaxbTableGeneratorImpl element, XMLContext.Default defaults) {
		if ( element != null ) {
			return buildTableGeneratorAnnotation( element, defaults );
		}
		else if ( defaults.canUseJavaAnnotations() && isPhysicalAnnotationPresent( TableGenerator.class ) ) {
			TableGenerator tableAnn = getPhysicalAnnotation( TableGenerator.class );
			if ( isNotEmpty( defaults.getSchema() )
					|| isNotEmpty( defaults.getCatalog() ) ) {
				AnnotationDescriptor annotation = new AnnotationDescriptor( TableGenerator.class );
				annotation.setValue( "name", tableAnn.name() );
				annotation.setValue( "table", tableAnn.table() );
				annotation.setValue( "catalog", tableAnn.table() );
				if ( isEmpty( (String) annotation.valueOf( "catalog" ) )
						&& isNotEmpty( defaults.getCatalog() ) ) {
					annotation.setValue( "catalog", defaults.getCatalog() );
				}
				annotation.setValue( "schema", tableAnn.table() );
				if ( isEmpty( (String) annotation.valueOf( "schema" ) )
						&& isNotEmpty( defaults.getSchema() ) ) {
					annotation.setValue( "catalog", defaults.getSchema() );
				}
				annotation.setValue( "pkColumnName", tableAnn.pkColumnName() );
				annotation.setValue( "valueColumnName", tableAnn.valueColumnName() );
				annotation.setValue( "pkColumnValue", tableAnn.pkColumnValue() );
				annotation.setValue( "initialValue", tableAnn.initialValue() );
				annotation.setValue( "allocationSize", tableAnn.allocationSize() );
				annotation.setValue( "uniqueConstraints", tableAnn.uniqueConstraints() );
				return AnnotationFactory.create( annotation );
			}
			else {
				return tableAnn;
			}
		}
		else {
			return null;
		}
	}

	public static TableGenerator buildTableGeneratorAnnotation(JaxbTableGeneratorImpl element, XMLContext.Default defaults) {
		AnnotationDescriptor ad = new AnnotationDescriptor( TableGenerator.class );
		copyAttribute( ad, "name", element.getName(), false );
		copyAttribute( ad, "table", element.getTable(), false );
		copyAttribute( ad, "catalog", element.getCatalog(), false );
		copyAttribute( ad, "schema", element.getSchema(), false );
		copyAttribute( ad, "pk-column-name", element.getPkColumnName(), false );
		copyAttribute( ad, "value-column-name", element.getValueColumnName(), false );
		copyAttribute( ad, "pk-column-value", element.getPkColumnValue(), false );
		copyAttribute( ad, "initial-value", element.getInitialValue(), false );
		copyAttribute( ad, "allocation-size", element.getAllocationSize(), false );
		buildUniqueConstraints( ad, element.getUniqueConstraint() );
		if ( isEmpty( (String) ad.valueOf( "schema" ) )
				&& isNotEmpty( defaults.getSchema() ) ) {
			ad.setValue( "schema", defaults.getSchema() );
		}
		if ( isEmpty( (String) ad.valueOf( "catalog" ) )
				&& isNotEmpty( defaults.getCatalog() ) ) {
			ad.setValue( "catalog", defaults.getCatalog() );
		}
		return AnnotationFactory.create( ad );
	}

	private SequenceGenerator getSequenceGenerator(JaxbManagedType root, XMLContext.Default defaults) {
		return getSequenceGenerator(
				root instanceof JaxbEntityImpl ? ( (JaxbEntityImpl) root ).getSequenceGenerator() : null,
				defaults
		);
	}

	private SequenceGenerator getSequenceGenerator(JaxbSequenceGeneratorImpl element, XMLContext.Default defaults) {
		if ( element != null ) {
			return buildSequenceGeneratorAnnotation( element );
		}
		else if ( defaults.canUseJavaAnnotations() ) {
			return getPhysicalAnnotation( SequenceGenerator.class );
		}
		else {
			return null;
		}
	}

	public static SequenceGenerator buildSequenceGeneratorAnnotation(JaxbSequenceGeneratorImpl element) {
		if ( element != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( SequenceGenerator.class );
			copyAttribute( ad, "name", element.getName(), false );
			copyAttribute( ad, "sequence-name", element.getSequenceName(), false );
			copyAttribute( ad, "initial-value", element.getInitialValue(), false );
			copyAttribute( ad, "allocation-size", element.getAllocationSize(), false );
			return AnnotationFactory.create( ad );
		}
		else {
			return null;
		}
	}

	private DiscriminatorColumn getDiscriminatorColumn(JaxbManagedType root, XMLContext.Default defaults) {
		JaxbDiscriminatorColumnImpl element = root instanceof JaxbEntityImpl
				? ( (JaxbEntityImpl) root ).getDiscriminatorColumn()
				: null;
		if ( element != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( DiscriminatorColumn.class );
			copyAttribute( ad, "name", element.getName(), false );
			copyAttribute( ad, "column-definition", element.getColumnDefinition(), false );
			DiscriminatorType type = element.getDiscriminatorType();
			if ( type != null ) {
				ad.setValue( "discriminatorType", type );
			}
			copyAttribute( ad, "length", element.getLength(), false );
			return AnnotationFactory.create( ad );
		}
		else if ( defaults.canUseJavaAnnotations() ) {
			return getPhysicalAnnotation( DiscriminatorColumn.class );
		}
		else {
			return null;
		}
	}

	private DiscriminatorValue getDiscriminatorValue(JaxbManagedType root, XMLContext.Default defaults) {
		String element = root instanceof JaxbEntityImpl ? ( (JaxbEntityImpl) root ).getDiscriminatorValue() : null;
		if ( element != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( DiscriminatorValue.class );
			ad.setValue( "value", element );
			return AnnotationFactory.create( ad );
		}
		else if ( defaults.canUseJavaAnnotations() ) {
			return getPhysicalAnnotation( DiscriminatorValue.class );
		}
		else {
			return null;
		}
	}

	private Inheritance getInheritance(JaxbManagedType root, XMLContext.Default defaults) {
		JaxbInheritanceImpl element = root instanceof JaxbEntityImpl ? ( (JaxbEntityImpl) root ).getInheritance() : null;
		if ( element != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( Inheritance.class );
			InheritanceType strategy = element.getStrategy();
			if ( strategy != null ) {
				ad.setValue( "strategy", strategy );
			}
			return AnnotationFactory.create( ad );
		}
		else if ( defaults.canUseJavaAnnotations() ) {
			return getPhysicalAnnotation( Inheritance.class );
		}
		else {
			return null;
		}
	}

	private IdClass getIdClass(JaxbManagedType root, XMLContext.Default defaults) {
		final JaxbIdClassImpl element = root instanceof JaxbEntityOrMappedSuperclass
				? ( (JaxbEntityOrMappedSuperclass) root ).getIdClass()
				: null;
		if ( element != null ) {
			String className = element.getClazz();
			if ( className != null ) {
				AnnotationDescriptor ad = new AnnotationDescriptor( IdClass.class );
				Class<?> clazz;
				try {
					clazz = classLoaderAccess.classForName( XMLContext.buildSafeClassName( className, defaults ) );
				}
				catch (ClassLoadingException e) {
					throw new AnnotationException( "Unable to find id-class: " + className, e );
				}
				ad.setValue( "value", clazz );
				return AnnotationFactory.create( ad );
			}
			else {
				throw new AnnotationException( "id-class without class. " + SCHEMA_VALIDATION );
			}
		}
		else if ( defaults.canUseJavaAnnotations() ) {
			return getPhysicalAnnotation( IdClass.class );
		}
		else {
			return null;
		}
	}

	private PrimaryKeyJoinColumns getPrimaryKeyJoinColumns(JaxbManagedType root, XMLContext.Default defaults) {
		return getPrimaryKeyJoinColumns(
				root instanceof JaxbEntityImpl ? ( (JaxbEntityImpl) root ).getPrimaryKeyJoinColumns() : Collections.emptyList(),
				defaults, true
		);
	}

	/**
	 * @param mergeWithAnnotations Whether to use Java annotations for this
	 * 		element, if present and not disabled by the XMLContext defaults.
	 * 		In some contexts (such as an association mapping) merging with
	 */
	private PrimaryKeyJoinColumns getPrimaryKeyJoinColumns(
			List<JaxbPrimaryKeyJoinColumnImpl> elements,
			XMLContext.Default defaults,
			boolean mergeWithAnnotations) {
		PrimaryKeyJoinColumn[] columns = buildPrimaryKeyJoinColumns( elements );
		if ( mergeWithAnnotations ) {
			if ( columns.length == 0 && defaults.canUseJavaAnnotations() ) {
				PrimaryKeyJoinColumn annotation = getPhysicalAnnotation( PrimaryKeyJoinColumn.class );
				if ( annotation != null ) {
					columns = new PrimaryKeyJoinColumn[] { annotation };
				}
				else {
					PrimaryKeyJoinColumns annotations = getPhysicalAnnotation( PrimaryKeyJoinColumns.class );
					columns = annotations != null ? annotations.value() : columns;
				}
			}
		}
		if ( columns.length > 0 ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( PrimaryKeyJoinColumns.class );
			ad.setValue( "value", columns );
			return AnnotationFactory.create( ad );
		}
		else {
			return null;
		}
	}

	private Entity getEntity(JaxbManagedType element, XMLContext.Default defaults) {
		if ( element == null ) {
			return defaults.canUseJavaAnnotations() ? getPhysicalAnnotation( Entity.class ) : null;
		}
		else {
			if ( element instanceof JaxbEntityImpl ) {
				JaxbEntityImpl entityElement = (JaxbEntityImpl) element;
				AnnotationDescriptor entity = new AnnotationDescriptor( Entity.class );
				copyAttribute( entity, "name", entityElement.getName(), false );
				if ( defaults.canUseJavaAnnotations()
						&& isEmpty( (String) entity.valueOf( "name" ) ) ) {
					Entity javaAnn = getPhysicalAnnotation( Entity.class );
					if ( javaAnn != null ) {
						entity.setValue( "name", javaAnn.name() );
					}
				}
				return AnnotationFactory.create( entity );
			}
			else {
				return null; //this is not an entity
			}
		}
	}

	private MappedSuperclass getMappedSuperclass(JaxbManagedType element, XMLContext.Default defaults) {
		if ( element == null ) {
			return defaults.canUseJavaAnnotations() ? getPhysicalAnnotation( MappedSuperclass.class ) : null;
		}
		else {
			if ( element instanceof JaxbMappedSuperclassImpl ) {
				AnnotationDescriptor entity = new AnnotationDescriptor( MappedSuperclass.class );
				return AnnotationFactory.create( entity );
			}
			else {
				return null; //this is not an entity
			}
		}
	}

	private Embeddable getEmbeddable(JaxbManagedType element, XMLContext.Default defaults) {
		if ( element == null ) {
			return defaults.canUseJavaAnnotations() ? getPhysicalAnnotation( Embeddable.class ) : null;
		}
		else {
			if ( element instanceof JaxbEmbeddableImpl ) {
				AnnotationDescriptor entity = new AnnotationDescriptor( Embeddable.class );
				return AnnotationFactory.create( entity );
			}
			else {
				//this is not an entity
				return null;
			}
		}
	}

	private Subselect getTableExpression(JaxbEntityImpl entity, XMLContext.Default defaults) {
		final String tableExpression = entity.getTableExpression();
		if ( isEmpty( tableExpression ) ) {
			return null;
		}

		final AnnotationDescriptor annotation = new AnnotationDescriptor( Subselect.class );
		annotation.setValue( "value", tableExpression );
		return AnnotationFactory.create( annotation );
	}

	private Table getTable(JaxbManagedType root, XMLContext.Default defaults) {
		final JaxbTableImpl element = root instanceof JaxbEntityImpl ? ( (JaxbEntityImpl) root ).getTable() : null;
		if ( element == null ) {
			//no element but might have some default or some annotation
			if ( isNotEmpty( defaults.getCatalog() )
					|| isNotEmpty( defaults.getSchema() ) ) {
				AnnotationDescriptor annotation = new AnnotationDescriptor( Table.class );
				if ( defaults.canUseJavaAnnotations() ) {
					Table table = getPhysicalAnnotation( Table.class );
					if ( table != null ) {
						annotation.setValue( "name", table.name() );
						annotation.setValue( "schema", table.schema() );
						annotation.setValue( "catalog", table.catalog() );
						annotation.setValue( "uniqueConstraints", table.uniqueConstraints() );
						annotation.setValue( "indexes", table.indexes() );
					}
				}
				if ( isEmpty( (String) annotation.valueOf( "schema" ) )
						&& isNotEmpty( defaults.getSchema() ) ) {
					annotation.setValue( "schema", defaults.getSchema() );
				}
				if ( isEmpty( (String) annotation.valueOf( "catalog" ) )
						&& isNotEmpty( defaults.getCatalog() ) ) {
					annotation.setValue( "catalog", defaults.getCatalog() );
				}
				return AnnotationFactory.create( annotation );
			}
			else if ( defaults.canUseJavaAnnotations() ) {
				return getPhysicalAnnotation( Table.class );
			}
			else {
				return null;
			}
		}
		else {
			//ignore java annotation, an element is defined
			AnnotationDescriptor annotation = new AnnotationDescriptor( Table.class );
			copyAttribute( annotation, "name", element.getName(), false );
			copyAttribute( annotation, "catalog", element.getCatalog(), false );
			if ( isNotEmpty( defaults.getCatalog() )
					&& isEmpty( (String) annotation.valueOf( "catalog" ) ) ) {
				annotation.setValue( "catalog", defaults.getCatalog() );
			}
			copyAttribute( annotation, "schema", element.getSchema(), false );
			if ( isNotEmpty( defaults.getSchema() )
					&& isEmpty( (String) annotation.valueOf( "schema" ) ) ) {
				annotation.setValue( "schema", defaults.getSchema() );
			}
			buildUniqueConstraints( annotation, element.getUniqueConstraint() );
			buildIndex( annotation, element.getIndex() );
			return AnnotationFactory.create( annotation );
		}
	}

	private SecondaryTables getSecondaryTables(JaxbManagedType root, XMLContext.Default defaults) {
		final List<JaxbSecondaryTableImpl> elements = root instanceof JaxbEntity
				? ( (JaxbEntity) root ).getSecondaryTables()
				: Collections.emptyList();
		final List<SecondaryTable> secondaryTables = new ArrayList<>( 3 );
		for ( JaxbSecondaryTableImpl element : elements ) {
			AnnotationDescriptor annotation = new AnnotationDescriptor( SecondaryTable.class );
			copyAttribute( annotation, "name", element.getName(), false );
			copyAttribute( annotation, "catalog", element.getCatalog(), false );
			if ( isNotEmpty( defaults.getCatalog() )
					&& isEmpty( (String) annotation.valueOf( "catalog" ) ) ) {
				annotation.setValue( "catalog", defaults.getCatalog() );
			}
			copyAttribute( annotation, "schema", element.getSchema(), false );
			if ( isNotEmpty( defaults.getSchema() )
					&& isEmpty( (String) annotation.valueOf( "schema" ) ) ) {
				annotation.setValue( "schema", defaults.getSchema() );
			}
			buildUniqueConstraints( annotation, element.getUniqueConstraint() );
			buildIndex( annotation, element.getIndex() );
			annotation.setValue( "pkJoinColumns",
					buildPrimaryKeyJoinColumns( element.getPrimaryKeyJoinColumn() ) );
			secondaryTables.add( AnnotationFactory.create( annotation ) );
		}
		/*
		 * You can't have both secondary tables in XML and Java,
		 * since there would be no way to "remove" a secondary table
		 */
		if ( secondaryTables.size() == 0 && defaults.canUseJavaAnnotations() ) {
			SecondaryTable secTableAnn = getPhysicalAnnotation( SecondaryTable.class );
			overridesDefaultInSecondaryTable( secTableAnn, defaults, secondaryTables );
			SecondaryTables secTablesAnn = getPhysicalAnnotation( SecondaryTables.class );
			if ( secTablesAnn != null ) {
				for ( SecondaryTable table : secTablesAnn.value() ) {
					overridesDefaultInSecondaryTable( table, defaults, secondaryTables );
				}
			}
		}
		if ( secondaryTables.size() > 0 ) {
			AnnotationDescriptor descriptor = new AnnotationDescriptor( SecondaryTables.class );
			descriptor.setValue( "value", secondaryTables.toArray( new SecondaryTable[secondaryTables.size()] ) );
			return AnnotationFactory.create( descriptor );
		}
		else {
			return null;
		}
	}

	private void overridesDefaultInSecondaryTable(
			SecondaryTable secTableAnn, XMLContext.Default defaults, List<SecondaryTable> secondaryTables
	) {
		if ( secTableAnn != null ) {
			//handle default values
			if ( isNotEmpty( defaults.getCatalog() )
					|| isNotEmpty( defaults.getSchema() ) ) {
				AnnotationDescriptor annotation = new AnnotationDescriptor( SecondaryTable.class );
				annotation.setValue( "name", secTableAnn.name() );
				annotation.setValue( "schema", secTableAnn.schema() );
				annotation.setValue( "catalog", secTableAnn.catalog() );
				annotation.setValue( "uniqueConstraints", secTableAnn.uniqueConstraints() );
				annotation.setValue( "pkJoinColumns", secTableAnn.pkJoinColumns() );
				if ( isEmpty( (String) annotation.valueOf( "schema" ) )
						&& isNotEmpty( defaults.getSchema() ) ) {
					annotation.setValue( "schema", defaults.getSchema() );
				}
				if ( isEmpty( (String) annotation.valueOf( "catalog" ) )
						&& isNotEmpty( defaults.getCatalog() ) ) {
					annotation.setValue( "catalog", defaults.getCatalog() );
				}
				secondaryTables.add( AnnotationFactory.create( annotation ) );
			}
			else {
				secondaryTables.add( secTableAnn );
			}
		}
	}

	private static void buildIndex(AnnotationDescriptor annotation, List<JaxbIndexImpl> elements) {
		Index[] indexes = new Index[ elements.size() ];
		int i = 0;
		for ( JaxbIndexImpl element : elements ) {
			AnnotationDescriptor indexAnn = new AnnotationDescriptor( Index.class );
			copyAttribute( indexAnn, "name", element.getName(), false );
			copyAttribute( indexAnn, "column-list", element.getColumnList(), true );
			copyAttribute( indexAnn, "unique", element.isUnique(), false );
			indexes[ i++ ] = AnnotationFactory.create( indexAnn );
		}
		annotation.setValue( "indexes", indexes );
	}

	private static void buildUniqueConstraints(
			AnnotationDescriptor annotation,
			List<JaxbUniqueConstraintImpl> elements) {
		UniqueConstraint[] uniqueConstraints = new UniqueConstraint[ elements.size() ];
		int i = 0;
		for ( JaxbUniqueConstraintImpl element : elements ) {
			String[] columnNames = element.getColumnName().toArray( new String[ 0 ] );
			AnnotationDescriptor ucAnn = new AnnotationDescriptor( UniqueConstraint.class );
			copyAttribute( ucAnn, "name", element.getName(), false );
			ucAnn.setValue( "columnNames", columnNames );
			uniqueConstraints[ i++ ] = AnnotationFactory.create( ucAnn );
		}
		annotation.setValue( "uniqueConstraints", uniqueConstraints );
	}

	private PrimaryKeyJoinColumn[] buildPrimaryKeyJoinColumns(List<JaxbPrimaryKeyJoinColumnImpl> elements) {
		PrimaryKeyJoinColumn[] pkJoinColumns = new PrimaryKeyJoinColumn[ elements.size() ];
		int i = 0;
		for ( JaxbPrimaryKeyJoinColumnImpl element : elements ) {
			AnnotationDescriptor pkAnn = new AnnotationDescriptor( PrimaryKeyJoinColumn.class );
			copyAttribute( pkAnn, "name", element.getName(), false );
			copyAttribute( pkAnn, "referenced-column-name", element.getReferencedColumnName(), false );
			copyAttribute( pkAnn, "column-definition", element.getColumnDefinition(), false );
			pkJoinColumns[ i++ ] = AnnotationFactory.create( pkAnn );
		}
		return pkJoinColumns;
	}

	/**
	 * Copy an attribute from an XML element to an annotation descriptor. The name of the annotation attribute is
	 * computed from the name of the XML attribute by {@link #getJavaAttributeNameFromXMLOne(String)}.
	 *
	 * @param annotation annotation descriptor where to copy to the attribute.
	 * @param attributeName name of the XML attribute to copy.
	 * @param attributeValue value of the XML attribute to copy.
	 * @param mandatory whether the attribute is mandatory.
	 */
	private static void copyAttribute(
			final AnnotationDescriptor annotation,
			final String attributeName, final Object attributeValue,
			final boolean mandatory) {
		copyAttribute(
				annotation,
				getJavaAttributeNameFromXMLOne( attributeName ),
				attributeName,
				attributeValue,
				mandatory
		);
	}

	/**
	 * Copy an attribute from an XML element to an annotation descriptor. The name of the annotation attribute is
	 * explicitly given.
	 *
	 * @param annotation annotation where to copy to the attribute.
	 * @param annotationAttributeName name of the annotation attribute where to copy.
	 * @param attributeValue value of the XML attribute to copy.
	 * @param mandatory whether the attribute is mandatory.
	 */
	private static void copyAttribute(
			final AnnotationDescriptor annotation,
			final String annotationAttributeName, final Object attributeName,
			final Object attributeValue,
			boolean mandatory) {
		if ( attributeValue != null ) {
			annotation.setValue( annotationAttributeName, attributeValue );
		}
		else {
			if ( mandatory ) {
				throw new AnnotationException(
						annotationToXml.getOrDefault( annotation.type(), annotation.type().getName() )
								+ "." + attributeName
								+ " is mandatory in XML overriding. " + SCHEMA_VALIDATION
				);
			}
		}
	}

	private static String getJavaAttributeNameFromXMLOne(String attributeName) {
		StringBuilder annotationAttributeName = new StringBuilder( attributeName );
		int index = annotationAttributeName.indexOf( WORD_SEPARATOR );
		while ( index != -1 ) {
			annotationAttributeName.deleteCharAt( index );
			annotationAttributeName.setCharAt(
					index, Character.toUpperCase( annotationAttributeName.charAt( index ) )
			);
			index = annotationAttributeName.indexOf( WORD_SEPARATOR );
		}
		return annotationAttributeName.toString();
	}

	private <T extends Annotation> T getPhysicalAnnotation(Class<T> annotationType) {
		return element.getAnnotation( annotationType );
	}

	private <T extends Annotation> boolean isPhysicalAnnotationPresent(Class<T> annotationType) {
		return element.isAnnotationPresent( annotationType );
	}

	private Annotation[] getPhysicalAnnotations() {
		return element.getAnnotations();
	}

}
