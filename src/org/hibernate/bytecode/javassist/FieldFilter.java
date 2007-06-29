package org.hibernate.bytecode.javassist;

/**
 * Contract for deciding whether fields should be read and/or write intercepted.
 *
 * @author Muga Nishizawa
 */
public interface FieldFilter {
	/**
	 * Should the given field be read intercepted?
	 *
	 * @param desc
	 * @param name
	 * @return true if the given field should be read intercepted; otherwise
	 * false.
	 */
	boolean handleRead(String desc, String name);

	/**
	 * Should the given field be write intercepted?
	 *
	 * @param desc
	 * @param name
	 * @return true if the given field should be write intercepted; otherwise
	 * false.
	 */
	boolean handleWrite(String desc, String name);
}
