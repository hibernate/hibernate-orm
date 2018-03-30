/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * Basically a map from {@link Class} -> {@link JavaTypeDescriptor}
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @since 5.3
 */
public class JavaTypeDescriptorRegistry implements Serializable {
	private static final Logger log = Logger.getLogger( JavaTypeDescriptorRegistry.class );


	private ConcurrentHashMap<Class, JavaTypeDescriptor> descriptorsByClass = new ConcurrentHashMap<>();

	@SuppressWarnings("unused")
	public JavaTypeDescriptorRegistry(TypeConfiguration typeConfiguration) {
	}

	public <T> JavaTypeDescriptor<T> getDescriptor(Class<T> javaType) {
		return RegistryHelper.INSTANCE.resolveDescriptor(
				descriptorsByClass,
				javaType,
				() -> {
					log.debugf(
							"Could not find matching scoped JavaTypeDescriptor for requested Java class [%s]; " +
									"falling back to static registry",
							javaType.getName()
					);

					return org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry.INSTANCE.getDescriptor( javaType );
				}
		);
	}

	public void addDescriptor(JavaTypeDescriptor descriptor) {
		JavaTypeDescriptor old = descriptorsByClass.put( descriptor.getJavaType(), descriptor );
		if ( old != null ) {
			log.debugf(
					"JavaTypeDescriptorRegistry entry replaced : %s -> %s (was %s)",
					descriptor.getJavaType(),
					descriptor,
					old
			);
		}
	}
}
