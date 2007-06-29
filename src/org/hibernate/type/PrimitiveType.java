//$Id$
package org.hibernate.type;

import java.io.Serializable;

/**
 * Superclass of primitive / primitive wrapper types.
 * @author Gavin King
 */
public abstract class PrimitiveType extends ImmutableType implements LiteralType {

	public abstract Class getPrimitiveClass();

	public String toString(Object value) {
		return value.toString();
	}
	
	public abstract Serializable getDefaultValue();

}





