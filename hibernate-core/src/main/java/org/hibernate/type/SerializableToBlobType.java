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
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.internal.SerializableJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.BlobTypeDescriptor;
import org.hibernate.usertype.DynamicParameterizedType;

/**
 * @author Brett Meyer
 */
public class SerializableToBlobType<T extends Serializable> extends BasicTypeImpl<T> implements DynamicParameterizedType {
	
	public static final String CLASS_NAME = "classname";
	
	private static final long serialVersionUID = 1L;

	private JavaTypeDescriptor javaTypeDescriptor;

	public SerializableToBlobType() {
		super( new SerializableJavaDescriptor( Serializable.class ), BlobTypeDescriptor.DEFAULT );
	}

	@Override
	public String getName() {
		return getClass().getName();
	}

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return javaTypeDescriptor == null ? super.getJavaTypeDescriptor() : javaTypeDescriptor;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setParameterValues(Properties parameters) {
		ParameterType reader = (ParameterType) parameters.get( PARAMETER_TYPE );
		if ( reader != null ) {
			javaTypeDescriptor = new SerializableJavaDescriptor<T>( reader.getReturnedClass() );
		}
		else {
			String className = parameters.getProperty( CLASS_NAME );
			if ( className == null ) {
				throw new MappingException( "No class name defined for type: " + SerializableToBlobType.class.getName() );
			}
			try {
				javaTypeDescriptor = new SerializableJavaDescriptor<T>( ReflectHelper.classForName( className ) );
			}
			catch ( ClassNotFoundException e ) {
				throw new MappingException( "Unable to load class from " + CLASS_NAME + " parameter", e );
			}
		}
	}

	@Override
	public JdbcLiteralFormatter<T> getJdbcLiteralFormatter() {
		// no literal support for BLOB
		return null;
	}
}
