/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.criteria.JpaPredicate;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.type.descriptor.java.BooleanTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public interface SqmPredicate
		extends SqmVisitableNode, JpaPredicate, SqmExpression<Boolean>, DomainResultProducer<Boolean> {
	@Override
	default JavaTypeDescriptor<Boolean> getJavaTypeDescriptor(){
		return BooleanTypeDescriptor.INSTANCE;
	}

	@Override
	SqmPredicate not();

}
