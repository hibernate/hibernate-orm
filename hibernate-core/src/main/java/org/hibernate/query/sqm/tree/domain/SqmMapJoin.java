/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Map;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaMapJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.criteria.PathException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmMapJoin<O,K,V> extends AbstractSqmPluralJoin<O,Map<K,V>,V> implements JpaMapJoin<O,K,V> {
	@SuppressWarnings("WeakerAccess")
	public SqmMapJoin(
			SqmFrom<?,O> lhs,
			MapPersistentAttribute<O,K,V> pluralValuedNavigable,
			String alias,
			SqmJoinType sqmJoinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, pluralValuedNavigable, alias, sqmJoinType, fetched, nodeBuilder );
	}

	@Override
	public MapPersistentAttribute<O,K,V> getReferencedPathSource() {
		//noinspection unchecked
		return(MapPersistentAttribute) super.getReferencedPathSource();
	}

	@Override
	public JavaTypeDescriptor<V> getJavaTypeDescriptor() {
		return getNodeJavaTypeDescriptor();
	}

	@Override
	public MapPersistentAttribute<O,K,V> getModel() {
		return (MapPersistentAttribute<O, K, V>) super.getModel();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	@SuppressWarnings("unchecked")
	public SqmPath<K> key() {
		final SqmPathSource keyPathSource = getReferencedPathSource().getKeyPathSource();
		final NavigablePath navigablePath = getNavigablePath().append( keyPathSource.getPathName() );

		if ( keyPathSource.getSqmPathType() instanceof BasicDomainType ) {
			return new SqmBasicValuedSimplePath(
					navigablePath,
					keyPathSource,
					this,
					null
			);
		}

		if ( keyPathSource.getSqmPathType() instanceof EmbeddableDomainType ) {
			return new SqmEmbeddedValuedSimplePath(
					navigablePath,
					keyPathSource,
					this,
					null
			);
		}

		if ( keyPathSource.getSqmPathType() instanceof EntityDomainType ) {
			return new SqmEntityValuedSimplePath(
					navigablePath,
					keyPathSource,
					this,
					null
			);
		}

		throw new UnsupportedOperationException( "Unrecognized Map key descriptor : " + keyPathSource );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Path<V> value() {
		final SqmPathSource elementPathSource = getReferencedPathSource().getElementPathSource();
		final NavigablePath navigablePath = getNavigablePath().append( elementPathSource.getPathName() );

		if ( elementPathSource.getSqmPathType() instanceof BasicDomainType ) {
			return new SqmBasicValuedSimplePath(
					navigablePath,
					elementPathSource,
					this,
					null
			);
		}

		if ( elementPathSource.getSqmPathType() instanceof EmbeddableDomainType ) {
			return new SqmEmbeddedValuedSimplePath(
					navigablePath,
					elementPathSource,
					this,
					null
			);
		}

		if ( elementPathSource.getSqmPathType() instanceof EntityDomainType ) {
			return new SqmEntityValuedSimplePath(
					navigablePath,
					elementPathSource,
					this,
					null
			);
		}

		throw new UnsupportedOperationException( "Unrecognized Map value descriptor : " + elementPathSource );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Expression<Map.Entry<K, V>> entry() {
		return new SqmMapEntryReference( this, nodeBuilder() );
	}

	@Override
	public SqmMapJoin<O,K,V> on(JpaExpression<Boolean> restriction) {
		return (SqmMapJoin<O, K, V>) super.on( restriction );
	}

	@Override
	public SqmMapJoin<O,K,V> on(Expression<Boolean> restriction) {
		return (SqmMapJoin<O, K, V>) super.on( restriction );
	}

	@Override
	public SqmMapJoin<O,K,V> on(JpaPredicate... restrictions) {
		return (SqmMapJoin<O, K, V>) super.on( restrictions );
	}

	@Override
	public SqmMapJoin<O,K,V> on(Predicate... restrictions) {
		return (SqmMapJoin<O, K, V>) super.on( restrictions );
	}

	@Override
	public SqmMapJoin<O,K,V> correlateTo(JpaSubQuery<V> subquery) {
		return (SqmMapJoin<O, K, V>) super.correlateTo( subquery );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends V> SqmTreatedMapJoin<O,K,V,S> treatAs(Class<S> treatJavaType) throws PathException {
		final EntityDomainType<S> targetDescriptor = nodeBuilder().getDomainModel().entity( treatJavaType );
		return new SqmTreatedMapJoin( this, targetDescriptor, null );
	}
}
