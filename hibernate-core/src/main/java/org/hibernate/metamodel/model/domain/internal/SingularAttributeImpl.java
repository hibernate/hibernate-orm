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
import org.hibernate.metamodel.ValueClassification;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

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
			SimpleDomainType<J> attributeType,
			Member member,
			boolean isIdentifier,
			boolean isVersion,
			boolean isOptional,
			NodeBuilder nodeBuilder) {
		super( declaringType, name, attributeType.getExpressableJavaTypeDescriptor(), attributeClassification, attributeType, member );
		this.isIdentifier = isIdentifier;
		this.isVersion = isVersion;
		this.isOptional = isOptional;


		this.sqmPathSource = DomainModelHelper.resolveSqmPathSource(
				determineValueClassification( attributeClassification ),
				name,
				attributeType,
				BindableType.SINGULAR_ATTRIBUTE,
				nodeBuilder
		);
	}

	private static ValueClassification determineValueClassification(AttributeClassification attributeClassification) {
		switch ( attributeClassification ) {
			case BASIC: {
				return ValueClassification.BASIC;
			}
			case ANY: {
				return ValueClassification.ANY;
			}
			case EMBEDDED: {
				return ValueClassification.EMBEDDED;
			}
			case ONE_TO_ONE:
			case MANY_TO_ONE: {
				return ValueClassification.ENTITY;
			}
			default: {
				throw new IllegalArgumentException(
						"Unrecognized AttributeClassification (for singular attribute): " + attributeClassification
				);
			}
		}
	}

	@Override
	public String getPathName() {
		return getName();
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

	public JavaTypeDescriptor<J> getExpressableJavaTypeDescriptor() {
		return sqmPathSource.getExpressableJavaTypeDescriptor();
	}

	@Override
	public Class<J> getBindableJavaType() {
		return getExpressableJavaTypeDescriptor().getJavaType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return sqmPathSource.findSubPathSource( name );
	}

	@Override
	public SqmAttributeJoin<D,J> createSqmJoin(
			SqmFrom lhs,
			SqmJoinType joinType,
			String alias,
			boolean fetched,
			SqmCreationState creationState) {
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
				NodeBuilder nodeBuilder) {
			super(
					declaringType,
					name,
					attributeClassification,
					attributeType,
					member,
					true,
					false,
					false,
					nodeBuilder
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
				SimpleDomainType<Y> attributeType,
				Member member,
				NodeBuilder nodeBuilder) {
			super(
					declaringType,
					name,
					attributeClassification,
					attributeType,
					member,
					false,
					true,
					false,
					nodeBuilder
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
	public SqmPath<J> createSqmPath(
			SqmPath lhs,
			SqmCreationState creationState) {
		return sqmPathSource.createSqmPath( lhs, creationState );
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
