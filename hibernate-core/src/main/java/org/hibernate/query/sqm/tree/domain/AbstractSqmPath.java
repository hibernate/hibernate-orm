/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.IllegalPathUsageException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmPath<T> extends AbstractSqmExpression<T> implements SqmPath<T> {
	private final NavigablePath navigablePath;
	private final SqmPath lhs;

	/**
	 * Note that this field is only really used to support Criteria building.
	 * For HQL processing the {@link org.hibernate.query.hql.spi.SqmPathRegistry}
	 * serves the same purpose.
	 */
	private Map<String, SqmPath> attributePathRegistry;

	/**
	 * For HQL processing - used to track implicit-join paths relative to this
	 * path.  E.g., given `p.mate.mate` the SqmRoot identified by `p` would
	 * have an implicit-join for the `p.mate` path.  Note however that the SqmPath
	 * for `p.mate` would not have one for `p.mate.mate` *unless* `p.mate.mate` were
	 * de-referenced somewhere else in the query.
	 */
	private Map<String,SqmPath<?>> implicitJoinPaths;

	@SuppressWarnings("WeakerAccess")
	protected AbstractSqmPath(
			NavigablePath navigablePath,
			SqmPathSource<T> referencedPathSource,
			SqmPath<?> lhs,
			NodeBuilder nodeBuilder) {
		super( referencedPathSource, nodeBuilder );
		this.navigablePath = navigablePath;
		this.lhs = lhs;
	}

	@Override
	public String getMappingRole() {
		return navigablePath.getUnqualifiedFullPath();
	}

	@Override
	public SqmPathSource<T> getNodeType() {
		return (SqmPathSource<T>) super.getNodeType();
	}

	@Override
	public SqmPathSource<T> getReferencedPathSource() {
		return (SqmPathSource<T>) super.getNodeType();
	}

	@SuppressWarnings("WeakerAccess")
	protected AbstractSqmPath(SqmPathSource<T> referencedPathSource, SqmPath lhs, NodeBuilder nodeBuilder) {
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
	public List<SqmPath<?>> getImplicitJoinPaths() {
		if ( implicitJoinPaths == null ) {
			return Collections.emptyList();
		}

		return new ArrayList<>( implicitJoinPaths.values() );
	}

	@Override
	public void visitImplicitJoinPaths(Consumer<SqmPath<?>> consumer) {
		if ( implicitJoinPaths != null ) {
			implicitJoinPaths.values().forEach( consumer );
		}
	}

	@Override
	public void registerImplicitJoinPath(SqmPath<?> path) {
		assert path.getLhs() == this;

		if ( implicitJoinPaths == null ) {
			implicitJoinPaths = new HashMap<>();
		}

		final String relativeName = path.getNavigablePath().getLocalName();

		final SqmPath<?> previous = implicitJoinPaths.put( relativeName, path );
		if ( previous != null && previous != path ) {
			log.debugf(
					"Implicit-join path registration unexpectedly overrode previous registration - `%s`",
					relativeName
			);
		}
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
	public SqmPathSource<T> getModel() {
		return getReferencedPathSource();
	}

	private SqmExpression pathTypeExpression;

	@Override
	@SuppressWarnings("unchecked")
	public SqmExpression<Class<? extends T>> type() {
		if ( pathTypeExpression == null ) {
			final String discriminatorPathName = "{type}";
			final NavigablePath discriminatorNavigablePath = getNavigablePath().append( discriminatorPathName );

			final DomainType sqmNodeType = getReferencedPathSource().getSqmPathType();

			if ( sqmNodeType instanceof EntityDomainType ) {
				final SqmPathSource discriminatorPathSource = new SqmPathSource() {
					@Override
					public String getPathName() {
						return discriminatorPathName;
					}

					@Override
					public DomainType<?> getSqmPathType() {
						// the BasicType for Class?
						return null;
					}

					@Override
					public SqmPathSource<?> findSubPathSource(String name) {
						throw new IllegalPathUsageException( "Entity discriminator cannot be de-referenced" );
					}

					@Override
					public SqmPath createSqmPath(SqmPath lhs, SqmCreationState creationState) {
						return new SqmBasicValuedSimplePath( discriminatorNavigablePath, this, AbstractSqmPath.this, nodeBuilder() );
					}

					@Override
					public BindableType getBindableType() {
						return BindableType.SINGULAR_ATTRIBUTE;
					}

					@Override
					public Class getBindableJavaType() {
						return null;
					}

					@Override
					public JavaTypeDescriptor getExpressableJavaTypeDescriptor() {
						return null;
					}
				};

				pathTypeExpression = new SqmBasicValuedSimplePath(
						discriminatorNavigablePath,
						discriminatorPathSource,
						this,
						nodeBuilder()
				);
			}
			else {
				// todo (6.0) : not sure this is strictly true
				throw new UnsupportedOperationException( "SqmPath [" + getClass().getName() + "] cannot be typed" );
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
						return createSingularPath( (SingularPersistentAttribute) subNavigable );
					}
					else {
						assert subNavigable instanceof PluralPersistentAttribute;
						return createPluralPath( (PluralPersistentAttribute) subNavigable );
					}
				}
		);
	}

	@SuppressWarnings("unchecked")
	private SqmPath createSingularPath(SingularPersistentAttribute attribute) {
		final NavigablePath subNavPath = getNavigablePath().append( attribute.getPathName() );

		switch ( attribute.getAttributeClassification() ) {
			case BASIC: {
				return new SqmBasicValuedSimplePath( subNavPath, attribute, this, nodeBuilder() );
			}
			case EMBEDDED: {
				return new SqmEmbeddedValuedSimplePath( subNavPath, attribute, this, nodeBuilder() );
			}
			case ANY: {
				return new SqmAnyValuedSimplePath( subNavPath, attribute, this, nodeBuilder() );
			}
			case ONE_TO_ONE:
			case MANY_TO_ONE: {
				return new SqmEntityValuedSimplePath( subNavPath, attribute, this, nodeBuilder() );
			}
			default: {
				throw new UnsupportedOperationException(
						String.format(
								Locale.ROOT,
								"Cannot create SqmPath from singular attribute [%s#%s] - unknown classification : %s",
								attribute.getDeclaringType().getTypeName(),
								attribute.getName(),
								attribute.getAttributeClassification()
						)
				);
			}
		}
	}

	private SqmPath createPluralPath(PluralPersistentAttribute pluralAttribute) {
		return new SqmPluralValuedSimplePath(
				getNavigablePath().append( pluralAttribute.getPathName() ),
				pluralAttribute,
				this,
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
				(pathSource, name) -> createSingularPath( attribute )
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmPath get(PluralAttribute attribute) {
		return resolvePath(
				attribute.getName(),
				(pathSource, name) -> createPluralPath( (PluralPersistentAttribute) attribute )
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmPath get(MapAttribute map) {
		return resolvePath(
				map.getName(),
				(pathSource, name) -> createPluralPath( (MapPersistentAttribute) map )
		);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '(' + navigablePath.getFullPath() + ')';
	}
}
