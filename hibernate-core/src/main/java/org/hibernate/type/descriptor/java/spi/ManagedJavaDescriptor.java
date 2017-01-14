/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.spi;

/**
 * Contract for "managed types", which is taken from the JPA term for commonality between
 * entity, embeddable and "mapped superclass" types.
 *
 * @author Steve Ebersole
 */
public interface ManagedJavaDescriptor<J> extends JavaTypeDescriptor<J> {
	/**
	 * Obtain the super-type for this type.
	 * <p/>
	 * Note that for embeddables this is currently no-op, so at the moment really only
	 * IdentifiableType implementations can have a super-type.  But Hibernate does have plans
	 * to add support for embeddable inheritance, so this is here for future compatibility.
	 *
	 * @return The type's super-type
	 */
	ManagedJavaDescriptor<? super J> getSuperType();
}
