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
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.SerializableTypeDescriptor;
import org.hibernate.type.descriptor.sql.BlobTypeDescriptor;
import org.hibernate.usertype.DynamicParameterizedType;

/**
 * @author Brett Meyer
 */
public class SerializableToBlobType<T extends Serializable> extends AbstractSingleColumnStandardBasicType<T> implements DynamicParameterizedType {
	
	public static final String CLASS_NAME = "classname";
	
	private static final long serialVersionUID = 1L;

	private JavaTypeDescriptor<T> javaTypeDescriptor = new SerializableTypeDescriptor( Serializable.class );

	/**
	 * @param sqlTypeDescriptor
	 * @param javaTypeDescriptor
	 */
	// TODO: After HHH-7586, this should eventually use the actual T class.
	// But, for now, just use Serializable.
	public SerializableToBlobType() {
		super( BlobTypeDescriptor.DEFAULT, new SerializableTypeDescriptor( Serializable.class ) );
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	// TODO: This method (and DynamicParameterizedType) should go away after HHH-7586.
	public void setParameterValues(Properties parameters) {
		ParameterType reader = (ParameterType) parameters.get( PARAMETER_TYPE );
		if ( reader != null ) {
			javaTypeDescriptor = new SerializableTypeDescriptor<T>( reader.getReturnedClass() );
		} else {
			String className = parameters.getProperty( CLASS_NAME );
			if ( className == null ) {
				throw new MappingException( "No class name defined for type: " + SerializableToBlobType.class.getName() );
			}
			try {
				javaTypeDescriptor = new SerializableTypeDescriptor<T>( ReflectHelper.classForName( className ) );
			} catch ( ClassNotFoundException e ) {
				throw new MappingException( "Unable to load class from " + CLASS_NAME + " parameter", e );
			}
		}
		
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	// TODO: Remove after HHH-7586.
	protected ValueExtractor<T> getExtractor(WrapperOptions options) {
		return remapSqlTypeDescriptor( options ).getExtractor( javaTypeDescriptor );
	}
}
