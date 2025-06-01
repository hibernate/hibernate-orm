/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.lang.reflect.Member;

import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.internal.MetadataContext;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.internal.SqmMappingModelHelper;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularPersistentAttribute;
import org.hibernate.query.sqm.tree.expression.SqmSetReturningFunction;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFunctionJoin;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.descriptor.java.JavaType;

import static jakarta.persistence.metamodel.Bindable.BindableType.SINGULAR_ATTRIBUTE;
import static org.hibernate.query.sqm.spi.SqmCreationHelper.buildSubNavigablePath;
import static org.hibernate.query.sqm.spi.SqmCreationHelper.determineAlias;

/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class SingularAttributeImpl<D,J>
		extends AbstractAttribute<D,J,J>
		implements SqmSingularPersistentAttribute<D,J>, Serializable {
	private final boolean isIdentifier;
	private final boolean isVersion;
	private final boolean isOptional;

	private final SqmPathSource<J> sqmPathSource;

	public SingularAttributeImpl(
			ManagedDomainType<D> declaringType,
			String name,
			AttributeClassification attributeClassification,
			SqmDomainType<J> attributeType,
			JavaType<?> relationalJavaType,
			Member member,
			boolean isIdentifier,
			boolean isVersion,
			boolean isOptional,
			boolean isGeneric,
			MetadataContext metadataContext) {
		super(
				declaringType,
				name,
				attributeType.getExpressibleJavaType(),
				attributeClassification,
				attributeType,
				member
		);
		this.isIdentifier = isIdentifier;
		this.isVersion = isVersion;
		this.isOptional = isOptional;

		this.sqmPathSource = SqmMappingModelHelper.resolveSqmPathSource(
				name,
				this,
				attributeType,
				relationalJavaType,
				SINGULAR_ATTRIBUTE,
				isGeneric
		);
	}

	@Override
	public String getPathName() {
		return getName();
	}

	public JavaType<J> getExpressibleJavaType() {
		return sqmPathSource.getExpressibleJavaType();
	}

	@Override
	public SqmDomainType<J> getPathType() {
		return sqmPathSource.getPathType();
	}

	@Override
	public SqmDomainType<J> getValueGraphType() {
		return getPathType();
	}

	@Override
	public SimpleDomainType<?> getKeyGraphType() {
		return getType() instanceof IdentifiableDomainType<?> identifiableDomainType
				? identifiableDomainType.getIdType()
				: null;
	}

	@Override
	public SimpleDomainType<J> getType() {
		// TODO: very ugly and fragile, fix this
		return (SimpleDomainType<J>) sqmPathSource.getPathType();
	}

	@Override
	public Class<J> getBindableJavaType() {
//		return getJavaType();
		return sqmPathSource.getBindableJavaType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return sqmPathSource.findSubPathSource( name );
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name, boolean includeSubtypes) {
		return sqmPathSource.findSubPathSource( name, includeSubtypes );
	}

	@Override
	public SqmPathSource<J> getSqmPathSource() {
		return sqmPathSource;
	}

	@Override
	public SqmBindableType<J> getExpressible() {
		return sqmPathSource.getExpressible();
	}

	@Override
	public boolean isGeneric() {
		return sqmPathSource.isGeneric();
	}

	@Override
	public SqmJoin<D,J> createSqmJoin(
			SqmFrom<?,D> lhs,
			SqmJoinType joinType,
			String alias,
			boolean fetched,
			SqmCreationState creationState) {
		final NodeBuilder nodeBuilder = creationState.getCreationContext().getNodeBuilder();
		if ( getType() instanceof AnyMappingDomainType ) {
			throw new SemanticException( "An @Any attribute cannot be join fetched" );
		}
		else if ( sqmPathSource.getPathType() instanceof BasicPluralType<?,?> ) {
			final SqmSetReturningFunction<J> setReturningFunction =
					nodeBuilder.unnestArray( lhs.get( getName() ) );
			//noinspection unchecked
			final SqmFunctionJoin<J> join = new SqmFunctionJoin<>(
					createNavigablePath( lhs, alias ),
					setReturningFunction,
					true,
					setReturningFunction.getType(),
					alias,
					joinType,
					(SqmRoot<Object>) lhs
			);
			return (SqmJoin<D, J>) join;
		}
		else {
			return new SqmSingularJoin<>(
					lhs,
					this,
					alias,
					joinType,
					fetched,
					nodeBuilder
			);
		}
	}

	@Override
	public NavigablePath createNavigablePath(SqmPath<?> parent, String alias) {
		if ( parent == null ) {
			throw new IllegalArgumentException(
					"LHS cannot be null for a sub-navigable reference - " + getName()
			);
		}

		return buildSubNavigablePath( getParentNavigablePath( parent ), getName(), alias );
	}

	/**
	 * Subclass used to simplify instantiation of singular attributes representing an entity's
	 * identifier.
	 */
	public static class Identifier<D,J> extends SingularAttributeImpl<D,J> {
		public Identifier(
				ManagedDomainType<D> declaringType,
				String name,
				SqmDomainType<J> attributeType,
				Member member,
				AttributeClassification attributeClassification,
				boolean isGeneric,
				MetadataContext metadataContext) {
			super(
					declaringType,
					name,
					attributeClassification,
					attributeType,
					attributeType.getExpressibleJavaType(),
					member,
					true,
					false,
					false,
					isGeneric,
					metadataContext
			);
		}

		@Override
		public NavigablePath createNavigablePath(SqmPath<?> parent, String alias) {
			if ( parent == null ) {
				throw new IllegalArgumentException(
						"LHS cannot be null for a sub-navigable reference - " + getName()
				);
			}
			final SqmPathSource<?> parentPathSource = parent.getResolvedModel();
			final NavigablePath parentNavigablePath =
					parentPathSource instanceof PluralPersistentAttribute<?, ?, ?>
							? parent.getNavigablePath().append( CollectionPart.Nature.ELEMENT.getName() )
							: parent.getNavigablePath();
			if ( getDeclaringType() instanceof IdentifiableDomainType<?> declaringType
					&& !declaringType.hasSingleIdAttribute() ) {
				return new EntityIdentifierNavigablePath( parentNavigablePath, null )
						.append( getName(), determineAlias( alias ) );
			}
			else {
				return new EntityIdentifierNavigablePath( parentNavigablePath, determineAlias( alias ), getName() );
			}
		}
	}

	/**
	 * Subclass used to simply instantiation of singular attributes representing an entity's
	 * version.
	 */
	public static class Version<X,Y> extends SingularAttributeImpl<X,Y> {
		public Version(
				ManagedDomainType<X> declaringType,
				String name,
				AttributeClassification attributeClassification,
				SqmDomainType<Y> attributeType,
				Member member,
				MetadataContext metadataContext) {
			super(
					declaringType,
					name,
					attributeClassification,
					attributeType,
					attributeType.getExpressibleJavaType(),
					member,
					false,
					true,
					false,
					false,
					metadataContext
			);
		}
	}

	@Override
	public boolean isId() {
		return isIdentifier;
	}

	@Override
	public boolean isVersion() {
		return isVersion;
	}

	@Override
	public boolean isOptional() {
		return isOptional;
	}

	@Override
	public boolean isAssociation() {
		final PersistentAttributeType persistentAttributeType = getPersistentAttributeType();
		return persistentAttributeType == PersistentAttributeType.MANY_TO_ONE
			|| persistentAttributeType == PersistentAttributeType.ONE_TO_ONE;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public BindableType getBindableType() {
		return SINGULAR_ATTRIBUTE;
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		return sqmPathSource.createSqmPath( lhs, intermediatePathSource );
	}

	@Override
	public JavaType<?> getRelationalJavaType() {
		return sqmPathSource.getRelationalJavaType();
	}
}
