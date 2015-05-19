/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		}
		else {
			String className = parameters.getProperty( CLASS_NAME );
			if ( className == null ) {
				throw new MappingException( "No class name defined for type: " + SerializableToBlobType.class.getName() );
			}
			try {
				setJavaTypeDescriptor( new SerializableTypeDescriptor<T>( ReflectHelper.classForName( className ) ) );
			}
			catch ( ClassNotFoundException e ) {
				throw new MappingException( "Unable to load class from " + CLASS_NAME + " parameter", e );
			}
		}
	}
}
