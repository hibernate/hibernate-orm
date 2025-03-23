/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

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

	public RuntimeMetamodelsImpl(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	@Override
	public JpaMetamodelImplementor getJpaMetamodel() {
		return jpaMetamodel;
	}

	@Override
	public MappingMetamodelImplementor getMappingMetamodel() {
		return mappingMetamodel;
	}

	@Override
	public Type getIdentifierType(String className) throws MappingException {
		return mappingMetamodel.getEntityDescriptor( className ).getIdentifierType();
	}

	@Override
	public String getIdentifierPropertyName(String className) throws MappingException {
		return mappingMetamodel.getEntityDescriptor( className ).getIdentifierPropertyName();
	}

	@Override
	public Type getReferencedPropertyType(String className, String propertyName) throws MappingException {
		return mappingMetamodel.getEntityDescriptor( className ).getPropertyType( propertyName );
	}


	public void setMappingMetamodel(MappingMetamodelImplementor mappingMetamodel) {
		this.mappingMetamodel = mappingMetamodel;
	}

	public void setJpaMetamodel(JpaMetamodelImplementor jpaMetamodel) {
		this.jpaMetamodel = jpaMetamodel;
	}
}
