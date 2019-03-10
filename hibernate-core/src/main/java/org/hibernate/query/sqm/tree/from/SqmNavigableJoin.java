/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.SqmCreationHelper;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.domain.SqmAttributeReference;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * Models a join based on a mapped attribute reference.
 *
 * @author Steve Ebersole
 */
public class SqmNavigableJoin
		extends AbstractSqmJoin
		implements SqmQualifiedJoin {
	private static final Logger log = Logger.getLogger( SqmNavigableJoin.class );

	private final SqmFrom lhs;
	private final boolean fetched;

	private SqmPredicate onClausePredicate;

	public SqmNavigableJoin(
			String uid,
			SqmFrom lhs,
			NavigableContainer joinedNavigable,
			String alias,
			SqmJoinType joinType,
			boolean fetched,
			SqmCreationState creationState) {
		super(
				uid,
				SqmCreationHelper.buildSubNavigablePath(
						lhs.getNavigablePath(),
						joinedNavigable.getNavigableName(),
						alias
				),
				joinedNavigable,
				alias,
				joinType
		);
		this.lhs = lhs;
		this.fetched = fetched;
	}

	@Override
	public SqmFrom getLhs() {
		return lhs;
	}

	public SqmAttributeReference getAttributeReference() {
		return (SqmAttributeReference) getReferencedNavigable();
	}

	@Override
	public NavigableContainer getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	public Supplier<? extends NavigableContainer> getInferableType() {
		return this::getReferencedNavigable;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getReferencedNavigable().getJavaTypeDescriptor();
	}

	public boolean isFetched() {
		return fetched;
	}

	@Override
	public SqmPredicate getJoinPredicate() {
		return onClausePredicate;
	}

	public void setJoinPredicate(SqmPredicate predicate) {
		log.tracef(
				"Setting join predicate [%s] (was [%s])",
				predicate.toString(),
				this.onClausePredicate == null ? "<null>" : this.onClausePredicate.toString()
		);

		this.onClausePredicate = predicate;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitQualifiedAttributeJoinFromElement( this );
	}
}
