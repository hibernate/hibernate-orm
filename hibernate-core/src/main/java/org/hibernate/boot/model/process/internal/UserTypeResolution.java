/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class UserTypeResolution implements BasicValue.Resolution {
	private final CustomType userTypeAdapter;
	private final MutabilityPlan mutabilityPlan;

	public UserTypeResolution(
			CustomType userTypeAdapter,
			MutabilityPlan explicitMutabilityPlan) {
		this.userTypeAdapter = userTypeAdapter;
		this.mutabilityPlan = explicitMutabilityPlan != null
				? explicitMutabilityPlan
				: new UserTypeMutabilityPlanAdapter( userTypeAdapter.getUserType() );
	}

	@Override
	public JavaTypeDescriptor<?> getDomainJavaDescriptor() {
		return userTypeAdapter.getJavaTypeDescriptor();
	}

	@Override
	public JavaTypeDescriptor<?> getRelationalJavaDescriptor() {
		return userTypeAdapter.getJavaTypeDescriptor();
	}

	@Override
	public SqlTypeDescriptor getRelationalSqlTypeDescriptor() {
		return userTypeAdapter.getSqlTypeDescriptor();
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
	public BasicType getResolvedBasicType() {
		return userTypeAdapter;
	}
}
