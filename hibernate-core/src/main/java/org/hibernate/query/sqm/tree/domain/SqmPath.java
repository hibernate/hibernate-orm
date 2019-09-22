/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.PathException;
import org.hibernate.query.SemanticException;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Models a reference to a part of the application's domain model as part of an SQM tree.
 *
 * This correlates roughly to the JPA Criteria notion of Path, hence the name.
 *
 * @author Steve Ebersole
 */
public interface SqmPath<T> extends SqmExpression<T>, SemanticPathPart, JpaPath<T> {

	/**
	 * Returns the NavigablePath.
	 */
	NavigablePath getNavigablePath();

	/**
	 * The path source that this path refers to (and that most likely
	 * created it).
	 *
	 * @see SqmPathSource#createSqmPath
	 */
	SqmPathSource<?> getReferencedPathSource();

	/**
	 * Retrieve the explicit alias, if one.  May return null
	 */
	String getExplicitAlias();

	/**
	 * Set the explicit alias for this path
	 */
	void setExplicitAlias(String explicitAlias);


	/**
	 * Get the left-hand side of this path - may be null, indicating a
	 * root, cross-join or entity-join
	 */
	SqmPath<?> getLhs();

	/**
	 * Returns an immutable List of implicit-join paths
	 */
	List<SqmPath<?>> getImplicitJoinPaths();

	/**
	 * Visit each implicit-join path relative to this path
	 */
	void visitImplicitJoinPaths(Consumer<SqmPath<?>> consumer);

	/**
	 * Register an implicit-join path relative to this path
	 */
	void registerImplicitJoinPath(SqmPath<?> path);

	/**
	 * This node's type is its "referenced path source"
	 */
	@Override
	SqmPathSource<T> getNodeType();

	@Override
	default void applyInferableType(SqmExpressable<?> type) {
		// do nothing
	}

	@Override
	default JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return getNodeType().getExpressableJavaTypeDescriptor();
	}

	@Override
	<S extends T> SqmTreatedPath<T,S> treatAs(Class<S> treatJavaType) throws PathException;

	@Override
	<S extends T> SqmTreatedPath<T,S> treatAs(EntityDomainType<S> treatTarget) throws PathException;

	default SqmRoot findRoot() {
		final SqmPath lhs = getLhs();
		if ( lhs != null ) {
			return lhs.findRoot();
		}

		throw new ParsingException( "Could not find root" );
	}

	@Override
	default SqmPath resolveIndexedAccess(
			SqmExpression selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new SemanticException( "Non-plural path [" + getNavigablePath() + "] cannot be index-accessed" );
	}
}
