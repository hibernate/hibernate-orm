/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.criteria.JpaCompoundSelection;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.expression.AbstractSqmExpression;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.persistence.criteria.Selection;

/**
 * @asciidoctor
 *
 * Models either a `Tuple` or `Object[]` selection as defined by the
 * JPA Criteria API.
 *
 * @see org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder#tuple(Selection[])
 * @see jakarta.persistence.criteria.CriteriaBuilder#tuple(jakarta.persistence.criteria.Selection[])
 *
 * @see org.hibernate.query.sqm.internal.SqmCriteriaNodeBuilder#array(Selection[])
 * @see jakarta.persistence.criteria.CriteriaBuilder#array(jakarta.persistence.criteria.Selection[])
 *
 * @see org.hibernate.query.sqm.tree.expression.SqmTuple
 *
 * @author Steve Ebersole
 */
public class SqmJpaCompoundSelection<T>
		extends AbstractSqmExpression<T>
		implements JpaCompoundSelection<T>, SqmExpressible<T> {

	// todo (6.0) : should this really be SqmExpressible?
	//		- seems like it ought to be limited to just `SqmSelectableNode`.
	//			otherwise why the distinction? why not just just re-use the same
	//			impl between this and `org.hibernate.query.sqm.tree.expression.SqmTuple`?
	//			Seems like either:
	//				a) this contract should not define support for being used out side the select clause,
	//					which would mean implementing `SqmSelectableNode`, but not `SqmExpressible` - so it
	//					would not be usable as a
	//				b)
	//
	// todo (6.0) : either way we need to make sure we should support whether "tuples" can be used "outside the select clause"
	//		- see `org.hibernate.jpa.spi.JpaCompliance#isJpaQueryComplianceEnabled` = the spec only defines
	//			support for using "compound selections" in the select clause.  In most cases Hibernate
	//			can support using tuples in other clauses.  If we keep the Easy way is to add a switch in creation of these
	//			whether `SqmJpaCompoundSelection` or `SqmTuple` is used based on `JpaCompliance#isJpaQueryComplianceEnabled`

	private final List<SqmSelectableNode<?>> selectableNodes;
	private final JavaType<T> javaType;

	public SqmJpaCompoundSelection(
			List<SqmSelectableNode<?>> selectableNodes,
			JavaType<T> javaType,
			NodeBuilder criteriaBuilder) {
		super( null, criteriaBuilder );
		this.selectableNodes = selectableNodes;
		this.javaType = javaType;

		setExpressibleType( this );
	}

	@Override
	public SqmJpaCompoundSelection<T> copy(SqmCopyContext context) {
		final SqmJpaCompoundSelection<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final List<SqmSelectableNode<?>> selectableNodes = new ArrayList<>( this.selectableNodes.size() );
		for ( SqmSelectableNode<?> selectableNode : this.selectableNodes ) {
			selectableNodes.add( selectableNode.copy( context ) );
		}
		return context.registerCopy(
				this,
				new SqmJpaCompoundSelection<>(
						selectableNodes,
						javaType,
						nodeBuilder()
				)
		);
	}

	@Override
	public JavaType<T> getJavaTypeDescriptor() {
		return javaType;
	}

	@Override
	public JavaType<T> getExpressibleJavaType() {
		return getJavaTypeDescriptor();
	}

	@Override
	public Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaTypeClass();
	}

	@Override
	public Class<T> getBindableJavaType() {
		return getJavaType();
	}

	@Override
	public List<SqmSelectableNode<?>> getSelectionItems() {
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
	public void appendHqlString(StringBuilder sb) {
		selectableNodes.get( 0 ).appendHqlString( sb );
		for ( int i = 1; i < selectableNodes.size(); i++ ) {
			sb.append(", ");
			selectableNodes.get( i ).appendHqlString( sb );
		}
	}

	@Override
	public void visitSubSelectableNodes(Consumer<SqmSelectableNode<?>> jpaSelectionConsumer) {
		selectableNodes.forEach( jpaSelectionConsumer );
	}

	@Override
	public DomainType<T> getSqmType() {
		return null;
	}
}
