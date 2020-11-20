/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;

/**
 * Represents a literal value in the sqm, e.g.<ul>
 *     <li>1</li>
 *     <li>'some string'</li>
 *     <li>some.JavaClass.CONSTANT</li>
 *     <li>some.JavaEnum.VALUE</li>
 *     <li>etc</li>
 * </ul>
 * @author Steve Ebersole
 */
public class SqmLiteral<T>
		extends AbstractSqmExpression<T>
		implements DomainResultProducer<T> {
	private final T value;

	public SqmLiteral(T value, SqmExpressable<T> inherentType, NodeBuilder nodeBuilder) {
		super( inherentType, nodeBuilder );
		this.value = value;
	}

	public T getLiteralValue() {
		return value;
	}

	@Override
	public <R> R accept(SemanticQueryWalker<R> walker) {
		return walker.visitLiteral( this );
	}

	@Override
	public String asLoggableText() {
		return "Literal( " + value + ")";
	}

}
