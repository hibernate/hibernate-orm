/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.List;

import javax.persistence.criteria.Selection;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @asciidoctor
 *
 * Models either a `Tuple` or `Object[]` selection as defined by the
 * JPA Criteria API.
 *
 * @see org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder#tuple(Selection[])
 * @see javax.persistence.criteria.CriteriaBuilder#tuple(javax.persistence.criteria.Selection[])
 *
 * @see org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder#array(Selection[])
 * @see javax.persistence.criteria.CriteriaBuilder#array(javax.persistence.criteria.Selection[])
 *
 * @see org.hibernate.query.sqm.tree.expression.SqmTuple
 *
 * @author Steve Ebersole
 */
public class SqmJpaCompoundSelection<T>
		extends AbstractSqmExpression<T>
		implements JpaCompoundSelection<T>, SqmExpressable<T>, DomainResultProducer<T> {

	// todo (6.0) : should this really be SqmExpressable?
	//		- seems like it ought to be limited to just `SqmSelectableNode`.
	//			otherwise why the distinction? why not just just re-use the same
	//			impl between this and `org.hibernate.query.sqm.tree.expression.SqmTuple`?
	//			Seems like either:
	//				a) this contract should not define support for being used out side the select clause,
	//					which would mean implementing `SqmSelectableNode`, but not `SqmExpressable` - so it
	//					would not be usable as a
	//				b)
	//
	// todo (6.0) : either way we need to make sure we should support whether "tuples" can be used "outside the select clause"
	//		- see `org.hibernate.jpa.spi.JpaCompliance#isJpaQueryComplianceEnabled` = the spec only defines
	//			support for using "compound selections" in the select clause.  In most cases Hibernate
	//			can support using tuples in other clauses.  If we keep the Easy way is to add a switch in creation of these
	//			whether `SqmJpaCompoundSelection` or `SqmTuple` is used based on `JpaCompliance#isJpaQueryComplianceEnabled`

	private final List<SqmSelectableNode<?>> selectableNodes;
	private final JavaTypeDescriptor<T> javaTypeDescriptor;

	public SqmJpaCompoundSelection(
			List<SqmSelectableNode<?>> selectableNodes,
			JavaTypeDescriptor<T> javaTypeDescriptor,
			NodeBuilder criteriaBuilder) {
		super( null, criteriaBuilder );
		this.selectableNodes = selectableNodes;
		this.javaTypeDescriptor = javaTypeDescriptor;

		setExpressableType( this );
	}

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public JavaTypeDescriptor<T> getExpressableJavaTypeDescriptor() {
		return getJavaTypeDescriptor();
	}

	@Override
	public Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		return selectableNodes;
	}

	@Override
	public JpaSelection<T> alias(String name) {
		return this;
	}

	@Override
	public String getAlias() {
		return null;
	}

	@Override
	public boolean isCompoundSelection() {
		return true;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitJpaCompoundSelection( this );
	}

	@Override
	public DomainResultProducer<T> getDomainResultProducer() {
		// could technically return an array I guess.  See `SqmTuple`
		throw new UnsupportedOperationException(  );
	}
}
