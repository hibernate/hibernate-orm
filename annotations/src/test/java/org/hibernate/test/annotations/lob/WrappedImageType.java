//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
package org.hibernate.test.annotations.lob;

import org.hibernate.type.ImageType;

/**
 * A type that maps an SQL LONGVARBINARY to Java Byte[].
 * 
 * @author Strong Liu
 */
public class WrappedImageType extends ImageType{
	public Class getReturnedClass() {
		return Byte[].class;
	}

	protected Object toExternalFormat(byte[] bytes) {
		if(bytes==null)return null;
		return wrapPrimitive(bytes);
	}

	protected byte[] toInternalFormat(Object bytes) {
		if(bytes==null)return null;
		return unwrapNonPrimitive(( Byte[] ) bytes);
	}
	private Byte[] wrapPrimitive(byte[] bytes) {
		int length = bytes.length;
		Byte[] result = new Byte[length];
		for ( int index = 0; index < length ; index++ ) {
			result[index] = Byte.valueOf( bytes[index] );
		}
		return result;
	}
	
	private byte[] unwrapNonPrimitive(Byte[] bytes) {
		int length = bytes.length;
		byte[] result = new byte[length];
		for ( int i = 0; i < length ; i++ ) {
			result[i] = bytes[i].byteValue();
		}
		return result;
	}
}
