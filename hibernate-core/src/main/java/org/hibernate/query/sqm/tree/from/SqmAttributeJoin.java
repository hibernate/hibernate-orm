/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.persister.queryable.spi.EntityValuedExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.domain.SqmAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

import org.jboss.logging.Logger;

/**
 * Models a join based on a mapped attribute reference.
 *
 * @author Steve Ebersole
 */
public class SqmAttributeJoin
		extends AbstractSqmJoin
		implements SqmQualifiedJoin {
	private static final Logger log = Logger.getLogger( SqmAttributeJoin.class );

	private final SqmFrom lhs;
	private final SqmAttributeReference attributeBinding;
	private final EntityValuedExpressableType intrinsicSubclassIndicator;
	private final boolean fetched;

	private SqmPredicate onClausePredicate;

	public SqmAttributeJoin(
			SqmFrom lhs,
			SqmAttributeReference attributeBinding,
			String uid,
			String alias,
			EntityValuedExpressableType intrinsicSubclassIndicator,
			SqmJoinType joinType,
			boolean fetched) {
		super(
				attributeBinding.getSourceReference().getExportedFromElement().getContainingSpace(),
				uid,
				alias,
				attributeBinding,
				intrinsicSubclassIndicator,
				joinType
		);
		this.lhs = lhs;

		this.attributeBinding = attributeBinding;
		this.intrinsicSubclassIndicator = intrinsicSubclassIndicator;
		this.fetched = fetched;

		attributeBinding.injectExportedFromElement( this );
	}

	public SqmFrom getLhs() {
		return lhs;
	}

	public SqmAttributeReference getAttributeBinding() {
		return attributeBinding;
	}

	@Override
	public SqmNavigableReference getBinding() {
		return getAttributeBinding();
	}

	@Override
	public EntityValuedExpressableType getIntrinsicSubclassIndicator() {
		return intrinsicSubclassIndicator;
	}

	public boolean isFetched() {
		return fetched;
	}

	@Override
	public SqmPredicate getOnClausePredicate() {
		return onClausePredicate;
	}

	public void setOnClausePredicate(SqmPredicate predicate) {
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
