/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.internal;

import java.util.Properties;

import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Steve Ebersole
 */
public class UserTypeResolution<T> implements BasicValue.Resolution<T> {
	private final CustomType<T> userTypeAdapter;
	private final MutabilityPlan<T> mutabilityPlan;

	/**
	 * We need this for the way envers interprets the boot-model
	 * and builds its own :(
	 */
	private final Properties combinedTypeParameters;

	public UserTypeResolution(
			CustomType<T> userTypeAdapter,
			MutabilityPlan<T> explicitMutabilityPlan,
			Properties combinedTypeParameters) {
		this.userTypeAdapter = userTypeAdapter;
		this.combinedTypeParameters = combinedTypeParameters;
		this.mutabilityPlan = explicitMutabilityPlan != null
				? explicitMutabilityPlan
				: new UserTypeMutabilityPlanAdapter<>( userTypeAdapter.getUserType() );
	}

	@Override
	public JavaType<T> getDomainJavaType() {
		return userTypeAdapter.getJavaTypeDescriptor();
	}

	@Override
	public JavaType<?> getRelationalJavaType() {
		return userTypeAdapter.getJavaTypeDescriptor();
	}

	@Override
	public JdbcType getJdbcType() {
		return userTypeAdapter.getJdbcType();
	}

	@Override
	public BasicValueConverter<T,?> getValueConverter() {
		// Even though we could expose the value converter of the user type here,
		// we can not do it, as the conversion is done behind the scenes in the binder/extractor,
		// whereas the converter returned here would, AFAIU, be used to construct a converted attribute mapping
		return null;
	}

	@Override
	public MutabilityPlan<T> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public BasicType<T> getLegacyResolvedBasicType() {
		return userTypeAdapter;
	}

	@Override
	public Properties getCombinedTypeParameters() {
		return combinedTypeParameters;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return userTypeAdapter;
	}
}
