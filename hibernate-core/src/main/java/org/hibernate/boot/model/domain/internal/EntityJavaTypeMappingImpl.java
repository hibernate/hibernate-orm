/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain.internal;

import org.hibernate.boot.model.domain.EntityJavaTypeMapping;
import org.hibernate.boot.model.domain.IdentifiableJavaTypeMapping;
import org.hibernate.boot.model.source.internal.SourceHelper;
import org.hibernate.boot.model.source.spi.EntityNamingSource;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.descriptor.java.internal.EntityJavaDescriptorImpl;
import org.hibernate.type.descriptor.java.spi.IdentifiableJavaDescriptor;

/**
 * @author Chris Cranford
 */
public class EntityJavaTypeMappingImpl<T> extends AbstractIdentifiableJavaTypeMapping<T> implements EntityJavaTypeMapping<T> {
	private final EntityNamingSource entityNamingSource;

	public EntityJavaTypeMappingImpl(MetadataBuildingContext buildingContext, EntityNamingSource entityNamingSource, IdentifiableJavaTypeMapping<? super T> superJavaTypeMapping) {
		super( buildingContext, superJavaTypeMapping );

		this.entityNamingSource = entityNamingSource;
	}

	@Override
	public String getTypeName() {
		return entityNamingSource.getTypeName();
	}

	@Override
	public String getEntityName() {
		return entityNamingSource.getEntityName();
	}

	@Override
	public String getJpaEntityName() {
		return entityNamingSource.getJpaEntityName();
	}

	@Override
	public IdentifiableJavaDescriptor<T> resolveJavaTypeDescriptor() {
		final String name;
		if ( entityNamingSource.getClassName() == null ) {
			name = entityNamingSource.getTypeName();
		}
		else {
			name = entityNamingSource.getClassName();
		}

		final BootstrapContext bootstrapContext = getMetadataBuildingContext().getBootstrapContext();
		return SourceHelper.resolveJavaDescriptor(
				name,
				bootstrapContext.getTypeConfiguration(),
				() -> new EntityJavaDescriptorImpl(
						getTypeName(),
						getEntityName(),
						SourceHelper.resolveJavaType( entityNamingSource.getClassName(), bootstrapContext ),
						getSuperType() == null ? null : getSuperType().resolveJavaTypeDescriptor(),
						null,
						null
				)
		);
	}
}
