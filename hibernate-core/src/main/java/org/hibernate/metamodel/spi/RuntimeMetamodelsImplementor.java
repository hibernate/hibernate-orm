package org.hibernate.metamodel.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.type.BindingContext;
import org.hibernate.type.MappingContext;

/**
 * SPI extending {@link RuntimeMetamodels} and mixing in {@link MappingContext}.
 *
 * @author Steve Ebersole
 */
public interface RuntimeMetamodelsImplementor extends RuntimeMetamodels, MappingContext, BindingContext {
	@Override
	@Nonnull
	MappingMetamodelImplementor getMappingMetamodel();

	@Override
	@Nonnull
	JpaMetamodelImplementor getJpaMetamodel();
}
