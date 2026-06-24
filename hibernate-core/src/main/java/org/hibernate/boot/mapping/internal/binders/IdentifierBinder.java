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
			final boolean idClassMemberIsToOne = idClassMember != null && isToOneMember( idClassMember );
			entityIdentifierBinding.addAttribute( new IdentifierAttributeBinding(
						idAttribute.getName(),
						member,
						idClassMember,
						idAttribute.getNature() == AttributeNature.TO_ONE || idClassMemberIsToOne
								? wholeDerivedIdClass
								? IdentifierExtractionKind.WHOLE_TARGET_ID
								: IdentifierExtractionKind.ASSOCIATION_TARGET_ID
								: IdentifierExtractionKind.DIRECT
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
			if ( idAttribute.getNature() != AttributeNature.BASIC || isToOneMember( idClassMember ) ) {
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
			final boolean associationIdentifier = idAttribute.getNature() == AttributeNature.TO_ONE
					|| isToOneMember( idClassMember );
			entityIdentifierBinding.addAttribute( new IdentifierAttributeBinding(
					idAttribute.getName(),
					idAttribute.getMember(),
					idClassMember,
					associationIdentifier
							? IdentifierExtractionKind.ASSOCIATION_TARGET_ID
							: IdentifierExtractionKind.DIRECT
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
			else if ( idAttribute.getNature() != AttributeNature.BASIC || isToOneMember( idClassMember ) ) {
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
			if ( idAttribute.getNature() == AttributeNature.TO_ONE || isToOneMember( idClassMember ) ) {
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
			validateIdClassMemberType( idAttribute, idClassMember, type );
		}

		final Set<String> idClassMemberNames = new HashSet<>( idClassMemberNames( idMapping.getIdClassType() ) );
		for ( String idClassMemberName : idClassMemberNames ) {
			if ( !idAttributeNames.contains( idClassMemberName ) ) {
				throw new AnnotationException(
						"Property '" + idClassMemberName
								+ "' belongs to an '@IdClass' but has no matching property in entity class"
				);
			}
		}
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
				if ( !field.isPersistable() ) {
					continue;
				}
				final String attributeName = field.resolveAttributeName();
				if ( attributeName != null && !memberNames.contains( attributeName ) ) {
					memberNames.add( attributeName );
				}
			}
			for ( MethodDetails method : currentType.getMethods() ) {
				if ( !method.isPersistable() || method.getMethodKind() == MethodDetails.MethodKind.OTHER ) {
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

	private boolean isToOneMember(MemberDetails member) {
		return member.hasDirectAnnotationUsage( jakarta.persistence.ManyToOne.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToOne.class );
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
				if ( field.isPersistable() && idAttribute.getName().equals( field.resolveAttributeName() ) ) {
					return field;
				}
			}
			for ( MemberDetails method : idClassType.getMethods() ) {
				if ( method.isPersistable() && idAttribute.getName().equals( method.resolveAttributeName() ) ) {
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
			final IdentifierAttributeBinding attributeBinding = new IdentifierAttributeBinding(
					componentMember.attributeName(),
					componentMember.member(),
					componentMember.member(),
					IdentifierExtractionKind.DIRECT
			);
			entityIdentifierBinding.addAttribute( attributeBinding );
		}
		return entityIdentifierBinding;
	}
}
