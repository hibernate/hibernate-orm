/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.buildtime.spi;

/**
 * Contract describing the information Hibernate needs in terms of instrumenting
 * a class, either via ant task or dynamic classloader.
 *
 * @author Steve Ebersole
 */
public interface ClassDescriptor {
	/**
	 * The name of the class.
	 *
	 * @return The class name.
	 */
	public String getName();

	/**
	 * Determine if the class is already instrumented.
	 *
	 * @return True if already instrumented; false otherwise.
	 */
	public boolean isInstrumented();

	/**
	 * The bytes making up the class' bytecode.
	 *
	 * @return The bytecode bytes.
	 */
	public byte[] getBytes();
}
