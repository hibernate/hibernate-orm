/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.javassist;

/**
 * Interface introduced to the enhanced class in order to be able to
 * inject a {@link FieldHandler} to define the interception behavior.
 *
 * @author Muga Nishizawa
 */
public interface FieldHandled {
	/**
	 * Inject the field interception handler to be used.
	 *
	 * @param handler The field interception handler.
	 */
	public void setFieldHandler(FieldHandler handler);

	/**
	 * Access to the current field interception handler.
	 *
	 * @return The current field interception handler.
	 */
	public FieldHandler getFieldHandler();
}
