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

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
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
			SqmPathSource<?> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		super( referencedPathSource, nodeBuilder );
		this.navigablePath = navigablePath;
		this.lhs = lhs;
	}

	@SuppressWarnings("WeakerAccess")
	protected AbstractSqmPath(SqmPathSource<?> referencedPathSource, SqmPath lhs, NodeBuilder nodeBuilder) {
		this(
				lhs == null
						? new NavigablePath( referencedPathSource.getPathName() )
						: lhs.getNavigablePath().append( referencedPathSource.getPathName() ),
				referencedPathSource,
				lhs,
				nodeBuilder
		);
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
		return (Bindable<T>) getReferencedPathSource();
	}

	private SqmExpression pathTypeExpression;

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Class<? extends T>> type() {
		if ( pathTypeExpression == null ) {
			final DomainType sqmNodeType = getReferencedPathSource().getSqmNodeType();
			if ( sqmNodeType instanceof EntityDomainType ) {
				pathTypeExpression = new SqmBasicValuedSimplePath(
						getNavigablePath().append( "{type}" ),
						???,
						this,
						nodeBuilder()
				);
			}
			else {
				throw new ...
			}
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
					final SqmPathSource subNavigable = getReferencedPathSource().findSubPathSource( attributeName );

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

	private SqmPath createSingularPath(SqmPath lhs, SingularPersistentAttribute attribute) {
		final NavigablePath subNavPath = getNavigablePath().append( attribute.getPathName() );

		switch ( attribute.getAttributeClassification() ) {
			case BASIC: {
				return new SqmBasicValuedSimplePath( subNavPath, );
			}
			case EMBEDDED: {
				return new SqmEmbeddedValuedSimplePath( subNavPath, );
			}
			case ANY: {
				return new SqmAnyValuedSimplePath( subNavPath, );
			}
			case ONE_TO_ONE:
			case MANY_TO_ONE: {
				return new SqmEntityValuedSimplePath( subNavPath, );
			}
			default: {
				throw new UnsupportedOperationException(
						String.format(
								Locale.ROOT,
								"Cannot create SqmPath from singular attribute [%s#%s] - unknown classification : %s",
								attribute.getDeclaringType().getName(),
								attribute.getName(),
								attribute.getAttributeClassification()
						)
				);
			}
		}
	}

	private SqmPath createPluralPath(
			SqmPath lhs,
			PluralPersistentAttribute pluralAttribute) {
		return new SqmPluralValuedSimplePath(
				getNavigablePath().append( pluralAttribute.getPathName(),
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
				(pathSource, name) -> createPluralPath( pathSource, (MapPersistentAttribute) map )
		);
	}
}
