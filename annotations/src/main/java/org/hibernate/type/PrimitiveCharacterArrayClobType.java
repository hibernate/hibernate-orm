//$Id$
package org.hibernate.type;


/**
 * Map a char[] to a Clob
 *
 * @author Emmanuel Bernard
 */
public class PrimitiveCharacterArrayClobType extends CharacterArrayClobType {
	public Class returnedClass() {
		return char[].class;
	}
}
