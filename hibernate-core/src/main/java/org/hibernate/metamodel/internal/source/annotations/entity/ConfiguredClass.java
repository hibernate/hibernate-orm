/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations.entity;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.AccessType;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.SingularAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.util.AnnotationParserHelper;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding.NaturalIdMutability;
import org.hibernate.metamodel.spi.source.MappingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.ResolvedMember;

/**
 * Base class for a configured entity, mapped super class or embeddable
 *
 * @author Hardy Ferentschik
 * @author Brett Meyer
 */
public class ConfiguredClass {
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, AssertionFailure.class.getName());

	/**
	 * The parent of this configured class or {@code null} in case this configured class is the root of a hierarchy.
	 */
	private final ConfiguredClass parent;

	/**
	 * The Jandex class info for this configured class. Provides access to the annotation defined on this configured class.
	 */
	private final ClassInfo classInfo;

	/**
	 * The actual java type.
	 */
	private final Class<?> clazz;

	/**
	 * Is this class abstract?
	 */
	private final boolean isAbstract;

	/**
	 * The default access type for this entity
	 */
	private final AccessType classAccessType;

	/**
	 * The generically resolved type
	 */
	private final ResolvedTypeWithMembers genericResolvedType;

	/**
	 * The id attributes
	 */
	private final Map<String, MappedAttribute> idAttributeMap;

	/**
	 * The mapped association attributes for this entity
	 */
	private final Map<String, AssociationAttribute> associationAttributeMap;

	/**
	 * The mapped simple attributes for this entity
	 */
	private final Map<String, BasicAttribute> simpleAttributeMap;

	/**
	 * The version attribute or {@code null} in case none exists.
	 */
	private BasicAttribute versionAttribute;

	/**
	 * The embedded classes for this entity
	 */
	private final Map<String, EmbeddableClass> embeddedClasses
			= new HashMap<String, EmbeddableClass>();

	/**
	 * The collection element embedded classes for this entity
	 */
	private final Map<String, EmbeddableClass> collectionEmbeddedClasses
			= new HashMap<String, EmbeddableClass>();

	private final Map<String, AttributeOverride> attributeOverrideMap =new HashMap<String, AttributeOverride>(  );
	private final Map<String, AssociationOverride> associationOverrideMap = new HashMap<String, AssociationOverride>(  );

	private final Set<String> transientFieldNames = new HashSet<String>();
	private final Set<String> transientMethodNames = new HashSet<String>();

	/**
	 * Fully qualified name of a custom tuplizer
	 */
	private final String customTuplizer;

	private final EntityBindingContext localBindingContext;
	/**
	 *
 	 */
	private final String contextPath;

	public ConfiguredClass(
			final ClassInfo classInfo,
			final ResolvedTypeWithMembers fullyResolvedType,
			final AccessType defaultAccessType,
			final ConfiguredClass parent,
			final AnnotationBindingContext context) {
		this(classInfo, fullyResolvedType, defaultAccessType, parent, "",context );
	}


	public ConfiguredClass(
			final ClassInfo classInfo,
			final ResolvedTypeWithMembers fullyResolvedType,
			final AccessType defaultAccessType,
			final ConfiguredClass parent,
			final String contextPath,
			final AnnotationBindingContext context) {
		this.parent = parent;
		this.classInfo = classInfo;
		this.contextPath = contextPath;
		this.clazz = context.locateClassByName( classInfo.toString() );
		this.genericResolvedType = fullyResolvedType;
		this.localBindingContext = new EntityBindingContext( context, this );
		this.isAbstract = ReflectHelper.isAbstractClass( this.clazz );
		this.classAccessType = determineClassAccessType( defaultAccessType );
		this.customTuplizer = AnnotationParserHelper.determineCustomTuplizer( classInfo.annotations(), classInfo,
				localBindingContext.getServiceRegistry().getService( ClassLoaderService.class ) );
		this.simpleAttributeMap = new TreeMap<String, BasicAttribute>();
		this.idAttributeMap = new TreeMap<String, MappedAttribute>();
		this.associationAttributeMap = new TreeMap<String, AssociationAttribute>();
		collectAttributeOverrides();
		collectAssociationOverrides();

		collectAttributes();

	}

	public String getName() {
		return clazz.getName();
	}

	public Class<?> getConfiguredClass() {
		return clazz;
	}

	public ClassInfo getClassInfo() {
		return classInfo;
	}

	public ConfiguredClass getParent() {
		return parent;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public EntityBindingContext getLocalBindingContext() {
		return localBindingContext;
	}

	public boolean hostsAnnotation(DotName annotationName) {
		List<AnnotationInstance> annotationList = classInfo.annotations().get( annotationName );
		return CollectionHelper.isNotEmpty( annotationList );
	}

	public Map<String, BasicAttribute> getSimpleAttributes() {
		return simpleAttributeMap;
	}

	public boolean isIdAttribute(String attributeName) {
		return idAttributeMap.containsKey( attributeName );
	}

	public Map<String, MappedAttribute> getIdAttributes() {
		return idAttributeMap;
	}

	public BasicAttribute getVersionAttribute() {
		return versionAttribute;
	}

	public Map<String,AssociationAttribute> getAssociationAttributes() {
		return associationAttributeMap;
	}

	public Map<String, EmbeddableClass> getEmbeddedClasses() {
		return embeddedClasses;
	}

	public Map<String, EmbeddableClass> getCollectionEmbeddedClasses() {
		return collectionEmbeddedClasses;
	}

	private final ValueHolder<Map<String, AttributeOverride>> attributeOverrideMapHolder = new ValueHolder<Map<String, AttributeOverride>>(
			new ValueHolder.DeferredInitializer<Map<String, AttributeOverride>>() {
				@Override
				public Map<String, AttributeOverride> initialize() {
					Map<String, AttributeOverride> map = new HashMap<String, AttributeOverride>();
					for ( EmbeddableClass embeddableClass : getEmbeddedClasses().values() ) {
						map.putAll( embeddableClass.getAttributeOverrideMap() );
					}
					for(EmbeddableClass embeddableClass : getCollectionEmbeddedClasses().values()){
						map.putAll( embeddableClass.getAttributeOverrideMap() );
					}
					map.putAll( attributeOverrideMap );
					return map;
				}
			}
	);

	public Map<String, AttributeOverride> getAttributeOverrideMap() {
		return attributeOverrideMapHolder.getValue();
	}

	private final ValueHolder<Map<String, AssociationOverride>> associationOverrideMapHolder = new ValueHolder<Map<String, AssociationOverride>>( new ValueHolder.DeferredInitializer<Map<String, AssociationOverride>>() {
		@Override
		public Map<String, AssociationOverride> initialize() {
			Map<String, AssociationOverride> map = new HashMap<String, AssociationOverride>(  );
			for ( EmbeddableClass embeddableClass : getEmbeddedClasses().values() ) {
				map.putAll( embeddableClass.getAssociationOverrideMap() );
			}
			for(EmbeddableClass embeddableClass : getCollectionEmbeddedClasses().values()){
				map.putAll( embeddableClass.getAssociationOverrideMap() );
			}
			map.putAll( associationOverrideMap );
			return map;
		}
	} );

	public Map<String, AssociationOverride> getAssociationOverrideMap() {
		return associationOverrideMapHolder.getValue();
	}

	public AccessType getClassAccessType() {
		return classAccessType;
	}

	public String getCustomTuplizer() {
		return customTuplizer;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ConfiguredClass" );
		sb.append( "{clazz=" ).append( clazz.getSimpleName() );
		sb.append( '}' );
		return sb.toString();
	}

	private AccessType determineClassAccessType(AccessType defaultAccessType) {
		// default to the hierarchy access type to start with
		AccessType accessType = defaultAccessType;

		AnnotationInstance accessAnnotation = JandexHelper.getSingleAnnotation(
				classInfo,
				JPADotNames.ACCESS,
				ClassInfo.class
		);
		if ( accessAnnotation != null ) {
			accessType = JandexHelper.getEnumValue( accessAnnotation, "value", AccessType.class,
					localBindingContext.getServiceRegistry().getService( ClassLoaderService.class ) );
		} else {
			accessAnnotation = JandexHelper.getSingleAnnotation( classInfo, HibernateDotNames.ACCESS_TYPE, ClassInfo.class );
			if ( accessAnnotation != null ) {
				accessType = AccessType.valueOf( accessAnnotation.value().asString().toUpperCase() );
			}
		}

		return accessType;
	}

	/**
	 * Find all attributes for this configured class and add them to the corresponding map
	 */
	private void collectAttributes() {
		// find transient field and method names
		findTransientFieldAndMethodNames();


		final Set<String> explicitlyConfiguredMemberNames = createExplicitlyConfiguredAccessProperties( );

		if ( AccessType.FIELD.equals( classAccessType ) ) {
			Field fields[] = clazz.getDeclaredFields();
			Field.setAccessible( fields, true );
			for ( Field field : fields ) {
				if ( AnnotationParserHelper.isPersistentMember(
						transientFieldNames,
						explicitlyConfiguredMemberNames,
						field
				) ) {
					createMappedAttribute( field, AccessType.FIELD );
				}
			}
		}
		else {
			Method[] methods = clazz.getDeclaredMethods();
			Method.setAccessible( methods, true );
			for ( Method method : methods ) {
				if ( AnnotationParserHelper.isPersistentMember(
						transientMethodNames,
						explicitlyConfiguredMemberNames,
						method
				) ) {
					createMappedAttribute( method, AccessType.PROPERTY );
				}
			}
		}
	}

	/**
	 * Creates {@code MappedProperty} instances for the explicitly configured persistent properties
	 *
	 *
	 * @return the property names of the explicitly configured attribute names in a set
	 */
	private Set<String> createExplicitlyConfiguredAccessProperties() {
		Set<String> explicitAccessPropertyNames = new HashSet<String>();

		List<AnnotationInstance> accessAnnotations = classInfo.annotations().get( JPADotNames.ACCESS );
		List<AnnotationInstance> hibernateAccessAnnotations = classInfo.annotations().get( HibernateDotNames.ACCESS_TYPE );
		if ( accessAnnotations == null ) {
			accessAnnotations = hibernateAccessAnnotations;
			if ( accessAnnotations == null ) {
				return explicitAccessPropertyNames;
			}
		} else if ( hibernateAccessAnnotations != null ) {
			accessAnnotations.addAll( hibernateAccessAnnotations );
		}

		// iterate over all @Access annotations defined on the current class
		for ( AnnotationInstance accessAnnotation : accessAnnotations ) {
			// we are only interested at annotations defined on fields and methods
			AnnotationTarget annotationTarget = accessAnnotation.target();
			if ( !( annotationTarget.getClass().equals( MethodInfo.class ) || annotationTarget.getClass()
					.equals( FieldInfo.class ) ) ) {
				continue;
			}

			final AccessType accessType;
			if ( JPADotNames.ACCESS.equals( accessAnnotation.name() ) ) {
				accessType = JandexHelper.getEnumValue( accessAnnotation, "value", AccessType.class,
						localBindingContext.getServiceRegistry().getService( ClassLoaderService.class ) );
				checkExplicitJpaAttributeAccessAnnotationPlacedCorrectly( annotationTarget, accessType );
			} else {
				accessType = AccessType.valueOf( accessAnnotation.value().asString().toUpperCase() );
			}

			// the placement is correct, get the member
			Member member;
			if ( annotationTarget instanceof MethodInfo ) {
				try {
					member = clazz.getMethod( ( ( MethodInfo ) annotationTarget ).name() );
				}
				catch ( NoSuchMethodException e ) {
					throw new HibernateException(
							"Unable to load method "
									+ ( ( MethodInfo ) annotationTarget ).name()
									+ " of class " + clazz.getName()
					);
				}
			}
			else {
				try {
					member = clazz.getField( ( ( FieldInfo ) annotationTarget ).name() );
				}
				catch ( NoSuchFieldException e ) {
					throw new HibernateException(
							"Unable to load field "
									+ ( ( FieldInfo ) annotationTarget ).name()
									+ " of class " + clazz.getName()
					);
				}
			}
			if ( ReflectHelper.isProperty( member ) ) {
				createMappedAttribute( member, accessType );
				explicitAccessPropertyNames.add( ReflectHelper.getPropertyName( member ) );
			}
		}
		return explicitAccessPropertyNames;
	}

	private void checkExplicitJpaAttributeAccessAnnotationPlacedCorrectly(AnnotationTarget annotationTarget, AccessType accessType) {
		// when the access type of the class is FIELD
		// overriding access annotations must be placed on properties AND have the access type PROPERTY
		if ( AccessType.FIELD.equals( classAccessType ) ) {
			if ( !MethodInfo.class.isInstance( annotationTarget ) ) {
				String msg = LOG.accessTypeOverrideShouldBeAnnotatedOnProperty( classInfo.name().toString() );
				LOG.trace( msg );
				throw new AnnotationException( msg );
			}

			if ( !AccessType.PROPERTY.equals( accessType ) ) {
				String msg = LOG.accessTypeOverrideShouldBeProperty( classInfo.name().toString() );
				LOG.trace( msg );
				throw new AnnotationException( msg );
			}
		}

		// when the access type of the class is PROPERTY
		// overriding access annotations must be placed on fields and have the access type FIELD
		if ( AccessType.PROPERTY.equals( classAccessType ) ) {
			if ( !FieldInfo.class.isInstance( annotationTarget ) ) {
				String msg = LOG.accessTypeOverrideShouldBeAnnotatedOnField( classInfo.name().toString() );
				LOG.trace( msg );
				throw new AnnotationException( msg );
			}

			if ( !AccessType.FIELD.equals( accessType ) ) {
				String msg = LOG.accessTypeOverrideShouldBeField( classInfo.name().toString() );
				LOG.trace( msg );
				throw new AnnotationException( msg );
			}
		}
	}

	private void createMappedAttribute(Member member, AccessType accessType) {
		final String attributeName = ReflectHelper.getPropertyName( member );
		final ResolvedMember[] resolvedMembers = Field.class.isInstance( member ) ? genericResolvedType.getMemberFields() : genericResolvedType
				.getMemberMethods();
		ResolvedMember resolvedMember = findResolvedMember( member.getName(), resolvedMembers );
		final Map<DotName, List<AnnotationInstance>> annotations = JandexHelper.getMemberAnnotations(
				classInfo, member.getName(), localBindingContext.getServiceRegistry()
		);
		Class<?> attributeType = resolvedMember.getType().getErasedType();
		Class<?> collectionElementType = AnnotationParserHelper.resolveCollectionElementType(
				resolvedMember,
				annotations,
				getLocalBindingContext()
		);
		Class<?> collectionIndexType = null;
		if ( Map.class.isAssignableFrom( attributeType ) && !resolvedMember.getType().getTypeParameters().isEmpty()) {
			collectionIndexType = resolvedMember.getType().getTypeParameters().get( 0 ).getErasedType();
		}
		MappedAttribute.Nature attributeNature = determineAttributeNature(
				annotations, attributeType, collectionElementType
		);
		String accessTypeString = accessType.toString().toLowerCase();
		switch ( attributeNature ) {
			case BASIC: {
				BasicAttribute attribute = BasicAttribute.createSimpleAttribute(
						attributeName,
						attributeType,
						attributeNature,
						annotations,
						accessTypeString,
						getLocalBindingContext()
				);
				if ( attribute.isId() ) {
					idAttributeMap.put( attributeName, attribute );
				}
				else if ( attribute.isVersioned() ) {
					if ( versionAttribute == null ) {
						versionAttribute = attribute;
					}
					else {
						throw new MappingException( "Multiple version attributes", localBindingContext.getOrigin() );
					}
				}
				else {
					simpleAttributeMap.put( attributeName, attribute );
				}
				break;
			}
			case EMBEDDED_ID: {
				final BasicAttribute attribute = BasicAttribute.createSimpleAttribute(
						attributeName,
						attributeType,
						attributeNature,
						annotations,
						accessTypeString,
						getLocalBindingContext()
				);
				idAttributeMap.put( attributeName, attribute );
			}
			case EMBEDDED: {
				final AnnotationInstance targetAnnotation = JandexHelper.getSingleAnnotation(
						getClassInfo(),
						HibernateDotNames.TARGET
				);
				if ( targetAnnotation != null ) {
					attributeType = localBindingContext.locateClassByName(
							JandexHelper.getValue( targetAnnotation, "value", String.class,
									localBindingContext.getServiceRegistry().getService( ClassLoaderService.class ) )
					);
				}
				embeddedClasses.put( attributeName, resolveEmbeddable(
						attributeName, attributeType, resolvedMember.getType(), annotations ) );
				break;
			}
			case ONE_TO_ONE:
			case MANY_TO_ONE: {
				final AssociationAttribute attribute = SingularAssociationAttribute.createSingularAssociationAttribute(
						classInfo,
						attributeName,
						resolvedMember.getType().getErasedType(),
						attributeNature,
						accessTypeString,
						annotations,
						getLocalBindingContext()
				);
				if ( attribute.isId() ) {
					idAttributeMap.put( attributeName, attribute );
				}
				associationAttributeMap.put( attributeName, attribute );
				break;
			}
			case ELEMENT_COLLECTION_EMBEDDABLE:
				collectionEmbeddedClasses.put( attributeName, resolveEmbeddable(
						attributeName+".element", collectionElementType,resolvedMember.getType().getTypeBindings().getBoundType( 0 ), annotations ) );
				// fall through
			case ELEMENT_COLLECTION_BASIC:
			case ONE_TO_MANY:
			case MANY_TO_MANY: {
				AssociationAttribute attribute = PluralAssociationAttribute.createPluralAssociationAttribute(
						classInfo,
						attributeName,
						resolvedMember.getType().getErasedType(),
						collectionIndexType,
						collectionElementType,
						attributeNature,
						accessTypeString,
						annotations,
						getLocalBindingContext()
				);
				associationAttributeMap.put( attributeName, attribute );
				break;
			}
			case MANY_TO_ANY: {}
		}
	}

	private EmbeddableClass resolveEmbeddable(
			String attributeName,
			Class<?> type,
			ResolvedType resolvedType,
			Map<DotName,List<AnnotationInstance>> annotations) {
		final ClassInfo embeddableClassInfo = localBindingContext.getClassInfo( type.getName() );
		if ( embeddableClassInfo == null ) {
			final String msg = String.format(
					"Attribute '%s#%s' is annotated with @Embedded,\n but '%s' does not seem to be annotated " +
							"with @Embeddable.\n Are all annotated classes added to the configuration?",
					getConfiguredClass().getName(),
					attributeName,
					type.getName()
			);
			throw new AnnotationException( msg );
		}

		final NaturalIdMutability naturalIdMutability =  AnnotationParserHelper.checkNaturalId( annotations );
		//tuplizer on field
		final String customTuplizerClass = AnnotationParserHelper.determineCustomTuplizer( annotations,
				localBindingContext.getServiceRegistry().getService( ClassLoaderService.class ) );

		final EmbeddableHierarchy hierarchy = EmbeddableHierarchy.createEmbeddableHierarchy(
				localBindingContext.<Object>locateClassByName( embeddableClassInfo.toString() ),
				attributeName,
				resolvedType,
				classAccessType,
				naturalIdMutability,
				customTuplizerClass,
				localBindingContext
		);
		return hierarchy.getLeaf();
	}

	/**
	 * Given the annotations defined on a persistent attribute this methods determines the attribute type.
	 *
	 * @param annotations the annotations defined on the persistent attribute
	 * @param attributeType the attribute's type
	 * @param referencedCollectionType the type of the collection element in case the attribute is collection valued
	 *
	 * @return an instance of the {@code AttributeType} enum
	 */
	private MappedAttribute.Nature determineAttributeNature(
			final Map<DotName,List<AnnotationInstance>> annotations,
			final Class<?> attributeType,
			final Class<?> referencedCollectionType ) {
		EnumSet<MappedAttribute.Nature>  discoveredAttributeTypes = EnumSet.noneOf( MappedAttribute.Nature.class );
		AnnotationInstance oneToOne = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ONE_TO_ONE );
		if ( oneToOne != null ) {
			discoveredAttributeTypes.add( MappedAttribute.Nature.ONE_TO_ONE );
		}

		AnnotationInstance oneToMany = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ONE_TO_MANY );
		if ( oneToMany != null ) {
			discoveredAttributeTypes.add( MappedAttribute.Nature.ONE_TO_MANY );
		}

		AnnotationInstance manyToOne = JandexHelper.getSingleAnnotation( annotations, JPADotNames.MANY_TO_ONE );
		if ( manyToOne != null ) {
			discoveredAttributeTypes.add( MappedAttribute.Nature.MANY_TO_ONE );
		}

		AnnotationInstance manyToMany = JandexHelper.getSingleAnnotation( annotations, JPADotNames.MANY_TO_MANY );
		if ( manyToMany != null ) {
			discoveredAttributeTypes.add( MappedAttribute.Nature.MANY_TO_MANY );
		}

		AnnotationInstance embeddedId = JandexHelper.getSingleAnnotation( annotations, JPADotNames.EMBEDDED_ID );
		if ( embeddedId != null ) {
			discoveredAttributeTypes.add( MappedAttribute.Nature.EMBEDDED_ID );
		}
		AnnotationInstance id = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ID );
		AnnotationInstance embedded = JandexHelper.getSingleAnnotation( 
				annotations, JPADotNames.EMBEDDED );
		if ( embedded != null ) {
			discoveredAttributeTypes.add( MappedAttribute.Nature.EMBEDDED );
		} else if ( embeddedId == null ) {
			// For backward compatibility, we're allowing attributes of an
			// @Embeddable type to leave off @Embedded.  Check the type's
			// annotations.  (see HHH-7678)
			// However, it's important to ignore this if the field is
			// annotated with @EmbeddedId.
			if ( isEmbeddableType( attributeType ) ) {
				LOG.warn( attributeType.getName() + " has @Embeddable on it, but the attribute of this type in entity["
						+ getName()
						+ "] doesn't have @Embedded, which may cause compatibility issue" );
				discoveredAttributeTypes.add( id!=null? MappedAttribute.Nature.EMBEDDED_ID :   MappedAttribute.Nature.EMBEDDED );
			}
		}



		AnnotationInstance elementCollection = JandexHelper.getSingleAnnotation(
				annotations,
				JPADotNames.ELEMENT_COLLECTION
		);
		if ( elementCollection != null || ( discoveredAttributeTypes.isEmpty() && CollectionHelper.isCollectionOrArray( attributeType ) )) {
			boolean isEmbeddable = isEmbeddableType( referencedCollectionType );
			discoveredAttributeTypes.add( isEmbeddable? MappedAttribute.Nature.ELEMENT_COLLECTION_EMBEDDABLE : MappedAttribute.Nature.ELEMENT_COLLECTION_BASIC );
		}

		int size = discoveredAttributeTypes.size();
		switch ( size ) {
			case 0:
				return MappedAttribute.Nature.BASIC;
			case 1:
				return discoveredAttributeTypes.iterator().next();
			default:
				throw new AnnotationException( "More than one association type configured for property  " + getName() + " of class " + getName() );
		}
	}

	protected boolean isEmbeddableType(Class<?> referencedCollectionType) {
		// class info can be null for types like string, etc where there are no annotations
		ClassInfo classInfo = getLocalBindingContext().getIndex().getClassByName(
				DotName.createSimple(
						referencedCollectionType.getName()
				)
		);
		return classInfo != null && CollectionHelper.isNotEmpty( classInfo.annotations().get( JPADotNames.EMBEDDABLE ) );
	}

	private ResolvedMember findResolvedMember(String name, ResolvedMember[] resolvedMembers) {
		for ( ResolvedMember resolvedMember : resolvedMembers ) {
			if ( resolvedMember.getName().equals( name ) ) {
				return resolvedMember;
			}
		}
		throw new AssertionFailure(
				String.format(
						"Unable to resolve type of attribute %s of class %s",
						name,
						classInfo.name().toString()
				)
		);
	}

	/**
	 * Populates the sets of transient field and method names.
	 */
	private void findTransientFieldAndMethodNames() {
		List<AnnotationInstance> transientMembers = classInfo.annotations().get( JPADotNames.TRANSIENT );
		if ( transientMembers == null ) {
			return;
		}

		for ( AnnotationInstance transientMember : transientMembers ) {
			AnnotationTarget target = transientMember.target();
			if ( target instanceof FieldInfo ) {
				transientFieldNames.add( ( ( FieldInfo ) target ).name() );
			}
			else if(target instanceof MethodInfo) {
				transientMethodNames.add( ( ( MethodInfo ) target ).name() );
			}
			else {
				throw new MappingException( "@Transient can be only defined on field or property", getLocalBindingContext().getOrigin() );
			}
		}
	}

	protected void collectAssociationOverrides() {

		// Add all instances of @AssociationOverride
		List<AnnotationInstance> overrideAnnotations = JandexHelper
				.getAnnotations( classInfo, JPADotNames.ASSOCIATION_OVERRIDE );
		if ( overrideAnnotations != null ) {
			for ( AnnotationInstance annotation : overrideAnnotations ) {
				AssociationOverride override = new AssociationOverride(
						createPathPrefix( annotation.target() ), annotation, localBindingContext
				);
				associationOverrideMap.put(
						override.getAttributePath(), override
				);
			}
		}

		// Add all instances of @AssociationOverrides children
		List<AnnotationInstance> overridesAnnotations = JandexHelper
				.getAnnotations( classInfo, JPADotNames.ASSOCIATION_OVERRIDES );
		if ( overridesAnnotations != null ) {
			for ( AnnotationInstance attributeOverridesAnnotation : overridesAnnotations ) {
				AnnotationInstance[] annotationInstances
						= attributeOverridesAnnotation.value().asNestedArray();
				for ( AnnotationInstance annotation : annotationInstances ) {
					AssociationOverride override = new AssociationOverride(
							createPathPrefix(
									attributeOverridesAnnotation.target()
							),
							annotation,
							localBindingContext
					);
					associationOverrideMap.put(
							override.getAttributePath(), override
					);
				}
			}
		}
	}

	protected void collectAttributeOverrides() {

		// Add all instances of @AttributeOverride
		List<AnnotationInstance> attributeOverrideAnnotations = JandexHelper
				.getAnnotations(classInfo, JPADotNames.ATTRIBUTE_OVERRIDE );
		if ( attributeOverrideAnnotations != null ) {
			for ( AnnotationInstance annotation : attributeOverrideAnnotations ) {
				AttributeOverride override = new AttributeOverride( 
						createPathPrefix( annotation.target() ), annotation, localBindingContext );
				attributeOverrideMap.put(
						override.getAttributePath(), override
				);
			}
		}

		// Add all instances of @AttributeOverrides children
		List<AnnotationInstance> attributeOverridesAnnotations = JandexHelper
				.getAnnotations(classInfo, JPADotNames.ATTRIBUTE_OVERRIDES);
		if ( attributeOverridesAnnotations != null ) {
			for ( AnnotationInstance attributeOverridesAnnotation : attributeOverridesAnnotations ) {
				AnnotationInstance[] annotationInstances
						= attributeOverridesAnnotation.value().asNestedArray();
				for ( AnnotationInstance annotation : annotationInstances ) {
					AttributeOverride override = new AttributeOverride( 
							createPathPrefix( 
									attributeOverridesAnnotation.target() ),
									annotation,
									localBindingContext );
					attributeOverrideMap.put(
							override.getAttributePath(), override
					);
				}
			}
		}
	}

	protected String createPathPrefix(AnnotationTarget target) {
		if ( target instanceof FieldInfo || target instanceof MethodInfo ) {
			String path = JandexHelper.getPropertyName( target );
			if( StringHelper.isEmpty( contextPath )){
				return path;
			}
			return contextPath + "." + path;
		}
		return contextPath;
	}
}
