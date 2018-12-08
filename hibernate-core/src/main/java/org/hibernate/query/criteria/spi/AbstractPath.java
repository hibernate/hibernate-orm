/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import javax.persistence.criteria.Expression;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeBasic;
import org.hibernate.metamodel.model.domain.spi.BagPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.ListPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SetPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.criteria.PathException;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractPath<T> extends AbstractExpression<T> implements PathImplementor<T> {
	private final Navigable<T> navigable;
	private final PathSourceImplementor<?> pathSource;

	private final NavigablePath navigablePath;

	private Map<String, PathImplementor<?>> attributePathRegistry;

	protected AbstractPath(Navigable<T> navigable, PathSourceImplementor<?> pathSource, CriteriaNodeBuilder criteriaBuilder) {
		super( navigable.getJavaTypeDescriptor(), criteriaBuilder );
		this.navigable = navigable;
		this.pathSource = pathSource;

		this.navigablePath = pathSource == null
				? new NavigablePath( navigable.getNavigableRole().getFullPath() )
				: pathSource.getNavigablePath().append( navigable.getNavigableName() );
	}

	public Navigable<T> getNavigable() {
		return navigable;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> PathSourceImplementor<X> getSource() {
		return (PathSourceImplementor<X>) pathSource;
	}

	private PathTypeExpression<T> pathTypeExpression;

	@Override
	@SuppressWarnings("unchecked")
	public Expression<Class<? extends T>> type() {
		if ( pathTypeExpression == null ) {
			pathTypeExpression = new PathTypeExpression<>( this, nodeBuilder() );
		}
		return (Expression) pathTypeExpression;
	}


	@Override
	@SuppressWarnings("unchecked")
	public <Y> PathImplementor<Y> get(String attributeName) {

		// todo (6.0) : this is similar to the idea of  creating an SqmExpression for a Navigable
		//		should make these stylistically consistent, either -
		//			1) add `Navigable#createCriteriaExpression` (ala, the exist `#createSqmExpression`)
		//			2) remove `Navigable#createSqmExpression` and use the approach used here instead.

		return resolvePath(
				attributeName,
				(pathSource, name) -> {
					final PersistentAttributeDescriptor attribute = pathSource.getManagedType().getAttribute( attributeName );

					if ( attribute instanceof SingularPersistentAttribute ) {
						return createSingularPath( pathSource, (SingularPersistentAttribute) attribute );
					}
					else {
						assert attribute instanceof PluralPersistentAttribute;
						return createPluralPath( pathSource, (PluralPersistentAttribute) attribute );
					}
				}
		);
	}

	@SuppressWarnings("unchecked")
	private <Y> PathImplementor<Y> createSingularPath(PathSourceImplementor<?> pathSource, SingularPersistentAttribute<?,Y> attribute) {
		if ( attribute instanceof SingularPersistentAttributeBasic ) {
			return new SingularPathBasic<>(
					pathSource,
					(SingularPersistentAttributeBasic) attribute,
					nodeBuilder()
			);
		}
		else {
			return new SingularPathManaged<>(
					pathSource,
					attribute,
					nodeBuilder()
			);
		}
	}

	@SuppressWarnings("unchecked")
	private <E> PluralPath<E> createPluralPath(
			PathSourceImplementor<T> pathSource,
			PluralPersistentAttribute<T,?,E> pluralAttribute) {
		if ( pluralAttribute instanceof MapPersistentAttribute ) {
			return createMapPath( pathSource, (MapPersistentAttribute) pluralAttribute );
		}
		else if ( pluralAttribute instanceof SetPersistentAttribute ) {
			return createSetPath( pathSource, (SetPersistentAttribute) pluralAttribute );
		}
		else if ( pluralAttribute instanceof ListPersistentAttribute ) {
			return createListPath( pathSource, (ListPersistentAttribute) pluralAttribute );
		}
		else if ( pluralAttribute instanceof BagPersistentAttribute ) {
			return getBagPath( pathSource, (BagPersistentAttribute) pluralAttribute );
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Unexpected PluralPersistentAttribute type [%s] encountered processing path [%s] -> [%s]",
						pluralAttribute.getClass().getName(),
						pathSource.getNavigablePath().getFullPath(),
						pluralAttribute.getAttributeName()
				)
		);
	}

	@SuppressWarnings("unchecked")
	private <K,V> PluralPath<V> createMapPath(
			PathSourceImplementor<T> pathSource,
			MapPersistentAttribute<T,K,V> attribute) {
		throw new NotYetImplementedFor6Exception();
	}

	private <E> PluralPath<T> createSetPath(
			PathSourceImplementor<T> pathSource,
			SetPersistentAttribute<T,E> attribute) {
		throw new NotYetImplementedFor6Exception();
	}

	private <E> PluralPath<E> createListPath(
			PathSourceImplementor<T> pathSource,
			ListPersistentAttribute<T, E> attribute) {
		throw new NotYetImplementedFor6Exception();
	}

	private <E> PluralPath<E> getBagPath(
			PathSourceImplementor<T> pathSource,
			BagPersistentAttribute<T,E> attribute) {
		throw new NotYetImplementedFor6Exception();
	}



	@SuppressWarnings("unchecked")
	private <Y> PathImplementor<Y> resolvePath(String attributeName, BiFunction<PathSourceImplementor, String, PathImplementor<Y>> creator) {
		final PathSourceImplementor<T> pathSource = asPathSource( attributeName );

		if ( attributePathRegistry == null ) {
			attributePathRegistry = new HashMap<>();
			final PathImplementor<Y> path = creator.apply( pathSource, attributeName );
			attributePathRegistry.put( attributeName, path );
			return path;
		}
		else {
			return (PathImplementor<Y>) attributePathRegistry.computeIfAbsent(
					attributeName,
					name -> creator.apply( pathSource, attributeName )
			);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <Y> PathImplementor<Y> get(SingularAttribute<? super T,Y> jpaAttribute) {
		final SingularPersistentAttribute<?,Y> attribute = (SingularPersistentAttribute) jpaAttribute;
		return resolvePath(
				attribute.getName(),
				(pathSource, name) -> createSingularPath( pathSource, attribute )
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E, C extends Collection<E>> ExpressionImplementor<C> get(PluralAttribute<T, C, E> attribute) {
		return resolvePath(
				attribute.getName(),
				(pathSource, name) -> createPluralPath( pathSource, (PluralPersistentAttribute) attribute )
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <K, V, M extends Map<K, V>> ExpressionImplementor<M> get(MapAttribute<T, K, V> map) {
		return resolvePath(
				map.getName(),
				(pathSource, name) -> createMapPath( pathSource, (MapPersistentAttribute) map )
		);
	}

	@Override
	public <S extends T> PathImplementor<S> treatAs(Class<S> treatJavaType) throws PathException {
		return new TreatedPath<>(
				this,
				nodeBuilder().getSessionFactory().getMetamodel().getEntityDescriptor( treatJavaType ),
				nodeBuilder()
		);
	}

	protected final PathException illegalDereference(String name) {
		return new PathException(
				String.format(
						"Illegal attempt to dereference path [%s] as the source for a sub-path [%s]",
						getNavigablePath(),
						name
				)
		);
	}

	protected PathException notTreatable() {
		return new PathException(
				String.format(
						"Illegal attempt to TREAT non-entity path [%s]",
						getNavigablePath()
				)
		);
	}
}
