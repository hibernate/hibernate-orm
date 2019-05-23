/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.jpa;

import org.hibernate.query.criteria.JpaTupleElement;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;

/**
 * Base support for {@link JpaTupleElement} impls
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJpaTupleElement<T>
		extends AbstractSqmNode
		implements SqmVisitableNode, JpaTupleElement<T> {

	private SqmExpressable<T> expressableType;
	private String alias;

	protected AbstractJpaTupleElement(SqmExpressable<T> expressableType, NodeBuilder criteriaBuilder) {
		super( criteriaBuilder );

		setExpressableType( expressableType );
	}

	@SuppressWarnings("unused")
	protected AbstractJpaTupleElement(Class<T> javaType, NodeBuilder criteriaBuilder) {
		super( criteriaBuilder );

		if ( javaType != null ) {
			setJavaType( javaType );
		}
		else {
			setExpressableType( StandardSpiBasicTypes.OBJECT_TYPE );
		}
	}

	@Override
	public String getAlias() {
		return alias;
	}

	/**
	 * Protected access to set the alias.
	 */
	protected void setAlias(String alias) {
		this.alias = alias;
	}

	public SqmExpressable<T> getNodeType() {
		return expressableType;
	}

	protected final void setExpressableType(SqmExpressable<?> expressableType) {
		//noinspection unchecked
		this.expressableType = (SqmExpressable) expressableType;
	}

	/**
	 * Protected access to set the JavaTypeDescriptor via Java Class
	 */
	@SuppressWarnings({ "unchecked" })
	protected void setJavaType(Class targetType) {
		if ( targetType != null ) {
			setExpressableType(
					nodeBuilder().getDomainModel()
							.getTypeConfiguration()
							.standardExpressableTypeForJavaType( targetType )
			);
		}
		else {
			setExpressableType( StandardSpiBasicTypes.OBJECT_TYPE );
		}
	}

}
