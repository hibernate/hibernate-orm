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

import java.io.Serializable;

import org.hibernate.type.ImageType;
import org.hibernate.util.SerializationHelper;

/**
 * A type that maps an SQL LONGVARBINARY to a serializable Java object.
 * 
 * @author Strong Liu
 */
public class SerializableToImageType extends ImageType {
	public Class getReturnedClass() {
		return Serializable.class;
	}

	protected Object toExternalFormat(byte[] bytes) {
		if (bytes == null)
			return null;
		return SerializationHelper.deserialize( bytes, getReturnedClass().getClassLoader() );
	}

	protected byte[] toInternalFormat(Object bytes) {
		return SerializationHelper.serialize((Serializable) bytes);
	}
}
