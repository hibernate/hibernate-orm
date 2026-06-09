/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.boot.mapping.internal.model.IdentifierAttributeBinding;
import org.hibernate.boot.mapping.internal.model.EntityIdentifierBinding;
import org.hibernate.boot.mapping.internal.model.IdentifierExtractionKind;
import org.hibernate.boot.mapping.internal.materialize.IdentifierMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.PrimaryTableKeyMappingMaterializer;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.mapping.internal.categorize.AggregatedKeyMapping;
import org.hibernate.boot.mapping.internal.categorize.AttributeMetadata;
import org.hibernate.boot.mapping.internal.categorize.BasicKeyMapping;
import org.hibernate.boot.mapping.internal.categorize.EntityHierarchy;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.KeyMapping;
import org.hibernate.boot.mapping.internal.categorize.NonAggregatedKeyMapping;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.Id;
import jakarta.persistence.Transient;

/// Binds the root identifier shape for an entity hierarchy.
///
/// The identifier phase creates the mapping model's primary identifier value,
/// identifier property, primary-key columns, and an [IdentifierBinding] snapshot
/// consumed by later phases.  It supports basic ids, aggregated component ids,
/// and non-aggregated `IdClass` ids.
///
/// Association-valued `IdClass` attributes are only partially bound here.  Their
/// `ManyToOne` value is created so the identifier component has the right shape,
/// but the actual join columns are deferred through [AssociationIdentifierBinding]
/// until target identifier bindings are available.
///
/// @since 9.0
/// @author Steve Ebersole
public class IdentifierBinder {
	private final ModelBinders modelBinders;

	private final BindingState state;
	private final BindingOptions options;
	private final BindingContext context;
	private final IdentifierMappingMaterializer identifierMappingMaterializer;
	private final PrimaryTableKeyMappingMaterializer primaryTableKeyMappingMaterializer;

	public IdentifierBinder(
			ModelBinders modelBinders,
			BindingState state,
			BindingOptions options,
			BindingContext context) {
		this.modelBinders = modelBinders;
		this.state = state;
		this.options = options;
		this.context = context;
		this.identifierMappingMaterializer = new IdentifierMappingMaterializer( modelBinders, state, options, context );
		this.primaryTableKeyMappingMaterializer = new PrimaryTableKeyMappingMaterializer(
				state.getMetadataBuildingContext()
		);
	}

	public static IdentifierBinding bindIdentifier(
			EntityTypeMetadata type,
			RootClass typeBinding,
			ModelBinders modelBinders,
			BindingState state,
			BindingOptions options,
			BindingContext context) {
		final IdentifierBinder identifierBinder = new IdentifierBinder( modelBinders, state, options, context );
		return identifierBinder.bindIdentifier( type, typeBinding );
	}

	private IdentifierBinding bindIdentifier(EntityTypeMetadata type, RootClass typeBinding) {
		final EntityHierarchy hierarchy = type.getHierarchy();
		final KeyMapping idMapping = hierarchy.getIdMapping();
		final Table table = typeBinding.getTable();

		primaryTableKeyMappingMaterializer.initializePrimaryKey(
				primaryTableKeyMappingMaterializer.resolvePrimaryKey( typeBinding, table )
		);

		if ( idMapping instanceof BasicKeyMapping basicKeyMapping ) {
			return bindBasicIdentifier( basicKeyMapping, table, type, typeBinding );
		}
		else if ( idMapping instanceof AggregatedKeyMapping aggregatedKeyMapping ) {
			return bindAggregatedIdentifier( aggregatedKeyMapping, table, type, typeBinding );
		}
		else {
			return bindNonAggregatedIdentifier( (NonAggregatedKeyMapping) idMapping, table, type, typeBinding );
		}
	}

	private IdentifierBinding bindBasicIdentifier(
			BasicKeyMapping basicKeyMapping,
			Table table,
			EntityTypeMetadata typeMetadata,
			RootClass typeBinding) {
		final AttributeMetadata idAttribute = basicKeyMapping.getAttribute();
		final MemberDetails idAttributeMember = idAttribute.getMember();

		final EntityIdentifierBinding entityIdentifierBinding = new EntityIdentifierBinding(
				typeMetadata,
				false,
				null
		);
		final IdentifierAttributeBinding attributeBinding = new IdentifierAttributeBinding(
				idAttribute.getName(),
				idAttributeMember,
				idAttributeMember,
				IdentifierExtractionKind.DIRECT
		);
		entityIdentifierBinding.addAttribute( attributeBinding );
		state.addEntityIdentifierBinding( typeMetadata, entityIdentifierBinding );

		return identifierMappingMaterializer.materializeBasicIdentifier(
				typeMetadata,
				typeBinding,
				basicKeyMapping,
				table,
				entityIdentifierBinding
		);
	}

	private IdentifierBinding bindAggregatedIdentifier(
			AggregatedKeyMapping aggregatedKeyMapping,
			Table table,
			EntityTypeMetadata type,
			RootClass typeBinding) {
		final ClassDetails keyType = resolveAggregatedIdentifierKeyType( aggregatedKeyMapping, type );
		final ComponentSource componentSource = ComponentSource.embeddedIdentifier(
				aggregatedKeyMapping.getAttribute().getMember(),
				keyType,
				resolveAggregatedIdentifierType( aggregatedKeyMapping, type ),
				type.getAccessType(),
				context
		);
		final EntityIdentifierBinding entityIdentifierBinding = createAggregatedEntityIdentifierBinding( type, componentSource );
		state.addEntityIdentifierBinding( type, entityIdentifierBinding );

		return identifierMappingMaterializer.materializeAggregatedIdentifier(
				type,
				typeBinding,
				aggregatedKeyMapping,
				table,
				keyType,
				componentSource,
				entityIdentifierBinding
		);
	}

	private ClassDetails resolveAggregatedIdentifierKeyType(
			AggregatedKeyMapping aggregatedKeyMapping,
			EntityTypeMetadata type) {
		return aggregatedKeyMapping.getAttribute()
				.getMember()
				.resolveRelativeType( type.getClassDetails() )
				.determineRawClass();
	}

	private TypeDetails resolveAggregatedIdentifierType(
			AggregatedKeyMapping aggregatedKeyMapping,
			EntityTypeMetadata type) {
		return aggregatedKeyMapping.getAttribute()
				.getMember()
				.resolveRelativeType( type.getClassDetails() );
	}

	private IdentifierBinding bindNonAggregatedIdentifier(
			NonAggregatedKeyMapping idMapping,
			Table table,
			EntityTypeMetadata type,
			RootClass typeBinding) {
		final boolean hasIdClass = idMapping.getIdClassType() != null;
		final boolean wholeDerivedIdClass = hasWholeDerivedIdClass( idMapping );
		if ( hasIdClass && !wholeDerivedIdClass ) {
			validateIdClassMembers( idMapping, type );
		}
		if ( isScalarIdClass( idMapping, wholeDerivedIdClass ) ) {
			return bindScalarIdClassIdentifier( idMapping, table, type, typeBinding );
		}
		if ( isEmbeddedIdClass( idMapping, wholeDerivedIdClass ) ) {
			return bindEmbeddedIdClassIdentifier( idMapping, table, type, typeBinding );
		}
		if ( isAssociationIdClass( idMapping, wholeDerivedIdClass ) ) {
			return bindAssociationIdClassIdentifier( idMapping, table, type, typeBinding );
		}
		final boolean noIdClassMapsId = !hasIdClass && hasMapsIdAttribute( type );
		final EntityIdentifierBinding entityIdentifierBinding = new EntityIdentifierBinding(
				type,
				hasIdClass,
				idMapping.getIdClassType()
		);
		for ( AttributeMetadata idAttribute : idMapping.getIdAttributes() ) {
			final MemberDetails member = idAttribute.getMember();
			final MemberDetails idClassMember = hasIdClass && !wholeDerivedIdClass
					? resolveIdClassMember( idMapping, idAttribute )
					: member;
			entityIdentifierBinding.addAttribute( new IdentifierAttributeBinding(
						idAttribute.getName(),
						member,
						idClassMember,
						identifierExtractionKind( idAttribute, idClassMember, wholeDerivedIdClass )
			) );
		}
		if ( hasIdClass && !wholeDerivedIdClass ) {
			entityIdentifierBinding.reorderAttributes( idClassMemberNames( idMapping.getIdClassType() ) );
		}
		state.addEntityIdentifierBinding( type, entityIdentifierBinding );
		return identifierMappingMaterializer.materializeNonAggregatedIdentifier(
				type,
				typeBinding,
				idMapping,
				table,
				entityIdentifierBinding,
				hasIdClass,
				wholeDerivedIdClass,
				noIdClassMapsId
		);
	}

	private IdentifierBinding bindScalarIdClassIdentifier(
			NonAggregatedKeyMapping idMapping,
			Table table,
			EntityTypeMetadata type,
			RootClass typeBinding) {
		final EntityIdentifierBinding entityIdentifierBinding = new EntityIdentifierBinding(
				type,
				true,
				idMapping.getIdClassType()
		);
		for ( AttributeMetadata idAttribute : idMapping.getIdAttributes() ) {
			entityIdentifierBinding.addAttribute( new IdentifierAttributeBinding(
					idAttribute.getName(),
					idAttribute.getMember(),
					resolveIdClassMember( idMapping, idAttribute ),
					IdentifierExtractionKind.DIRECT
			) );
		}
		entityIdentifierBinding.reorderAttributes( idClassMemberNames( idMapping.getIdClassType() ) );
		state.addEntityIdentifierBinding( type, entityIdentifierBinding );

		return identifierMappingMaterializer.materializeScalarIdClassIdentifier(
				type,
				typeBinding,
				idMapping,
				table,
				entityIdentifierBinding
		);
	}

	private boolean isScalarIdClass(NonAggregatedKeyMapping idMapping, boolean wholeDerivedIdClass) {
		if ( idMapping.getIdClassType() == null || wholeDerivedIdClass ) {
			return false;
		}
		for ( AttributeMetadata idAttribute : idMapping.getIdAttributes() ) {
			final MemberDetails idClassMember = resolveIdClassMember( idMapping, idAttribute );
			if ( idAttribute.getNature() != AttributeNature.BASIC
					|| idClassMemberStoresAssociation( idAttribute, idClassMember ) ) {
				return false;
			}
		}
		return true;
	}

	private IdentifierBinding bindEmbeddedIdClassIdentifier(
			NonAggregatedKeyMapping idMapping,
			Table table,
			EntityTypeMetadata type,
			RootClass typeBinding) {
		final EntityIdentifierBinding entityIdentifierBinding = new EntityIdentifierBinding(
				type,
				true,
				idMapping.getIdClassType()
		);
		for ( AttributeMetadata idAttribute : idMapping.getIdAttributes() ) {
			entityIdentifierBinding.addAttribute( new IdentifierAttributeBinding(
					idAttribute.getName(),
					idAttribute.getMember(),
					resolveIdClassMember( idMapping, idAttribute ),
					IdentifierExtractionKind.DIRECT
			) );
		}
		entityIdentifierBinding.reorderAttributes( idClassMemberNames( idMapping.getIdClassType() ) );
		state.addEntityIdentifierBinding( type, entityIdentifierBinding );

		return identifierMappingMaterializer.materializeEmbeddedIdClassIdentifier(
				type,
				typeBinding,
				idMapping,
				table,
				entityIdentifierBinding
		);
	}

	private IdentifierBinding bindAssociationIdClassIdentifier(
			NonAggregatedKeyMapping idMapping,
			Table table,
			EntityTypeMetadata type,
			RootClass typeBinding) {
		final EntityIdentifierBinding entityIdentifierBinding = new EntityIdentifierBinding(
				type,
				true,
				idMapping.getIdClassType()
		);
		for ( AttributeMetadata idAttribute : idMapping.getIdAttributes() ) {
			final MemberDetails idClassMember = resolveIdClassMember( idMapping, idAttribute );
			entityIdentifierBinding.addAttribute( new IdentifierAttributeBinding(
					idAttribute.getName(),
					idAttribute.getMember(),
					idClassMember,
					identifierExtractionKind( idAttribute, idClassMember, false )
			) );
		}
		entityIdentifierBinding.reorderAttributes( idClassMemberNames( idMapping.getIdClassType() ) );
		state.addEntityIdentifierBinding( type, entityIdentifierBinding );

		return identifierMappingMaterializer.materializeAssociationIdClassIdentifier(
				type,
				typeBinding,
				idMapping,
				table,
				entityIdentifierBinding
		);
	}

	private boolean isEmbeddedIdClass(NonAggregatedKeyMapping idMapping, boolean wholeDerivedIdClass) {
		if ( idMapping.getIdClassType() == null || wholeDerivedIdClass ) {
			return false;
		}
		boolean hasEmbeddedAttribute = false;
		for ( AttributeMetadata idAttribute : idMapping.getIdAttributes() ) {
			final MemberDetails idClassMember = resolveIdClassMember( idMapping, idAttribute );
			if ( idAttribute.getNature() == AttributeNature.EMBEDDED ) {
				hasEmbeddedAttribute = true;
			}
			else if ( idAttribute.getNature() != AttributeNature.BASIC
					|| idClassMemberStoresAssociation( idAttribute, idClassMember ) ) {
				return false;
			}
		}
		return hasEmbeddedAttribute;
	}

	private boolean isAssociationIdClass(NonAggregatedKeyMapping idMapping, boolean wholeDerivedIdClass) {
		if ( idMapping.getIdClassType() == null || wholeDerivedIdClass ) {
			return false;
		}
		for ( AttributeMetadata idAttribute : idMapping.getIdAttributes() ) {
			final MemberDetails idClassMember = resolveIdClassMember( idMapping, idAttribute );
			if ( idAttribute.getNature() == AttributeNature.TO_ONE
					|| idClassMemberStoresAssociation( idAttribute, idClassMember ) ) {
				return true;
			}
		}
		return false;
	}

	private boolean hasMapsIdAttribute(EntityTypeMetadata type) {
		for ( AttributeMetadata attribute : type.getAttributes() ) {
			if ( attribute.getMember().hasDirectAnnotationUsage( jakarta.persistence.MapsId.class ) ) {
				return true;
			}
		}
		return false;
	}

	private boolean hasWholeDerivedIdClass(NonAggregatedKeyMapping idMapping) {
		if ( idMapping.getIdClassType() == null || idMapping.getIdAttributes().size() != 1 ) {
			return false;
		}
		final AttributeMetadata idAttribute = idMapping.getIdAttributes().get( 0 );
		return idAttribute.getMember().hasDirectAnnotationUsage( Id.class )
				&& idAttribute.getNature() == AttributeNature.TO_ONE
				&& findIdClassMember( idMapping, idAttribute ) == null;
	}

	private void validateIdClassMembers(NonAggregatedKeyMapping idMapping, EntityTypeMetadata type) {
		final Set<String> idAttributeNames = new HashSet<>();
		for ( AttributeMetadata idAttribute : idMapping.getIdAttributes() ) {
			idAttributeNames.add( idAttribute.getName() );
			final MemberDetails idClassMember = findIdClassMember( idMapping, idAttribute );
			if ( idClassMember == null ) {
				throw new AnnotationException(
						"Property '" + idAttribute.getName()
								+ "' which do not match properties of the specified '@IdClass'"
				);
			}
			if ( isInverseOneToOneMember( idAttribute.getMember() ) && isInverseOneToOneMember( idClassMember ) ) {
				throwInverseOneToOneIdentifierException( idAttribute.getName() );
			}
			validateIdClassMemberType( idAttribute, idClassMember, type );
		}

		final Set<String> idClassMemberNames = new HashSet<>( idClassMemberNames( idMapping.getIdClassType() ) );
		for ( String idClassMemberName : idClassMemberNames ) {
			if ( !idAttributeNames.contains( idClassMemberName ) ) {
				final MemberDetails idClassMember = findIdClassMember( idMapping.getIdClassType(), idClassMemberName );
				final jakarta.persistence.OneToOne oneToOne = idClassMember == null
						? null
						: idClassMember.getDirectAnnotationUsage( jakarta.persistence.OneToOne.class );
				if ( oneToOne != null && !oneToOne.mappedBy().isEmpty() ) {
					throw new AnnotationException(
							"Property '" + idClassMemberName
									+ "' is the inverse side of a '@OneToOne' association and cannot be used as identifier"
					);
				}
				if ( idClassMember != null
						&& isNestedAssociationIdClassMember( idMapping, idClassMemberName, idClassMember ) ) {
					continue;
				}
				throw new AnnotationException(
						"Property '" + idClassMemberName
								+ "' belongs to an '@IdClass' but has no matching property in entity class"
				);
			}
		}
		validateExtraInverseOneToOneIdClassMembers( idMapping.getIdClassType(), idAttributeNames );
	}

	private boolean isNestedAssociationIdClassMember(
			NonAggregatedKeyMapping idMapping,
			String idClassMemberName,
			MemberDetails idClassMember) {
		for ( AttributeMetadata idAttribute : idMapping.getIdAttributes() ) {
			if ( idAttribute.getNature() != AttributeNature.TO_ONE ) {
				continue;
			}
			final MemberDetails associationIdClassMember = findIdClassMember( idMapping, idAttribute );
			if ( associationIdClassMember == null ) {
				continue;
			}
			final ClassDetails associationIdClassType = associationIdClassMember.getType().determineRawClass();
			if ( sameClass( associationIdClassType, idClassMember.getType().determineRawClass() )
					|| findIdClassMember( associationIdClassType, idClassMemberName ) != null ) {
				return true;
			}
		}
		return false;
	}

	private boolean sameClass(ClassDetails one, ClassDetails another) {
		return one != null
				&& another != null
				&& one.getClassName() != null
				&& one.getClassName().equals( another.getClassName() );
	}

	private void validateExtraInverseOneToOneIdClassMembers(ClassDetails idClassType, Set<String> idAttributeNames) {
		ClassDetails currentType = idClassType;
		while ( currentType != null && currentType != ClassDetails.OBJECT_CLASS_DETAILS ) {
			for ( MemberDetails field : currentType.getFields() ) {
				validateExtraInverseOneToOneIdClassMember( field, idAttributeNames );
			}
			for ( MethodDetails method : currentType.getMethods() ) {
				if ( method.getMethodKind() != MethodDetails.MethodKind.OTHER ) {
					validateExtraInverseOneToOneIdClassMember( method, idAttributeNames );
				}
			}
			currentType = currentType.getSuperClass();
		}
	}

	private void validateExtraInverseOneToOneIdClassMember(MemberDetails member, Set<String> idAttributeNames) {
		final String attributeName = member.resolveAttributeName();
		if ( attributeName != null && !idAttributeNames.contains( attributeName ) && isInverseOneToOneMember( member ) ) {
			throwInverseOneToOneIdentifierException( attributeName );
		}
	}

	private boolean isInverseOneToOneMember(MemberDetails member) {
		final jakarta.persistence.OneToOne oneToOne = member.getDirectAnnotationUsage( jakarta.persistence.OneToOne.class );
		return oneToOne != null && !oneToOne.mappedBy().isEmpty();
	}

	private void throwInverseOneToOneIdentifierException(String attributeName) {
		throw new AnnotationException(
				"Property '" + attributeName
						+ "' is the inverse side of a '@OneToOne' association and cannot be used as identifier"
		);
	}

	private void validateIdClassMemberType(
			AttributeMetadata idAttribute,
			MemberDetails idClassMember,
			EntityTypeMetadata type) {
		if ( isToOneMember( idClassMember )
				|| idAttribute.getNature() == AttributeNature.TO_ONE ) {
			return;
		}

		final String entityMemberType = idAttribute.getMember()
				.resolveRelativeType( type.getClassDetails() )
				.determineRawClass()
				.getClassName();
		final String idClassMemberType = idClassMember
				.getType()
				.determineRawClass()
				.getClassName();
		if ( !canonicalizePrimitiveTypeName( entityMemberType ).equals( canonicalizePrimitiveTypeName( idClassMemberType ) ) ) {
			throw new AnnotationException(
					"Property '" + idAttribute.getName()
							+ "' belongs to an '@IdClass' but doesn't match type of entity id member"
			);
		}
	}

	private String canonicalizePrimitiveTypeName(String typeName) {
		return switch ( typeName ) {
			case "boolean" -> Boolean.class.getName();
			case "char" -> Character.class.getName();
			case "byte" -> Byte.class.getName();
			case "short" -> Short.class.getName();
			case "int" -> Integer.class.getName();
			case "long" -> Long.class.getName();
			case "float" -> Float.class.getName();
			case "double" -> Double.class.getName();
			default -> typeName;
		};
	}

	private List<String> idClassMemberNames(ClassDetails idClassType) {
		final ArrayList<String> memberNames = new ArrayList<>();
		ClassDetails currentType = idClassType;
		while ( currentType != null && currentType != ClassDetails.OBJECT_CLASS_DETAILS ) {
			for ( MemberDetails field : currentType.getFields() ) {
				if ( !isIdClassMemberCandidate( field ) ) {
					continue;
				}
				final String attributeName = field.resolveAttributeName();
				if ( attributeName != null && !memberNames.contains( attributeName ) ) {
					memberNames.add( attributeName );
				}
			}
			for ( MethodDetails method : currentType.getMethods() ) {
				if ( !isIdClassMemberCandidate( method ) || method.getMethodKind() == MethodDetails.MethodKind.OTHER ) {
					continue;
				}
				final String attributeName = method.resolveAttributeName();
				if ( attributeName != null && !memberNames.contains( attributeName ) ) {
					memberNames.add( attributeName );
				}
			}
			currentType = currentType.getSuperClass();
		}
		return memberNames;
	}

	private MemberDetails findIdClassMember(ClassDetails idClassType, String attributeName) {
		ClassDetails currentType = idClassType;
		while ( currentType != null && currentType != ClassDetails.OBJECT_CLASS_DETAILS ) {
			for ( MemberDetails field : currentType.getFields() ) {
				if ( isIdClassMemberCandidate( field ) && attributeName.equals( field.resolveAttributeName() ) ) {
					return field;
				}
			}
			for ( MethodDetails method : currentType.getMethods() ) {
				if ( isIdClassMemberCandidate( method ) && attributeName.equals( method.resolveAttributeName() ) ) {
					return method;
				}
			}
			currentType = currentType.getSuperClass();
		}
		return null;
	}

	private boolean isIdClassMemberCandidate(MemberDetails member) {
		return ( member.isPersistable() || isToOneMember( member ) )
				&& !member.hasDirectAnnotationUsage( Transient.class );
	}

	private boolean isToOneMember(MemberDetails member) {
		return member.hasDirectAnnotationUsage( jakarta.persistence.ManyToOne.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToOne.class );
	}

	private IdentifierExtractionKind identifierExtractionKind(
			AttributeMetadata idAttribute,
			MemberDetails idClassMember,
			boolean wholeDerivedIdClass) {
		if ( idClassMemberStoresAssociation( idAttribute, idClassMember ) ) {
			return IdentifierExtractionKind.DIRECT;
		}
		if ( idAttribute.getNature() == AttributeNature.TO_ONE ) {
			return wholeDerivedIdClass
					? IdentifierExtractionKind.WHOLE_TARGET_ID
					: IdentifierExtractionKind.ASSOCIATION_TARGET_ID;
		}
		return IdentifierExtractionKind.DIRECT;
	}

	private boolean idClassMemberStoresAssociation(AttributeMetadata idAttribute, MemberDetails idClassMember) {
		if ( isToOneMember( idClassMember ) ) {
			return true;
		}
		return idAttribute.getNature() == AttributeNature.TO_ONE
				&& idAttribute.getMember().getType().determineRawClass()
						.getClassName()
						.equals( idClassMember.getType().determineRawClass().getClassName() );
	}

	private MemberDetails resolveIdClassMember(NonAggregatedKeyMapping idMapping, AttributeMetadata idAttribute) {
		final MemberDetails member = findIdClassMember( idMapping, idAttribute );
		if ( member != null ) {
			return member;
		}
		final ClassDetails idClassType = idMapping.getIdClassType();
		throw new MappingException(
				"Could not resolve IdClass member `" + idAttribute.getName() + "` on "
						+ idClassType.getClassName()
		);
	}

	private MemberDetails findIdClassMember(NonAggregatedKeyMapping idMapping, AttributeMetadata idAttribute) {
		ClassDetails idClassType = idMapping.getIdClassType();
		if ( idClassType == null ) {
			return idAttribute.getMember();
		}
		while ( idClassType != null && idClassType != ClassDetails.OBJECT_CLASS_DETAILS ) {
			for ( MemberDetails field : idClassType.getFields() ) {
				if ( isIdClassMemberCandidate( field ) && idAttribute.getName().equals( field.resolveAttributeName() ) ) {
					return field;
				}
			}
			for ( MemberDetails method : idClassType.getMethods() ) {
				if ( isIdClassMemberCandidate( method ) && idAttribute.getName().equals( method.resolveAttributeName() ) ) {
					return method;
				}
			}
			idClassType = idClassType.getSuperClass();
		}
		return null;
	}

	private EntityIdentifierBinding createAggregatedEntityIdentifierBinding(
			EntityTypeMetadata type,
			ComponentSource componentSource) {
		final EntityIdentifierBinding entityIdentifierBinding = new EntityIdentifierBinding(
				type,
				false,
				null
		);
		for ( ComponentSource.ComponentMember componentMember : componentSource.members() ) {
			if ( componentMember.member().hasDirectAnnotationUsage( org.hibernate.annotations.Parent.class ) ) {
				continue;
			}
			final IdentifierAttributeBinding attributeBinding = new IdentifierAttributeBinding(
					componentMember.attributeName(),
					componentMember.member(),
					componentSource.sourceMember(),
					IdentifierExtractionKind.DIRECT
			);
			entityIdentifierBinding.addAttribute( attributeBinding );
		}
		return entityIdentifierBinding;
	}
}
