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
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * BasicValueMapper bridging to legacy {@link BasicType}
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked")
public class BasicTypeResolution<J> implements BasicValueMapping.Resolution<J>, BasicValueMapper<J> {
	private final BasicTypeAdapter basicTypeAdapter;
	private final MutabilityPlan mutabilityPlan;

	public BasicTypeResolution(
			BasicTypeAdapter basicTypeAdapter,
			MutabilityPlan mutabilityPlan) {
		this.basicTypeAdapter = basicTypeAdapter;
		this.mutabilityPlan = mutabilityPlan;
	}

	public BasicTypeResolution(
			BasicType basicType,
			String name,
			BasicJavaDescriptor explicitJtd,
			SqlTypeDescriptor explicitStd,
			MutabilityPlan explicitMutabilityPlan,
			MetadataBuildingContext context) {
		this.basicTypeAdapter = new BasicTypeAdapter(
				basicType,
				name,
				explicitJtd,
				explicitStd,
				context
		);

		this.mutabilityPlan = explicitMutabilityPlan != null
				? explicitMutabilityPlan
				: new BasicTypeMutabilityPlanAdapter( basicType );
	}

	@Override
	public BasicJavaDescriptor<J> getDomainJavaDescriptor() {
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
	public MutabilityPlan<J> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public org.hibernate.type.spi.BasicType getBasicType() {
		return basicTypeAdapter;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return basicTypeAdapter.getSqlExpressableType();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BasicValueMapper

	@Override
	public BasicValueMapper<J> getValueMapper() {
		return this;
	}
}
