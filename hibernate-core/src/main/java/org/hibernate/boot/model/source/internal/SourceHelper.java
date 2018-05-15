/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.source.internal;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class SourceHelper {
	public static <T extends JavaTypeDescriptor> T resolveJavaDescriptor(
			String name,
			TypeConfiguration typeConfiguration,
			java.util.function.Supplier<T> generator) {
		final JavaTypeDescriptorRegistry jtr = typeConfiguration.getJavaTypeDescriptorRegistry();

		JavaTypeDescriptor jtd = jtr.getDescriptor( name );
		if ( jtd == null ) {
			// we need to create one and register it...
			//		use the passed Supplier
			jtd = generator.get();
			jtr.addDescriptor( jtd );
		}

		return (T) jtd;
	}

	public static <T> Class<T> resolveJavaType(String className, MetadataBuildingContext metadataBuildingContext) {
		return resolveJavaType( className, metadataBuildingContext.getBootstrapContext() );
	}

	public static <T> Class<T> resolveJavaType(String className, BootstrapContext bootstrapContext) {
		final ClassLoaderService cls = bootstrapContext.getServiceRegistry().getService( ClassLoaderService.class );
		try {
			return cls.classForName( className );
		}
		catch ( Exception ignore ) {
		}

		return null;
	}

	private SourceHelper() {
	}
}
