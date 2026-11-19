package org.hibernate.metamodel.internal;

import jakarta.annotation.Nonnull;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.ImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;

/**
 * ImplicitDiscriminatorStrategy implementation using entity {@linkplain EntityMappingType#getEntityName() full-names}.
 *
 * @author Steve Ebersole
 */
public class FullNameImplicitDiscriminatorStrategy implements ImplicitDiscriminatorStrategy {
	public static final FullNameImplicitDiscriminatorStrategy FULL_NAME_STRATEGY = new FullNameImplicitDiscriminatorStrategy();

	@Override
	@Nonnull
	public Object toDiscriminatorValue(
			@Nonnull EntityMappingType entityMapping,
			@Nonnull NavigableRole discriminatorRole,
			@Nonnull MappingMetamodelImplementor mappingModel) {
		return entityMapping.getEntityName();
	}

	@Override
	@Nonnull
	public EntityMappingType toEntityMapping(
			@Nonnull Object discriminatorValue,
			@Nonnull NavigableRole discriminatorRole,
			@Nonnull MappingMetamodelImplementor mappingModel) {
		if ( discriminatorValue instanceof String assumedEntityName ) {
			final var persister = mappingModel.findEntityDescriptor( assumedEntityName );
			if ( persister != null ) {
				return persister;
			}
		}

		throw new HibernateException( "Cannot interpret discriminator value (" + discriminatorRole + ") : " + discriminatorValue );
	}
}
