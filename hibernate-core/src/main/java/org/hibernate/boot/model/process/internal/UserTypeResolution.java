/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import java.util.Properties;

import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class UserTypeResolution implements BasicValue.Resolution {
	private final CustomType userTypeAdapter;
	private final MutabilityPlan mutabilityPlan;

	/**
	 * We need this for the way envers interprets the boot-model
	 * and builds its own :(
	 */
	private final Properties combinedTypeParameters;

	public UserTypeResolution(
			CustomType userTypeAdapter,
			MutabilityPlan explicitMutabilityPlan,
			Properties combinedTypeParameters) {
		this.userTypeAdapter = userTypeAdapter;
		this.combinedTypeParameters = combinedTypeParameters;
		this.mutabilityPlan = explicitMutabilityPlan != null
				? explicitMutabilityPlan
				: new UserTypeMutabilityPlanAdapter( userTypeAdapter.getUserType() );
	}

	@Override
	public JavaType<?> getDomainJavaDescriptor() {
		return userTypeAdapter.getJavaTypeDescriptor();
	}

	@Override
	public JavaType<?> getRelationalJavaDescriptor() {
		return userTypeAdapter.getJavaTypeDescriptor();
	}

	@Override
	public JdbcTypeDescriptor getJdbcTypeDescriptor() {
		return userTypeAdapter.getJdbcTypeDescriptor();
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return null;
	}

	@Override
	public MutabilityPlan getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public BasicType getLegacyResolvedBasicType() {
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
