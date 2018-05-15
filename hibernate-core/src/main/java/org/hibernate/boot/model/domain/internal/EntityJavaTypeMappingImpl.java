/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain.internal;

import org.hibernate.boot.model.domain.EntityJavaTypeMapping;
import org.hibernate.boot.model.domain.IdentifiableJavaTypeMapping;
import org.hibernate.boot.model.domain.NotYetResolvedException;
import org.hibernate.boot.model.source.internal.SourceHelper;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.internal.ImmutableEntityEntry;
import org.hibernate.engine.internal.MutableEntityEntryFactory;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.internal.EntityJavaDescriptorImpl;
import org.hibernate.type.descriptor.java.internal.EntityMutabilityPlanImpl;
import org.hibernate.type.descriptor.java.spi.IdentifiableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Chris Cranford
 */
public class EntityJavaTypeMappingImpl<T> extends AbstractIdentifiableJavaTypeMapping<T> implements EntityJavaTypeMapping<T> {
	private PersistentClass persistentClass;

	public EntityJavaTypeMappingImpl(MetadataBuildingContext buildingContext, PersistentClass persistentClass, IdentifiableJavaTypeMapping<? super T> superJavaTypeMapping) {
		super( buildingContext, superJavaTypeMapping);
		this.persistentClass = persistentClass;
	}

	@Override
	public String getTypeName() {
		return StringHelper.isNotEmpty( persistentClass.getClassName() ) ? persistentClass.getClassName() : persistentClass.getEntityName();
	}

	@Override
	public String getEntityName() {
		return persistentClass.getEntityName();
	}

	@Override
	public String getJpaEntityName() {
		return persistentClass.getJpaEntityName();
	}

	@Override
	@SuppressWarnings("unchecked")
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() throws NotYetResolvedException {

		final String name =  getTypeName();

		final BootstrapContext bootstrapContext = getMetadataBuildingContext().getBootstrapContext();
		return SourceHelper.resolveJavaDescriptor(
				name,
				bootstrapContext.getTypeConfiguration(),
				() -> new EntityJavaDescriptorImpl(
						getTypeName(),
						getEntityName(),
						SourceHelper.resolveJavaType( name, bootstrapContext ),
						getSuperType() == null ? null : (IdentifiableJavaDescriptor) getSuperType().getJavaTypeDescriptor(),
						getMutabilityPlan(),
						null
				)
		);
	}

	private MutabilityPlan getMutabilityPlan() {
		if ( persistentClass.isMutable() ) {
			return new EntityMutabilityPlanImpl(
					MutableEntityEntryFactory.INSTANCE,
					true
			);
		}
		else {
			return new EntityMutabilityPlanImpl(
					(status, loadedState, rowId, id, version, lockMode, existsInDatabase, descriptor, disableVersionIncrement, persistenceContext) -> new ImmutableEntityEntry(
							status,
							loadedState,
							rowId,
							id,
							version,
							lockMode,
							existsInDatabase,
							descriptor,
							disableVersionIncrement,
							persistenceContext
					),
					false
			);
		}
	}
}
