package org.hibernate.bytecode.util;

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
