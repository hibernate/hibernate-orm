/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.Locale;
import java.util.function.Consumer;

import org.hibernate.HibernateException;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.PathException;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.Clause;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class SqmSingularJoin<O,T> extends AbstractSqmAttributeJoin<O,T> implements DomainResultProducer<T> {
	public SqmSingularJoin(
			SqmFrom<?,O> lhs,
			SingularPersistentAttribute<O, T> joinedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			NodeBuilder nodeBuilder) {
		super( lhs, joinedNavigable, alias, joinType, fetched, nodeBuilder );
	}

	@Override
	public SingularPersistentAttribute<O, T> getReferencedPathSource() {
		return (SingularPersistentAttribute<O, T>) super.getReferencedPathSource();
	}

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return getNodeJavaTypeDescriptor();
	}

	@Override
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(Class<S> treatJavaType) throws PathException {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends T> SqmTreatedSingularJoin<O,T,S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		//noinspection unchecked
		return new SqmTreatedSingularJoin( this, treatTarget, null );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"SqmSingularJoin(%s : %s)",
				getNavigablePath().getFullPath(),
				getReferencedPathSource().getPathName()
		);
	}

	@Override
	public SqmAttributeJoin makeCopy(SqmCreationProcessingState creationProcessingState) {
		//noinspection unchecked
		return new SqmSingularJoin(
				creationProcessingState.getPathRegistry().findFromByPath( getLhs().getNavigablePath() ),
				getReferencedPathSource(),
				getExplicitAlias(),
				getSqmJoinType(),
				isFetched(),
				nodeBuilder()
		);
	}


	@Override
	public void visitJdbcTypes(Consumer<JdbcMapping> action, TypeConfiguration typeConfiguration) {
		// todo (6.0) : better as some form of PersistentAttribute -> org.hibernate.persister.walking.spi.AttributeDefinition resolution

		final SingularPersistentAttribute<O, T> attribute = getReferencedPathSource();
		final ManagedDomainType<O> attributeType = (ManagedDomainType<O>) attribute.getType();

		if ( attributeType instanceof EntityDomainType<?> ) {
			final EntityDomainType<?> entityDomainType = (EntityDomainType<?>) attributeType;
			final String entityName = entityDomainType.getHibernateEntityName();
			final EntityPersister entityDescriptor = typeConfiguration.getSessionFactory()
					.getMetamodel()
					.getEntityDescriptor( entityName );
			entityDescriptor.visitSubParts(
					valueMapping -> valueMapping.visitJdbcTypes(
							action,
							Clause.IRRELEVANT,
							typeConfiguration
					),
					entityDescriptor
			);
		}
		else if ( attributeType instanceof EmbeddableDomainType<?> ) {
			throw new NotYetImplementedFor6Exception( getClass() );
		}
		else {
			throw new HibernateException( "Unexpected declaring " );
		}
	}
}
