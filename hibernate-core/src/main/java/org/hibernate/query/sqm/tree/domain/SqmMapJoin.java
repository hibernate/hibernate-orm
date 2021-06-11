/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Map;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.PathException;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaMapJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmMapJoin<O, K, V>
		extends AbstractSqmPluralJoin<O, Map<K, V>, V>
		implements JpaMapJoin<O, K, V> {
	public SqmMapJoin(
			SqmFrom<?,O> lhs,
			MapPersistentAttribute<O, K, V> pluralValuedNavigable,
			String alias,
			SqmJoinType sqmJoinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, pluralValuedNavigable, alias, sqmJoinType, fetched, nodeBuilder );
	}

	@Override
	public MapPersistentAttribute<O, K, V> getReferencedPathSource() {
		//noinspection unchecked
		return(MapPersistentAttribute) super.getReferencedPathSource();
	}

	@Override
	public JavaTypeDescriptor<V> getJavaTypeDescriptor() {
		return getNodeJavaTypeDescriptor();
	}

	@Override
	public MapPersistentAttribute<O, K, V> getModel() {
		return (MapPersistentAttribute<O, K, V>) super.getModel();
	}

	@Override
	public MapPersistentAttribute<O, K, V> getAttribute() {
		//noinspection unchecked
		return (MapPersistentAttribute<O, K, V>) super.getAttribute();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	@SuppressWarnings("unchecked")
	public SqmPath<K> key() {
		final SqmPathSource keyPathSource = getReferencedPathSource().getKeyPathSource();
		final NavigablePath navigablePath = getNavigablePath().append( keyPathSource.getPathName() );

		final SqmPath<K> sqmPath;
		if ( keyPathSource.getSqmPathType() instanceof BasicDomainType ) {
			sqmPath = new SqmBasicValuedSimplePath(
					navigablePath,
					keyPathSource,
					this,
					null
			);
		}
		else if ( keyPathSource.getSqmPathType() instanceof EmbeddableDomainType ) {
			sqmPath = new SqmEmbeddedValuedSimplePath(
					navigablePath,
					keyPathSource,
					this,
					null
			);
		}
		else if ( keyPathSource.getSqmPathType() instanceof EntityDomainType ) {
			sqmPath = new SqmEntityValuedSimplePath(
					navigablePath,
					keyPathSource,
					this,
					null
			);
		}
		else {
			throw new UnsupportedOperationException( "Unrecognized Map key descriptor : " + keyPathSource );
		}
		registerReusablePath( sqmPath );
		return sqmPath;
	}

	@Override
	@SuppressWarnings("unchecked")
	public SqmPath<V> value() {
		final SqmPathSource elementPathSource = getReferencedPathSource().getElementPathSource();
		final NavigablePath navigablePath = getNavigablePath().append( elementPathSource.getPathName() );

		final SqmPath<V> sqmPath;
		if ( elementPathSource.getSqmPathType() instanceof BasicDomainType ) {
			sqmPath = new SqmBasicValuedSimplePath(
					navigablePath,
					elementPathSource,
					this,
					null
			);
		}
		else if ( elementPathSource.getSqmPathType() instanceof EmbeddableDomainType ) {
			sqmPath = new SqmEmbeddedValuedSimplePath(
					navigablePath,
					elementPathSource,
					this,
					null
			);
		}
		else if ( elementPathSource.getSqmPathType() instanceof EntityDomainType ) {
			sqmPath = new SqmEntityValuedSimplePath(
					navigablePath,
					elementPathSource,
					this,
					null
			);
		}
		else {
			throw new UnsupportedOperationException( "Unrecognized Map value descriptor : " + elementPathSource );
		}
		registerReusablePath( sqmPath );
		return sqmPath;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Expression<Map.Entry<K, V>> entry() {
		key();
		value();
		return new SqmMapEntryReference( this, nodeBuilder() );
	}

	@Override
	public SqmMapJoin<O, K, V> on(JpaExpression<Boolean> restriction) {
		return (SqmMapJoin<O, K, V>) super.on( restriction );
	}

	@Override
	public SqmMapJoin<O, K, V> on(Expression<Boolean> restriction) {
		return (SqmMapJoin<O, K, V>) super.on( restriction );
	}

	@Override
	public SqmMapJoin<O, K, V> on(JpaPredicate... restrictions) {
		return (SqmMapJoin<O, K, V>) super.on( restrictions );
	}

	@Override
	public SqmMapJoin<O, K, V> on(Predicate... restrictions) {
		return (SqmMapJoin<O, K, V>) super.on( restrictions );
	}

	@Override
	public SqmCorrelatedMapJoin<O, K, V> createCorrelation() {
		return new SqmCorrelatedMapJoin<>( this );
	}

	@Override
	public <S extends V> SqmTreatedMapJoin<O, K, V, S> treatAs(Class<S> treatJavaType) throws PathException {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends V> SqmTreatedMapJoin<O, K, V, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		//noinspection unchecked
		return new SqmTreatedMapJoin( this, treatTarget, null );
	}

	@Override
	public SqmAttributeJoin makeCopy(SqmCreationProcessingState creationProcessingState) {
		//noinspection unchecked
		return new SqmMapJoin(
				creationProcessingState.getPathRegistry().findFromByPath( getLhs().getNavigablePath() ),
				getReferencedPathSource(),
				getExplicitAlias(),
				getSqmJoinType(),
				isFetched(),
				nodeBuilder()
		);
	}
}
