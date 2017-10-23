/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain;

import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Chris Cranford
 */
public interface JavaTypeMapping<T> {
	String getTypeName();

	JavaTypeDescriptor<T> resolveJavaTypeDescriptor(RuntimeModelCreationContext creationContext);
}
