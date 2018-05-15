/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.type.descriptor.java.internal.BooleanJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public interface SqmPredicate extends SqmVisitableNode, SqmTypedNode {
	@Override
	default JavaTypeDescriptor<Boolean> getJavaTypeDescriptor(){
		return BooleanJavaDescriptor.INSTANCE;
	}
}
