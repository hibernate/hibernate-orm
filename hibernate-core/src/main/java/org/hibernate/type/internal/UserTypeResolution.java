/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.usertype.UserType;

/**
 * @author Steve Ebersole
 */
public class UserTypeResolution implements BasicValueMapping.Resolution, BasicValueMapper {
	private final UserTypeAdapter basicTypeAdapter;
	private final MutabilityPlan mutabilityPlan;

	public UserTypeResolution(
			UserTypeAdapter basicTypeAdapter,
			MutabilityPlan mutabilityPlan) {
		this.basicTypeAdapter = basicTypeAdapter;
		this.mutabilityPlan = mutabilityPlan;
	}

	public UserTypeResolution(
			UserType userType,
			String name,
			BasicJavaDescriptor explicitJtd,
			SqlTypeDescriptor explicitStd,
			MutabilityPlan explicitMutabilityPlan,
			MetadataBuildingContext context) {
		this.basicTypeAdapter = new UserTypeAdapter(
				userType,
				name,
				explicitJtd,
				explicitStd,
				context
		);

		this.mutabilityPlan = explicitMutabilityPlan != null
				? explicitMutabilityPlan
				: new UserTypeMutabilityPlanAdapter( userType );
	}

	@Override
	public BasicJavaDescriptor<?> getDomainJavaDescriptor() {
		return basicTypeAdapter.getJavaTypeDescriptor();
	}

	@Override
	public BasicJavaDescriptor<?> getRelationalJavaDescriptor() {
		return basicTypeAdapter.getJavaTypeDescriptor();
	}

	@Override
	public SqlTypeDescriptor getRelationalSqlTypeDescriptor() {
		return basicTypeAdapter.getSqlTypeDescriptor();
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
	public BasicType getBasicType() {
		return basicTypeAdapter;
	}

	@Override
	public BasicValueMapper getValueMapper() {
		return this;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return basicTypeAdapter.getSqlExpressableType();
	}
}
