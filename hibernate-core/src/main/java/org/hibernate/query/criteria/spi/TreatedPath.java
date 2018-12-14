/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.Collection;
import java.util.Map;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.criteria.PathException;

/**
 * @author Steve Ebersole
 */
public class TreatedPath<T> extends AbstractPath<T> {
	private final PathImplementor<? super T> wrappedPath;
	private final EntityTypeDescriptor<T> treatType;

	public TreatedPath(
			PathImplementor<? super T> wrappedPath,
			EntityTypeDescriptor<T> treatType,
			CriteriaNodeBuilder nodeBuilder) {
		super( treatType, wrappedPath.getSource(), nodeBuilder );
		this.wrappedPath = wrappedPath;
		this.treatType = treatType;
	}

	public PathImplementor<? super T> getWrappedPath() {
		return wrappedPath;
	}

	public EntityTypeDescriptor<T> getTreatType() {
		return treatType;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return wrappedPath.getNavigablePath();
	}

	@Override
	public <X> PathSourceImplementor<X> getSource() {
		return wrappedPath.getSource();
	}

	@Override
	@SuppressWarnings("unchecked")
	public PathSourceImplementor asPathSource(String name) throws PathException {
		return wrappedPath.asPathSource( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	public PathImplementor treatAs(Class treatJavaType) throws PathException {
		final EntityTypeDescriptor treatTypeDescriptor = nodeBuilder().getSessionFactory()
				.getMetamodel()
				.getEntityDescriptor( treatJavaType );
		return new TreatedPath( wrappedPath, treatTypeDescriptor, nodeBuilder() );
	}

	@Override
	public Bindable<T> getModel() {
		return treatType;
	}


	protected PathImplementor getWrappedPathDetyped() {
		return wrappedPath;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y> PathImplementor<Y> get(String attributeName) {
		return getWrappedPathDetyped().get( attributeName );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y> PathImplementor<Y> get(SingularAttribute<? super T, Y> attribute) {
		return getWrappedPathDetyped().get( attribute );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E, C extends Collection<E>> ExpressionImplementor<C> get(PluralAttribute<T, C, E> collection) {
		return getWrappedPathDetyped().get( collection );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <K, V, M extends Map<K, V>> ExpressionImplementor<M> get(MapAttribute<T, K, V> map) {
		return getWrappedPathDetyped().get( map );
	}

	@Override
	public <R> R accept(CriteriaVisitor visitor) {
		return visitor.visitTreatedPath( this );
	}
}
