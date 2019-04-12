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

import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.MapPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaMapJoin;
import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.criteria.JpaSubQuery;
import org.hibernate.query.criteria.PathException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmMapJoin<O,K,V> extends AbstractSqmPluralJoin<O,Map<K,V>,V> implements JpaMapJoin<O,K,V> {
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
	public MapPersistentAttribute getReferencedNavigable() {
		return(MapPersistentAttribute) super.getReferencedNavigable();
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
		final CollectionIndex mapKeyDescriptor = getReferencedNavigable().getCollectionDescriptor().getIndexDescriptor();
		final NavigablePath navigablePath = getNavigablePath().append( mapKeyDescriptor.getNavigableName() );

		if ( mapKeyDescriptor instanceof BasicValuedNavigable ) {
			return new SqmBasicValuedSimplePath(
					navigablePath,
					(BasicValuedNavigable) mapKeyDescriptor,
					this,
					null
			);
		}

		if ( mapKeyDescriptor instanceof EmbeddedValuedNavigable ) {
			return new SqmEmbeddedValuedSimplePath(
					navigablePath,
					(EmbeddedValuedNavigable) mapKeyDescriptor,
					this,
					null
			);
		}

		if ( mapKeyDescriptor instanceof EntityValuedNavigable ) {
			return new SqmEntityValuedSimplePath(
					navigablePath,
					(EntityValuedNavigable) mapKeyDescriptor,
					this,
					null
			);
		}

		throw new UnsupportedOperationException( "Unrecognized Map key descriptor : " + mapKeyDescriptor );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Path<V> value() {
		final CollectionElement valueDescriptor = getReferencedNavigable().getCollectionDescriptor().getElementDescriptor();
		final NavigablePath navigablePath = getNavigablePath().append( valueDescriptor.getNavigableName() );

		if ( valueDescriptor instanceof BasicValuedNavigable ) {
			return new SqmBasicValuedSimplePath(
					navigablePath,
					(BasicValuedNavigable) valueDescriptor,
					this,
					null
			);
		}

		if ( valueDescriptor instanceof EmbeddedValuedNavigable ) {
			return new SqmEmbeddedValuedSimplePath(
					navigablePath,
					(EmbeddedValuedNavigable) valueDescriptor,
					this,
					null
			);
		}

		if ( valueDescriptor instanceof EntityValuedNavigable ) {
			return new SqmEntityValuedSimplePath(
					navigablePath,
					(EntityValuedNavigable) valueDescriptor,
					this,
					null
			);
		}

		throw new UnsupportedOperationException( "Unrecognized Map value descriptor : " + valueDescriptor );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Expression<Map.Entry<K, V>> entry() {
		return new SqmMapEntryReference<>(
				this,
				(BasicJavaDescriptor) nodeBuilder().getDomainModel()
						.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( Map.Entry.class ),
				nodeBuilder()
		);
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
		final EntityTypeDescriptor<S> targetDescriptor = nodeBuilder().getDomainModel().entity( treatJavaType );
		return new SqmTreatedMapJoin( this, targetDescriptor, null );
	}
}
