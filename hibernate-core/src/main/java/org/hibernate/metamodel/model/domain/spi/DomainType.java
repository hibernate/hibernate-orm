/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Encapsulates the type of any Navigable in the application's model (our runtime view of it).
 * <p/>
 * One aspect of a DomainType is to describe this model type in regards producing
 * and consuming SQM and SQL AST queries via ExpressableType
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface DomainType<J> extends ExpressableType<J> {
	JavaTypeDescriptor<J> getJavaTypeDescriptor();

	@Override
	default Class<J> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}
}
