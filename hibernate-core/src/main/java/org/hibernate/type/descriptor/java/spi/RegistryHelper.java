/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.type.descriptor.java.EnumJavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.SerializableTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class RegistryHelper {
	private static final Logger log = Logger.getLogger( RegistryHelper.class );

	/**
	 * Singleton access
	 */
	public static final RegistryHelper INSTANCE = new RegistryHelper();

	private RegistryHelper() {
	}

	@SuppressWarnings("unchecked")
	public <J> JavaTypeDescriptor<J> resolveDescriptor(
			Map<Class,JavaTypeDescriptor> descriptorsByClass,
			Class<J> cls,
			Supplier<JavaTypeDescriptor<J>> defaultValueSupplier) {
		if ( cls == null ) {
			throw new IllegalArgumentException( "Class passed to locate JavaTypeDescriptor cannot be null" );
		}

		JavaTypeDescriptor<J> descriptor = descriptorsByClass.get( cls );
		if ( descriptor != null ) {
			return descriptor;
		}

		if ( cls.isEnum() ) {
			descriptor = new EnumJavaTypeDescriptor( cls );
			descriptorsByClass.put( cls, descriptor );
			return descriptor;
		}

		// find the first "assignable" match
		for ( Map.Entry<Class, JavaTypeDescriptor> entry : descriptorsByClass.entrySet() ) {
			if ( entry.getKey().isAssignableFrom( cls ) ) {
				log.debugf( "Using  cached JavaTypeDescriptor instance for Java class [%s]", cls.getName() );
				return entry.getValue();
			}
		}

		return defaultValueSupplier.get();
	}
}
