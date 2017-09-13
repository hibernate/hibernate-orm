/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.domain.SqmAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

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
	private final boolean fetched;

	private SqmPredicate onClausePredicate;

	public SqmAttributeJoin(
			SqmFrom lhs,
			SqmAttributeReference attributeBinding,
			String uid,
			String alias,
			EntityDescriptor intrinsicSubclassIndicator,
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
		this.fetched = fetched;

		attributeBinding.injectExportedFromElement( this );
	}

	public SqmFrom getLhs() {
		return lhs;
	}

	public SqmAttributeReference getAttributeReference() {
		return attributeBinding;
	}

	@Override
	public SqmNavigableReference getNavigableReference() {
		return getAttributeReference();
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

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return attributeBinding.getJavaTypeDescriptor();
	}
}
