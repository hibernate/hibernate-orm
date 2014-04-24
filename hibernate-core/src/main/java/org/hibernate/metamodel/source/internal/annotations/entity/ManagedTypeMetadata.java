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
package org.hibernate.metamodel.source.internal.annotations.entity;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.persistence.AccessType;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.reflite.spi.ClassDescriptor;
import org.hibernate.metamodel.reflite.spi.FieldDescriptor;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.reflite.spi.MethodDescriptor;
import org.hibernate.metamodel.source.internal.AttributeConversionInfo;
import org.hibernate.metamodel.source.internal.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.internal.annotations.attribute.AbstractPersistentAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.AssociationOverride;
import org.hibernate.metamodel.source.internal.annotations.attribute.AttributeOverride;
import org.hibernate.metamodel.source.internal.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.EmbeddedAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.OverrideAndConverterCollector;
import org.hibernate.metamodel.source.internal.annotations.attribute.PersistentAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.PluralAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.SingularAssociationAttribute;
import org.hibernate.metamodel.source.internal.annotations.util.AnnotationParserHelper;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.spi.MappingException;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.xml.spi.Origin;
import org.hibernate.xml.spi.SourceType;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

/**
 * Models metadata about what JPA calls a {@link javax.persistence.metamodel.ManagedType}.
 * <p/>
 * Concretely, may be:<ul>
 *     <li>
 *         IdentifiableTypeMetadata<ul>
 *             <li>EntityTypeMetadata</li>
 *             <li>MappedSuperclassTypeMetadata</li>
 *         </ul>
 *     </li>
 *     <li>
 *         EmbeddableTypeMetadata
 *     </li>
 * </ul>
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public abstract class ManagedTypeMetadata implements OverrideAndConverterCollector {
    private static final CoreMessageLogger LOG = CoreLogging.messageLogger( ManagedTypeMetadata.class );

	private final JavaTypeDescriptor javaTypeDescriptor;
	private final EntityBindingContext localBindingContext;

	private final ManagedTypeMetadata superType;
	private Set<ManagedTypeMetadata> subclasses;
	private final AttributePath attributePathBase;
	private final AttributeRole attributeRoleBase;

	private final AccessType classLevelAccessType;
	private final String explicitClassLevelAccessorStrategy;
	private final String customTuplizerClassName;

	private Map<String, PersistentAttribute> persistentAttributeMap;


	/**
	 * This form is intended for construction of the root of an entity hierarchy,
	 * and its MappedSuperclasses
	 *
	 * @param javaTypeDescriptor Metadata for the Entity/MappedSuperclass
	 * @param defaultAccessType The default AccessType for the entity hierarchy
	 * @param bindingContext The binding context
	 */
	public ManagedTypeMetadata(
			JavaTypeDescriptor javaTypeDescriptor,
			AccessType defaultAccessType,
			boolean isRootEntity,
			AnnotationBindingContext bindingContext) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.localBindingContext = new EntityBindingContext( bindingContext, this );

		this.classLevelAccessType = determineAccessType( javaTypeDescriptor, defaultAccessType );
		this.explicitClassLevelAccessorStrategy = determineExplicitAccessorStrategy(
				javaTypeDescriptor,
				null
		);

		this.customTuplizerClassName = determineCustomTuplizer( javaTypeDescriptor );

		// walk up
		this.superType = walkRootSuperclasses( javaTypeDescriptor, defaultAccessType, bindingContext );
		if ( superType != null ) {
			superType.addSubclass( this );
		}

		if ( isRootEntity ) {
			// walk down
			walkSubclasses( javaTypeDescriptor, (IdentifiableTypeMetadata) this, defaultAccessType, bindingContext );
		}

		this.attributeRoleBase = new AttributeRole( javaTypeDescriptor.getName().toString() );
		this.attributePathBase = new AttributePath();
	}

	private String determineCustomTuplizer(JavaTypeDescriptor javaTypeDescriptor) {
		final AnnotationInstance tuplizerAnnotation = javaTypeDescriptor.findLocalTypeAnnotation(
				HibernateDotNames.TUPLIZER
		);

		if ( tuplizerAnnotation == null ) {
			return null;
		}

		final AnnotationValue implValue = tuplizerAnnotation.value( "impl" );
		if ( implValue == null ) {
			return null;
		}

		return StringHelper.nullIfEmpty( implValue.asString() );
	}

	private void addSubclass(ManagedTypeMetadata subclass) {
		if ( subclasses == null ) {
			subclasses = new HashSet<ManagedTypeMetadata>();
		}
		subclasses.add( subclass );
	}

	/**
	 * This form is intended for construction of entity hierarchy subclasses.
	 *
	 * @param javaTypeDescriptor Metadata for the Entity/MappedSuperclass
	 * @param superType Metadata for the super type
	 * @param defaultAccessType The default AccessType for the entity hierarchy
	 * @param bindingContext The binding context
	 */
	public ManagedTypeMetadata(
			JavaTypeDescriptor javaTypeDescriptor,
			ManagedTypeMetadata superType,
			AccessType defaultAccessType,
			AnnotationBindingContext bindingContext) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.localBindingContext = new EntityBindingContext( bindingContext, this );

		this.classLevelAccessType = determineAccessType( javaTypeDescriptor, defaultAccessType );

		// does the mapping defaults define a default?
		// 		trouble is that the defaults do define a default.. ALL THE TIME
		//final String mappingDefault = bindingContext.getMappingDefaults().getPropertyAccessorName();
		//if
		this.explicitClassLevelAccessorStrategy = determineExplicitAccessorStrategy(
				javaTypeDescriptor,
				null
		);

		this.customTuplizerClassName = determineCustomTuplizer( javaTypeDescriptor );

		this.superType = superType;

		this.attributeRoleBase = new AttributeRole( javaTypeDescriptor.getName().toString() );
		this.attributePathBase = new AttributePath();
	}

	/**
	 * This form is used to create Embedded references
	 *
	 * @param embeddableType The Embeddable descriptor
	 * @param attributeRoleBase The base for the roles of attributes created *from* here
	 * @param attributePathBase The base for the paths of attributes created *from* here
	 * @param defaultAccessType The default AccessType from the context of this Embedded
	 * @param defaultAccessorStrategy The default accessor strategy from the context of this Embedded
	 * @param bindingContext The binding context
	 */
	public ManagedTypeMetadata(
			JavaTypeDescriptor embeddableType,
			AttributeRole attributeRoleBase,
			AttributePath attributePathBase,
			AccessType defaultAccessType,
			String defaultAccessorStrategy,
			AnnotationBindingContext bindingContext) {
		this.javaTypeDescriptor = embeddableType;
		this.localBindingContext = new EntityBindingContext( bindingContext, this );

		this.classLevelAccessType = determineAccessType( javaTypeDescriptor, defaultAccessType );
		this.explicitClassLevelAccessorStrategy = determineExplicitAccessorStrategy(
				javaTypeDescriptor,
				defaultAccessorStrategy
		);

		this.customTuplizerClassName = determineCustomTuplizer( javaTypeDescriptor );

		this.superType = null;

		this.attributeRoleBase = attributeRoleBase;
		this.attributePathBase = attributePathBase;
	}

	private static AccessType determineAccessType(JavaTypeDescriptor javaTypeDescriptor, AccessType defaultAccessType) {
		final AnnotationInstance localAccessAnnotation = javaTypeDescriptor.findLocalTypeAnnotation( JPADotNames.ACCESS );
		if ( localAccessAnnotation != null ) {
			final AnnotationValue accessTypeValue = localAccessAnnotation.value();
			if ( accessTypeValue != null ) {
				return AccessType.valueOf( accessTypeValue.asEnum() );
			}
		}

		// legacy alert!
		// In the absence of a JPA @Access annotation, we interpret our custom
		// @AttributeAccessor as indicating access *if* it is "field" or "property"
		final AnnotationInstance accessorAnnotation = javaTypeDescriptor.findLocalTypeAnnotation( HibernateDotNames.ATTRIBUTE_ACCESSOR );
		if ( accessorAnnotation != null ) {
			final AnnotationValue strategyValue = accessorAnnotation.value();
			if ( strategyValue != null ) {
				final String strategyName = strategyValue.asString();
				if ( StringHelper.isNotEmpty( strategyName ) ) {
					if ( "field".equals( strategyName ) ) {
						return AccessType.FIELD;
					}
					else if ( "property".equals( strategyName ) ) {
						return AccessType.PROPERTY;
					}
				}
			}
		}

		return defaultAccessType;
	}

	private static String determineExplicitAccessorStrategy(
			JavaTypeDescriptor javaTypeDescriptor,
			String defaultValue) {
		// look for a @AttributeAccessor annotation
		final AnnotationInstance attributeAccessorAnnotation = javaTypeDescriptor.findLocalTypeAnnotation(
				HibernateDotNames.ATTRIBUTE_ACCESSOR
		);
		if ( attributeAccessorAnnotation != null ) {
			String explicitAccessorStrategy = attributeAccessorAnnotation.value().asString();
			if ( StringHelper.isEmpty( explicitAccessorStrategy ) ) {
				LOG.warnf(
						"Class [%s] specified @AttributeAccessor with empty value",
						javaTypeDescriptor.getName()
				);
			}
			else {
				return explicitAccessorStrategy;
			}
		}

		final AnnotationInstance localAccessAnnotation = javaTypeDescriptor.findLocalTypeAnnotation( JPADotNames.ACCESS );
		if ( localAccessAnnotation != null ) {
			final AnnotationValue accessTypeValue = localAccessAnnotation.value();
			if ( accessTypeValue != null ) {
				return AccessType.valueOf( accessTypeValue.asEnum() ).name().toLowerCase();
			}
		}

		return defaultValue;
	}

	private IdentifiableTypeMetadata walkRootSuperclasses(
			JavaTypeDescriptor type,
			AccessType defaultAccessType,
			AnnotationBindingContext context) {
		if ( !ClassDescriptor.class.isInstance( type ) ) {
			return null;
		}

		final ClassDescriptor superType = ( (ClassDescriptor) type ).getSuperType();

		if ( superType == null ) {
			// no super type
			return null;
		}

		// make triple sure there is no @Entity annotation
		if ( isEntity( superType ) ) {
			throw new MappingException(
					String.format(
							Locale.ENGLISH,
							"Unexpected @Entity [%s] as MappedSuperclass of entity hierarchy",
							type.getName()
					),
					new Origin( SourceType.ANNOTATION, superType.getName().toString() )
			);
		}
		else if ( isMappedSuperclass( superType ) ) {
			return new MappedSuperclassTypeMetadata( superType, defaultAccessType, context );
		}
		else {
			// otherwise, we might have an "intermediate" subclass
			if ( superType.getSuperType() != null ) {
				return walkRootSuperclasses( superType, defaultAccessType, context );
			}
			else {
				return null;
			}
		}
	}

	private void walkSubclasses(
			JavaTypeDescriptor javaTypeDescriptor,
			IdentifiableTypeMetadata superType,
			AccessType defaultAccessType,
			AnnotationBindingContext bindingContext) {
		// ask Jandex for all the known *direct* subclasses of `superType`
		// and iterate them to create the subclass metadata
		final Collection<ClassInfo> classInfos = bindingContext.getJandexAccess()
				.getIndex()
				.getKnownDirectSubclasses( javaTypeDescriptor.getName() );

		for ( ClassInfo classInfo : classInfos ) {
			final ClassDescriptor subclassTypeDescriptor = (ClassDescriptor) bindingContext.getJavaTypeDescriptorRepository().getType( classInfo.name() );

			final IdentifiableTypeMetadata subclassMeta;
			if ( isEntity( subclassTypeDescriptor ) ) {
				subclassMeta = new EntityTypeMetadata(
						subclassTypeDescriptor,
						superType,
						defaultAccessType,
						bindingContext
				);
				( (ManagedTypeMetadata) superType ).addSubclass( subclassMeta );
			}
			else if ( isMappedSuperclass( subclassTypeDescriptor ) ) {
				subclassMeta = new MappedSuperclassTypeMetadata(
						subclassTypeDescriptor,
						superType,
						defaultAccessType,
						bindingContext
				);
				( (ManagedTypeMetadata) superType ).addSubclass( subclassMeta );
			}
			else {
				subclassMeta = superType;
			}

			walkSubclasses( subclassTypeDescriptor, subclassMeta, defaultAccessType, bindingContext );
		}
	}

	private boolean isMappedSuperclass(JavaTypeDescriptor javaTypeDescriptor) {
		return javaTypeDescriptor.findLocalTypeAnnotation( JPADotNames.MAPPED_SUPERCLASS ) != null;
	}

	private boolean isEntity(JavaTypeDescriptor javaTypeDescriptor) {
		return javaTypeDescriptor.findLocalTypeAnnotation( JPADotNames.ENTITY ) != null;
	}

	public AttributeRole getAttributeRoleBase() {
		return attributeRoleBase;
	}

	public AttributePath getAttributePathBase() {
		return attributePathBase;
	}

	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}


	public String getName() {
		return javaTypeDescriptor.getName().toString();
	}

	public ManagedTypeMetadata getSuperType() {
		return superType;
	}

	public Set<ManagedTypeMetadata> getSubclasses() {
		return subclasses == null ? Collections.<ManagedTypeMetadata>emptySet() : subclasses;
	}

	public EntityBindingContext getLocalBindingContext() {
		return localBindingContext;
	}

	public boolean isAbstract() {
		return Modifier.isAbstract( javaTypeDescriptor.getModifiers() );
	}

	public Map<String, PersistentAttribute> getPersistentAttributeMap() {
		collectAttributesIfNeeded();
		return persistentAttributeMap;
	}

	protected void collectAttributesIfNeeded() {
		if ( persistentAttributeMap == null ) {
			persistentAttributeMap = new HashMap<String, PersistentAttribute>();
			collectPersistentAttributes();
		}
	}

	public AccessType getClassLevelAccessType() {
		return classLevelAccessType;
	}

	public String getCustomTuplizerClassName() {
		return customTuplizerClassName;
	}

	@Override
	public String toString() {
		return "ManagedTypeMetadata{javaType=" + javaTypeDescriptor.getName().toString() + "}";
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// attribute handling

	/**
	 * Collect all persistent attributes for this managed type
	 */
	private void collectPersistentAttributes() {
		// Call the strategy responsible for resolving the members that identify a persistent attribute
		final List<MemberDescriptor> backingMembers = localBindingContext.getBuildingOptions()
				.getPersistentAttributeMemberResolver()
				.resolveAttributesMembers( getJavaTypeDescriptor(), classLevelAccessType, localBindingContext );

		for ( MemberDescriptor backingMember : backingMembers ) {
			final AccessType accessType = determineAttributeAccessType( backingMember );
			final String attributeName;
			if ( FieldDescriptor.class.isInstance( backingMember ) ) {
				attributeName = backingMember.getName();
			}
			else if ( MethodDescriptor.class.isInstance( backingMember ) ) {
				final MethodDescriptor methodDescriptor = (MethodDescriptor) backingMember;
				attributeName = ReflectHelper.getPropertyNameFromGetterMethod( methodDescriptor.getName() );
			}
			else {
				throw localBindingContext.makeMappingException(
						"Backing member for a persistent attribute needs to be either a field or (getter) method : "
								+ backingMember.toLoggableForm()
				);
			}

			createPersistentAttribute( attributeName, backingMember, accessType );
		}
	}

	private AccessType determineAttributeAccessType(MemberDescriptor backingMember) {
		final AnnotationInstance explicitAccessAnnotation = backingMember.getAnnotations().get( JPADotNames.ACCESS );
		if ( explicitAccessAnnotation != null ) {
			return localBindingContext.getJandexAccess().getTypedValueExtractor( AccessType.class )
					.extract( explicitAccessAnnotation, "value" );
		}

		return classLevelAccessType;
	}

	private void createPersistentAttribute(String attributeName, MemberDescriptor member, AccessType accessType) {
		final String accessorStrategy = determineAttributeLevelAccessorStrategy( member, accessType );

		final JavaTypeDescriptor collectionElementType = AnnotationParserHelper.resolveCollectionElementType(
				member,
				getLocalBindingContext()
		);

		AttributeCategorization attributeCategory = determineAttributeCategorization(
				member,
				member.getType().getErasedType()
		);

		switch ( attributeCategory ) {
			case BASIC: {
				final BasicAttribute attr = new BasicAttribute(
						ManagedTypeMetadata.this,
						attributeName,
						attributePathBase.append( attributeName ),
						attributeRoleBase.append( attributeName ),
						member,
						accessType,
						accessorStrategy
				);
				categorizeAttribute( attr );
				break;
			}
			case EMBEDDED: {
				// NOTE that this models the Embedded, not the Embeddable!
				final EmbeddedAttribute attr = new EmbeddedAttribute(
						ManagedTypeMetadata.this,
						attributeName,
						attributePathBase.append( attributeName ),
						attributeRoleBase.append( attributeName ),
						member,
						accessType,
						accessorStrategy
				);
				categorizeAttribute( attr );
				break;
			}
			case TO_ONE: {
				final SingularAssociationAttribute attr = new SingularAssociationAttribute(
						ManagedTypeMetadata.this,
						attributeName,
						attributePathBase.append( attributeName ),
						attributeRoleBase.append( attributeName ),
						member,
						determineToOneNature( member ),
						accessType,
						accessorStrategy
				);
				categorizeAttribute( attr );
				break;
			}
			case PLURAL: {
				final JavaTypeDescriptor mapKeyType = AnnotationParserHelper.resolveMapKeyType(
						member,
						getLocalBindingContext()
				);
				PluralAttribute attr = new PluralAttribute(
						ManagedTypeMetadata.this,
						attributeName,
						attributePathBase.append( attributeName ),
						attributeRoleBase.append( attributeName ),
						member,
						determinePluralAttributeNature( member, collectionElementType ),
						collectionElementType,
						mapKeyType,
						accessType,
						accessorStrategy
				);
				categorizeAttribute( attr );
				break;
			}
			case ANY : {}
			case MANY_TO_ANY: {}
		}
	}

	private PersistentAttribute.Nature determineToOneNature(MemberDescriptor member) {
		final AnnotationInstance oneToOne = member.getAnnotations().get( JPADotNames.ONE_TO_ONE );
		final AnnotationInstance manyToOne = member.getAnnotations().get( JPADotNames.MANY_TO_ONE );
		if ( oneToOne != null ) {
			return PersistentAttribute.Nature.ONE_TO_ONE;
		}

		if ( manyToOne != null ) {
			return PersistentAttribute.Nature.MANY_TO_ONE;
		}

		throw getLocalBindingContext().makeMappingException(
				"Could not determine Nature of TO_ONE attribute : " + member.getName()
		);
	}

	private PersistentAttribute.Nature determinePluralAttributeNature(
			MemberDescriptor member,
			JavaTypeDescriptor collectionElementType) {
		final boolean isCollectionElementTypeEmbeddable = collectionElementType != null
				&& isEmbeddableType( collectionElementType );

		final AnnotationInstance oneToMany = member.getAnnotations().get( JPADotNames.ONE_TO_MANY );
		if ( oneToMany != null ) {
			return PersistentAttribute.Nature.ONE_TO_MANY;
		}

		final AnnotationInstance manyToMany = member.getAnnotations().get( JPADotNames.MANY_TO_MANY );
		if ( manyToMany != null ) {
			return PersistentAttribute.Nature.MANY_TO_MANY;
		}

		final AnnotationInstance elementCollection = member.getAnnotations().get( JPADotNames.ELEMENT_COLLECTION );
		if ( elementCollection != null ) {
			if ( isCollectionElementTypeEmbeddable ) {
				return PersistentAttribute.Nature.ELEMENT_COLLECTION_EMBEDDABLE;
			}
			else {
				return PersistentAttribute.Nature.ELEMENT_COLLECTION_BASIC;
			}
		}

		if ( member.getAnnotations().containsKey( JPADotNames.TEMPORAL )
				|| member.getAnnotations().containsKey( JPADotNames.LOB )
				|| member.getAnnotations().containsKey( JPADotNames.ENUMERATED )
				|| member.getAnnotations().containsKey( HibernateDotNames.TYPE ) ) {
			return PersistentAttribute.Nature.ELEMENT_COLLECTION_BASIC;
		}

		if ( member.getAnnotations().containsKey( HibernateDotNames.COLLECTION_ID )
				|| member.getAnnotations().containsKey( HibernateDotNames.COLLECTION_TYPE )
				|| member.getAnnotations().containsKey( HibernateDotNames.LIST_INDEX_BASE )
				|| member.getAnnotations().containsKey( HibernateDotNames.MAP_KEY_TYPE )
				|| member.getAnnotations().containsKey( JPADotNames.MAP_KEY )
				|| member.getAnnotations().containsKey( JPADotNames.MAP_KEY_CLASS )
				|| member.getAnnotations().containsKey( JPADotNames.MAP_KEY_COLUMN )
				|| member.getAnnotations().containsKey( JPADotNames.MAP_KEY_JOIN_COLUMN )
				|| member.getAnnotations().containsKey( JPADotNames.MAP_KEY_JOIN_COLUMNS ) ) {
			if ( isCollectionElementTypeEmbeddable ) {
				return AbstractPersistentAttribute.Nature.ELEMENT_COLLECTION_EMBEDDABLE;
			}
			else {
				return AbstractPersistentAttribute.Nature.ELEMENT_COLLECTION_BASIC;
			}
		}

		throw getLocalBindingContext().makeMappingException(
				"Could not determine Nature of PLURAL attribute : " + member.getName()
		);
	}

	protected void categorizeAttribute(PersistentAttribute attr) {
		persistentAttributeMap.put( attr.getName(), attr );
	}

	private String determineAttributeLevelAccessorStrategy(MemberDescriptor member, AccessType accessType) {
		// first and foremost, does the attribute define a local accessor strategy
		final AnnotationInstance attributeAccessorAnnotation = member.getAnnotations().get(
				HibernateDotNames.ATTRIBUTE_ACCESSOR
		);
		if ( attributeAccessorAnnotation != null ) {
			String explicitAccessorStrategy = attributeAccessorAnnotation.value().asString();
			if ( StringHelper.isEmpty( explicitAccessorStrategy ) ) {
				LOG.warnf( "Attribute [%s] specified @AttributeAccessor with empty value", member );
			}
			else {
				return explicitAccessorStrategy;
			}
		}

		// finally use the attribute AccessType as default...
		return accessType.name().toLowerCase();
	}

	/**
	 * Represents a rough categorization of type of attributes
	 */
	private static enum AttributeCategorization {
		BASIC,
		EMBEDDED,
		TO_ONE,
		PLURAL,
		ANY,
		MANY_TO_ANY
	}

	private AttributeCategorization determineAttributeCategorization(
			MemberDescriptor member,
			JavaTypeDescriptor attributeType) {

		EnumSet<AttributeCategorization>  categories = EnumSet.noneOf( AttributeCategorization.class );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// first, look for explicit nature annotations

		final AnnotationInstance basic = member.getAnnotations().get( JPADotNames.BASIC );
		if ( basic != null ) {
			categories.add( AttributeCategorization.BASIC );
		}

		final AnnotationInstance embeddedId = member.getAnnotations().get( JPADotNames.EMBEDDED_ID );
		final AnnotationInstance embedded = member.getAnnotations().get( JPADotNames.EMBEDDED );
		if ( embeddedId != null || embedded != null ) {
			categories.add( AttributeCategorization.EMBEDDED );
		}

		final AnnotationInstance any = member.getAnnotations().get( HibernateDotNames.ANY );
		if ( any != null ) {
			categories.add( AttributeCategorization.ANY );
		}

		final AnnotationInstance oneToOne = member.getAnnotations().get( JPADotNames.ONE_TO_ONE );
		final AnnotationInstance manyToOne = member.getAnnotations().get( JPADotNames.MANY_TO_ONE );
		if ( oneToOne != null || manyToOne != null ) {
			categories.add( AttributeCategorization.TO_ONE );
		}

		final AnnotationInstance oneToMany = member.getAnnotations().get( JPADotNames.ONE_TO_MANY );
		final AnnotationInstance manyToMany = member.getAnnotations().get( JPADotNames.MANY_TO_MANY );
		final AnnotationInstance elementCollection = member.getAnnotations().get( JPADotNames.ELEMENT_COLLECTION );
		if ( oneToMany != null || manyToMany != null || elementCollection != null ) {
			categories.add( AttributeCategorization.PLURAL );
		}

		final AnnotationInstance manyToAny = member.getAnnotations().get( HibernateDotNames.MANY_TO_ANY );
		if ( manyToAny != null ) {
			categories.add( AttributeCategorization.MANY_TO_ANY );
		}


		// For backward compatibility, we're allowing attributes of an
		// @Embeddable type to leave off @Embedded.  Check the type's
		// annotations.  (see HHH-7678)
		// However, it's important to ignore this if the field is
		// annotated with @EmbeddedId.
		if ( embedded == null && embeddedId == null ) {
			if ( isEmbeddableType( attributeType ) ) {
				LOG.warnf(
						"Class %s was annotated as @Embeddable. However a persistent attribute [%s] " +
								"of this type was found that did not contain the @Embedded (or @EmbeddedId) annotation.  " +
								"This may cause compatibility issues",
						attributeType.getName(),
						member.toLoggableForm()
				);
				categories.add( AttributeCategorization.EMBEDDED );
			}
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// then we look at annotations that can be used to infer natures

		if ( member.getAnnotations().containsKey( JPADotNames.TEMPORAL )
				|| member.getAnnotations().containsKey( JPADotNames.LOB )
				|| member.getAnnotations().containsKey( JPADotNames.ENUMERATED )
				|| member.getAnnotations().containsKey( HibernateDotNames.TYPE ) ) {
			// technically these could describe the elements of a "element collection"
			// but without requiring the @ElementCollection annotation we
			// run into problems mapping things like our "materialized LOB"
			// support where @Lob might be combined with an array
			//
			// All in all, supporting that inference would require a lot of checks.
			// Not sure its worth the effort.  For future reference the checks would
			// be along the lines of:
			// 		in order for this to indicate a persistent (element) collection
			//		we'd have to unequivocally know the collection element type (or
			//		array component type) and that type would need to be consistent
			//		with the inferred type.  For example, given a collection marked
			// 		@Lob, in order for us to interpret that as indicating a
			//		LOB-based ElementCollection we would need to be able to verify
			//		that the elements of the Collection are in fact Lobs.
			if ( elementCollection == null ) {
				categories.add( AttributeCategorization.BASIC );
			}
		}

		if ( member.getAnnotations().containsKey( HibernateDotNames.COLLECTION_ID )
				|| member.getAnnotations().containsKey( HibernateDotNames.COLLECTION_TYPE )
				|| member.getAnnotations().containsKey( HibernateDotNames.LIST_INDEX_BASE )
				|| member.getAnnotations().containsKey( HibernateDotNames.MAP_KEY_TYPE )
				|| member.getAnnotations().containsKey( JPADotNames.MAP_KEY )
				|| member.getAnnotations().containsKey( JPADotNames.MAP_KEY_CLASS )
				|| member.getAnnotations().containsKey( JPADotNames.MAP_KEY_COLUMN )
				|| member.getAnnotations().containsKey( JPADotNames.MAP_KEY_JOIN_COLUMN )
				|| member.getAnnotations().containsKey( JPADotNames.MAP_KEY_JOIN_COLUMNS ) ) {
			categories.add( AttributeCategorization.PLURAL );
		}

		// todo : other "inferences"?

		int size = categories.size();
		switch ( size ) {
			case 0: {
				return AttributeCategorization.BASIC;
			}
			case 1: {
				return categories.iterator().next();
			}
			default: {
				throw getLocalBindingContext().makeMappingException(
						"More than one attribute type was discovered for attribute " + member.getName() +
								" : " + categories.toString()
				);
			}
		}

	}

	protected boolean isEmbeddableType(JavaTypeDescriptor descriptor) {
		return descriptor.findTypeAnnotation( JPADotNames.EMBEDDABLE ) != null;
	}


	// NOTE : the idea here is gto route this call back to "the base",
	// 		the assumption being that all *relevant* converters have previously
	// 		been normalized to the base
	public abstract AttributeConversionInfo locateConversionInfo(AttributePath attributePath);

	public abstract AttributeOverride locateAttributeOverride(AttributePath attributePath);

	public abstract AssociationOverride locateAssociationOverride(AttributePath attributePath);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Stuff affecting attributes built from this managed type.

	public boolean canAttributesBeInsertable() {
		return true;
	}

	public boolean canAttributesBeUpdatable() {
		return true;
	}

	public NaturalIdMutability getContainerNaturalIdMutability() {
		return NaturalIdMutability.NOT_NATURAL_ID;
	}
}
