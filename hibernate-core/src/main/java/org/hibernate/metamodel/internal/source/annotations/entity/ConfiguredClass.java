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
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.AccessType;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JPADotNames;
import org.hibernate.metamodel.internal.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.MappingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.HierarchicType;
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
	 * The id attributes
	 */
	private final Map<String, BasicAttribute> idAttributeMap;

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

	/**
	 * A map of all attribute overrides defined in this class. The override name is "normalised", meaning as if specified
	 * on class level. If the override is specified on attribute level the attribute name is used as prefix.
	 */
	private final Map<String, AttributeOverride> attributeOverrideMap;

	private final Set<String> transientFieldNames = new HashSet<String>();
	private final Set<String> transientMethodNames = new HashSet<String>();

	/**
	 * Fully qualified name of a custom tuplizer
	 */
	private final String customTuplizer;

	private final EntityBindingContext localBindingContext;

	public ConfiguredClass(
			ClassInfo classInfo,
			AccessType defaultAccessType,
			ConfiguredClass parent,
			AnnotationBindingContext context) {
		this.parent = parent;
		this.classInfo = classInfo;
		this.clazz = context.locateClassByName( classInfo.toString() );
		this.isAbstract = ReflectHelper.isAbstractClass( this.clazz );
		this.classAccessType = determineClassAccessType( defaultAccessType );
		this.customTuplizer = determineCustomTuplizer();

		this.simpleAttributeMap = new TreeMap<String, BasicAttribute>();
		this.idAttributeMap = new TreeMap<String, BasicAttribute>();
		this.associationAttributeMap = new TreeMap<String, AssociationAttribute>();

		this.localBindingContext = new EntityBindingContext( context, this );

		collectAttributes();
		attributeOverrideMap = Collections.unmodifiableMap( findAttributeOverrides() );
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
		if ( annotationList == null ) {
			return false;
		}
		else {
			return annotationList.size() > 0;
		}
	}

	public Collection<BasicAttribute> getSimpleAttributes() {
		return simpleAttributeMap.values();
	}

	public Collection<BasicAttribute> getIdAttributes() {
		return idAttributeMap.values();
	}

	public BasicAttribute getVersionAttribute() {
		return versionAttribute;
	}

	public Iterable<AssociationAttribute> getAssociationAttributes() {
		return associationAttributeMap.values();
	}

	public Map<String, EmbeddableClass> getEmbeddedClasses() {
		return embeddedClasses;
	}

	public Map<String, EmbeddableClass> getCollectionEmbeddedClasses() {
		return collectionEmbeddedClasses;
	}

	public Map<String, AttributeOverride> getAttributeOverrideMap() {
		return attributeOverrideMap;
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

		AnnotationInstance accessAnnotation = JandexHelper.getSingleAnnotation( classInfo, JPADotNames.ACCESS, ClassInfo.class );
		if ( accessAnnotation != null ) {
			accessType = JandexHelper.getEnumValue( accessAnnotation, "value", AccessType.class );
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

		// use the class mate library to generic types
		ResolvedTypeWithMembers resolvedType = localBindingContext.resolveMemberTypes(
				localBindingContext.getResolvedType( clazz )
		);
		for ( HierarchicType hierarchicType : resolvedType.allTypesAndOverrides() ) {
			if ( hierarchicType.getType().getErasedType().equals( clazz ) ) {
				resolvedType = localBindingContext.resolveMemberTypes( hierarchicType.getType() );
				break;
			}
		}

		if ( resolvedType == null ) {
			throw new AssertionFailure( "Unable to resolve types for " + clazz.getName() );
		}

		Set<String> explicitlyConfiguredMemberNames = createExplicitlyConfiguredAccessProperties( resolvedType );

		if ( AccessType.FIELD.equals( classAccessType ) ) {
			Field fields[] = clazz.getDeclaredFields();
			Field.setAccessible( fields, true );
			for ( Field field : fields ) {
				if ( isPersistentMember( transientFieldNames, explicitlyConfiguredMemberNames, field ) ) {
					createMappedAttribute( field, resolvedType, AccessType.FIELD );
				}
			}
		}
		else {
			Method[] methods = clazz.getDeclaredMethods();
			Method.setAccessible( methods, true );
			for ( Method method : methods ) {
				if ( isPersistentMember( transientMethodNames, explicitlyConfiguredMemberNames, method ) ) {
					createMappedAttribute( method, resolvedType, AccessType.PROPERTY );
				}
			}
		}
	}

	private boolean isPersistentMember(Set<String> transientNames, Set<String> explicitlyConfiguredMemberNames, Member member) {
		if ( !ReflectHelper.isProperty( member ) ) {
			return false;
		}

		if ( member instanceof Field && Modifier.isStatic( member.getModifiers() ) ) {
			// static fields are no instance variables! Catches also the case of serialVersionUID
			return false;
		}

		if ( member instanceof Method && Method.class.cast( member ).getReturnType().equals( void.class ) ){
			// not a getter
			return false;
		}

		if ( transientNames.contains( member.getName() ) ) {
			return false;
		}

		if ( explicitlyConfiguredMemberNames.contains( ReflectHelper.getPropertyName( member ) ) ) {
			return false;
		}

		return true;
	}

	/**
	 * Creates {@code MappedProperty} instances for the explicitly configured persistent properties
	 *
	 * @param resolvedMembers the resolved type parameters for this class
	 *
	 * @return the property names of the explicitly configured attribute names in a set
	 */
	private Set<String> createExplicitlyConfiguredAccessProperties(ResolvedTypeWithMembers resolvedMembers) {
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

			AccessType accessType;
			if ( JPADotNames.ACCESS.equals( accessAnnotation.name() ) ) {
				accessType = JandexHelper.getEnumValue( accessAnnotation, "value", AccessType.class );
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
				createMappedAttribute( member, resolvedMembers, accessType );
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

	private void createMappedAttribute(Member member, ResolvedTypeWithMembers resolvedType, AccessType accessType) {
		final String attributeName = ReflectHelper.getPropertyName( member );
		final ResolvedMember[] resolvedMembers = Field.class.isInstance( member ) ? resolvedType.getMemberFields() : resolvedType
				.getMemberMethods();
		ResolvedMember resolvedMember = findResolvedMember( member.getName(), resolvedMembers );
		Class<?> attributeType = resolvedMember.getType().getErasedType();
		Class<?> referencedCollectionType = resolveCollectionValuedReferenceType( resolvedMember );
		final Map<DotName, List<AnnotationInstance>> annotations = JandexHelper.getMemberAnnotations(
				classInfo, member.getName(), localBindingContext.getServiceRegistry()
		);

		MappedAttribute.Nature attributeNature = determineAttributeNature( 
				annotations, attributeType, referencedCollectionType );
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
							JandexHelper.getValue( targetAnnotation, "value", String.class )
					);
				}
				embeddedClasses.put( attributeName, resolveEmbeddable(
						attributeName, attributeType, annotations ) );
				break;
			}
			case ONE_TO_ONE:
			case MANY_TO_ONE: {
				final AssociationAttribute attribute = AssociationAttribute.createAssociationAttribute(
						attributeName,
						resolvedMember.getType().getErasedType(),
						attributeNature,
						accessTypeString,
						annotations,
						getLocalBindingContext()
				);
				associationAttributeMap.put( attributeName, attribute );
				break;
			}
			case ELEMENT_COLLECTION_EMBEDDABLE:
				collectionEmbeddedClasses.put( attributeName, resolveEmbeddable(
						attributeName, referencedCollectionType, annotations ) );
				// fall through
			case ELEMENT_COLLECTION_BASIC:
			case ONE_TO_MANY:
			case MANY_TO_MANY: {
				AssociationAttribute attribute = PluralAssociationAttribute.createPluralAssociationAttribute(
						classInfo,
						attributeName,
						resolvedMember.getType().getErasedType(),
						referencedCollectionType,
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

	private EmbeddableClass resolveEmbeddable(String attributeName, Class<?> type, Map<DotName, List<AnnotationInstance>> annotations) {
		final ClassInfo embeddableClassInfo = localBindingContext.getClassInfo( type.getName() );
		if ( embeddableClassInfo == null ) {
			final String msg = String.format(
					"Attribute '%s#%s' is annotated with @Embedded, but '%s' does not seem to be annotated " +
							"with @Embeddable.\n Are all annotated classes added to the configuration?",
					getConfiguredClass().getName(),
					attributeName,
					type.getName()
			);
			throw new AnnotationException( msg );
		}

		localBindingContext.resolveAllTypes( type.getName() );
		AnnotationInstance naturalIdAnnotationInstance = JandexHelper.getSingleAnnotation(
				annotations,
				HibernateDotNames.NATURAL_ID
		);
		SingularAttributeBinding.NaturalIdMutability naturalIdMutability;
		if ( naturalIdAnnotationInstance != null ) {
			naturalIdMutability = JandexHelper.getValue(
					naturalIdAnnotationInstance,
					"mutable",
					Boolean.class
			) ? SingularAttributeBinding.NaturalIdMutability.MUTABLE : SingularAttributeBinding.NaturalIdMutability.IMMUTABLE;
		}
		else {
			naturalIdMutability = SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID;
		}
		final EmbeddableHierarchy hierarchy = EmbeddableHierarchy.createEmbeddableHierarchy(
				localBindingContext.<Object>locateClassByName( embeddableClassInfo.toString() ),
				attributeName,
				classAccessType,
				naturalIdMutability,
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
	private MappedAttribute.Nature determineAttributeNature( Map<DotName,
			List<AnnotationInstance>> annotations,
			Class<?> attributeType,
			Class<?> referencedCollectionType ) {
		EnumMap<MappedAttribute.Nature, AnnotationInstance> discoveredAttributeTypes =
				new EnumMap<MappedAttribute.Nature, AnnotationInstance>( MappedAttribute.Nature.class );

		AnnotationInstance oneToOne = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ONE_TO_ONE );
		if ( oneToOne != null ) {
			discoveredAttributeTypes.put( MappedAttribute.Nature.ONE_TO_ONE, oneToOne );
		}

		AnnotationInstance oneToMany = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ONE_TO_MANY );
		if ( oneToMany != null ) {
			discoveredAttributeTypes.put( MappedAttribute.Nature.ONE_TO_MANY, oneToMany );
		}

		AnnotationInstance manyToOne = JandexHelper.getSingleAnnotation( annotations, JPADotNames.MANY_TO_ONE );
		if ( manyToOne != null ) {
			discoveredAttributeTypes.put( MappedAttribute.Nature.MANY_TO_ONE, manyToOne );
		}

		AnnotationInstance manyToMany = JandexHelper.getSingleAnnotation( annotations, JPADotNames.MANY_TO_MANY );
		if ( manyToMany != null ) {
			discoveredAttributeTypes.put( MappedAttribute.Nature.MANY_TO_MANY, manyToMany );
		}

		AnnotationInstance embeddedId = JandexHelper.getSingleAnnotation( annotations, JPADotNames.EMBEDDED_ID );
		if ( embeddedId != null ) {
			discoveredAttributeTypes.put( MappedAttribute.Nature.EMBEDDED_ID, embeddedId );
		}

		AnnotationInstance embedded = JandexHelper.getSingleAnnotation( 
				annotations, JPADotNames.EMBEDDED );
		if ( embedded != null ) {
			discoveredAttributeTypes.put( MappedAttribute.Nature.EMBEDDED, 
					embedded );
		} else if ( embeddedId == null ) {
			// For backward compatibility, we're allowing attributes of an
			// @Embeddable type to leave off @Embedded.  Check the type's
			// annotations.  (see HHH-7678)
			// However, it's important to ignore this if the field is
			// annotated with @EmbeddedId.
			ClassInfo typeClassInfo = localBindingContext.getIndex()
					.getClassByName( DotName.createSimple( attributeType.getName() ) );
			if ( typeClassInfo != null
					&& JandexHelper.getSingleAnnotation( 
							typeClassInfo.annotations(),
							JPADotNames.EMBEDDABLE ) != null ) {
				discoveredAttributeTypes.put( MappedAttribute.Nature.EMBEDDED,
						null );
			}
		}

		AnnotationInstance elementCollection = JandexHelper.getSingleAnnotation(
				annotations,
				JPADotNames.ELEMENT_COLLECTION
		);
		if ( elementCollection != null ) {
			// class info can be null for types like string, etc where there are no annotations
			ClassInfo classInfo = getLocalBindingContext().getIndex().getClassByName(
					DotName.createSimple(
							referencedCollectionType.getName()
					)
			);
			if ( classInfo != null && classInfo.annotations().get( JPADotNames.EMBEDDABLE ) != null ) {
				discoveredAttributeTypes.put( MappedAttribute.Nature.ELEMENT_COLLECTION_EMBEDDABLE, elementCollection );
			}
			else {
				discoveredAttributeTypes.put( MappedAttribute.Nature.ELEMENT_COLLECTION_BASIC, elementCollection );
			}
		}

		int size = discoveredAttributeTypes.size();
		switch ( size ) {
			case 0:
				return MappedAttribute.Nature.BASIC;
			case 1:
				return discoveredAttributeTypes.keySet().iterator().next();
			default:
				throw new AnnotationException( "More than one association type configured for property  " + getName() + " of class " + getName() );
		}
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

	private Class<?> resolveCollectionValuedReferenceType(ResolvedMember resolvedMember) {
		if ( resolvedMember.getType().isArray() ) {
			return resolvedMember.getType().getArrayElementType().getErasedType();
		}
		if ( resolvedMember.getType().getTypeParameters().isEmpty() ) {
			return null; // no generic at all
		}
		Class<?> type = resolvedMember.getType().getErasedType();
		if ( Collection.class.isAssignableFrom( type ) ) {
			return resolvedMember.getType().getTypeParameters().get( 0 ).getErasedType();
		}
		else if ( Map.class.isAssignableFrom( type ) ) {
			return resolvedMember.getType().getTypeParameters().get( 1 ).getErasedType();
		}
		else {
			return null;
		}
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
			else {
				transientMethodNames.add( ( ( MethodInfo ) target ).name() );
			}
		}
	}

	private Map<String, AttributeOverride> findAttributeOverrides() {
		Map<String, AttributeOverride> attributeOverrideList
				= new HashMap<String, AttributeOverride>();

		// Add all instances of @AttributeOverride
		List<AnnotationInstance> attributeOverrideAnnotations = JandexHelper
				.getAnnotations(classInfo, JPADotNames.ATTRIBUTE_OVERRIDE );
		if ( attributeOverrideAnnotations != null ) {
			for ( AnnotationInstance annotation : attributeOverrideAnnotations ) {
				AttributeOverride override = new AttributeOverride( 
						createPathPrefix( annotation.target() ), annotation );
				attributeOverrideList.put( 
						override.getAttributePath(), override );
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
									annotation );
					attributeOverrideList.put( 
							override.getAttributePath(), override );
				}
			}
		}
		return attributeOverrideList;
	}

	private String createPathPrefix(AnnotationTarget target) {
		String prefix = null;
		if ( target instanceof FieldInfo || target instanceof MethodInfo ) {
			prefix = JandexHelper.getPropertyName( target );
		}
		return prefix;
	}

	private String determineCustomTuplizer() {
		final AnnotationInstance tuplizersAnnotation = JandexHelper.getSingleAnnotation(
				classInfo, HibernateDotNames.TUPLIZERS
		);
		final AnnotationInstance tuplizerAnnotation = JandexHelper.getSingleAnnotation(
				classInfo,
				HibernateDotNames.TUPLIZER
		);
		if ( tuplizersAnnotation != null ) {
			AnnotationInstance[] annotations = JandexHelper.getValue(
					tuplizersAnnotation,
					"value",
					AnnotationInstance[].class
			);
			for ( final AnnotationInstance annotationInstance : annotations ) {
				final String impl = findTuplizerImpl( annotationInstance );
				if ( StringHelper.isNotEmpty( impl ) ) {
					return impl;
				}
			}
		}
		else if ( tuplizerAnnotation != null ) {
			final String impl = findTuplizerImpl( tuplizerAnnotation );
			if ( StringHelper.isNotEmpty( impl ) ) {
				return impl;
			}
		}
		return null;
	}

	private String findTuplizerImpl(final AnnotationInstance tuplizerAnnotation) {
		EntityMode mode;
		if ( tuplizerAnnotation.value( "entityModeType" ) != null ) {
			mode = EntityMode.valueOf( tuplizerAnnotation.value( "entityModeType" ).asEnum() );
		}
		else if ( tuplizerAnnotation.value( "entityMode" ) != null ) {
			mode = EntityMode.parse( tuplizerAnnotation.value( "entityMode" ).asString() );
		}
		else {
			mode = EntityMode.POJO;
		}
		return mode == EntityMode.POJO ? tuplizerAnnotation.value( "impl" ).asString() : null;

	}
}
