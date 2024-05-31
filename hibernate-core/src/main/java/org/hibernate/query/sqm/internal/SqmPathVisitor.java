/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.DiscriminatorSqmPath;
import org.hibernate.query.sqm.spi.BaseSemanticQueryWalker;
import org.hibernate.query.sqm.tree.domain.NonAggregatedCompositeSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmAnyValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;

/**
 * Generic {@link org.hibernate.query.sqm.SemanticQueryWalker} that applies the provided
 * {@link Consumer} to all {@link SqmPath paths} encountered during visitation.
 *
 * @author Marco Belladelli
 */
public class SqmPathVisitor extends BaseSemanticQueryWalker {
	private final Consumer<SqmPath<?>> pathConsumer;

	public SqmPathVisitor(Consumer<SqmPath<?>> pathConsumer) {
		this.pathConsumer = pathConsumer;
	}

	@Override
	public Object visitBasicValuedPath(SqmBasicValuedSimplePath<?> path) {
		pathConsumer.accept( path );
		return path;
	}

	@Override
	public Object visitEmbeddableValuedPath(SqmEmbeddedValuedSimplePath<?> path) {
		pathConsumer.accept( path );
		return path;
	}

	@Override
	public Object visitEntityValuedPath(SqmEntityValuedSimplePath<?> path) {
		pathConsumer.accept( path );
		return path;
	}

	@Override
	public Object visitAnyValuedValuedPath(SqmAnyValuedSimplePath<?> path) {
		pathConsumer.accept( path );
		return path;
	}

	@Override
	public Object visitQualifiedAttributeJoin(SqmAttributeJoin<?, ?> path) {
		pathConsumer.accept( path );
		return path;
	}

	@Override
	public Object visitTreatedPath(SqmTreatedPath<?, ?> path) {
		pathConsumer.accept( path );
		return path;
	}

	@Override
	public Object visitDiscriminatorPath(DiscriminatorSqmPath<?> path) {
		pathConsumer.accept( path );
		return path;
	}

	@Override
	public Object visitPluralValuedPath(SqmPluralValuedSimplePath<?> path) {
		pathConsumer.accept( path );
		return path;
	}

	@Override
	public Object visitNonAggregatedCompositeValuedPath(NonAggregatedCompositeSimplePath<?> path) {
		pathConsumer.accept( path );
		return path;
	}
}
