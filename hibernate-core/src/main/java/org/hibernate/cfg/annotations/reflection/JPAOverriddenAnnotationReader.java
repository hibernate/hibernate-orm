/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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

package org.hibernate.cfg.annotations.reflection;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EntityResult;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ExcludeDefaultListeners;
import javax.persistence.ExcludeSuperclassListeners;
import javax.persistence.FetchType;
import javax.persistence.FieldResult;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.MapKeyClass;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.MapKeyJoinColumns;
import javax.persistence.MapKeyTemporal;
import javax.persistence.MappedSuperclass;
import javax.persistence.MapsId;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;
import javax.persistence.QueryHint;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.jboss.logging.Logger;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.hibernate.annotations.common.reflection.AnnotationReader;
import org.hibernate.annotations.common.reflection.Filter;
import org.hibernate.annotations.common.reflection.ReflectionUtil;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;

/**
 * Encapsulates the overriding of Java annotations from an EJB 3.0 descriptor.
 *
 * @author Paolo Perrotta
 * @author Davide Marchignoli
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
@SuppressWarnings("unchecked")
public class JPAOverriddenAnnotationReader implements AnnotationReader {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       JPAOverriddenAnnotationReader.class.getName());
	private static final Map<Class, String> annotationToXml;
	private static final String SCHEMA_VALIDATION = "Activate schema validation for more information";
	private static final Filter FILTER = new Filter() {
		public boolean returnStatic() {
			return false;
		}

		public boolean returnTransient() {
			return false;
		}
	};

	static {
		annotationToXml = new HashMap<Class, String>();
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
		annotationToXml.put( NamedQuery.class, "named-query" );
		annotationToXml.put( NamedQueries.class, "named-query" );
		annotationToXml.put( NamedNativeQuery.class, "named-native-query" );
		annotationToXml.put( NamedNativeQueries.class, "named-native-query" );
		annotationToXml.put( SqlResultSetMapping.class, "sql-result-set-mapping" );
		annotationToXml.put( SqlResultSetMappings.class, "sql-result-set-mapping" );
		annotationToXml.put( ExcludeDefaultListeners.class, "exclude-default-listeners" );
		annotationToXml.put( ExcludeSuperclassListeners.class, "exclude-superclass-listeners" );
		annotationToXml.put( AccessType.class, "access" );
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
	}

	private XMLContext xmlContext;
	private String className;
	private String propertyName;
	private PropertyType propertyType;
	private transient Annotation[] annotations;
	private transient Map<Class, Annotation> annotationsMap;
	private static final String WORD_SEPARATOR = "-";
	private transient List<Element> elementsForProperty;
	private AccessibleObject mirroredAttribute;
	private final AnnotatedElement element;

	private enum PropertyType {
		PROPERTY,
		FIELD,
		METHOD
	}

	public JPAOverriddenAnnotationReader(AnnotatedElement el, XMLContext xmlContext) {
		this.element = el;
		this.xmlContext = xmlContext;
		if ( el instanceof Class ) {
			Class clazz = (Class) el;
			className = clazz.getName();
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
			catch ( NoSuchMethodException e ) {
				//no method
			}
		}
		else if ( el instanceof Method ) {
			Method method = (Method) el;
			className = method.getDeclaringClass().getName();
			propertyName = method.getName();
			if ( ReflectionUtil.isProperty(
					method,
					null, //this is yukky!! we'd rather get the TypeEnvironment()
					FILTER
			) ) {
				if ( propertyName.startsWith( "get" ) ) {
					propertyName = Introspector.decapitalize( propertyName.substring( "get".length() ) );
				}
				else if ( propertyName.startsWith( "is" ) ) {
					propertyName = Introspector.decapitalize( propertyName.substring( "is".length() ) );
				}
				else {
					throw new RuntimeException( "Method " + propertyName + " is not a property getter" );
				}
				propertyType = PropertyType.PROPERTY;
				try {
					mirroredAttribute = method.getDeclaringClass().getDeclaredField( propertyName );
				}
				catch ( NoSuchFieldException e ) {
					//no method
				}
			}
			else {
				propertyType = PropertyType.METHOD;
			}
		}
		else {
			className = null;
			propertyName = null;
		}
	}

	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		initAnnotations();
		return (T) annotationsMap.get( annotationType );
	}

	public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
		initAnnotations();
		return (T) annotationsMap.get( annotationType ) != null;
	}

	public Annotation[] getAnnotations() {
		initAnnotations();
		return annotations;
	}

	/*
	 * The idea is to create annotation proxies for the xml configuration elements. Using this proxy annotations together
	 * with the {@code JPAMetadataprovider} allows to handle xml configuration the same way as annotation configuration.
	 */
	private void initAnnotations() {
		if ( annotations == null ) {
			XMLContext.Default defaults = xmlContext.getDefault( className );
			if ( className != null && propertyName == null ) {
				//is a class
				Element tree = xmlContext.getXMLTree( className );
				Annotation[] annotations = getJavaAnnotations();
				List<Annotation> annotationList = new ArrayList<Annotation>( annotations.length + 5 );
				annotationsMap = new HashMap<Class, Annotation>( annotations.length + 5 );
				for ( Annotation annotation : annotations ) {
					if ( !annotationToXml.containsKey( annotation.annotationType() ) ) {
						//unknown annotations are left over
						annotationList.add( annotation );
					}
				}
				addIfNotNull( annotationList, getEntity( tree, defaults ) );
				addIfNotNull( annotationList, getMappedSuperclass( tree, defaults ) );
				addIfNotNull( annotationList, getEmbeddable( tree, defaults ) );
				addIfNotNull( annotationList, getTable( tree, defaults ) );
				addIfNotNull( annotationList, getSecondaryTables( tree, defaults ) );
				addIfNotNull( annotationList, getPrimaryKeyJoinColumns( tree, defaults, true ) );
				addIfNotNull( annotationList, getIdClass( tree, defaults ) );
				addIfNotNull( annotationList, getCacheable( tree, defaults ) );
				addIfNotNull( annotationList, getInheritance( tree, defaults ) );
				addIfNotNull( annotationList, getDiscriminatorValue( tree, defaults ) );
				addIfNotNull( annotationList, getDiscriminatorColumn( tree, defaults ) );
				addIfNotNull( annotationList, getSequenceGenerator( tree, defaults ) );
				addIfNotNull( annotationList, getTableGenerator( tree, defaults ) );
				addIfNotNull( annotationList, getNamedQueries( tree, defaults ) );
				addIfNotNull( annotationList, getNamedNativeQueries( tree, defaults ) );
				addIfNotNull( annotationList, getSqlResultSetMappings( tree, defaults ) );
				addIfNotNull( annotationList, getExcludeDefaultListeners( tree, defaults ) );
				addIfNotNull( annotationList, getExcludeSuperclassListeners( tree, defaults ) );
				addIfNotNull( annotationList, getAccessType( tree, defaults ) );
				addIfNotNull( annotationList, getAttributeOverrides( tree, defaults, true ) );
				addIfNotNull( annotationList, getAssociationOverrides( tree, defaults, true ) );
				addIfNotNull( annotationList, getEntityListeners( tree, defaults ) );
				this.annotations = annotationList.toArray( new Annotation[annotationList.size()] );
				for ( Annotation ann : this.annotations ) {
					annotationsMap.put( ann.annotationType(), ann );
				}
				checkForOrphanProperties( tree );
			}
			else if ( className != null ) { //&& propertyName != null ) { //always true but less confusing
				Element tree = xmlContext.getXMLTree( className );
				Annotation[] annotations = getJavaAnnotations();
				List<Annotation> annotationList = new ArrayList<Annotation>( annotations.length + 5 );
				annotationsMap = new HashMap<Class, Annotation>( annotations.length + 5 );
				for ( Annotation annotation : annotations ) {
					if ( !annotationToXml.containsKey( annotation.annotationType() ) ) {
						//unknown annotations are left over
						annotationList.add( annotation );
					}
				}
				preCalculateElementsForProperty( tree );
				Transient transientAnn = getTransient( defaults );
				if ( transientAnn != null ) {
					annotationList.add( transientAnn );
				}
				else {
					if ( defaults.canUseJavaAnnotations() ) {
						Annotation annotation = getJavaAnnotation( Access.class );
						addIfNotNull( annotationList, annotation );
					}
					getId( annotationList, defaults );
					getEmbeddedId( annotationList, defaults );
					getEmbedded( annotationList, defaults );
					getBasic( annotationList, defaults );
					getVersion( annotationList, defaults );
					getAssociation( ManyToOne.class, annotationList, defaults );
					getAssociation( OneToOne.class, annotationList, defaults );
					getAssociation( OneToMany.class, annotationList, defaults );
					getAssociation( ManyToMany.class, annotationList, defaults );
					getElementCollection( annotationList, defaults );
					addIfNotNull( annotationList, getSequenceGenerator( elementsForProperty, defaults ) );
					addIfNotNull( annotationList, getTableGenerator( elementsForProperty, defaults ) );
				}
				processEventAnnotations( annotationList, defaults );
				//FIXME use annotationsMap rather than annotationList this will be faster since the annotation type is usually known at put() time
				this.annotations = annotationList.toArray( new Annotation[annotationList.size()] );
				for ( Annotation ann : this.annotations ) {
					annotationsMap.put( ann.annotationType(), ann );
				}
			}
			else {
				this.annotations = getJavaAnnotations();
				annotationsMap = new HashMap<Class, Annotation>( annotations.length + 5 );
				for ( Annotation ann : this.annotations ) {
					annotationsMap.put( ann.annotationType(), ann );
				}
			}
		}
	}

	private void checkForOrphanProperties(Element tree) {
		Class clazz;
		try {
			clazz = ReflectHelper.classForName( className, this.getClass() );
		}
		catch ( ClassNotFoundException e ) {
			return; //a primitive type most likely
		}
		Element element = tree != null ? tree.element( "attributes" ) : null;
		//put entity.attributes elements
		if ( element != null ) {
			//precompute the list of properties
			//TODO is it really useful...
			Set<String> properties = new HashSet<String>();
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
			for ( Element subelement : (List<Element>) element.elements() ) {
				String propertyName = subelement.attributeValue( "name" );
				if ( !properties.contains( propertyName ) ) {
					LOG.propertyNotFound( StringHelper.qualify( className, propertyName ) );
				}
			}
		}
	}

	/**
	 * Adds {@code annotation} to the list (only if it's not null) and then returns it.
	 *
	 * @param annotationList The list of annotations.
	 * @param annotation The annotation to add to the list.
	 *
	 * @return The annotation which was added to the list or {@code null}.
	 */
	private Annotation addIfNotNull(List<Annotation> annotationList, Annotation annotation) {
		if ( annotation != null ) {
			annotationList.add( annotation );
		}
		return annotation;
	}

	//TODO mutualize the next 2 methods
	private Annotation getTableGenerator(List<Element> elementsForProperty, XMLContext.Default defaults) {
		for ( Element element : elementsForProperty ) {
			Element subelement = element != null ? element.element( annotationToXml.get( TableGenerator.class ) ) : null;
			if ( subelement != null ) {
				return buildTableGeneratorAnnotation( subelement, defaults );
			}
		}
		if ( elementsForProperty.size() == 0 && defaults.canUseJavaAnnotations() ) {
			return getJavaAnnotation( TableGenerator.class );
		}
		else {
			return null;
		}
	}

	private Annotation getSequenceGenerator(List<Element> elementsForProperty, XMLContext.Default defaults) {
		for ( Element element : elementsForProperty ) {
			Element subelement = element != null ? element.element( annotationToXml.get( SequenceGenerator.class ) ) : null;
			if ( subelement != null ) {
				return buildSequenceGeneratorAnnotation( subelement );
			}
		}
		if ( elementsForProperty.size() == 0 && defaults.canUseJavaAnnotations() ) {
			return getJavaAnnotation( SequenceGenerator.class );
		}
		else {
			return null;
		}
	}

	private void processEventAnnotations(List<Annotation> annotationList, XMLContext.Default defaults) {
		boolean eventElement = false;
		for ( Element element : elementsForProperty ) {
			String elementName = element.getName();
			if ( "pre-persist".equals( elementName ) ) {
				AnnotationDescriptor ad = new AnnotationDescriptor( PrePersist.class );
				annotationList.add( AnnotationFactory.create( ad ) );
				eventElement = true;
			}
			else if ( "pre-remove".equals( elementName ) ) {
				AnnotationDescriptor ad = new AnnotationDescriptor( PreRemove.class );
				annotationList.add( AnnotationFactory.create( ad ) );
				eventElement = true;
			}
			else if ( "pre-update".equals( elementName ) ) {
				AnnotationDescriptor ad = new AnnotationDescriptor( PreUpdate.class );
				annotationList.add( AnnotationFactory.create( ad ) );
				eventElement = true;
			}
			else if ( "post-persist".equals( elementName ) ) {
				AnnotationDescriptor ad = new AnnotationDescriptor( PostPersist.class );
				annotationList.add( AnnotationFactory.create( ad ) );
				eventElement = true;
			}
			else if ( "post-remove".equals( elementName ) ) {
				AnnotationDescriptor ad = new AnnotationDescriptor( PostRemove.class );
				annotationList.add( AnnotationFactory.create( ad ) );
				eventElement = true;
			}
			else if ( "post-update".equals( elementName ) ) {
				AnnotationDescriptor ad = new AnnotationDescriptor( PostUpdate.class );
				annotationList.add( AnnotationFactory.create( ad ) );
				eventElement = true;
			}
			else if ( "post-load".equals( elementName ) ) {
				AnnotationDescriptor ad = new AnnotationDescriptor( PostLoad.class );
				annotationList.add( AnnotationFactory.create( ad ) );
				eventElement = true;
			}
		}
		if ( !eventElement && defaults.canUseJavaAnnotations() ) {
			Annotation ann = getJavaAnnotation( PrePersist.class );
			addIfNotNull( annotationList, ann );
			ann = getJavaAnnotation( PreRemove.class );
			addIfNotNull( annotationList, ann );
			ann = getJavaAnnotation( PreUpdate.class );
			addIfNotNull( annotationList, ann );
			ann = getJavaAnnotation( PostPersist.class );
			addIfNotNull( annotationList, ann );
			ann = getJavaAnnotation( PostRemove.class );
			addIfNotNull( annotationList, ann );
			ann = getJavaAnnotation( PostUpdate.class );
			addIfNotNull( annotationList, ann );
			ann = getJavaAnnotation( PostLoad.class );
			addIfNotNull( annotationList, ann );
		}
	}

	private EntityListeners getEntityListeners(Element tree, XMLContext.Default defaults) {
		Element element = tree != null ? tree.element( "entity-listeners" ) : null;
		if ( element != null ) {
			List<Class> entityListenerClasses = new ArrayList<Class>();
			for ( Element subelement : (List<Element>) element.elements( "entity-listener" ) ) {
				String className = subelement.attributeValue( "class" );
				try {
					entityListenerClasses.add(
							ReflectHelper.classForName(
									XMLContext.buildSafeClassName( className, defaults ),
									this.getClass()
							)
					);
				}
				catch ( ClassNotFoundException e ) {
					throw new AnnotationException(
							"Unable to find " + element.getPath() + ".class: " + className, e
					);
				}
			}
			AnnotationDescriptor ad = new AnnotationDescriptor( EntityListeners.class );
			ad.setValue( "value", entityListenerClasses.toArray( new Class[entityListenerClasses.size()] ) );
			return AnnotationFactory.create( ad );
		}
		else if ( defaults.canUseJavaAnnotations() ) {
			return getJavaAnnotation( EntityListeners.class );
		}
		else {
			return null;
		}
	}

	private JoinTable overridesDefaultsInJoinTable(Annotation annotation, XMLContext.Default defaults) {
		//no element but might have some default or some annotation
		boolean defaultToJoinTable = !( isJavaAnnotationPresent( JoinColumn.class )
				|| isJavaAnnotationPresent( JoinColumns.class ) );
		final Class<? extends Annotation> annotationClass = annotation.annotationType();
		defaultToJoinTable = defaultToJoinTable &&
				( ( annotationClass == ManyToMany.class && StringHelper.isEmpty( ( (ManyToMany) annotation ).mappedBy() ) )
						|| ( annotationClass == OneToMany.class && StringHelper.isEmpty( ( (OneToMany) annotation ).mappedBy() ) )
						|| ( annotationClass == ElementCollection.class )
				);
		final Class<JoinTable> annotationType = JoinTable.class;
		if ( defaultToJoinTable
				&& ( StringHelper.isNotEmpty( defaults.getCatalog() )
				|| StringHelper.isNotEmpty( defaults.getSchema() ) ) ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( annotationType );
			if ( defaults.canUseJavaAnnotations() ) {
				JoinTable table = getJavaAnnotation( annotationType );
				if ( table != null ) {
					ad.setValue( "name", table.name() );
					ad.setValue( "schema", table.schema() );
					ad.setValue( "catalog", table.catalog() );
					ad.setValue( "uniqueConstraints", table.uniqueConstraints() );
					ad.setValue( "joinColumns", table.joinColumns() );
					ad.setValue( "inverseJoinColumns", table.inverseJoinColumns() );
				}
			}
			if ( StringHelper.isEmpty( (String) ad.valueOf( "schema" ) )
					&& StringHelper.isNotEmpty( defaults.getSchema() ) ) {
				ad.setValue( "schema", defaults.getSchema() );
			}
			if ( StringHelper.isEmpty( (String) ad.valueOf( "catalog" ) )
					&& StringHelper.isNotEmpty( defaults.getCatalog() ) ) {
				ad.setValue( "catalog", defaults.getCatalog() );
			}
			return AnnotationFactory.create( ad );
		}
		else if ( defaults.canUseJavaAnnotations() ) {
			return getJavaAnnotation( annotationType );
		}
		else {
			return null;
		}
	}

	private void getJoinTable(List<Annotation> annotationList, Element tree, XMLContext.Default defaults) {
		addIfNotNull( annotationList, buildJoinTable( tree, defaults ) );
	}

	/*
	 * no partial overriding possible
	 */
	private JoinTable buildJoinTable(Element tree, XMLContext.Default defaults) {
		Element subelement = tree == null ? null : tree.element( "join-table" );
		final Class<JoinTable> annotationType = JoinTable.class;
		if ( subelement == null ) {
			return null;
		}
		//ignore java annotation, an element is defined
		AnnotationDescriptor annotation = new AnnotationDescriptor( annotationType );
		copyStringAttribute( annotation, subelement, "name", false );
		copyStringAttribute( annotation, subelement, "catalog", false );
		if ( StringHelper.isNotEmpty( defaults.getCatalog() )
				&& StringHelper.isEmpty( (String) annotation.valueOf( "catalog" ) ) ) {
			annotation.setValue( "catalog", defaults.getCatalog() );
		}
		copyStringAttribute( annotation, subelement, "schema", false );
		if ( StringHelper.isNotEmpty( defaults.getSchema() )
				&& StringHelper.isEmpty( (String) annotation.valueOf( "schema" ) ) ) {
			annotation.setValue( "schema", defaults.getSchema() );
		}
		buildUniqueConstraints( annotation, subelement );
		annotation.setValue( "joinColumns", getJoinColumns( subelement, false ) );
		annotation.setValue( "inverseJoinColumns", getJoinColumns( subelement, true ) );
		return AnnotationFactory.create( annotation );
	}

	/**
	 * As per section 12.2 of the JPA 2.0 specification, the association
	 * subelements (many-to-one, one-to-many, one-to-one, many-to-many,
	 * element-collection) completely override the mapping for the specified
	 * field or property.  Thus, any methods which might in some contexts merge
	 * with annotations must not do so in this context.
	 *
	 * @see #getElementCollection(List, org.hibernate.cfg.annotations.reflection.XMLContext.Default)
	 */
	private void getAssociation(
			Class<? extends Annotation> annotationType, List<Annotation> annotationList, XMLContext.Default defaults
	) {
		String xmlName = annotationToXml.get( annotationType );
		for ( Element element : elementsForProperty ) {
			if ( xmlName.equals( element.getName() ) ) {
				AnnotationDescriptor ad = new AnnotationDescriptor( annotationType );
				addTargetClass( element, ad, "target-entity", defaults );
				getFetchType( ad, element );
				getCascades( ad, element, defaults );
				getJoinTable( annotationList, element, defaults );
				buildJoinColumns( annotationList, element );
				Annotation annotation = getPrimaryKeyJoinColumns( element, defaults, false );
				addIfNotNull( annotationList, annotation );
				copyBooleanAttribute( ad, element, "optional" );
				copyBooleanAttribute( ad, element, "orphan-removal" );
				copyStringAttribute( ad, element, "mapped-by", false );
				getOrderBy( annotationList, element );
				getMapKey( annotationList, element );
				getMapKeyClass( annotationList, element, defaults );
				getMapKeyColumn( annotationList, element );
				getOrderColumn( annotationList, element );
				getMapKeyTemporal( annotationList, element );
				getMapKeyEnumerated( annotationList, element );
				annotation = getMapKeyAttributeOverrides( element, defaults );
				addIfNotNull( annotationList, annotation );
				buildMapKeyJoinColumns( annotationList, element );
				getAssociationId( annotationList, element );
				getMapsId( annotationList, element );
				annotationList.add( AnnotationFactory.create( ad ) );
				getAccessType( annotationList, element );
			}
		}
		if ( elementsForProperty.size() == 0 && defaults.canUseJavaAnnotations() ) {
			Annotation annotation = getJavaAnnotation( annotationType );
			if ( annotation != null ) {
				annotationList.add( annotation );
				annotation = overridesDefaultsInJoinTable( annotation, defaults );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( JoinColumn.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( JoinColumns.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( PrimaryKeyJoinColumn.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( PrimaryKeyJoinColumns.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( MapKey.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( OrderBy.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AttributeOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AttributeOverrides.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AssociationOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AssociationOverrides.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( Lob.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( Enumerated.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( Temporal.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( Column.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( Columns.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( MapKeyClass.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( MapKeyTemporal.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( MapKeyEnumerated.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( MapKeyColumn.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( MapKeyJoinColumn.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( MapKeyJoinColumns.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( OrderColumn.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( Cascade.class );
				addIfNotNull( annotationList, annotation );
			}
			else if ( isJavaAnnotationPresent( ElementCollection.class ) ) { //JPA2
				annotation = overridesDefaultsInJoinTable( getJavaAnnotation( ElementCollection.class ), defaults );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( MapKey.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( OrderBy.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AttributeOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AttributeOverrides.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AssociationOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AssociationOverrides.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( Lob.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( Enumerated.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( Temporal.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( Column.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( OrderColumn.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( MapKeyClass.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( MapKeyTemporal.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( MapKeyEnumerated.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( MapKeyColumn.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( MapKeyJoinColumn.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( MapKeyJoinColumns.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( CollectionTable.class );
				addIfNotNull( annotationList, annotation );
			}
		}
	}

	private void buildMapKeyJoinColumns(List<Annotation> annotationList, Element element) {
		MapKeyJoinColumn[] joinColumns = getMapKeyJoinColumns( element );
		if ( joinColumns.length > 0 ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( MapKeyJoinColumns.class );
			ad.setValue( "value", joinColumns );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private MapKeyJoinColumn[] getMapKeyJoinColumns(Element element) {
		List<Element> subelements = element != null ? element.elements( "map-key-join-column" ) : null;
		List<MapKeyJoinColumn> joinColumns = new ArrayList<MapKeyJoinColumn>();
		if ( subelements != null ) {
			for ( Element subelement : subelements ) {
				AnnotationDescriptor column = new AnnotationDescriptor( MapKeyJoinColumn.class );
				copyStringAttribute( column, subelement, "name", false );
				copyStringAttribute( column, subelement, "referenced-column-name", false );
				copyBooleanAttribute( column, subelement, "unique" );
				copyBooleanAttribute( column, subelement, "nullable" );
				copyBooleanAttribute( column, subelement, "insertable" );
				copyBooleanAttribute( column, subelement, "updatable" );
				copyStringAttribute( column, subelement, "column-definition", false );
				copyStringAttribute( column, subelement, "table", false );
				joinColumns.add( (MapKeyJoinColumn) AnnotationFactory.create( column ) );
			}
		}
		return joinColumns.toArray( new MapKeyJoinColumn[joinColumns.size()] );
	}

	private AttributeOverrides getMapKeyAttributeOverrides(Element tree, XMLContext.Default defaults) {
		List<AttributeOverride> attributes = buildAttributeOverrides( tree, "map-key-attribute-override" );
		return mergeAttributeOverrides( defaults, attributes, false );
	}

	private Cacheable getCacheable(Element element, XMLContext.Default defaults){
		if(element==null)return null;
		String attValue = element.attributeValue( "cacheable" );
		if(attValue!=null){
			AnnotationDescriptor ad = new AnnotationDescriptor( Cacheable.class );
			ad.setValue( "value", Boolean.valueOf( attValue ) );
			return AnnotationFactory.create( ad );
		}
		if ( defaults.canUseJavaAnnotations() ) {
			return getJavaAnnotation( Cacheable.class );
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
	private void getMapKeyEnumerated(List<Annotation> annotationList, Element element) {
		Element subelement = element != null ? element.element( "map-key-enumerated" ) : null;
		if ( subelement != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( MapKeyEnumerated.class );
			EnumType value = EnumType.valueOf( subelement.getTextTrim() );
			ad.setValue( "value", value );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	/**
	 * Adds a @MapKeyTemporal annotation to the specified annotationList if the specified element
	 * contains a map-key-temporal sub-element. This should only be the case for element-collection,
	 * many-to-many, or one-to-many associations.
	 */
	private void getMapKeyTemporal(List<Annotation> annotationList, Element element) {
		Element subelement = element != null ? element.element( "map-key-temporal" ) : null;
		if ( subelement != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( MapKeyTemporal.class );
			TemporalType value = TemporalType.valueOf( subelement.getTextTrim() );
			ad.setValue( "value", value );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	/**
	 * Adds an @OrderColumn annotation to the specified annotationList if the specified element
	 * contains an order-column sub-element. This should only be the case for element-collection,
	 * many-to-many, or one-to-many associations.
	 */
	private void getOrderColumn(List<Annotation> annotationList, Element element) {
		Element subelement = element != null ? element.element( "order-column" ) : null;
		if ( subelement != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( OrderColumn.class );
			copyStringAttribute( ad, subelement, "name", false );
			copyBooleanAttribute( ad, subelement, "nullable" );
			copyBooleanAttribute( ad, subelement, "insertable" );
			copyBooleanAttribute( ad, subelement, "updatable" );
			copyStringAttribute( ad, subelement, "column-definition", false );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	/**
	 * Adds a @MapsId annotation to the specified annotationList if the specified element has the
	 * maps-id attribute set. This should only be the case for many-to-one or one-to-one
	 * associations.
	 */
	private void getMapsId(List<Annotation> annotationList, Element element) {
		String attrVal = element.attributeValue( "maps-id" );
		if ( attrVal != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( MapsId.class );
			ad.setValue( "value", attrVal );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	/**
	 * Adds an @Id annotation to the specified annotationList if the specified element has the id
	 * attribute set to true. This should only be the case for many-to-one or one-to-one
	 * associations.
	 */
	private void getAssociationId(List<Annotation> annotationList, Element element) {
		String attrVal = element.attributeValue( "id" );
		if ( "true".equals( attrVal ) ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( Id.class );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void addTargetClass(Element element, AnnotationDescriptor ad, String nodeName, XMLContext.Default defaults) {
		String className = element.attributeValue( nodeName );
		if ( className != null ) {
			Class clazz;
			try {
				clazz = ReflectHelper.classForName(
						XMLContext.buildSafeClassName( className, defaults ), this.getClass()
				);
			}
			catch ( ClassNotFoundException e ) {
				throw new AnnotationException(
						"Unable to find " + element.getPath() + " " + nodeName + ": " + className, e
				);
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
		for ( Element element : elementsForProperty ) {
			if ( "element-collection".equals( element.getName() ) ) {
				AnnotationDescriptor ad = new AnnotationDescriptor( ElementCollection.class );
				addTargetClass( element, ad, "target-class", defaults );
				getFetchType( ad, element );
				getOrderBy( annotationList, element );
				getOrderColumn( annotationList, element );
				getMapKey( annotationList, element );
				getMapKeyClass( annotationList, element, defaults );
				getMapKeyTemporal( annotationList, element );
				getMapKeyEnumerated( annotationList, element );
				getMapKeyColumn( annotationList, element );
				buildMapKeyJoinColumns( annotationList, element );
				Annotation annotation = getColumn( element.element( "column" ), false, element );
				addIfNotNull( annotationList, annotation );
				getTemporal( annotationList, element );
				getEnumerated( annotationList, element );
				getLob( annotationList, element );
				//Both map-key-attribute-overrides and attribute-overrides
				//translate into AttributeOverride annotations, which need
				//need to be wrapped in the same AttributeOverrides annotation.
				List<AttributeOverride> attributes = new ArrayList<AttributeOverride>();
				attributes.addAll( buildAttributeOverrides( element, "map-key-attribute-override" ) );
				attributes.addAll( buildAttributeOverrides( element, "attribute-override" ) );
				annotation = mergeAttributeOverrides( defaults, attributes, false );
				addIfNotNull( annotationList, annotation );
				annotation = getAssociationOverrides( element, defaults, false );
				addIfNotNull( annotationList, annotation );
				getCollectionTable( annotationList, element, defaults );
				annotationList.add( AnnotationFactory.create( ad ) );
				getAccessType( annotationList, element );
			}
		}
	}

	private void getOrderBy(List<Annotation> annotationList, Element element) {
		Element subelement = element != null ? element.element( "order-by" ) : null;
		if ( subelement != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( OrderBy.class );
			copyStringElement( subelement, ad, "value" );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getMapKey(List<Annotation> annotationList, Element element) {
		Element subelement = element != null ? element.element( "map-key" ) : null;
		if ( subelement != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( MapKey.class );
			copyStringAttribute( ad, subelement, "name", false );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getMapKeyColumn(List<Annotation> annotationList, Element element) {
		Element subelement = element != null ? element.element( "map-key-column" ) : null;
		if ( subelement != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( MapKeyColumn.class );
			copyStringAttribute( ad, subelement, "name", false );
			copyBooleanAttribute( ad, subelement, "unique" );
			copyBooleanAttribute( ad, subelement, "nullable" );
			copyBooleanAttribute( ad, subelement, "insertable" );
			copyBooleanAttribute( ad, subelement, "updatable" );
			copyStringAttribute( ad, subelement, "column-definition", false );
			copyStringAttribute( ad, subelement, "table", false );
			copyIntegerAttribute( ad, subelement, "length" );
			copyIntegerAttribute( ad, subelement, "precision" );
			copyIntegerAttribute( ad, subelement, "scale" );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getMapKeyClass(List<Annotation> annotationList, Element element, XMLContext.Default defaults) {
		String nodeName = "map-key-class";
		Element subelement = element != null ? element.element( nodeName ) : null;
		if ( subelement != null ) {
			String mapKeyClassName = subelement.attributeValue( "class" );
			AnnotationDescriptor ad = new AnnotationDescriptor( MapKeyClass.class );
			if ( StringHelper.isNotEmpty( mapKeyClassName ) ) {
				Class clazz;
				try {
					clazz = ReflectHelper.classForName(
							XMLContext.buildSafeClassName( mapKeyClassName, defaults ),
							this.getClass()
					);
				}
				catch ( ClassNotFoundException e ) {
					throw new AnnotationException(
							"Unable to find " + element.getPath() + " " + nodeName + ": " + mapKeyClassName, e
					);
				}
				ad.setValue( "value", clazz );
			}
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getCollectionTable(List<Annotation> annotationList, Element element, XMLContext.Default defaults) {
		Element subelement = element != null ? element.element( "collection-table" ) : null;
		if ( subelement != null ) {
			AnnotationDescriptor annotation = new AnnotationDescriptor( CollectionTable.class );
			copyStringAttribute( annotation, subelement, "name", false );
			copyStringAttribute( annotation, subelement, "catalog", false );
			if ( StringHelper.isNotEmpty( defaults.getCatalog() )
					&& StringHelper.isEmpty( (String) annotation.valueOf( "catalog" ) ) ) {
				annotation.setValue( "catalog", defaults.getCatalog() );
			}
			copyStringAttribute( annotation, subelement, "schema", false );
			if ( StringHelper.isNotEmpty( defaults.getSchema() )
					&& StringHelper.isEmpty( (String) annotation.valueOf( "schema" ) ) ) {
				annotation.setValue( "schema", defaults.getSchema() );
			}
			JoinColumn[] joinColumns = getJoinColumns( subelement, false );
			if ( joinColumns.length > 0 ) {
				annotation.setValue( "joinColumns", joinColumns );
			}
			buildUniqueConstraints( annotation, subelement );
			annotationList.add( AnnotationFactory.create( annotation ) );
		}
	}

	private void buildJoinColumns(List<Annotation> annotationList, Element element) {
		JoinColumn[] joinColumns = getJoinColumns( element, false );
		if ( joinColumns.length > 0 ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( JoinColumns.class );
			ad.setValue( "value", joinColumns );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getCascades(AnnotationDescriptor ad, Element element, XMLContext.Default defaults) {
		List<Element> elements = element != null ? element.elements( "cascade" ) : new ArrayList<Element>( 0 );
		List<CascadeType> cascades = new ArrayList<CascadeType>();
		for ( Element subelement : elements ) {
			if ( subelement.element( "cascade-all" ) != null ) {
				cascades.add( CascadeType.ALL );
			}
			if ( subelement.element( "cascade-persist" ) != null ) {
				cascades.add( CascadeType.PERSIST );
			}
			if ( subelement.element( "cascade-merge" ) != null ) {
				cascades.add( CascadeType.MERGE );
			}
			if ( subelement.element( "cascade-remove" ) != null ) {
				cascades.add( CascadeType.REMOVE );
			}
			if ( subelement.element( "cascade-refresh" ) != null ) {
				cascades.add( CascadeType.REFRESH );
			}
			if ( subelement.element( "cascade-detach" ) != null ) {
				cascades.add( CascadeType.DETACH );
			}
		}
		if ( Boolean.TRUE.equals( defaults.getCascadePersist() )
				&& !cascades.contains( CascadeType.ALL ) && !cascades.contains( CascadeType.PERSIST ) ) {
			cascades.add( CascadeType.PERSIST );
		}
		if ( cascades.size() > 0 ) {
			ad.setValue( "cascade", cascades.toArray( new CascadeType[cascades.size()] ) );
		}
	}

	private void getEmbedded(List<Annotation> annotationList, XMLContext.Default defaults) {
		for ( Element element : elementsForProperty ) {
			if ( "embedded".equals( element.getName() ) ) {
				AnnotationDescriptor ad = new AnnotationDescriptor( Embedded.class );
				annotationList.add( AnnotationFactory.create( ad ) );
				Annotation annotation = getAttributeOverrides( element, defaults, false );
				addIfNotNull( annotationList, annotation );
				annotation = getAssociationOverrides( element, defaults, false );
				addIfNotNull( annotationList, annotation );
				getAccessType( annotationList, element );
			}
		}
		if ( elementsForProperty.size() == 0 && defaults.canUseJavaAnnotations() ) {
			Annotation annotation = getJavaAnnotation( Embedded.class );
			if ( annotation != null ) {
				annotationList.add( annotation );
				annotation = getJavaAnnotation( AttributeOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AttributeOverrides.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AssociationOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AssociationOverrides.class );
				addIfNotNull( annotationList, annotation );
			}
		}
	}

	private Transient getTransient(XMLContext.Default defaults) {
		for ( Element element : elementsForProperty ) {
			if ( "transient".equals( element.getName() ) ) {
				AnnotationDescriptor ad = new AnnotationDescriptor( Transient.class );
				return AnnotationFactory.create( ad );
			}
		}
		if ( elementsForProperty.size() == 0 && defaults.canUseJavaAnnotations() ) {
			return getJavaAnnotation( Transient.class );
		}
		else {
			return null;
		}
	}

	private void getVersion(List<Annotation> annotationList, XMLContext.Default defaults) {
		for ( Element element : elementsForProperty ) {
			if ( "version".equals( element.getName() ) ) {
				Annotation annotation = buildColumns( element );
				addIfNotNull( annotationList, annotation );
				getTemporal( annotationList, element );
				AnnotationDescriptor basic = new AnnotationDescriptor( Version.class );
				annotationList.add( AnnotationFactory.create( basic ) );
				getAccessType( annotationList, element );
			}
		}
		if ( elementsForProperty.size() == 0 && defaults.canUseJavaAnnotations() ) {
			//we have nothing, so Java annotations might occurs
			Annotation annotation = getJavaAnnotation( Version.class );
			if ( annotation != null ) {
				annotationList.add( annotation );
				annotation = getJavaAnnotation( Column.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( Columns.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( Temporal.class );
				addIfNotNull( annotationList, annotation );
			}
		}
	}

	private void getBasic(List<Annotation> annotationList, XMLContext.Default defaults) {
		for ( Element element : elementsForProperty ) {
			if ( "basic".equals( element.getName() ) ) {
				Annotation annotation = buildColumns( element );
				addIfNotNull( annotationList, annotation );
				getAccessType( annotationList, element );
				getTemporal( annotationList, element );
				getLob( annotationList, element );
				getEnumerated( annotationList, element );
				AnnotationDescriptor basic = new AnnotationDescriptor( Basic.class );
				getFetchType( basic, element );
				copyBooleanAttribute( basic, element, "optional" );
				annotationList.add( AnnotationFactory.create( basic ) );
			}
		}
		if ( elementsForProperty.size() == 0 && defaults.canUseJavaAnnotations() ) {
			//no annotation presence constraint, basic is the default
			Annotation annotation = getJavaAnnotation( Basic.class );
			addIfNotNull( annotationList, annotation );
			annotation = getJavaAnnotation( Lob.class );
			addIfNotNull( annotationList, annotation );
			annotation = getJavaAnnotation( Enumerated.class );
			addIfNotNull( annotationList, annotation );
			annotation = getJavaAnnotation( Temporal.class );
			addIfNotNull( annotationList, annotation );
			annotation = getJavaAnnotation( Column.class );
			addIfNotNull( annotationList, annotation );
			annotation = getJavaAnnotation( Columns.class );
			addIfNotNull( annotationList, annotation );
			annotation = getJavaAnnotation( AttributeOverride.class );
			addIfNotNull( annotationList, annotation );
			annotation = getJavaAnnotation( AttributeOverrides.class );
			addIfNotNull( annotationList, annotation );
			annotation = getJavaAnnotation( AssociationOverride.class );
			addIfNotNull( annotationList, annotation );
			annotation = getJavaAnnotation( AssociationOverrides.class );
			addIfNotNull( annotationList, annotation );
		}
	}

	private void getEnumerated(List<Annotation> annotationList, Element element) {
		Element subElement = element != null ? element.element( "enumerated" ) : null;
		if ( subElement != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( Enumerated.class );
			String enumerated = subElement.getTextTrim();
			if ( "ORDINAL".equalsIgnoreCase( enumerated ) ) {
				ad.setValue( "value", EnumType.ORDINAL );
			}
			else if ( "STRING".equalsIgnoreCase( enumerated ) ) {
				ad.setValue( "value", EnumType.STRING );
			}
			else if ( StringHelper.isNotEmpty( enumerated ) ) {
				throw new AnnotationException( "Unknown EnumType: " + enumerated + ". " + SCHEMA_VALIDATION );
			}
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getLob(List<Annotation> annotationList, Element element) {
		Element subElement = element != null ? element.element( "lob" ) : null;
		if ( subElement != null ) {
			annotationList.add( AnnotationFactory.create( new AnnotationDescriptor( Lob.class ) ) );
		}
	}

	private void getFetchType(AnnotationDescriptor descriptor, Element element) {
		String fetchString = element != null ? element.attributeValue( "fetch" ) : null;
		if ( fetchString != null ) {
			if ( "eager".equalsIgnoreCase( fetchString ) ) {
				descriptor.setValue( "fetch", FetchType.EAGER );
			}
			else if ( "lazy".equalsIgnoreCase( fetchString ) ) {
				descriptor.setValue( "fetch", FetchType.LAZY );
			}
		}
	}

	private void getEmbeddedId(List<Annotation> annotationList, XMLContext.Default defaults) {
		for ( Element element : elementsForProperty ) {
			if ( "embedded-id".equals( element.getName() ) ) {
				if ( isProcessingId( defaults ) ) {
					Annotation annotation = getAttributeOverrides( element, defaults, false );
					addIfNotNull( annotationList, annotation );
					annotation = getAssociationOverrides( element, defaults, false );
					addIfNotNull( annotationList, annotation );
					AnnotationDescriptor ad = new AnnotationDescriptor( EmbeddedId.class );
					annotationList.add( AnnotationFactory.create( ad ) );
					getAccessType( annotationList, element );
				}
			}
		}
		if ( elementsForProperty.size() == 0 && defaults.canUseJavaAnnotations() ) {
			Annotation annotation = getJavaAnnotation( EmbeddedId.class );
			if ( annotation != null ) {
				annotationList.add( annotation );
				annotation = getJavaAnnotation( Column.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( Columns.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( GeneratedValue.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( Temporal.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( TableGenerator.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( SequenceGenerator.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AttributeOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AttributeOverrides.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AssociationOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AssociationOverrides.class );
				addIfNotNull( annotationList, annotation );
			}
		}
	}

	private void preCalculateElementsForProperty(Element tree) {
		elementsForProperty = new ArrayList<Element>();
		Element element = tree != null ? tree.element( "attributes" ) : null;
		//put entity.attributes elements
		if ( element != null ) {
			for ( Element subelement : (List<Element>) element.elements() ) {
				if ( propertyName.equals( subelement.attributeValue( "name" ) ) ) {
					elementsForProperty.add( subelement );
				}
			}
		}
		//add pre-* etc from entity and pure entity listener classes
		if ( tree != null ) {
			for ( Element subelement : (List<Element>) tree.elements() ) {
				if ( propertyName.equals( subelement.attributeValue( "method-name" ) ) ) {
					elementsForProperty.add( subelement );
				}
			}
		}
	}

	private void getId(List<Annotation> annotationList, XMLContext.Default defaults) {
		for ( Element element : elementsForProperty ) {
			if ( "id".equals( element.getName() ) ) {
				boolean processId = isProcessingId( defaults );
				if ( processId ) {
					Annotation annotation = buildColumns( element );
					addIfNotNull( annotationList, annotation );
					annotation = buildGeneratedValue( element );
					addIfNotNull( annotationList, annotation );
					getTemporal( annotationList, element );
					//FIXME: fix the priority of xml over java for generator names
					annotation = getTableGenerator( element, defaults );
					addIfNotNull( annotationList, annotation );
					annotation = getSequenceGenerator( element, defaults );
					addIfNotNull( annotationList, annotation );
					AnnotationDescriptor id = new AnnotationDescriptor( Id.class );
					annotationList.add( AnnotationFactory.create( id ) );
					getAccessType( annotationList, element );
				}
			}
		}
		if ( elementsForProperty.size() == 0 && defaults.canUseJavaAnnotations() ) {
			Annotation annotation = getJavaAnnotation( Id.class );
			if ( annotation != null ) {
				annotationList.add( annotation );
				annotation = getJavaAnnotation( Column.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( Columns.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( GeneratedValue.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( Temporal.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( TableGenerator.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( SequenceGenerator.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AttributeOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AttributeOverrides.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AssociationOverride.class );
				addIfNotNull( annotationList, annotation );
				annotation = getJavaAnnotation( AssociationOverrides.class );
				addIfNotNull( annotationList, annotation );
			}
		}
	}

	private boolean isProcessingId(XMLContext.Default defaults) {
		boolean isExplicit = defaults.getAccess() != null;
		boolean correctAccess =
				( PropertyType.PROPERTY.equals( propertyType ) && AccessType.PROPERTY.equals( defaults.getAccess() ) )
						|| ( PropertyType.FIELD.equals( propertyType ) && AccessType.FIELD
						.equals( defaults.getAccess() ) );
		boolean hasId = defaults.canUseJavaAnnotations()
				&& ( isJavaAnnotationPresent( Id.class ) || isJavaAnnotationPresent( EmbeddedId.class ) );
		//if ( properAccessOnMetadataComplete || properOverridingOnMetadataNonComplete ) {
		boolean mirrorAttributeIsId = defaults.canUseJavaAnnotations() &&
				( mirroredAttribute != null &&
						( mirroredAttribute.isAnnotationPresent( Id.class )
								|| mirroredAttribute.isAnnotationPresent( EmbeddedId.class ) ) );
		boolean propertyIsDefault = PropertyType.PROPERTY.equals( propertyType )
				&& !mirrorAttributeIsId;
		return correctAccess || ( !isExplicit && hasId ) || ( !isExplicit && propertyIsDefault );
	}

	private Columns buildColumns(Element element) {
		List<Element> subelements = element.elements( "column" );
		List<Column> columns = new ArrayList<Column>( subelements.size() );
		for ( Element subelement : subelements ) {
			columns.add( getColumn( subelement, false, element ) );
		}
		if ( columns.size() > 0 ) {
			AnnotationDescriptor columnsDescr = new AnnotationDescriptor( Columns.class );
			columnsDescr.setValue( "columns", columns.toArray( new Column[columns.size()] ) );
			return AnnotationFactory.create( columnsDescr );
		}
		else {
			return null;
		}
	}

	private GeneratedValue buildGeneratedValue(Element element) {
		Element subElement = element != null ? element.element( "generated-value" ) : null;
		if ( subElement != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( GeneratedValue.class );
			String strategy = subElement.attributeValue( "strategy" );
			if ( "TABLE".equalsIgnoreCase( strategy ) ) {
				ad.setValue( "strategy", GenerationType.TABLE );
			}
			else if ( "SEQUENCE".equalsIgnoreCase( strategy ) ) {
				ad.setValue( "strategy", GenerationType.SEQUENCE );
			}
			else if ( "IDENTITY".equalsIgnoreCase( strategy ) ) {
				ad.setValue( "strategy", GenerationType.IDENTITY );
			}
			else if ( "AUTO".equalsIgnoreCase( strategy ) ) {
				ad.setValue( "strategy", GenerationType.AUTO );
			}
			else if ( StringHelper.isNotEmpty( strategy ) ) {
				throw new AnnotationException( "Unknown GenerationType: " + strategy + ". " + SCHEMA_VALIDATION );
			}
			copyStringAttribute( ad, subElement, "generator", false );
			return AnnotationFactory.create( ad );
		}
		else {
			return null;
		}
	}

	private void getTemporal(List<Annotation> annotationList, Element element) {
		Element subElement = element != null ? element.element( "temporal" ) : null;
		if ( subElement != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( Temporal.class );
			String temporal = subElement.getTextTrim();
			if ( "DATE".equalsIgnoreCase( temporal ) ) {
				ad.setValue( "value", TemporalType.DATE );
			}
			else if ( "TIME".equalsIgnoreCase( temporal ) ) {
				ad.setValue( "value", TemporalType.TIME );
			}
			else if ( "TIMESTAMP".equalsIgnoreCase( temporal ) ) {
				ad.setValue( "value", TemporalType.TIMESTAMP );
			}
			else if ( StringHelper.isNotEmpty( temporal ) ) {
				throw new AnnotationException( "Unknown TemporalType: " + temporal + ". " + SCHEMA_VALIDATION );
			}
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	private void getAccessType(List<Annotation> annotationList, Element element) {
		if ( element == null ) {
			return;
		}
		String access = element.attributeValue( "access" );
		if ( access != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( Access.class );
			AccessType type;
			try {
				type = AccessType.valueOf( access );
			}
			catch ( IllegalArgumentException e ) {
				throw new AnnotationException( access + " is not a valid access type. Check you xml confguration." );
			}

			if ( ( AccessType.PROPERTY.equals( type ) && this.element instanceof Method ) ||
					( AccessType.FIELD.equals( type ) && this.element instanceof Field ) ) {
				return;
			}

			ad.setValue( "value", type );
			annotationList.add( AnnotationFactory.create( ad ) );
		}
	}

	/**
	 * @param mergeWithAnnotations Whether to use Java annotations for this
	 * element, if present and not disabled by the XMLContext defaults.
	 * In some contexts (such as an element-collection mapping) merging
	 * with annotations is never allowed.
	 */
	private AssociationOverrides getAssociationOverrides(Element tree, XMLContext.Default defaults, boolean mergeWithAnnotations) {
		List<AssociationOverride> attributes = buildAssociationOverrides( tree, defaults );
		if ( mergeWithAnnotations && defaults.canUseJavaAnnotations() ) {
			AssociationOverride annotation = getJavaAnnotation( AssociationOverride.class );
			addAssociationOverrideIfNeeded( annotation, attributes );
			AssociationOverrides annotations = getJavaAnnotation( AssociationOverrides.class );
			if ( annotations != null ) {
				for ( AssociationOverride current : annotations.value() ) {
					addAssociationOverrideIfNeeded( current, attributes );
				}
			}
		}
		if ( attributes.size() > 0 ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( AssociationOverrides.class );
			ad.setValue( "value", attributes.toArray( new AssociationOverride[attributes.size()] ) );
			return AnnotationFactory.create( ad );
		}
		else {
			return null;
		}
	}

	private List<AssociationOverride> buildAssociationOverrides(Element element, XMLContext.Default defaults) {
		List<Element> subelements = element == null ? null : element.elements( "association-override" );
		List<AssociationOverride> overrides = new ArrayList<AssociationOverride>();
		if ( subelements != null && subelements.size() > 0 ) {
			for ( Element current : subelements ) {
				AnnotationDescriptor override = new AnnotationDescriptor( AssociationOverride.class );
				copyStringAttribute( override, current, "name", true );
				override.setValue( "joinColumns", getJoinColumns( current, false ) );
				JoinTable joinTable = buildJoinTable( current, defaults );
				if ( joinTable != null ) {
					override.setValue( "joinTable", joinTable );
				}
				overrides.add( (AssociationOverride) AnnotationFactory.create( override ) );
			}
		}
		return overrides;
	}

	private JoinColumn[] getJoinColumns(Element element, boolean isInverse) {
		List<Element> subelements = element != null ?
				element.elements( isInverse ? "inverse-join-column" : "join-column" ) :
				null;
		List<JoinColumn> joinColumns = new ArrayList<JoinColumn>();
		if ( subelements != null ) {
			for ( Element subelement : subelements ) {
				AnnotationDescriptor column = new AnnotationDescriptor( JoinColumn.class );
				copyStringAttribute( column, subelement, "name", false );
				copyStringAttribute( column, subelement, "referenced-column-name", false );
				copyBooleanAttribute( column, subelement, "unique" );
				copyBooleanAttribute( column, subelement, "nullable" );
				copyBooleanAttribute( column, subelement, "insertable" );
				copyBooleanAttribute( column, subelement, "updatable" );
				copyStringAttribute( column, subelement, "column-definition", false );
				copyStringAttribute( column, subelement, "table", false );
				joinColumns.add( (JoinColumn) AnnotationFactory.create( column ) );
			}
		}
		return joinColumns.toArray( new JoinColumn[joinColumns.size()] );
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

	/**
	 * @param mergeWithAnnotations Whether to use Java annotations for this
	 * element, if present and not disabled by the XMLContext defaults.
	 * In some contexts (such as an association mapping) merging with
	 * annotations is never allowed.
	 */
	private AttributeOverrides getAttributeOverrides(Element tree, XMLContext.Default defaults, boolean mergeWithAnnotations) {
		List<AttributeOverride> attributes = buildAttributeOverrides( tree, "attribute-override" );
		return mergeAttributeOverrides( defaults, attributes, mergeWithAnnotations );
	}

	/**
	 * @param mergeWithAnnotations Whether to use Java annotations for this
	 * element, if present and not disabled by the XMLContext defaults.
	 * In some contexts (such as an association mapping) merging with
	 * annotations is never allowed.
	 */
	private AttributeOverrides mergeAttributeOverrides(XMLContext.Default defaults, List<AttributeOverride> attributes, boolean mergeWithAnnotations) {
		if ( mergeWithAnnotations && defaults.canUseJavaAnnotations() ) {
			AttributeOverride annotation = getJavaAnnotation( AttributeOverride.class );
			addAttributeOverrideIfNeeded( annotation, attributes );
			AttributeOverrides annotations = getJavaAnnotation( AttributeOverrides.class );
			if ( annotations != null ) {
				for ( AttributeOverride current : annotations.value() ) {
					addAttributeOverrideIfNeeded( current, attributes );
				}
			}
		}
		if ( attributes.size() > 0 ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( AttributeOverrides.class );
			ad.setValue( "value", attributes.toArray( new AttributeOverride[attributes.size()] ) );
			return AnnotationFactory.create( ad );
		}
		else {
			return null;
		}
	}

	private List<AttributeOverride> buildAttributeOverrides(Element element, String nodeName) {
		List<Element> subelements = element == null ? null : element.elements( nodeName );
		return buildAttributeOverrides( subelements, nodeName );
	}

	private List<AttributeOverride> buildAttributeOverrides(List<Element> subelements, String nodeName) {
		List<AttributeOverride> overrides = new ArrayList<AttributeOverride>();
		if ( subelements != null && subelements.size() > 0 ) {
			for ( Element current : subelements ) {
				if ( !current.getName().equals( nodeName ) ) {
					continue;
				}
				AnnotationDescriptor override = new AnnotationDescriptor( AttributeOverride.class );
				copyStringAttribute( override, current, "name", true );
				Element column = current.element( "column" );
				override.setValue( "column", getColumn( column, true, current ) );
				overrides.add( (AttributeOverride) AnnotationFactory.create( override ) );
			}
		}
		return overrides;
	}

	private Column getColumn(Element element, boolean isMandatory, Element current) {
		//Element subelement = element != null ? element.element( "column" ) : null;
		if ( element != null ) {
			AnnotationDescriptor column = new AnnotationDescriptor( Column.class );
			copyStringAttribute( column, element, "name", false );
			copyBooleanAttribute( column, element, "unique" );
			copyBooleanAttribute( column, element, "nullable" );
			copyBooleanAttribute( column, element, "insertable" );
			copyBooleanAttribute( column, element, "updatable" );
			copyStringAttribute( column, element, "column-definition", false );
			copyStringAttribute( column, element, "table", false );
			copyIntegerAttribute( column, element, "length" );
			copyIntegerAttribute( column, element, "precision" );
			copyIntegerAttribute( column, element, "scale" );
			return (Column) AnnotationFactory.create( column );
		}
		else {
			if ( isMandatory ) {
				throw new AnnotationException( current.getPath() + ".column is mandatory. " + SCHEMA_VALIDATION );
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

	private Access getAccessType(Element tree, XMLContext.Default defaults) {
		String access = tree == null ? null : tree.attributeValue( "access" );
		if ( access != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( Access.class );
			AccessType type;
			try {
				type = AccessType.valueOf( access );
			}
			catch ( IllegalArgumentException e ) {
				throw new AnnotationException( access + " is not a valid access type. Check you xml confguration." );
			}
			ad.setValue( "value", type );
			return AnnotationFactory.create( ad );
		}
		else if ( defaults.canUseJavaAnnotations() && isJavaAnnotationPresent( Access.class ) ) {
			return getJavaAnnotation( Access.class );
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

	private ExcludeSuperclassListeners getExcludeSuperclassListeners(Element tree, XMLContext.Default defaults) {
		return (ExcludeSuperclassListeners) getMarkerAnnotation( ExcludeSuperclassListeners.class, tree, defaults );
	}

	private ExcludeDefaultListeners getExcludeDefaultListeners(Element tree, XMLContext.Default defaults) {
		return (ExcludeDefaultListeners) getMarkerAnnotation( ExcludeDefaultListeners.class, tree, defaults );
	}

	private Annotation getMarkerAnnotation(
			Class<? extends Annotation> clazz, Element element, XMLContext.Default defaults
	) {
		Element subelement = element == null ? null : element.element( annotationToXml.get( clazz ) );
		if ( subelement != null ) {
			return AnnotationFactory.create( new AnnotationDescriptor( clazz ) );
		}
		else if ( defaults.canUseJavaAnnotations() ) {
			//TODO wonder whether it should be excluded so that user can undone it
			return getJavaAnnotation( clazz );
		}
		else {
			return null;
		}
	}

	private SqlResultSetMappings getSqlResultSetMappings(Element tree, XMLContext.Default defaults) {
		List<SqlResultSetMapping> results = buildSqlResultsetMappings( tree, defaults );
		if ( defaults.canUseJavaAnnotations() ) {
			SqlResultSetMapping annotation = getJavaAnnotation( SqlResultSetMapping.class );
			addSqlResultsetMappingIfNeeded( annotation, results );
			SqlResultSetMappings annotations = getJavaAnnotation( SqlResultSetMappings.class );
			if ( annotations != null ) {
				for ( SqlResultSetMapping current : annotations.value() ) {
					addSqlResultsetMappingIfNeeded( current, results );
				}
			}
		}
		if ( results.size() > 0 ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( SqlResultSetMappings.class );
			ad.setValue( "value", results.toArray( new SqlResultSetMapping[results.size()] ) );
			return AnnotationFactory.create( ad );
		}
		else {
			return null;
		}
	}

	public static List<SqlResultSetMapping> buildSqlResultsetMappings(Element element, XMLContext.Default defaults) {
		if ( element == null ) {
			return new ArrayList<SqlResultSetMapping>();
		}
		List resultsetElementList = element.elements( "sql-result-set-mapping" );
		List<SqlResultSetMapping> resultsets = new ArrayList<SqlResultSetMapping>();
		Iterator it = resultsetElementList.listIterator();
		while ( it.hasNext() ) {
			Element subelement = (Element) it.next();
			AnnotationDescriptor ann = new AnnotationDescriptor( SqlResultSetMapping.class );
			copyStringAttribute( ann, subelement, "name", true );
			List<Element> elements = subelement.elements( "entity-result" );
			List<EntityResult> entityResults = new ArrayList<EntityResult>( elements.size() );
			for ( Element entityResult : elements ) {
				AnnotationDescriptor entityResultDescriptor = new AnnotationDescriptor( EntityResult.class );
				String clazzName = entityResult.attributeValue( "entity-class" );
				if ( clazzName == null ) {
					throw new AnnotationException( "<entity-result> without entity-class. " + SCHEMA_VALIDATION );
				}
				Class clazz;
				try {
					clazz = ReflectHelper.classForName(
							XMLContext.buildSafeClassName( clazzName, defaults ),
							JPAOverriddenAnnotationReader.class
					);
				}
				catch ( ClassNotFoundException e ) {
					throw new AnnotationException( "Unable to find entity-class: " + clazzName, e );
				}
				entityResultDescriptor.setValue( "entityClass", clazz );
				copyStringAttribute( entityResultDescriptor, entityResult, "discriminator-column", false );
				List<FieldResult> fieldResults = new ArrayList<FieldResult>();
				for ( Element fieldResult : (List<Element>) entityResult.elements( "field-result" ) ) {
					AnnotationDescriptor fieldResultDescriptor = new AnnotationDescriptor( FieldResult.class );
					copyStringAttribute( fieldResultDescriptor, fieldResult, "name", true );
					copyStringAttribute( fieldResultDescriptor, fieldResult, "column", true );
					fieldResults.add( (FieldResult) AnnotationFactory.create( fieldResultDescriptor ) );
				}
				entityResultDescriptor.setValue(
						"fields", fieldResults.toArray( new FieldResult[fieldResults.size()] )
				);
				entityResults.add( (EntityResult) AnnotationFactory.create( entityResultDescriptor ) );
			}
			ann.setValue( "entities", entityResults.toArray( new EntityResult[entityResults.size()] ) );

			elements = subelement.elements( "column-result" );
			List<ColumnResult> columnResults = new ArrayList<ColumnResult>( elements.size() );
			for ( Element columnResult : elements ) {
				AnnotationDescriptor columnResultDescriptor = new AnnotationDescriptor( ColumnResult.class );
				copyStringAttribute( columnResultDescriptor, columnResult, "name", true );
				columnResults.add( (ColumnResult) AnnotationFactory.create( columnResultDescriptor ) );
			}
			ann.setValue( "columns", columnResults.toArray( new ColumnResult[columnResults.size()] ) );
			//FIXME there is never such a result-class, get rid of it?
			String clazzName = subelement.attributeValue( "result-class" );
			if ( StringHelper.isNotEmpty( clazzName ) ) {
				Class clazz;
				try {
					clazz = ReflectHelper.classForName(
							XMLContext.buildSafeClassName( clazzName, defaults ),
							JPAOverriddenAnnotationReader.class
					);
				}
				catch ( ClassNotFoundException e ) {
					throw new AnnotationException( "Unable to find entity-class: " + clazzName, e );
				}
				ann.setValue( "resultClass", clazz );
			}
			copyStringAttribute( ann, subelement, "result-set-mapping", false );
			resultsets.add( (SqlResultSetMapping) AnnotationFactory.create( ann ) );
		}
		return resultsets;
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

	private NamedQueries getNamedQueries(Element tree, XMLContext.Default defaults) {
		//TODO avoid the Proxy Creation (@NamedQueries) when possible
		List<NamedQuery> queries = (List<NamedQuery>) buildNamedQueries( tree, false, defaults );
		if ( defaults.canUseJavaAnnotations() ) {
			NamedQuery annotation = getJavaAnnotation( NamedQuery.class );
			addNamedQueryIfNeeded( annotation, queries );
			NamedQueries annotations = getJavaAnnotation( NamedQueries.class );
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

	private NamedNativeQueries getNamedNativeQueries(Element tree, XMLContext.Default defaults) {
		List<NamedNativeQuery> queries = (List<NamedNativeQuery>) buildNamedQueries( tree, true, defaults );
		if ( defaults.canUseJavaAnnotations() ) {
			NamedNativeQuery annotation = getJavaAnnotation( NamedNativeQuery.class );
			addNamedNativeQueryIfNeeded( annotation, queries );
			NamedNativeQueries annotations = getJavaAnnotation( NamedNativeQueries.class );
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

	public static List buildNamedQueries(Element element, boolean isNative, XMLContext.Default defaults) {
		if ( element == null ) {
			return new ArrayList();
		}
		List namedQueryElementList = isNative ?
				element.elements( "named-native-query" ) :
				element.elements( "named-query" );
		List namedQueries = new ArrayList();
		Iterator it = namedQueryElementList.listIterator();
		while ( it.hasNext() ) {
			Element subelement = (Element) it.next();
			AnnotationDescriptor ann = new AnnotationDescriptor(
					isNative ? NamedNativeQuery.class : NamedQuery.class
			);
			copyStringAttribute( ann, subelement, "name", false );
			Element queryElt = subelement.element( "query" );
			if ( queryElt == null ) {
				throw new AnnotationException( "No <query> element found." + SCHEMA_VALIDATION );
			}
			copyStringElement( queryElt, ann, "query" );
			List<Element> elements = subelement.elements( "hint" );
			List<QueryHint> queryHints = new ArrayList<QueryHint>( elements.size() );
			for ( Element hint : elements ) {
				AnnotationDescriptor hintDescriptor = new AnnotationDescriptor( QueryHint.class );
				String value = hint.attributeValue( "name" );
				if ( value == null ) {
					throw new AnnotationException( "<hint> without name. " + SCHEMA_VALIDATION );
				}
				hintDescriptor.setValue( "name", value );
				value = hint.attributeValue( "value" );
				if ( value == null ) {
					throw new AnnotationException( "<hint> without value. " + SCHEMA_VALIDATION );
				}
				hintDescriptor.setValue( "value", value );
				queryHints.add( (QueryHint) AnnotationFactory.create( hintDescriptor ) );
			}
			ann.setValue( "hints", queryHints.toArray( new QueryHint[queryHints.size()] ) );
			String clazzName = subelement.attributeValue( "result-class" );
			if ( StringHelper.isNotEmpty( clazzName ) ) {
				Class clazz;
				try {
					clazz = ReflectHelper.classForName(
							XMLContext.buildSafeClassName( clazzName, defaults ),
							JPAOverriddenAnnotationReader.class
					);
				}
				catch ( ClassNotFoundException e ) {
					throw new AnnotationException( "Unable to find entity-class: " + clazzName, e );
				}
				ann.setValue( "resultClass", clazz );
			}
			copyStringAttribute( ann, subelement, "result-set-mapping", false );
			namedQueries.add( AnnotationFactory.create( ann ) );
		}
		return namedQueries;
	}

	private TableGenerator getTableGenerator(Element tree, XMLContext.Default defaults) {
		Element element = tree != null ? tree.element( annotationToXml.get( TableGenerator.class ) ) : null;
		if ( element != null ) {
			return buildTableGeneratorAnnotation( element, defaults );
		}
		else if ( defaults.canUseJavaAnnotations() && isJavaAnnotationPresent( TableGenerator.class ) ) {
			TableGenerator tableAnn = getJavaAnnotation( TableGenerator.class );
			if ( StringHelper.isNotEmpty( defaults.getSchema() )
					|| StringHelper.isNotEmpty( defaults.getCatalog() ) ) {
				AnnotationDescriptor annotation = new AnnotationDescriptor( TableGenerator.class );
				annotation.setValue( "name", tableAnn.name() );
				annotation.setValue( "table", tableAnn.table() );
				annotation.setValue( "catalog", tableAnn.table() );
				if ( StringHelper.isEmpty( (String) annotation.valueOf( "catalog" ) )
						&& StringHelper.isNotEmpty( defaults.getCatalog() ) ) {
					annotation.setValue( "catalog", defaults.getCatalog() );
				}
				annotation.setValue( "schema", tableAnn.table() );
				if ( StringHelper.isEmpty( (String) annotation.valueOf( "schema" ) )
						&& StringHelper.isNotEmpty( defaults.getSchema() ) ) {
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

	public static TableGenerator buildTableGeneratorAnnotation(Element element, XMLContext.Default defaults) {
		AnnotationDescriptor ad = new AnnotationDescriptor( TableGenerator.class );
		copyStringAttribute( ad, element, "name", false );
		copyStringAttribute( ad, element, "table", false );
		copyStringAttribute( ad, element, "catalog", false );
		copyStringAttribute( ad, element, "schema", false );
		copyStringAttribute( ad, element, "pk-column-name", false );
		copyStringAttribute( ad, element, "value-column-name", false );
		copyStringAttribute( ad, element, "pk-column-value", false );
		copyIntegerAttribute( ad, element, "initial-value" );
		copyIntegerAttribute( ad, element, "allocation-size" );
		buildUniqueConstraints( ad, element );
		if ( StringHelper.isEmpty( (String) ad.valueOf( "schema" ) )
				&& StringHelper.isNotEmpty( defaults.getSchema() ) ) {
			ad.setValue( "schema", defaults.getSchema() );
		}
		if ( StringHelper.isEmpty( (String) ad.valueOf( "catalog" ) )
				&& StringHelper.isNotEmpty( defaults.getCatalog() ) ) {
			ad.setValue( "catalog", defaults.getCatalog() );
		}
		return AnnotationFactory.create( ad );
	}

	private SequenceGenerator getSequenceGenerator(Element tree, XMLContext.Default defaults) {
		Element element = tree != null ? tree.element( annotationToXml.get( SequenceGenerator.class ) ) : null;
		if ( element != null ) {
			return buildSequenceGeneratorAnnotation( element );
		}
		else if ( defaults.canUseJavaAnnotations() ) {
			return getJavaAnnotation( SequenceGenerator.class );
		}
		else {
			return null;
		}
	}

	public static SequenceGenerator buildSequenceGeneratorAnnotation(Element element) {
		if ( element != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( SequenceGenerator.class );
			copyStringAttribute( ad, element, "name", false );
			copyStringAttribute( ad, element, "sequence-name", false );
			copyIntegerAttribute( ad, element, "initial-value" );
			copyIntegerAttribute( ad, element, "allocation-size" );
			return AnnotationFactory.create( ad );
		}
		else {
			return null;
		}
	}

	private DiscriminatorColumn getDiscriminatorColumn(Element tree, XMLContext.Default defaults) {
		Element element = tree != null ? tree.element( "discriminator-column" ) : null;
		if ( element != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( DiscriminatorColumn.class );
			copyStringAttribute( ad, element, "name", false );
			copyStringAttribute( ad, element, "column-definition", false );
			String value = element.attributeValue( "discriminator-type" );
			DiscriminatorType type = DiscriminatorType.STRING;
			if ( value != null ) {
				if ( "STRING".equals( value ) ) {
					type = DiscriminatorType.STRING;
				}
				else if ( "CHAR".equals( value ) ) {
					type = DiscriminatorType.CHAR;
				}
				else if ( "INTEGER".equals( value ) ) {
					type = DiscriminatorType.INTEGER;
				}
				else {
					throw new AnnotationException(
							"Unknown DiscrimiatorType in XML: " + value + " (" + SCHEMA_VALIDATION + ")"
					);
				}
			}
			ad.setValue( "discriminatorType", type );
			copyIntegerAttribute( ad, element, "length" );
			return AnnotationFactory.create( ad );
		}
		else if ( defaults.canUseJavaAnnotations() ) {
			return getJavaAnnotation( DiscriminatorColumn.class );
		}
		else {
			return null;
		}
	}

	private DiscriminatorValue getDiscriminatorValue(Element tree, XMLContext.Default defaults) {
		Element element = tree != null ? tree.element( "discriminator-value" ) : null;
		if ( element != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( DiscriminatorValue.class );
			copyStringElement( element, ad, "value" );
			return AnnotationFactory.create( ad );
		}
		else if ( defaults.canUseJavaAnnotations() ) {
			return getJavaAnnotation( DiscriminatorValue.class );
		}
		else {
			return null;
		}
	}

	private Inheritance getInheritance(Element tree, XMLContext.Default defaults) {
		Element element = tree != null ? tree.element( "inheritance" ) : null;
		if ( element != null ) {
			AnnotationDescriptor ad = new AnnotationDescriptor( Inheritance.class );
			Attribute attr = element.attribute( "strategy" );
			InheritanceType strategy = InheritanceType.SINGLE_TABLE;
			if ( attr != null ) {
				String value = attr.getValue();
				if ( "SINGLE_TABLE".equals( value ) ) {
					strategy = InheritanceType.SINGLE_TABLE;
				}
				else if ( "JOINED".equals( value ) ) {
					strategy = InheritanceType.JOINED;
				}
				else if ( "TABLE_PER_CLASS".equals( value ) ) {
					strategy = InheritanceType.TABLE_PER_CLASS;
				}
				else {
					throw new AnnotationException(
							"Unknown InheritanceType in XML: " + value + " (" + SCHEMA_VALIDATION + ")"
					);
				}
			}
			ad.setValue( "strategy", strategy );
			return AnnotationFactory.create( ad );
		}
		else if ( defaults.canUseJavaAnnotations() ) {
			return getJavaAnnotation( Inheritance.class );
		}
		else {
			return null;
		}
	}

	private IdClass getIdClass(Element tree, XMLContext.Default defaults) {
		Element element = tree == null ? null : tree.element( "id-class" );
		if ( element != null ) {
			Attribute attr = element.attribute( "class" );
			if ( attr != null ) {
				AnnotationDescriptor ad = new AnnotationDescriptor( IdClass.class );
				Class clazz;
				try {
					clazz = ReflectHelper.classForName(
							XMLContext.buildSafeClassName( attr.getValue(), defaults ),
							this.getClass()
					);
				}
				catch ( ClassNotFoundException e ) {
					throw new AnnotationException( "Unable to find id-class: " + attr.getValue(), e );
				}
				ad.setValue( "value", clazz );
				return AnnotationFactory.create( ad );
			}
			else {
				throw new AnnotationException( "id-class without class. " + SCHEMA_VALIDATION );
			}
		}
		else if ( defaults.canUseJavaAnnotations() ) {
			return getJavaAnnotation( IdClass.class );
		}
		else {
			return null;
		}
	}

	/**
	 * @param mergeWithAnnotations Whether to use Java annotations for this
	 * element, if present and not disabled by the XMLContext defaults.
	 * In some contexts (such as an association mapping) merging with
	 * annotations is never allowed.
	 */
	private PrimaryKeyJoinColumns getPrimaryKeyJoinColumns(Element element, XMLContext.Default defaults, boolean mergeWithAnnotations) {
		PrimaryKeyJoinColumn[] columns = buildPrimaryKeyJoinColumns( element );
		if ( mergeWithAnnotations ) {
			if ( columns.length == 0 && defaults.canUseJavaAnnotations() ) {
				PrimaryKeyJoinColumn annotation = getJavaAnnotation( PrimaryKeyJoinColumn.class );
				if ( annotation != null ) {
					columns = new PrimaryKeyJoinColumn[] { annotation };
				}
				else {
					PrimaryKeyJoinColumns annotations = getJavaAnnotation( PrimaryKeyJoinColumns.class );
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

	private Entity getEntity(Element tree, XMLContext.Default defaults) {
		if ( tree == null ) {
			return defaults.canUseJavaAnnotations() ? getJavaAnnotation( Entity.class ) : null;
		}
		else {
			if ( "entity".equals( tree.getName() ) ) {
				AnnotationDescriptor entity = new AnnotationDescriptor( Entity.class );
				copyStringAttribute( entity, tree, "name", false );
				if ( defaults.canUseJavaAnnotations()
						&& StringHelper.isEmpty( (String) entity.valueOf( "name" ) ) ) {
					Entity javaAnn = getJavaAnnotation( Entity.class );
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

	private MappedSuperclass getMappedSuperclass(Element tree, XMLContext.Default defaults) {
		if ( tree == null ) {
			return defaults.canUseJavaAnnotations() ? getJavaAnnotation( MappedSuperclass.class ) : null;
		}
		else {
			if ( "mapped-superclass".equals( tree.getName() ) ) {
				AnnotationDescriptor entity = new AnnotationDescriptor( MappedSuperclass.class );
				return AnnotationFactory.create( entity );
			}
			else {
				return null; //this is not an entity
			}
		}
	}

	private Embeddable getEmbeddable(Element tree, XMLContext.Default defaults) {
		if ( tree == null ) {
			return defaults.canUseJavaAnnotations() ? getJavaAnnotation( Embeddable.class ) : null;
		}
		else {
			if ( "embeddable".equals( tree.getName() ) ) {
				AnnotationDescriptor entity = new AnnotationDescriptor( Embeddable.class );
				return AnnotationFactory.create( entity );
			}
			else {
				return null; //this is not an entity
			}
		}
	}

	private Table getTable(Element tree, XMLContext.Default defaults) {
		Element subelement = tree == null ? null : tree.element( "table" );
		if ( subelement == null ) {
			//no element but might have some default or some annotation
			if ( StringHelper.isNotEmpty( defaults.getCatalog() )
					|| StringHelper.isNotEmpty( defaults.getSchema() ) ) {
				AnnotationDescriptor annotation = new AnnotationDescriptor( Table.class );
				if ( defaults.canUseJavaAnnotations() ) {
					Table table = getJavaAnnotation( Table.class );
					if ( table != null ) {
						annotation.setValue( "name", table.name() );
						annotation.setValue( "schema", table.schema() );
						annotation.setValue( "catalog", table.catalog() );
						annotation.setValue( "uniqueConstraints", table.uniqueConstraints() );
					}
				}
				if ( StringHelper.isEmpty( (String) annotation.valueOf( "schema" ) )
						&& StringHelper.isNotEmpty( defaults.getSchema() ) ) {
					annotation.setValue( "schema", defaults.getSchema() );
				}
				if ( StringHelper.isEmpty( (String) annotation.valueOf( "catalog" ) )
						&& StringHelper.isNotEmpty( defaults.getCatalog() ) ) {
					annotation.setValue( "catalog", defaults.getCatalog() );
				}
				return AnnotationFactory.create( annotation );
			}
			else if ( defaults.canUseJavaAnnotations() ) {
				return getJavaAnnotation( Table.class );
			}
			else {
				return null;
			}
		}
		else {
			//ignore java annotation, an element is defined
			AnnotationDescriptor annotation = new AnnotationDescriptor( Table.class );
			copyStringAttribute( annotation, subelement, "name", false );
			copyStringAttribute( annotation, subelement, "catalog", false );
			if ( StringHelper.isNotEmpty( defaults.getCatalog() )
					&& StringHelper.isEmpty( (String) annotation.valueOf( "catalog" ) ) ) {
				annotation.setValue( "catalog", defaults.getCatalog() );
			}
			copyStringAttribute( annotation, subelement, "schema", false );
			if ( StringHelper.isNotEmpty( defaults.getSchema() )
					&& StringHelper.isEmpty( (String) annotation.valueOf( "schema" ) ) ) {
				annotation.setValue( "schema", defaults.getSchema() );
			}
			buildUniqueConstraints( annotation, subelement );
			return AnnotationFactory.create( annotation );
		}
	}

	private SecondaryTables getSecondaryTables(Element tree, XMLContext.Default defaults) {
		List<Element> elements = tree == null ?
				new ArrayList<Element>() :
				(List<Element>) tree.elements( "secondary-table" );
		List<SecondaryTable> secondaryTables = new ArrayList<SecondaryTable>( 3 );
		for ( Element element : elements ) {
			AnnotationDescriptor annotation = new AnnotationDescriptor( SecondaryTable.class );
			copyStringAttribute( annotation, element, "name", false );
			copyStringAttribute( annotation, element, "catalog", false );
			if ( StringHelper.isNotEmpty( defaults.getCatalog() )
					&& StringHelper.isEmpty( (String) annotation.valueOf( "catalog" ) ) ) {
				annotation.setValue( "catalog", defaults.getCatalog() );
			}
			copyStringAttribute( annotation, element, "schema", false );
			if ( StringHelper.isNotEmpty( defaults.getSchema() )
					&& StringHelper.isEmpty( (String) annotation.valueOf( "schema" ) ) ) {
				annotation.setValue( "schema", defaults.getSchema() );
			}
			buildUniqueConstraints( annotation, element );
			annotation.setValue( "pkJoinColumns", buildPrimaryKeyJoinColumns( element ) );
			secondaryTables.add( (SecondaryTable) AnnotationFactory.create( annotation ) );
		}
		/*
		 * You can't have both secondary table in XML and Java,
		 * since there would be no way to "remove" a secondary table
		 */
		if ( secondaryTables.size() == 0 && defaults.canUseJavaAnnotations() ) {
			SecondaryTable secTableAnn = getJavaAnnotation( SecondaryTable.class );
			overridesDefaultInSecondaryTable( secTableAnn, defaults, secondaryTables );
			SecondaryTables secTablesAnn = getJavaAnnotation( SecondaryTables.class );
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
			if ( StringHelper.isNotEmpty( defaults.getCatalog() )
					|| StringHelper.isNotEmpty( defaults.getSchema() ) ) {
				AnnotationDescriptor annotation = new AnnotationDescriptor( SecondaryTable.class );
				annotation.setValue( "name", secTableAnn.name() );
				annotation.setValue( "schema", secTableAnn.schema() );
				annotation.setValue( "catalog", secTableAnn.catalog() );
				annotation.setValue( "uniqueConstraints", secTableAnn.uniqueConstraints() );
				annotation.setValue( "pkJoinColumns", secTableAnn.pkJoinColumns() );
				if ( StringHelper.isEmpty( (String) annotation.valueOf( "schema" ) )
						&& StringHelper.isNotEmpty( defaults.getSchema() ) ) {
					annotation.setValue( "schema", defaults.getSchema() );
				}
				if ( StringHelper.isEmpty( (String) annotation.valueOf( "catalog" ) )
						&& StringHelper.isNotEmpty( defaults.getCatalog() ) ) {
					annotation.setValue( "catalog", defaults.getCatalog() );
				}
				secondaryTables.add( (SecondaryTable) AnnotationFactory.create( annotation ) );
			}
			else {
				secondaryTables.add( secTableAnn );
			}
		}
	}

	private static void buildUniqueConstraints(AnnotationDescriptor annotation, Element element) {
		List uniqueConstraintElementList = element.elements( "unique-constraint" );
		UniqueConstraint[] uniqueConstraints = new UniqueConstraint[uniqueConstraintElementList.size()];
		int ucIndex = 0;
		Iterator ucIt = uniqueConstraintElementList.listIterator();
		while ( ucIt.hasNext() ) {
			Element subelement = (Element) ucIt.next();
			List<Element> columnNamesElements = subelement.elements( "column-name" );
			String[] columnNames = new String[columnNamesElements.size()];
			int columnNameIndex = 0;
			Iterator it = columnNamesElements.listIterator();
			while ( it.hasNext() ) {
				Element columnNameElt = (Element) it.next();
				columnNames[columnNameIndex++] = columnNameElt.getTextTrim();
			}
			AnnotationDescriptor ucAnn = new AnnotationDescriptor( UniqueConstraint.class );
			copyStringAttribute( ucAnn, subelement, "name", false );
			ucAnn.setValue( "columnNames", columnNames );
			uniqueConstraints[ucIndex++] = AnnotationFactory.create( ucAnn );
		}
		annotation.setValue( "uniqueConstraints", uniqueConstraints );
	}

	private PrimaryKeyJoinColumn[] buildPrimaryKeyJoinColumns(Element element) {
		if ( element == null ) {
			return new PrimaryKeyJoinColumn[] { };
		}
		List pkJoinColumnElementList = element.elements( "primary-key-join-column" );
		PrimaryKeyJoinColumn[] pkJoinColumns = new PrimaryKeyJoinColumn[pkJoinColumnElementList.size()];
		int index = 0;
		Iterator pkIt = pkJoinColumnElementList.listIterator();
		while ( pkIt.hasNext() ) {
			Element subelement = (Element) pkIt.next();
			AnnotationDescriptor pkAnn = new AnnotationDescriptor( PrimaryKeyJoinColumn.class );
			copyStringAttribute( pkAnn, subelement, "name", false );
			copyStringAttribute( pkAnn, subelement, "referenced-column-name", false );
			copyStringAttribute( pkAnn, subelement, "column-definition", false );
			pkJoinColumns[index++] = AnnotationFactory.create( pkAnn );
		}
		return pkJoinColumns;
	}

	private static void copyStringAttribute(
			AnnotationDescriptor annotation, Element element, String attributeName, boolean mandatory
	) {
		String attribute = element.attributeValue( attributeName );
		if ( attribute != null ) {
			String annotationAttributeName = getJavaAttributeNameFromXMLOne( attributeName );
			annotation.setValue( annotationAttributeName, attribute );
		}
		else {
			if ( mandatory ) {
				throw new AnnotationException(
						element.getName() + "." + attributeName + " is mandatory in XML overriding. " + SCHEMA_VALIDATION
				);
			}
		}
	}

	private static void copyIntegerAttribute(AnnotationDescriptor annotation, Element element, String attributeName) {
		String attribute = element.attributeValue( attributeName );
		if ( attribute != null ) {
			String annotationAttributeName = getJavaAttributeNameFromXMLOne( attributeName );
			annotation.setValue( annotationAttributeName, attribute );
			try {
				int length = Integer.parseInt( attribute );
				annotation.setValue( annotationAttributeName, length );
			}
			catch ( NumberFormatException e ) {
				throw new AnnotationException(
						element.getPath() + attributeName + " not parseable: " + attribute + " (" + SCHEMA_VALIDATION + ")"
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

	private static void copyStringElement(Element element, AnnotationDescriptor ad, String annotationAttribute) {
		String discr = element.getTextTrim();
		ad.setValue( annotationAttribute, discr );
	}

	private static void copyBooleanAttribute(AnnotationDescriptor descriptor, Element element, String attribute) {
		String attributeValue = element.attributeValue( attribute );
		if ( StringHelper.isNotEmpty( attributeValue ) ) {
			String javaAttribute = getJavaAttributeNameFromXMLOne( attribute );
			descriptor.setValue( javaAttribute, Boolean.parseBoolean( attributeValue ) );
		}
	}

	private <T extends Annotation> T getJavaAnnotation(Class<T> annotationType) {
		return element.getAnnotation( annotationType );
	}

	private <T extends Annotation> boolean isJavaAnnotationPresent(Class<T> annotationType) {
		return element.isAnnotationPresent( annotationType );
	}

	private Annotation[] getJavaAnnotations() {
		return element.getAnnotations();
	}
}
