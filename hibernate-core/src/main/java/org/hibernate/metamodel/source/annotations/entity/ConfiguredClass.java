/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.annotations.entity;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.persistence.AccessType;

import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.HierarchicType;
import com.fasterxml.classmate.members.ResolvedMember;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.metamodel.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.annotations.ConfiguredClassHierarchyBuilder;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.metamodel.source.annotations.ReflectionHelper;
import org.hibernate.metamodel.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.source.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.source.annotations.attribute.AttributeType;
import org.hibernate.metamodel.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.source.annotations.attribute.SimpleAttribute;

/**
 * Base class for a configured entity, mapped super class or embeddable
 *
 * @author Hardy Ferentschik
 */
public class ConfiguredClass {
	public static final Logger LOG = Logger.getLogger( ConfiguredClass.class.getName() );

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
	 * The default access type for this entity
	 */
	private final AccessType classAccessType;

	/**
	 * The type of configured class, entity, mapped super class, embeddable, ...
	 */
	private final ConfiguredClassType configuredClassType;

	/**
	 * The id attributes
	 */
	private final Map<String, SimpleAttribute> idAttributeMap;

	/**
	 * The mapped association attributes for this entity
	 */
	private final Map<String, AssociationAttribute> associationAttributeMap;

	/**
	 * The mapped simple attributes for this entity
	 */
	private final Map<String, SimpleAttribute> simpleAttributeMap;

	/**
	 * The version attribute or {@code null} in case none exists.
	 */
	private SimpleAttribute versionAttribute;

	/**
	 * The embedded classes for this entity
	 */
	private final Map<String, EmbeddableClass> embeddedClasses = new HashMap<String, EmbeddableClass>();

	/**
	 * A map of all attribute overrides defined in this class. The override name is "normalised", meaning as if specified
	 * on class level. If the override is specified on attribute level the attribute name is used as prefix.
	 */
	private final Map<String, AttributeOverride> attributeOverrideMap;

	private final Set<String> transientFieldNames = new HashSet<String>();
	private final Set<String> transientMethodNames = new HashSet<String>();

	private final AnnotationBindingContext context;

	public ConfiguredClass(
			ClassInfo classInfo,
			AccessType defaultAccessType,
			ConfiguredClass parent,
			AnnotationBindingContext context) {
		this.parent = parent;
		this.context = context;
		this.classInfo = classInfo;
		this.clazz = context.locateClassByName( classInfo.toString() );
		this.configuredClassType = determineType();
		this.classAccessType = determineClassAccessType( defaultAccessType );
		this.simpleAttributeMap = new TreeMap<String, SimpleAttribute>();
		this.idAttributeMap = new TreeMap<String, SimpleAttribute>();
		this.associationAttributeMap = new TreeMap<String, AssociationAttribute>();

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

	public AnnotationBindingContext getContext() {
		return context;
	}

	public ConfiguredClassType getConfiguredClassType() {
		return configuredClassType;
	}

	public Iterable<SimpleAttribute> getSimpleAttributes() {
		return simpleAttributeMap.values();
	}

	public Iterable<SimpleAttribute> getIdAttributes() {
		return idAttributeMap.values();
	}

	public SimpleAttribute getVersionAttribute() {
		return versionAttribute;
	}

	public Iterable<AssociationAttribute> getAssociationAttributes() {
		return associationAttributeMap.values();
	}

	public Map<String, EmbeddableClass> getEmbeddedClasses() {
		return embeddedClasses;
	}

	public MappedAttribute getMappedAttribute(String propertyName) {
		MappedAttribute attribute;
		attribute = simpleAttributeMap.get( propertyName );
		if ( attribute == null ) {
			attribute = associationAttributeMap.get( propertyName );
		}
		if ( attribute == null ) {
			attribute = idAttributeMap.get( propertyName );
		}
		return attribute;
	}

	public Map<String, AttributeOverride> getAttributeOverrideMap() {
		return attributeOverrideMap;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ConfiguredClass" );
		sb.append( "{clazz=" ).append( clazz.getSimpleName() );
		sb.append( ", classAccessType=" ).append( classAccessType );
		sb.append( ", configuredClassType=" ).append( configuredClassType );
		sb.append( ", idAttributeMap=" ).append( idAttributeMap );
		sb.append( ", simpleAttributeMap=" ).append( simpleAttributeMap );
		sb.append( ", associationAttributeMap=" ).append( associationAttributeMap );
		sb.append( '}' );
		return sb.toString();
	}

	private ConfiguredClassType determineType() {
		if ( classInfo.annotations().containsKey( JPADotNames.ENTITY ) ) {
			return ConfiguredClassType.ENTITY;
		}
		else if ( classInfo.annotations().containsKey( JPADotNames.MAPPED_SUPERCLASS ) ) {
			return ConfiguredClassType.MAPPED_SUPERCLASS;
		}
		else if ( classInfo.annotations().containsKey( JPADotNames.EMBEDDABLE ) ) {
			return ConfiguredClassType.EMBEDDABLE;
		}
		else {
			return ConfiguredClassType.NON_ENTITY;
		}
	}

	private AccessType determineClassAccessType(AccessType defaultAccessType) {
		// default to the hierarchy access type to start with
		AccessType accessType = defaultAccessType;

		AnnotationInstance accessAnnotation = JandexHelper.getSingleAnnotation( classInfo, JPADotNames.ACCESS );
		if ( accessAnnotation != null && accessAnnotation.target().getClass().equals( ClassInfo.class ) ) {
			accessType = JandexHelper.getValueAsEnum( accessAnnotation, "value", AccessType.class );
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
		ResolvedTypeWithMembers resolvedType = context.resolveMemberTypes( context.getResolvedType( clazz ) );
		for ( HierarchicType hierarchicType : resolvedType.allTypesAndOverrides() ) {
			if ( hierarchicType.getType().getErasedType().equals( clazz ) ) {
				resolvedType = context.resolveMemberTypes( hierarchicType.getType() );
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
		if ( !ReflectionHelper.isProperty( member ) ) {
			return false;
		}

		if ( transientNames.contains( member.getName() ) ) {
			return false;
		}

		if ( explicitlyConfiguredMemberNames.contains( ReflectionHelper.getPropertyName( member ) ) ) {
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
		if ( accessAnnotations == null ) {
			return explicitAccessPropertyNames;
		}

		// iterate over all @Access annotations defined on the current class
		for ( AnnotationInstance accessAnnotation : accessAnnotations ) {
			// we are only interested at annotations defined on fields and methods
			AnnotationTarget annotationTarget = accessAnnotation.target();
			if ( !( annotationTarget.getClass().equals( MethodInfo.class ) || annotationTarget.getClass()
					.equals( FieldInfo.class ) ) ) {
				continue;
			}

			AccessType accessType = JandexHelper.getValueAsEnum( accessAnnotation, "value", AccessType.class );

			if ( !isExplicitAttributeAccessAnnotationPlacedCorrectly( annotationTarget, accessType ) ) {
				continue;
			}

			// the placement is correct, get the member
			Member member;
			if ( annotationTarget instanceof MethodInfo ) {
				Method m;
				try {
					m = clazz.getMethod( ( (MethodInfo) annotationTarget ).name() );
				}
				catch ( NoSuchMethodException e ) {
					throw new HibernateException(
							"Unable to load method "
									+ ( (MethodInfo) annotationTarget ).name()
									+ " of class " + clazz.getName()
					);
				}
				member = m;
				accessType = AccessType.PROPERTY;
			}
			else {
				Field f;
				try {
					f = clazz.getField( ( (FieldInfo) annotationTarget ).name() );
				}
				catch ( NoSuchFieldException e ) {
					throw new HibernateException(
							"Unable to load field "
									+ ( (FieldInfo) annotationTarget ).name()
									+ " of class " + clazz.getName()
					);
				}
				member = f;
				accessType = AccessType.FIELD;
			}
			if ( ReflectionHelper.isProperty( member ) ) {
				createMappedAttribute( member, resolvedMembers, accessType );
				explicitAccessPropertyNames.add( ReflectionHelper.getPropertyName( member ) );
			}
		}
		return explicitAccessPropertyNames;
	}

	private boolean isExplicitAttributeAccessAnnotationPlacedCorrectly(AnnotationTarget annotationTarget, AccessType accessType) {
		// when the access type of the class is FIELD
		// overriding access annotations must be placed on properties AND have the access type PROPERTY
		if ( AccessType.FIELD.equals( classAccessType ) ) {
			if ( !( annotationTarget instanceof MethodInfo ) ) {
				LOG.tracef(
						"The access type of class %s is AccessType.FIELD. To override the access for an attribute " +
								"@Access has to be placed on the property (getter)", classInfo.name().toString()
				);
				return false;
			}

			if ( !AccessType.PROPERTY.equals( accessType ) ) {
				LOG.tracef(
						"The access type of class %s is AccessType.FIELD. To override the access for an attribute " +
								"@Access has to be placed on the property (getter) with an access type of AccessType.PROPERTY. " +
								"Using AccessType.FIELD on the property has no effect",
						classInfo.name().toString()
				);
				return false;
			}
		}

		// when the access type of the class is PROPERTY
		// overriding access annotations must be placed on fields and have the access type FIELD
		if ( AccessType.PROPERTY.equals( classAccessType ) ) {
			if ( !( annotationTarget instanceof FieldInfo ) ) {
				LOG.tracef(
						"The access type of class %s is AccessType.PROPERTY. To override the access for a field " +
								"@Access has to be placed on the field ", classInfo.name().toString()
				);
				return false;
			}

			if ( !AccessType.FIELD.equals( accessType ) ) {
				LOG.tracef(
						"The access type of class %s is AccessType.PROPERTY. To override the access for a field " +
								"@Access has to be placed on the field with an access type of AccessType.FIELD. " +
								"Using AccessType.PROPERTY on the field has no effect",
						classInfo.name().toString()
				);
				return false;
			}
		}
		return true;
	}

	private void createMappedAttribute(Member member, ResolvedTypeWithMembers resolvedType, AccessType accessType) {
		final String attributeName = ReflectionHelper.getPropertyName( member );
		ResolvedMember[] resolvedMembers;
		if ( member instanceof Field ) {
			resolvedMembers = resolvedType.getMemberFields();
		}
		else {
			resolvedMembers = resolvedType.getMemberMethods();
		}
		final Class<?> attributeType = (Class<?>) findResolvedType( member.getName(), resolvedMembers );
		final Map<DotName, List<AnnotationInstance>> annotations = JandexHelper.getMemberAnnotations(
				classInfo, member.getName()
		);

		AttributeType attributeNature = determineAttributeType( annotations );
		String accessTypeString = accessType.toString().toLowerCase();
		switch ( attributeNature ) {
			case BASIC: {
				SimpleAttribute attribute = SimpleAttribute.createSimpleAttribute(
						attributeName, attributeType, annotations, accessTypeString
				);
				if ( attribute.isId() ) {
					idAttributeMap.put( attributeName, attribute );
				}
				else if ( attribute.isVersioned() ) {
					// todo - error handling in case there are multiple version attributes
					versionAttribute = attribute;
				}
				else {
					simpleAttributeMap.put( attributeName, attribute );
				}
				break;
			}
			case ELEMENT_COLLECTION:
			case EMBEDDED_ID:

			case EMBEDDED: {
				resolveEmbeddable( attributeName, attributeType );
			}
			// TODO handle the different association types
			default: {
				AssociationAttribute attribute = AssociationAttribute.createAssociationAttribute(
						attributeName, attributeType, attributeNature, accessTypeString, annotations
				);
				associationAttributeMap.put( attributeName, attribute );
			}
		}
	}

	private void resolveEmbeddable(String attributeName, Class<?> type) {
		ClassInfo embeddableClassInfo = context.getClassInfo( type.getName() );
		if ( classInfo == null ) {
			String msg = String.format(
					"Attribute %s of entity %s is annotated with @Embedded, but no embeddable configuration for type %s can be found.",
					attributeName,
					getName(),
					type.getName()
			);
			throw new AnnotationException( msg );
		}

		context.resolveAllTypes( type.getName() );
		EmbeddableHierarchy<EmbeddableClass> hierarchy = ConfiguredClassHierarchyBuilder.createEmbeddableHierarchy(
				context.<Object>locateClassByName( embeddableClassInfo.toString() ),
				classAccessType,
				context
		);
		embeddedClasses.put( attributeName, hierarchy.getLeaf() );
	}

	/**
	 * Given the annotations defined on a persistent attribute this methods determines the attribute type.
	 *
	 * @param annotations the annotations defined on the persistent attribute
	 *
	 * @return an instance of the {@code AttributeType} enum
	 */
	private AttributeType determineAttributeType(Map<DotName, List<AnnotationInstance>> annotations) {
		EnumMap<AttributeType, AnnotationInstance> discoveredAttributeTypes =
				new EnumMap<AttributeType, AnnotationInstance>( AttributeType.class );

		AnnotationInstance oneToOne = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ONE_TO_ONE );
		if ( oneToOne != null ) {
			discoveredAttributeTypes.put( AttributeType.ONE_TO_ONE, oneToOne );
		}

		AnnotationInstance oneToMany = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ONE_TO_MANY );
		if ( oneToMany != null ) {
			discoveredAttributeTypes.put( AttributeType.ONE_TO_MANY, oneToMany );
		}

		AnnotationInstance manyToOne = JandexHelper.getSingleAnnotation( annotations, JPADotNames.MANY_TO_ONE );
		if ( manyToOne != null ) {
			discoveredAttributeTypes.put( AttributeType.MANY_TO_ONE, manyToOne );
		}

		AnnotationInstance manyToMany = JandexHelper.getSingleAnnotation( annotations, JPADotNames.MANY_TO_MANY );
		if ( manyToMany != null ) {
			discoveredAttributeTypes.put( AttributeType.MANY_TO_MANY, manyToMany );
		}

		AnnotationInstance embedded = JandexHelper.getSingleAnnotation( annotations, JPADotNames.EMBEDDED );
		if ( embedded != null ) {
			discoveredAttributeTypes.put( AttributeType.EMBEDDED, embedded );
		}

		AnnotationInstance embeddIded = JandexHelper.getSingleAnnotation( annotations, JPADotNames.EMBEDDED_ID );
		if ( embeddIded != null ) {
			discoveredAttributeTypes.put( AttributeType.EMBEDDED_ID, embeddIded );
		}

		AnnotationInstance elementCollection = JandexHelper.getSingleAnnotation(
				annotations,
				JPADotNames.ELEMENT_COLLECTION
		);
		if ( elementCollection != null ) {
			discoveredAttributeTypes.put( AttributeType.ELEMENT_COLLECTION, elementCollection );
		}

		if ( discoveredAttributeTypes.size() == 0 ) {
			return AttributeType.BASIC;
		}
		else if ( discoveredAttributeTypes.size() == 1 ) {
			return discoveredAttributeTypes.keySet().iterator().next();
		}
		else {
			throw new AnnotationException( "More than one association type configured for property  " + getName() + " of class " + getName() );
		}
	}

	private Type findResolvedType(String name, ResolvedMember[] resolvedMembers) {
		for ( ResolvedMember resolvedMember : resolvedMembers ) {
			if ( resolvedMember.getName().equals( name ) ) {
				return resolvedMember.getType().getErasedType();
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
				transientFieldNames.add( ( (FieldInfo) target ).name() );
			}
			else {
				transientMethodNames.add( ( (MethodInfo) target ).name() );
			}
		}
	}

	private Map<String, AttributeOverride> findAttributeOverrides() {
		Map<String, AttributeOverride> attributeOverrideList = new HashMap<String, AttributeOverride>();

		AnnotationInstance attributeOverrideAnnotation = JandexHelper.getSingleAnnotation(
				classInfo,
				JPADotNames.ATTRIBUTE_OVERRIDE
		);
		if ( attributeOverrideAnnotation != null ) {
			String prefix = createPathPrefix( attributeOverrideAnnotation );
			AttributeOverride override = new AttributeOverride( prefix, attributeOverrideAnnotation );
			attributeOverrideList.put( override.getAttributePath(), override );
		}

		AnnotationInstance attributeOverridesAnnotation = JandexHelper.getSingleAnnotation(
				classInfo,
				JPADotNames.ATTRIBUTE_OVERRIDES
		);
		if ( attributeOverridesAnnotation != null ) {
			AnnotationInstance[] annotationInstances = attributeOverridesAnnotation.value().asNestedArray();
			for ( AnnotationInstance annotationInstance : annotationInstances ) {
				String prefix = createPathPrefix( annotationInstance );
				AttributeOverride override = new AttributeOverride( prefix, annotationInstance );
				attributeOverrideList.put( override.getAttributePath(), override );
			}
		}
		return attributeOverrideList;
	}

	private String createPathPrefix(AnnotationInstance attributeOverrideAnnotation) {
		String prefix = null;
		AnnotationTarget target = attributeOverrideAnnotation.target();
		if ( target instanceof FieldInfo || target instanceof MethodInfo ) {
			prefix = JandexHelper.getPropertyName( target );
		}
		return prefix;
	}

	private List<AnnotationInstance> findAssociationOverrides() {
		List<AnnotationInstance> associationOverrideList = new ArrayList<AnnotationInstance>();

		AnnotationInstance associationOverrideAnnotation = JandexHelper.getSingleAnnotation(
				classInfo,
				JPADotNames.ASSOCIATION_OVERRIDE
		);
		if ( associationOverrideAnnotation != null ) {
			associationOverrideList.add( associationOverrideAnnotation );
		}

		AnnotationInstance associationOverridesAnnotation = JandexHelper.getSingleAnnotation(
				classInfo,
				JPADotNames.ASSOCIATION_OVERRIDES
		);
		if ( associationOverrideAnnotation != null ) {
			AnnotationInstance[] attributeOverride = associationOverridesAnnotation.value().asNestedArray();
			Collections.addAll( associationOverrideList, attributeOverride );
		}

		return associationOverrideList;
	}
}
