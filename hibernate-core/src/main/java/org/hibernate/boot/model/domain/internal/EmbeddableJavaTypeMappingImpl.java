/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain.internal;

import org.hibernate.boot.model.domain.EmbeddableJavaTypeMapping;
import org.hibernate.boot.model.domain.NotYetResolvedException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.descriptor.java.internal.EmbeddableJavaDescriptorImpl;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.java.spi.ManagedJavaDescriptor;

/**
 * @author Chris Cranford
 */
public class EmbeddableJavaTypeMappingImpl<T>
		extends AbstractManagedJavaTypeMapping<T>
		implements EmbeddableJavaTypeMapping<T> {
	private final String roleName;
	private final String componentClassName;

	public EmbeddableJavaTypeMappingImpl(MetadataBuildingContext buildingContext, String roleName, String componentClassName, EmbeddableJavaTypeMapping<? super T> superJavaTypeMapping) {
		super( buildingContext, superJavaTypeMapping );

		this.roleName = roleName;
		this.componentClassName = componentClassName;
	}

	@Override
	public String getTypeName() {
		return componentClassName;
	}

	@Override
	public EmbeddableJavaTypeMapping<? super T> getSuperType() {
		return (EmbeddableJavaTypeMapping<? super T>) super.getSuperType();
	}

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() throws NotYetResolvedException {
		final BootstrapContext bootstrapContext = getMetadataBuildingContext().getBootstrapContext();
		final JavaTypeDescriptorRegistry registry = bootstrapContext.getTypeConfiguration().getJavaTypeDescriptorRegistry();

		final String name = roleName != null ? roleName : componentClassName;

		ManagedJavaDescriptor javaDescriptor = (ManagedJavaDescriptor) registry.getDescriptor( name );
		if ( javaDescriptor == null ) {
			final Class javaType;
			if ( StringHelper.isEmpty( componentClassName ) ) {
				javaType = null;
			}
			else {
				javaType = bootstrapContext.getServiceRegistry().getService( ClassLoaderService.class ).classForName( componentClassName );
			}

			javaDescriptor = new EmbeddableJavaDescriptorImpl( name, javaType, null );
			registry.addDescriptor( javaDescriptor );
		}
		else if ( !( javaDescriptor instanceof EmbeddableJavaDescriptor ) ) {
			/*
				This may happen with hbm mapping:
				<class name="Entity"...>
					<composite-id>
					 </composite-id>
				</class>
				in such a case the componentClassName is the "Entity" so javaTypeDescriptorRegistry
				.getDescriptor( componentClassName ); is not returning an EmbeddableJavaDescriptor
			 */
			javaDescriptor = new EmbeddableJavaDescriptorImpl( name, javaDescriptor.getJavaType(), null );
		}

		return (EmbeddableJavaDescriptor) javaDescriptor;
	}

}
