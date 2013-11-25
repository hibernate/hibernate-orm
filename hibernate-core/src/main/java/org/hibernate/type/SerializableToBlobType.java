/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.descriptor.java.SerializableTypeDescriptor;
import org.hibernate.type.descriptor.sql.BlobTypeDescriptor;
import org.hibernate.usertype.DynamicParameterizedType;

/**
 * @author Brett Meyer
 */
public class SerializableToBlobType<T extends Serializable> extends AbstractSingleColumnStandardBasicType<T> implements DynamicParameterizedType {
	
	public static final String CLASS_NAME = "classname";
	
	private static final long serialVersionUID = 1L;

	public SerializableToBlobType() {
		super( BlobTypeDescriptor.DEFAULT, new SerializableTypeDescriptor( Serializable.class ) );
	}

	@Override
	public String getName() {
		return getClass().getName();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setParameterValues(Properties parameters) {
		ParameterType reader = (ParameterType) parameters.get( PARAMETER_TYPE );
		if ( reader != null ) {
			setJavaTypeDescriptor( new SerializableTypeDescriptor<T>( reader.getReturnedClass() ) );
		} else {
			String className = parameters.getProperty( CLASS_NAME );
			if ( className == null ) {
				throw new MappingException( "No class name defined for type: " + SerializableToBlobType.class.getName() );
			}
			try {
				setJavaTypeDescriptor( new SerializableTypeDescriptor<T>( ReflectHelper.classForName( className ) ) );
			} catch ( ClassNotFoundException e ) {
				throw new MappingException( "Unable to load class from " + CLASS_NAME + " parameter", e );
			}
		}
	}
}
