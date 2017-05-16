/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Encapsulates the type of any Navigable in the application's model (our runtime view of it).
 * <p/>
 * One aspect of a DomainType is to describe this model type in regards producing
 * and consuming SQM and SQL AST queries.
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface DomainType<J> extends ExpressableType<J> {
	// todo (6.0) : consider adding a `Expressable` type as a factory for Expression objects in regards to the domain model.
	//		e.g.,
	//		Expressable#createExpression(..)

	JavaTypeDescriptor<J> getJavaTypeDescriptor();
}
