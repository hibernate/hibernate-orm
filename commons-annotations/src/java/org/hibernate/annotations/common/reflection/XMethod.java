//$Id$
package org.hibernate.annotations.common.reflection;

/**
 * Represent an invokable method
 * <p/>
 * The underlying layer does not guaranty that xProperty == xMethod
 * if the underlying artefact is the same
 * However xProperty.equals(xMethod) is supposed to return true
 *
 * @author Emmanuel Bernard
 */
public interface XMethod extends XMember {
}
