/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.metamodel.model.domain.spi.BagPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.ListPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SetPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmPath<T> extends AbstractSqmExpression<T> implements SqmPath<T> {
	private final NavigablePath navigablePath;
	private final SqmPath lhs;

	private Map<String, SqmPath> attributePathRegistry;

	@SuppressWarnings("WeakerAccess")
	protected AbstractSqmPath(
			NavigablePath navigablePath,
			Navigable<T> navigable,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		super( navigable, nodeBuilder );
		this.navigablePath = navigablePath;
		this.lhs = lhs;
	}

	@SuppressWarnings("WeakerAccess")
	protected AbstractSqmPath(Navigable<T> navigable, SqmPath lhs, NodeBuilder nodeBuilder) {
		this(
				lhs == null
						? new NavigablePath( navigable.getNavigableRole().getFullPath() )
						: lhs.getNavigablePath().append( navigable.getNavigableName() ),
				navigable,
				lhs,
				nodeBuilder
		);
	}

	@Override
	public Navigable<T> getReferencedNavigable() {
		return (Navigable<T>) super.getExpressableType();
	}

	@Override
	public Navigable<T> getExpressableType() {
		return (Navigable<T>) super.getExpressableType();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public SqmPath<?> getLhs() {
		return lhs;
	}

	@Override
	public String getExplicitAlias() {
		return getAlias();
	}

	@Override
	public void setExplicitAlias(String explicitAlias) {
		setAlias( explicitAlias );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Bindable<T> getModel() {
		return (Bindable<T>) getReferencedNavigable();
	}

	private SqmExpression pathTypeExpression;

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Class<? extends T>> type() {
		if ( pathTypeExpression == null ) {
			pathTypeExpression = new SqmBasicValuedSimplePath(
					getNavigablePath().append( DiscriminatorDescriptor.NAVIGABLE_NAME ),
					sqmAs( EntityTypeDescriptor.class ).getHierarchy().getDiscriminatorDescriptor(),
					this,
					nodeBuilder()
			);
		}
		return pathTypeExpression;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmPath get(String attributeName) {

		// todo (6.0) : this is similar to the idea of  creating an SqmExpression for a Navigable
		//		should make these stylistically consistent, either -
		//			1) add `Navigable#createCriteriaExpression` (ala, the exist `#createSqmExpression`)
		//			2) remove `Navigable#createSqmExpression` and use the approach used here instead.

		return resolvePath(
				attributeName,
				(pathSource, name) -> {
					final Navigable subNavigable = sqmAs( NavigableContainer.class ).findNavigable( attributeName );

					if ( subNavigable instanceof SingularPersistentAttribute ) {
						return createSingularPath( pathSource, (SingularPersistentAttribute) subNavigable );
					}
					else {
						assert subNavigable instanceof PluralPersistentAttribute;
						return createPluralPath( pathSource, (PluralPersistentAttribute) subNavigable );
					}
				}
		);
	}

	@SuppressWarnings("unchecked")
	private SqmPath createSingularPath(SqmPath lhs, SingularPersistentAttribute attribute) {
		final NavigablePath subNavPath = getNavigablePath().append( attribute.getNavigableName() );

		if ( attribute instanceof BasicValuedNavigable ) {
			return new SqmBasicValuedSimplePath(
					subNavPath,
					(BasicValuedNavigable) attribute,
					this,
					null,
					nodeBuilder()
			);
		}

		if ( attribute instanceof EmbeddedValuedNavigable ) {
			return new SqmEmbeddedValuedSimplePath(
					subNavPath,
					(EmbeddedValuedNavigable) attribute,
					lhs,
					nodeBuilder()
			);
		}

		if ( attribute instanceof EntityValuedNavigable ) {
			return new SqmEntityValuedSimplePath(
					subNavPath,
					(EntityValuedNavigable) attribute,
					lhs,
					nodeBuilder()
			);
		}

		throw new UnsupportedOperationException( "Unknown TREAT source : " + attribute );
	}

	@SuppressWarnings("unchecked")
	private SqmPath createPluralPath(
			SqmPath lhs,
			PluralPersistentAttribute pluralAttribute) {
		if ( pluralAttribute instanceof MapPersistentAttribute ) {
			return createMapPath( lhs, (MapPersistentAttribute) pluralAttribute );
		}
		else if ( pluralAttribute instanceof SetPersistentAttribute ) {
			return createSetPath( lhs, (SetPersistentAttribute) pluralAttribute );
		}
		else if ( pluralAttribute instanceof ListPersistentAttribute ) {
			return createListPath( lhs, (ListPersistentAttribute) pluralAttribute );
		}
		else if ( pluralAttribute instanceof BagPersistentAttribute ) {
			return getBagPath( lhs, (BagPersistentAttribute) pluralAttribute );
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Unexpected PluralPersistentAttribute type [%s] encountered processing path [%s] -> [%s]",
						pluralAttribute.getClass().getName(),
						this.lhs.getNavigablePath().getFullPath(),
						pluralAttribute.getAttributeName()
				)
		);
	}

	@SuppressWarnings("unchecked")
	private <O,V> SqmPath<V> createMapPath(
			SqmPath<O> lhs,
			MapPersistentAttribute<O,?,V> attribute) {
		return new SqmPluralValuedSimplePath(
				getNavigablePath().append( attribute.getNavigableName() ),
				attribute,
				lhs,
				nodeBuilder()
		);
	}

	@SuppressWarnings("unchecked")
	private <O,E> SqmPath<E> createSetPath(
			SqmPath<O> lhs,
			SetPersistentAttribute<O,E> attribute) {
		return new SqmPluralValuedSimplePath(
				getNavigablePath().append( attribute.getNavigableName() ),
				attribute,
				lhs,
				nodeBuilder()
		);
	}

	@SuppressWarnings("unchecked")
	private <O,E> SqmPath<E> createListPath(
			SqmPath<O> lhs,
			ListPersistentAttribute<O,E> attribute) {
		return new SqmPluralValuedSimplePath(
				getNavigablePath().append( attribute.getNavigableName() ),
				attribute,
				lhs,
				nodeBuilder()
		);
	}

	@SuppressWarnings("unchecked")
	private <O,E> SqmPath<E> getBagPath(
			SqmPath<O> lhs,
			BagPersistentAttribute<O,E> attribute) {
		return new SqmPluralValuedSimplePath(
				getNavigablePath().append( attribute.getNavigableName() ),
				attribute,
				lhs,
				nodeBuilder()
		);
	}



	private SqmPath resolvePath(String attributeName, BiFunction<SqmPath, String, SqmPath> creator) {
		final SqmPath pathSource = getLhs();

		if ( attributePathRegistry == null ) {
			attributePathRegistry = new HashMap<>();
			final SqmPath path = creator.apply( pathSource, attributeName );
			attributePathRegistry.put( attributeName, path );
			return path;
		}
		else {
			return attributePathRegistry.computeIfAbsent(
					attributeName,
					name -> creator.apply( pathSource, attributeName )
			);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmPath get(SingularAttribute jpaAttribute) {
		final SingularPersistentAttribute attribute = (SingularPersistentAttribute) jpaAttribute;
		return resolvePath(
				attribute.getName(),
				(pathSource, name) -> createSingularPath( pathSource, attribute )
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmPath get(PluralAttribute attribute) {
		return resolvePath(
				attribute.getName(),
				(pathSource, name) -> createPluralPath( pathSource, (PluralPersistentAttribute) attribute )
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmPath get(MapAttribute map) {
		return resolvePath(
				map.getName(),
				(pathSource, name) -> createMapPath( pathSource, (MapPersistentAttribute) map )
		);
	}
}
