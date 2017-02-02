/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.path;

import java.io.Serializable;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.EmbeddableType;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.PathSource;
import org.hibernate.query.criteria.internal.compile.RenderingContext;

/**
 * Models a path for a {@link SingularAttribute} generally obtained from a
 * {@link javax.persistence.criteria.Path#get(SingularAttribute)} call
 *
 * @author Steve Ebersole
 */
public class SingularAttributePath<X> extends AbstractPathImpl<X> implements Serializable {
	private final SingularAttribute<?,X> attribute;
	private final ManagedType<X> managedType;

	@SuppressWarnings({ "unchecked" })
	public SingularAttributePath(
			CriteriaBuilderImpl criteriaBuilder,
			Class<X> javaType,
			PathSource pathSource,
			SingularAttribute<?, X> attribute) {
		super( criteriaBuilder, javaType, pathSource );
		this.attribute = attribute;
		this.managedType = resolveManagedType( attribute );
	}

	private ManagedType<X> resolveManagedType(SingularAttribute<?, X> attribute) {
		if ( Attribute.PersistentAttributeType.BASIC == attribute.getPersistentAttributeType() ) {
			return null;
		}
		else if ( Attribute.PersistentAttributeType.EMBEDDED == attribute.getPersistentAttributeType() ) {
			return (EmbeddableType<X>) attribute.getType();
		}
		else {
			return (IdentifiableType<X>) attribute.getType();
		}
	}

	@Override
	public SingularAttribute<?, X> getAttribute() {
		return attribute;
	}

	@Override
	public Bindable<X> getModel() {
		return getAttribute();
	}

	@Override
	protected boolean canBeDereferenced() {
		return managedType != null;
	}

	@Override
	protected Attribute locateAttributeInternal(String attributeName) {
		final Attribute attribute = managedType.getAttribute( attributeName );
		// ManagedType.locateAttribute should throw exception rather than return
		// null, but just to be safe...
		if ( attribute == null ) {
			throw new IllegalArgumentException( "Could not resolve attribute named " + attributeName );
		}
		return attribute;
	}

	@Override
	public <T extends X> SingularAttributePath<T> treatAs(Class<T> treatAsType) {
		return new TreatedSingularAttributePath<T>( this, treatAsType );
	}

	public static class TreatedSingularAttributePath<T> extends SingularAttributePath<T> {
		private final SingularAttributePath<? super T> original;
		private final Class<T> treatAsType;

		@SuppressWarnings("unchecked")
		public TreatedSingularAttributePath(SingularAttributePath<? super T> original, Class<T> treatAsType) {
			super(
					original.criteriaBuilder(),
					treatAsType,
					original.getPathSource(),
					(SingularAttribute<?, T>) original.getAttribute()
			);
			this.original = original;
			this.treatAsType = treatAsType;
		}

		@Override
		public String getAlias() {
			return original.getAlias();
		}

		@Override
		public void prepareAlias(RenderingContext renderingContext) {
			// do nothing...
		}

		@Override
		public String render(RenderingContext renderingContext) {
			return "treat(" + original.render( renderingContext ) + " as " + treatAsType.getName() + ")";
		}
	}
}
