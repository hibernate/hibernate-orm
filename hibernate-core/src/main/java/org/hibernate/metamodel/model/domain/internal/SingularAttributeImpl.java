/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.time.temporal.Temporal;

import jakarta.persistence.metamodel.BooleanAttribute;
import jakarta.persistence.metamodel.ComparableAttribute;
import jakarta.persistence.metamodel.NumericAttribute;
import jakarta.persistence.metamodel.TemporalAttribute;
import jakarta.persistence.metamodel.TextAttribute;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.domain.SqmSingularPersistentAttribute;
import org.hibernate.query.sqm.tree.expression.SqmSetReturningFunction;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFunctionJoin;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.descriptor.java.JavaType;

import static jakarta.persistence.metamodel.Bindable.BindableType.SINGULAR_ATTRIBUTE;
import static org.hibernate.metamodel.model.domain.internal.AttributeTypes.classification;
import static org.hibernate.query.sqm.internal.SqmMappingModelHelper.resolveSqmPathSource;
import static org.hibernate.query.sqm.spi.SqmCreationHelper.buildParentNavigablePath;
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
			boolean isGeneric) {
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

		this.sqmPathSource = resolveSqmPathSource(
				name,
				this,
				attributeType,
				relationalJavaType,
				SINGULAR_ATTRIBUTE,
				isGeneric
		);
	}

	public static <D, J> SingularAttributeImpl<D, J> create(
			ManagedDomainType<D> declaringType,
			String name,
			AttributeClassification attributeClassification,
			SqmDomainType<J> attributeType,
			JavaType<?> relationalJavaType,
			Member member,
			boolean isIdentifier,
			boolean isVersion,
			boolean isOptional,
			boolean isGeneric) {
		if ( !( attributeType instanceof BasicDomainType<?> ) ) {
			return new SingularAttributeImpl<>(
					declaringType,
					name,
					attributeClassification,
					attributeType,
					relationalJavaType,
					member,
					isIdentifier,
					isVersion,
					isOptional,
					isGeneric
			);
		}
		else {
			return switch ( classification( attributeType.getJavaType() ) ) {
				case TEXT -> //noinspection unchecked
						(SingularAttributeImpl<D, J>) new TextAttributeImpl<>(
								declaringType,
								name,
								attributeClassification,
								(SqmDomainType<String>) attributeType,
								relationalJavaType,
								member,
								isIdentifier,
								isVersion,
								isOptional,
								isGeneric
						);
				case BOOLEAN -> //noinspection unchecked
						(SingularAttributeImpl<D, J>) new BooleanAttributeImpl<>(
								declaringType,
								name,
								attributeClassification,
								(SqmDomainType<Boolean>) attributeType,
								relationalJavaType,
								member,
								isIdentifier,
								isVersion,
								isOptional,
								isGeneric
						);
				case NUMERIC -> //noinspection unchecked,rawtypes
						new NumericAttributeImpl(
								declaringType,
								name,
								attributeClassification,
								attributeType,
								relationalJavaType,
								member,
								isIdentifier,
								isVersion,
								isOptional,
								isGeneric
						);
				case TEMPORAL -> //noinspection unchecked,rawtypes
						new TemporalAttributeImpl(
								declaringType,
								name,
								attributeClassification,
								attributeType,
								relationalJavaType,
								member,
								isIdentifier,
								isVersion,
								isOptional,
								isGeneric
						);
				case COMPARABLE -> //noinspection unchecked,rawtypes
						new ComparableAttributeImpl(
								declaringType,
								name,
								attributeClassification,
								attributeType,
								relationalJavaType,
								member,
								isIdentifier,
								isVersion,
								isOptional,
								isGeneric
						);
				case BASIC ->
						new SingularAttributeImpl<>(
								declaringType,
								name,
								attributeClassification,
								attributeType,
								relationalJavaType,
								member,
								isIdentifier,
								isVersion,
								isOptional,
								isGeneric
						);
			};
		}
	}

	public static <D, J> SingularAttributeImpl<D, J> createIdentifier(
			ManagedDomainType<D> declaringType,
			String name,
			AttributeClassification attributeClassification,
			SqmDomainType<J> attributeType,
			Member member,
			boolean isGeneric) {
		if ( !( attributeType instanceof BasicDomainType<?> ) ) {
			return new Identifier<>(
					declaringType,
					name,
					attributeType,
					member,
					attributeClassification,
					isGeneric
			);
		}
		else {
			return switch ( classification( attributeType.getJavaType() ) ) {
				case TEXT -> //noinspection unchecked
						(SingularAttributeImpl<D, J>) new TextIdentifierAttributeImpl<>(
								declaringType,
								name,
								(SqmDomainType<String>) attributeType,
								member,
								attributeClassification,
								isGeneric
						);
				case BOOLEAN -> //noinspection unchecked
						(SingularAttributeImpl<D, J>) new BooleanIdentifierAttributeImpl<>(
								declaringType,
								name,
								(SqmDomainType<Boolean>) attributeType,
								member,
								attributeClassification,
								isGeneric
						);
				case NUMERIC -> //noinspection unchecked,rawtypes
						new NumericIdentifierAttributeImpl(
								declaringType,
								name,
								attributeType,
								member,
								attributeClassification,
								isGeneric
						);
				case TEMPORAL -> //noinspection unchecked,rawtypes
						new TemporalIdentifierAttributeImpl(
								declaringType,
								name,
								attributeType,
								member,
								attributeClassification,
								isGeneric
						);
				case COMPARABLE -> //noinspection unchecked,rawtypes
						new ComparableIdentifierAttributeImpl(
								declaringType,
								name,
								attributeType,
								member,
								attributeClassification,
								isGeneric
						);
				case BASIC ->
						new Identifier<>(
								declaringType,
								name,
								attributeType,
								member,
								attributeClassification,
								isGeneric
						);
			};
		}
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
	public @Nullable SqmPathSource<?> findSubPathSource(String name) {
		return sqmPathSource.findSubPathSource( name );
	}

	@Override
	public @Nullable SqmPathSource<?> findSubPathSource(String name, boolean includeSubtypes) {
		return sqmPathSource.findSubPathSource( name, includeSubtypes );
	}

	@Override
	public SqmPathSource<J> getSqmPathSource() {
		return sqmPathSource;
	}

	@Override
	public @NonNull SqmBindableType<J> getExpressible() {
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
			@Nullable String alias,
			boolean fetched,
			SqmCreationState creationState) {
		final var nodeBuilder = creationState.getCreationContext().getNodeBuilder();
		if ( sqmPathSource.getPathType() instanceof BasicPluralType<?,?> ) {
			final SqmSetReturningFunction<J> setReturningFunction =
					nodeBuilder.unnestArray( lhs.get( getName() ) );
			final var join = new SqmFunctionJoin<>(
					createNavigablePath( lhs, alias ),
					setReturningFunction,
					true,
					setReturningFunction.getType(),
					alias,
					joinType,
					(SqmFrom<?, Object>) lhs
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
	public NavigablePath createNavigablePath(SqmPath<?> parent, @Nullable String alias) {
		if ( parent == null ) {
			throw new IllegalArgumentException(
					"LHS cannot be null for a sub-navigable reference - " + getName()
			);
		}

		return buildSubNavigablePath( getParentNavigablePath( parent ), getName(), alias );
	}

	public static class ComparableAttributeImpl<D, J extends Comparable<? super J>>
			extends SingularAttributeImpl<D, J>
			implements ComparableAttribute<D, J> {
		public ComparableAttributeImpl(
				ManagedDomainType<D> declaringType,
				String name,
				AttributeClassification attributeClassification,
				SqmDomainType<J> attributeType,
				JavaType<?> relationalJavaType,
				Member member,
				boolean isIdentifier,
				boolean isVersion,
				boolean isOptional,
				boolean isGeneric) {
			super(
					declaringType,
					name,
					attributeClassification,
					attributeType,
					relationalJavaType,
					member,
					isIdentifier,
					isVersion,
					isOptional,
					isGeneric
			);
		}
	}

	public static class NumericAttributeImpl<D, J extends Number & Comparable<J>>
			extends ComparableAttributeImpl<D, J>
			implements NumericAttribute<D, J> {
		public NumericAttributeImpl(
				ManagedDomainType<D> declaringType,
				String name,
				AttributeClassification attributeClassification,
				SqmDomainType<J> attributeType,
				JavaType<?> relationalJavaType,
				Member member,
				boolean isIdentifier,
				boolean isVersion,
				boolean isOptional,
				boolean isGeneric) {
			super(
					declaringType,
					name,
					attributeClassification,
					attributeType,
					relationalJavaType,
					member,
					isIdentifier,
					isVersion,
					isOptional,
					isGeneric
			);
		}
	}

	public static class TextAttributeImpl<D>
			extends ComparableAttributeImpl<D, String>
			implements TextAttribute<D> {
		public TextAttributeImpl(
				ManagedDomainType<D> declaringType,
				String name,
				AttributeClassification attributeClassification,
				SqmDomainType<String> attributeType,
				JavaType<?> relationalJavaType,
				Member member,
				boolean isIdentifier,
				boolean isVersion,
				boolean isOptional,
				boolean isGeneric) {
			super(
					declaringType,
					name,
					attributeClassification,
					attributeType,
					relationalJavaType,
					member,
					isIdentifier,
					isVersion,
					isOptional,
					isGeneric
			);
		}
	}

	public static class TemporalAttributeImpl<D, J extends Temporal & Comparable<? super J>>
			extends ComparableAttributeImpl<D, J>
			implements TemporalAttribute<D, J> {
		public TemporalAttributeImpl(
				ManagedDomainType<D> declaringType,
				String name,
				AttributeClassification attributeClassification,
				SqmDomainType<J> attributeType,
				JavaType<?> relationalJavaType,
				Member member,
				boolean isIdentifier,
				boolean isVersion,
				boolean isOptional,
				boolean isGeneric) {
			super(
					declaringType,
					name,
					attributeClassification,
					attributeType,
					relationalJavaType,
					member,
					isIdentifier,
					isVersion,
					isOptional,
					isGeneric
			);
		}
	}

	public static class BooleanAttributeImpl<D>
			extends ComparableAttributeImpl<D, java.lang.Boolean>
			implements BooleanAttribute<D> {
		public BooleanAttributeImpl(
				ManagedDomainType<D> declaringType,
				String name,
				AttributeClassification attributeClassification,
				SqmDomainType<java.lang.Boolean> attributeType,
				JavaType<?> relationalJavaType,
				Member member,
				boolean isIdentifier,
				boolean isVersion,
				boolean isOptional,
				boolean isGeneric) {
			super(
					declaringType,
					name,
					attributeClassification,
					attributeType,
					relationalJavaType,
					member,
					isIdentifier,
					isVersion,
					isOptional,
					isGeneric
			);
		}
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
				boolean isGeneric) {
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
					isGeneric
			);
		}

		@Override
		public NavigablePath createNavigablePath(SqmPath<?> parent, @Nullable String alias) {
			if ( parent == null ) {
				throw new IllegalArgumentException(
						"LHS cannot be null for a sub-navigable reference - " + getName()
				);
			}
			final var parentNavigablePath = buildParentNavigablePath( parent, "" );
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

	public static class ComparableIdentifierAttributeImpl<D, J extends Comparable<? super J>>
			extends Identifier<D, J>
			implements ComparableAttribute<D, J> {
		public ComparableIdentifierAttributeImpl(
				ManagedDomainType<D> declaringType,
				String name,
				SqmDomainType<J> attributeType,
				Member member,
				AttributeClassification attributeClassification,
				boolean isGeneric) {
			super(
					declaringType,
					name,
					attributeType,
					member,
					attributeClassification,
					isGeneric
			);
		}
	}

	public static class NumericIdentifierAttributeImpl<D, J extends Number & Comparable<J>>
			extends ComparableIdentifierAttributeImpl<D, J>
			implements NumericAttribute<D, J> {
		public NumericIdentifierAttributeImpl(
				ManagedDomainType<D> declaringType,
				String name,
				SqmDomainType<J> attributeType,
				Member member,
				AttributeClassification attributeClassification,
				boolean isGeneric) {
			super(
					declaringType,
					name,
					attributeType,
					member,
					attributeClassification,
					isGeneric
			);
		}
	}

	public static class TextIdentifierAttributeImpl<D>
			extends ComparableIdentifierAttributeImpl<D, String>
			implements TextAttribute<D> {
		public TextIdentifierAttributeImpl(
				ManagedDomainType<D> declaringType,
				String name,
				SqmDomainType<String> attributeType,
				Member member,
				AttributeClassification attributeClassification,
				boolean isGeneric) {
			super(
					declaringType,
					name,
					attributeType,
					member,
					attributeClassification,
					isGeneric
			);
		}
	}

	public static class TemporalIdentifierAttributeImpl<D, J extends Temporal & Comparable<? super J>>
			extends ComparableIdentifierAttributeImpl<D, J>
			implements TemporalAttribute<D, J> {
		public TemporalIdentifierAttributeImpl(
				ManagedDomainType<D> declaringType,
				String name,
				SqmDomainType<J> attributeType,
				Member member,
				AttributeClassification attributeClassification,
				boolean isGeneric) {
			super(
					declaringType,
					name,
					attributeType,
					member,
					attributeClassification,
					isGeneric
			);
		}
	}

	public static class BooleanIdentifierAttributeImpl<D>
			extends ComparableIdentifierAttributeImpl<D, java.lang.Boolean>
			implements BooleanAttribute<D> {
		public BooleanIdentifierAttributeImpl(
				ManagedDomainType<D> declaringType,
				String name,
				SqmDomainType<java.lang.Boolean> attributeType,
				Member member,
				AttributeClassification attributeClassification,
				boolean isGeneric) {
			super(
					declaringType,
					name,
					attributeType,
					member,
					attributeClassification,
					isGeneric
			);
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
				Member member) {
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
					false
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
		final var persistentAttributeType = getPersistentAttributeType();
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
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, @Nullable SqmPathSource<?> intermediatePathSource) {
		return sqmPathSource.createSqmPath( lhs, intermediatePathSource );
	}

	@Override
	public JavaType<?> getRelationalJavaType() {
		return sqmPathSource.getRelationalJavaType();
	}
}
