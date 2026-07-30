/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;

import org.hibernate.AnnotationException;
import org.hibernate.boot.mapping.spi.EmbeddableTypeMetadata;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeVariableScope;

/// Standard [EmbeddableTypeMetadataImpl] implementation.
///
/// @since 9.0
/// @author Steve Ebersole
public final class EmbeddableTypeMetadataImpl implements EmbeddableTypeMetadata {
	private final ClassDetails classDetails;
	private final AccessType explicitAccessType;

	public EmbeddableTypeMetadataImpl(ClassDetails classDetails) {
		this.classDetails = classDetails;
		final Access access = classDetails.getDirectAnnotationUsage( Access.class );
		this.explicitAccessType = access == null ? null : access.value();
	}

	@Override
	public ClassDetails getClassDetails() {
		return classDetails;
	}

	@Override
	public AccessType getExplicitAccessType() {
		return explicitAccessType;
	}

	public EmbeddableUsageMetadataImpl resolveUsage(
			MemberDetails sourceMember,
			TypeVariableScope typeVariableScope,
			AccessType inheritedAccessType,
			CategorizationContext context) {
		return resolveUsage(
				sourceMember,
				typeVariableScope,
				inheritedAccessType,
				context,
				new LinkedHashSet<>()
		);
	}

	EmbeddableUsageMetadataImpl resolveUsage(
			MemberDetails sourceMember,
			TypeVariableScope typeVariableScope,
			AccessType inheritedAccessType,
			CategorizationContext context,
			Set<String> resolutionPath) {
		final String typeName = classDetails.getName();
		if ( !resolutionPath.add( typeName ) ) {
			throw new AnnotationException(
					"Recursive embeddable mapping detected involving '" + typeName
							+ "' in path " + resolutionPath
							+ " and inheritance hierarchy " + inheritancePath()
							+ " at '" + sourceMember.getDeclaringType().getName()
							+ "#" + sourceMember.resolveAttributeName() + "'"
			);
		}

		try {
			final AccessType accessType =
					explicitAccessType == null ? inheritedAccessType : explicitAccessType;
			final Map<String, PersistentMember> members = new LinkedHashMap<>();
			collectPersistentMembers( classDetails, accessType, context, members );
			final List<AttributeMetadataImplementor> attributes = new ArrayList<>( members.size() );
			for ( PersistentMember persistentMember : members.values() ) {
				final MemberDetails member = persistentMember.member();
				attributes.add(
						AttributeMetadataResolver.resolve(
								member,
								member.resolveRelativeType( typeVariableScope ),
								persistentMember.accessType(),
								context,
								resolutionPath
						)
				);
			}
			return new EmbeddableUsageMetadataImpl(
					this,
					sourceMember,
					typeVariableScope,
					accessType,
					attributes
			);
		}
		finally {
			resolutionPath.remove( typeName );
		}
	}

	private String inheritancePath() {
		final List<String> path = new ArrayList<>();
		for ( ClassDetails type = classDetails;
				type != null && type != ClassDetails.OBJECT_CLASS_DETAILS;
				type = type.getSuperClass() ) {
			path.add( type.getName() );
		}
		return path.toString();
	}

	private static void collectPersistentMembers(
			ClassDetails currentType,
			AccessType inheritedAccessType,
			CategorizationContext context,
			Map<String, PersistentMember> members) {
		final ClassDetails superClass = currentType.getSuperClass();
		if ( superClass != null && superClass != ClassDetails.OBJECT_CLASS_DETAILS ) {
			collectPersistentMembers( superClass, inheritedAccessType, context, members );
		}

		final Access access = currentType.getDirectAnnotationUsage( Access.class );
		final AccessType accessType = access == null ? inheritedAccessType : access.value();
		for ( MemberDetails member : context.getPersistentAttributeMemberResolver()
				.resolveAttributesMembers( currentType, accessType, ignored -> {
				}, context ) ) {
			members.put(
					member.resolveAttributeName(),
					new PersistentMember( member, accessType )
			);
		}
	}

	private record PersistentMember(MemberDetails member, AccessType accessType) {
	}
}
