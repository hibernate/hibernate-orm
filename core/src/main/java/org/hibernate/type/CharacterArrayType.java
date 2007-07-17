//$Id: $
package org.hibernate.type;

import org.hibernate.HibernateException;

/**
 * Bridge Character[] and VARCHAR
 * @author Emmanuel Bernard
 */
public class CharacterArrayType extends AbstractCharArrayType {
	protected Object toExternalFormat(char[] chars) {
		if (chars == null) return null;
		Character[] characters = new Character[chars.length];
		for (int i = 0 ; i < chars.length ; i++) {
			characters[i] = new Character( chars[i] );
		}
		return characters;
	}

	protected char[] toInternalFormat(Object value) {
		if (value == null) return null;
		Character[] characters = (Character[]) value;
		char[] chars = new char[characters.length];
		for (int i = 0 ; i < characters.length ; i++) {
			if (characters[i] == null)
				throw new HibernateException("Unable to store an Character[] when one of its element is null");
			chars[i] = characters[i].charValue();
		}
		return chars;
	}

	public Class getReturnedClass() {
		return Character[].class;
	}

	public String getName() { return "wrapper-characters"; }
}
