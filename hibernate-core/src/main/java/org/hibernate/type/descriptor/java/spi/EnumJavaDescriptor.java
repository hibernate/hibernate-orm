/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

/**
 * @author Steve Ebersole
 */
public interface EnumJavaDescriptor<E extends Enum> extends BasicJavaDescriptor<E> {
	Byte toOrdinal(E domainForm);

	String toName(E domainForm);

	E fromOrdinal(Byte relationalForm);

	E fromName(String relationalForm);
}
