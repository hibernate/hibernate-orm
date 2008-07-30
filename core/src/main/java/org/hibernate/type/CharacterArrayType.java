/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
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
