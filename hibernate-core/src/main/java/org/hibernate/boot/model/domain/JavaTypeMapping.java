/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain;

import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Contract which describes the Java side of a mapping that can be later
 * resolved to a {@link JavaTypeDescriptor} during the runtime model creation process.
 *
 * @author Chris Cranford
 */
public interface JavaTypeMapping<T> {
	String getTypeName();

	/**
	 * @throws NotYetResolvedException Can potentially throw if JTD has not yet been resolved
	 */
	JavaTypeDescriptor<T> getJavaTypeDescriptor() throws NotYetResolvedException;

}
