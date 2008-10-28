package org.hibernate.annotations.common.reflection;

/**
 * A member which actually is a property (as per the JavaBean spec)
 * Note that the same underlying artefact can be represented as both
 * XProperty and XMethod
 * The underlying layer does not guaranty that xProperty == xMethod
 * if the underlying artefact is the same
 * However xProperty.equals(xMethod) is supposed to return true
 *
 * @author Paolo Perrotta
 * @author Davide Marchignoli
 * @author Emmanuel Bernard
 */
public interface XProperty extends XMember {

	/**
	 * Unqualify the getter name
	 */
	String getName();
}
