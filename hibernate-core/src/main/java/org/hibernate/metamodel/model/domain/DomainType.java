/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Describes any type that occurs in the application's (mapped) domain model.  Also
 * acts as the base for Hibernate's extension of the JPA type system.
 *
 * One aspect of a DomainType is to describe this model type in regards producing
 * and consuming SQM and SQL AST queries via ExpressableType
 *
 * @apiNote The "real" JPA type system is more akin to Hibernate's {@link SimpleDomainType}.  This contract
 * represents a "higher level" construct including descriptors for collections (which JPA does not define)
 * as well as Hibernate-specific features (like dynamic models or ANY mappings).
 * <p/>
 * The `*DomainType` naming pattern is used to more easily (visually)
 * differentiate these extensions from the JPA ones in application use.
 *
 * @param <J> The Java type for this JPA Type
 *
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
