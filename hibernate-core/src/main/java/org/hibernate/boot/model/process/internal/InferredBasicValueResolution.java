/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.process.internal;

import org.hibernate.mapping.BasicValue;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * @author Steve Ebersole
 */
public class InferredBasicValueResolution<J,T> implements BasicValue.Resolution<J> {
	private final JavaType<J> domainJtd;
	private final JavaType<T> relationalJtd;
	private final JdbcType jdbcType;

	private final MutabilityPlan<J> mutabilityPlan;

	private final JdbcMapping jdbcMapping;

	private final BasicType<J> legacyType;

	public InferredBasicValueResolution(
			JdbcMapping jdbcMapping,
			JavaType<J> domainJtd,
			JavaType<T> relationalJtd,
			JdbcType jdbcType,
			BasicType<J> legacyType,
			MutabilityPlan<J> mutabilityPlan) {
		this.jdbcMapping = jdbcMapping;
		this.legacyType = legacyType;
		this.domainJtd = domainJtd;
		this.relationalJtd = relationalJtd;
		this.jdbcType = jdbcType;
		this.mutabilityPlan = mutabilityPlan == null ? domainJtd.getMutabilityPlan() : mutabilityPlan;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public BasicType<J> getLegacyResolvedBasicType() {
		return legacyType;
	}

	@Override
	public JavaType<J> getDomainJavaType() {
		return domainJtd;
	}

	@Override
	public JavaType<?> getRelationalJavaType() {
		return relationalJtd;
	}

	@Override
	public JdbcType getJdbcType() {
		return jdbcType;
	}

	@Override
	public BasicValueConverter<J,T> getValueConverter() {
		//noinspection unchecked
		return (BasicValueConverter<J, T>) jdbcMapping.getValueConverter();
	}

	@Override
	public MutabilityPlan<J> getMutabilityPlan() {
		return mutabilityPlan;
	}
}
