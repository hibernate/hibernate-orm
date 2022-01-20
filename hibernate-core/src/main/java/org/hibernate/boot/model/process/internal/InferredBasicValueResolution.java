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
public class InferredBasicValueResolution<J> implements BasicValue.Resolution<J> {
	private JavaType<J> domainJtd;
	private JavaType<J> relationalJtd;
	private JdbcType jdbcType;

	private MutabilityPlan mutabilityPlan;

	private JdbcMapping jdbcMapping;
	private BasicValueConverter valueConverter;

	private final BasicType<J> legacyType;

	public InferredBasicValueResolution(
			JdbcMapping jdbcMapping,
			JavaType<J> domainJtd,
			JavaType<J> relationalJtd,
			JdbcType jdbcType,
			BasicValueConverter valueConverter,
			BasicType<J> legacyType,
			MutabilityPlan mutabilityPlan) {
		this.jdbcMapping = jdbcMapping;
		this.legacyType = legacyType;
		this.domainJtd = domainJtd;
		this.relationalJtd = relationalJtd;
		this.jdbcType = jdbcType;
		this.valueConverter = valueConverter;
		this.mutabilityPlan = mutabilityPlan == null ? domainJtd.getMutabilityPlan() : mutabilityPlan;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public BasicType getLegacyResolvedBasicType() {
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
	public BasicValueConverter getValueConverter() {
		return valueConverter;
	}

	@Override
	public MutabilityPlan<J> getMutabilityPlan() {
		return mutabilityPlan;
	}
}
