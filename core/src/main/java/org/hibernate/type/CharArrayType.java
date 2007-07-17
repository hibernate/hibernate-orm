//$Id: $
package org.hibernate.type;

/**
 * put char[] into VARCHAR
 * @author Emmanuel Bernard
 */
public class CharArrayType extends AbstractCharArrayType {

	protected Object toExternalFormat(char[] chars) {
		return chars;
	}

	protected char[] toInternalFormat(Object chars) {
		return (char[]) chars;
	}

	public Class getReturnedClass() {
		return char[].class;
	}

	public String getName() { return "characters"; }
}
