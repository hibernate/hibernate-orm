/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain.internal;

import org.hibernate.boot.model.domain.IdentifiableJavaTypeMapping;
import org.hibernate.boot.model.domain.MappedSuperclassJavaTypeMapping;
import org.hibernate.boot.model.source.internal.SourceHelper;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.descriptor.java.internal.MappedSuperclassJavaDescriptorImpl;
import org.hibernate.type.descriptor.java.spi.IdentifiableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;

/**
 * @author Chris Cranford
 */
public class MappedSuperclassJavaTypeMappingImpl<T>
		extends AbstractIdentifiableJavaTypeMapping<T>
		implements MappedSuperclassJavaTypeMapping<T> {
	private final String typeName;

	public MappedSuperclassJavaTypeMappingImpl(MetadataBuildingContext buildingContext, String typeName, IdentifiableJavaTypeMapping<? super T> superJavaTypeMapping) {
		super( buildingContext, superJavaTypeMapping );

		this.typeName = typeName;
	}

	@Override
	public String getTypeName() {
		return typeName;
	}

	@Override
	@SuppressWarnings("unchecked")
	public IdentifiableJavaDescriptor<T> getJavaTypeDescriptor() {
		final BootstrapContext bootstrapContext = getMetadataBuildingContext().getBootstrapContext();
		final JavaTypeDescriptorRegistry registry = bootstrapContext.getTypeConfiguration().getJavaTypeDescriptorRegistry();

		IdentifiableJavaDescriptor javaDescriptor = (IdentifiableJavaDescriptor) registry.getDescriptor( typeName );
		if ( javaDescriptor == null ) {
			javaDescriptor = new MappedSuperclassJavaDescriptorImpl(
					typeName,
					SourceHelper.resolveJavaType( typeName, bootstrapContext ),
					getSuperType() == null ? null : (IdentifiableJavaDescriptor) getSuperType().getJavaTypeDescriptor(),
					null,
					null
			);

			registry.addDescriptor( javaDescriptor );
		}

		return javaDescriptor;
	}

}
