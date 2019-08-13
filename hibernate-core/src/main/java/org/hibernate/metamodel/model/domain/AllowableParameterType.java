/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.Incubating;
import org.hibernate.query.Query;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Specialization of DomainType for types that can be used as {@link Query} parameter bind values.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface AllowableParameterType<J> extends SimpleDomainType<J> {
	JavaTypeDescriptor<J> getExpressableJavaTypeDescriptor();
}
