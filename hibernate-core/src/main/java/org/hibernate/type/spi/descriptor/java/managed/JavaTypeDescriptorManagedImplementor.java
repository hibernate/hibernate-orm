/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java.managed;

import javax.persistence.metamodel.ManagedType;

import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public interface JavaTypeDescriptorManagedImplementor extends JavaTypeDescriptor, ManagedType {
	/**
	 * The descriptors for managed types are built iteratively.  Initially
	 * the descriptor state is empty and this method returns {@code false}.
	 * After the descriptor state is built completely, the instance is locked
	 * and this method returns {@code true}.
	 * <p/>
	 * Note that this "iterative initialization" is conducted via the
	 * {@link InitializationAccess} reference obtained via
	 * {@link #getInitializationAccess()}.  The InitializationAccess is only
	 * "valid" for use until the descriptor is fully initialized.
	 *
	 * @return Whether the descriptor is fully initialized.
	 */
	boolean isInitialized();

	JavaTypeDescriptorManagedImplementor getSupertype();

	InitializationAccess getInitializationAccess();
}
