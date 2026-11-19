package org.hibernate.metamodel.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.MappingException;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeMetamodelsImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class RuntimeMetamodelsImpl implements RuntimeMetamodelsImplementor {

	private final TypeConfiguration typeConfiguration;
	private JpaMetamodelImplementor jpaMetamodel;
	private MappingMetamodelImplementor mappingMetamodel;

	public RuntimeMetamodelsImpl(@Nonnull TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	@Nonnull
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	@Nonnull
	public JpaMetamodelImplementor getJpaMetamodel() {
		return jpaMetamodel;
	}

	@Override
	@Nonnull
	public MappingMetamodelImplementor getMappingMetamodel() {
		return mappingMetamodel;
	}

	@Override
	@Nonnull
	public Type getIdentifierType(@Nonnull String className) throws MappingException {
		return mappingMetamodel.getEntityDescriptor( className ).getIdentifierType();
	}

	@Override
	@Nullable
	public String getIdentifierPropertyName(@Nonnull String className) throws MappingException {
		return mappingMetamodel.getEntityDescriptor( className ).getIdentifierPropertyName();
	}

	@Override
	@Nonnull
	public Type getReferencedPropertyType(@Nonnull String className, @Nonnull String propertyName) throws MappingException {
		return mappingMetamodel.getEntityDescriptor( className ).getPropertyType( propertyName );
	}


	public void setMappingMetamodel(@Nonnull MappingMetamodelImplementor mappingMetamodel) {
		this.mappingMetamodel = mappingMetamodel;
	}

	public void setJpaMetamodel(@Nonnull JpaMetamodelImplementor jpaMetamodel) {
		this.jpaMetamodel = jpaMetamodel;
	}
}
