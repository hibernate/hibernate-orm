package org.hibernate.bytecode.javassist;

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
