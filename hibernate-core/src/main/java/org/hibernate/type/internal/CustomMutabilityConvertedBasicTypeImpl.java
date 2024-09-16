/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.internal;

import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Christian Beikov
 */
public class CustomMutabilityConvertedBasicTypeImpl<J> extends ConvertedBasicTypeImpl<J> {

	private final MutabilityPlan<J> mutabilityPlan;

	public CustomMutabilityConvertedBasicTypeImpl(
			String name,
			JdbcType jdbcType,
			BasicValueConverter<J, ?> converter,
			MutabilityPlan<J> mutabilityPlan) {
		super( name, jdbcType, converter );
		this.mutabilityPlan = mutabilityPlan;
	}

	public CustomMutabilityConvertedBasicTypeImpl(
			String name,
			String description,
			JdbcType jdbcType,
			BasicValueConverter<J, ?> converter,
			MutabilityPlan<J> mutabilityPlan) {
		super( name, description, jdbcType, converter );
		this.mutabilityPlan = mutabilityPlan;
	}

	@Override
	protected MutabilityPlan<J> getMutabilityPlan() {
		return mutabilityPlan;
	}
}
