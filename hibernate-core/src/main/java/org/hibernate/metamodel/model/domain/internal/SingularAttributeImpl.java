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
import org.hibernate.metamodel.model.domain.spi.ManagedTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.SingularAttributeImplementor;

/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class SingularAttributeImpl<D, J>
		extends AbstractAttribute<D, J>
		implements SingularAttributeImplementor<D, J>, Serializable {
	private final boolean isIdentifier;
	private final boolean isVersion;
	private final boolean isOptional;

	private final SimpleTypeImplementor<J> attributeType;

	// NOTE : delay access for timing reasons
	private final DelayedKeyTypeAccess graphKeyTypeAccess = new DelayedKeyTypeAccess();

	public SingularAttributeImpl(
			ManagedTypeImplementor<D> declaringType,
			String name,
			PersistentAttributeType attributeNature,
			SimpleTypeImplementor<J> attributeType,
			Member member,
			boolean isIdentifier,
			boolean isVersion,
			boolean isOptional) {
		super( declaringType, name, attributeNature, attributeType, member );
		this.isIdentifier = isIdentifier;
		this.isVersion = isVersion;
		this.isOptional = isOptional;

		this.attributeType = attributeType;
	}

	@Override
	public SimpleTypeImplementor<J> getValueGraphType() {
		return attributeType;
	}

	@Override
	public SimpleTypeImplementor<J> getKeyGraphType() {
		return graphKeyTypeAccess.get();
	}



	/**
	 * Subclass used to simplify instantiation of singular attributes representing an entity's
	 * identifier.
	 */
	public static class Identifier<D, J> extends SingularAttributeImpl<D, J> {
		public Identifier(
				ManagedTypeImplementor<D> declaringType,
				String name,
				SimpleTypeImplementor<J> attributeType,
				Member member,
				PersistentAttributeType attributeNature) {
			super( declaringType, name, attributeNature, attributeType, member, true, false, false );
		}
	}

	/**
	 * Subclass used to simply instantiation of singular attributes representing an entity's
	 * version.
	 */
	public static class Version<X,Y> extends SingularAttributeImpl<X,Y> {
		public Version(
				ManagedTypeImplementor<X> declaringType,
				String name,
				PersistentAttributeType attributeNature,
				SimpleTypeImplementor<Y> attributeType,
				Member member) {
			super( declaringType, name, attributeNature, attributeType, member, false, true, false );
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
	public SimpleTypeImplementor<J> getType() {
		return attributeType;
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
	public Class<J> getBindableJavaType() {
		return attributeType.getJavaType();
	}

	private class DelayedKeyTypeAccess implements Supplier<SimpleTypeImplementor<J>>, Serializable {
		private boolean resolved;
		private SimpleTypeImplementor<J> type;

		@Override
		public SimpleTypeImplementor<J> get() {
			if ( ! resolved ) {
				type = GraphHelper.resolveKeyTypeDescriptor( SingularAttributeImpl.this );
				resolved = true;
			}
			return type;
		}
	}
}
