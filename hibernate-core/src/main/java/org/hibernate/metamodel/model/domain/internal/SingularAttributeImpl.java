/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.util.function.Supplier;

import org.hibernate.graph.spi.GraphHelper;
import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.internal.MetadataContext;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.internal.SqmMappingModelHelper;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.query.sqm.spi.SqmCreationHelper.buildSubNavigablePath;

/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class SingularAttributeImpl<D,J>
		extends AbstractAttribute<D,J,J>
		implements SingularPersistentAttribute<D,J>, Serializable {
	private final boolean isIdentifier;
	private final boolean isVersion;
	private final boolean isOptional;

	private final SqmPathSource<J> sqmPathSource;

	// NOTE : delay access for timing reasons
	private final DelayedKeyTypeAccess graphKeyTypeAccess = new DelayedKeyTypeAccess();

	public SingularAttributeImpl(
			ManagedDomainType<D> declaringType,
			String name,
			AttributeClassification attributeClassification,
			DomainType<J> attributeType,
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
				member,
				metadataContext
		);
		this.isIdentifier = isIdentifier;
		this.isVersion = isVersion;
		this.isOptional = isOptional;


		this.sqmPathSource = SqmMappingModelHelper.resolveSqmPathSource(
				name,
				this,
				attributeType,
				relationalJavaType,
				BindableType.SINGULAR_ATTRIBUTE,
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
	public SimpleDomainType<J> getSqmPathType() {
		//noinspection unchecked
		return (SimpleDomainType<J>) sqmPathSource.getSqmPathType();
	}

	@Override
	public SimpleDomainType<J> getValueGraphType() {
		return getSqmPathType();
	}

	@Override
	public SimpleDomainType<J> getKeyGraphType() {
		return graphKeyTypeAccess.get();
	}

	@Override
	public SimpleDomainType<J> getType() {
		return getSqmPathType();
	}

	@Override
	public Class<J> getBindableJavaType() {
		return getJavaType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return sqmPathSource.findSubPathSource( name );
	}

	@Override
	public SqmPathSource<J> getPathSource() {
		return this.sqmPathSource;
	}

	@Override
	public boolean isGeneric() {
		return sqmPathSource.isGeneric();
	}

	@Override
	public SqmAttributeJoin<D,J> createSqmJoin(
			SqmFrom lhs,
			SqmJoinType joinType,
			String alias,
			boolean fetched,
			SqmCreationState creationState) {
		if ( getType() instanceof AnyMappingDomainType ) {
			throw new SemanticException( "An @Any attribute cannot be join fetched" );
		}

		//noinspection unchecked
		return new SqmSingularJoin(
				lhs,
				this,
				alias,
				joinType,
				fetched,
				creationState.getCreationContext().getNodeBuilder()
		);
	}

	@Override
	public NavigablePath createNavigablePath(SqmPath parent, String alias) {
		if ( parent == null ) {
			throw new IllegalArgumentException(
					"`lhs` cannot be null for a sub-navigable reference - " + getName()
			);
		}
		final SqmPathSource<?> parentPathSource = parent.getResolvedModel();
		NavigablePath navigablePath = parent.getNavigablePath();
		if ( parentPathSource instanceof PluralPersistentAttribute<?, ?, ?> ) {
			navigablePath = navigablePath.append( CollectionPart.Nature.ELEMENT.getName() );
		}
		final DomainType<?> parentType = parentPathSource.getSqmPathType();
		if ( parentType != getDeclaringType() && parentType instanceof EntityDomainType &&
				( (EntityDomainType<?>) parentType ).findSingularAttribute( getName() ) == null ) {
			// If the parent path is an entity type which does not contain the joined attribute
			// add an implicit treat to the parent's navigable path
			navigablePath = navigablePath.treatAs( getDeclaringType().getTypeName() );
		}
		return buildSubNavigablePath( navigablePath, getName(), alias );
	}

	/**
	 * Subclass used to simplify instantiation of singular attributes representing an entity's
	 * identifier.
	 */
	public static class Identifier<D,J> extends SingularAttributeImpl<D,J> {
		public Identifier(
				ManagedDomainType<D> declaringType,
				String name,
				SimpleDomainType<J> attributeType,
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
		public NavigablePath createNavigablePath(SqmPath parent, String alias) {
			if ( parent == null ) {
				throw new IllegalArgumentException(
						"`lhs` cannot be null for a sub-navigable reference - " + getName()
				);
			}
			NavigablePath navigablePath = parent.getNavigablePath();
			if ( parent.getResolvedModel() instanceof PluralPersistentAttribute<?, ?, ?> ) {
				navigablePath = navigablePath.append( CollectionPart.Nature.ELEMENT.getName() );
			}
			if ( getDeclaringType() instanceof IdentifiableDomainType<?> ) {
				final IdentifiableDomainType<?> declaringType = (IdentifiableDomainType<?>) getDeclaringType();
				if ( !declaringType.hasSingleIdAttribute() ) {
					return new EntityIdentifierNavigablePath( navigablePath, null )
							.append( getName(), SqmCreationHelper.determineAlias( alias ) );
				}
			}
			return new EntityIdentifierNavigablePath( navigablePath, SqmCreationHelper.determineAlias( alias ), getName() );
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
				SimpleDomainType<Y> attributeType,
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
		return getPersistentAttributeType() == PersistentAttributeType.MANY_TO_ONE
				|| getPersistentAttributeType() == PersistentAttributeType.ONE_TO_ONE;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.SINGULAR_ATTRIBUTE;
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		return sqmPathSource.createSqmPath( lhs, intermediatePathSource );
	}

	@Override
	public JavaType<?> getRelationalJavaType() {
		return sqmPathSource.getRelationalJavaType();
	}

	private class DelayedKeyTypeAccess implements Supplier<SimpleDomainType<J>>, Serializable {
		private boolean resolved;
		private SimpleDomainType<J> type;

		@Override
		public SimpleDomainType<J> get() {
			if ( ! resolved ) {
				type = GraphHelper.resolveKeyTypeDescriptor( SingularAttributeImpl.this );
				resolved = true;
			}
			return type;
		}
	}
}
