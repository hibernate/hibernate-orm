/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.persister.SqlExpressableType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.PathException;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.domain.AbstractSqmFrom;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedRoot;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class SqmRoot<E> extends AbstractSqmFrom<E,E> implements JpaRoot<E> {
	public SqmRoot(
			EntityDomainType<E> entityType,
			String alias,
			NodeBuilder nodeBuilder) {
		super( entityType, alias, nodeBuilder );
	}

	@Override
	public SqmPath<?> getLhs() {
		// a root has no LHS
		return null;
	}

	@Override
	public SqmRoot findRoot() {
		return this;
	}

	@Override
	public EntityDomainType<E> getReferencedPathSource() {
		return (EntityDomainType<E>) super.getReferencedPathSource();
	}

	public String getEntityName() {
		return getReferencedPathSource().getHibernateEntityName();
	}

	@Override
	public JavaTypeDescriptor<E> getJavaTypeDescriptor() {
		return getReferencedPathSource().getExpressableJavaTypeDescriptor();
	}

	@Override
	public String toString() {
		return getExplicitAlias() == null
				? getEntityName()
				: getEntityName() + " as " + getExplicitAlias();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitRootPath( this );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public EntityDomainType<E> getManagedType() {
		return getReferencedPathSource();
	}

	@Override
	public EntityDomainType<E> getModel() {
		return getReferencedPathSource();
	}

	@Override
	public <S extends E> SqmTreatedRoot<E, S> treatAs(Class<S> treatJavaType) throws PathException {
		return (SqmTreatedRoot<E, S>) treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends E> SqmTreatedPath<E, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		return new SqmTreatedRoot<>( this, treatTarget, nodeBuilder() );
	}

	@Override
	public void visitJdbcTypes(Consumer<SqlExpressableType> action, TypeConfiguration typeConfiguration) {
		final String entityName = getReferencedPathSource().getHibernateEntityName();
		final EntityPersister entityDescriptor = typeConfiguration.getSessionFactory()
				.getMetamodel()
				.getEntityDescriptor( entityName );
		entityDescriptor.visitValueMappings(
				valueMapping -> valueMapping.getWriteable().visitJdbcTypes(
						action,
						Clause.IRRELEVANT,
						typeConfiguration
				)
		);
	}

	@Override
	public DomainResult<E> createDomainResult(
			int valuesArrayPosition,
			String resultVariable,
			DomainResultCreationState creationState) {
		final String entityName = getReferencedPathSource().getHibernateEntityName();
		final EntityPersister entityDescriptor = creationState.getSqlAstCreationState()
				.getCreationContext()
				.getDomainModel()
				.getEntityDescriptor( entityName );
		return entityDescriptor.createDomainResult(
				getNavigablePath(),
				valuesArrayPosition,
				resultVariable,
				creationState
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		final String entityName = getReferencedPathSource().getHibernateEntityName();
		final EntityPersister entityDescriptor = creationState.getSqlAstCreationState()
				.getCreationContext()
				.getDomainModel()
				.getEntityDescriptor( entityName );
		entityDescriptor.applySqlSelections( getNavigablePath(), creationState );

	}
}
