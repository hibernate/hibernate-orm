/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cfg;

import java.util.Comparator;
import javax.persistence.TemporalType;

import org.hibernate.boot.model.type.spi.BasicTypeResolver;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.BasicTypeParameters;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public abstract class BasicTypeResolverSupport<T>
		implements BasicTypeResolver, JdbcRecommendedSqlTypeMappingContext, BasicTypeParameters {
	private final MetadataBuildingContext buildingContext;

	public BasicTypeResolverSupport(MetadataBuildingContext buildingContext) {
		this.buildingContext = buildingContext;
	}

	protected MetadataBuildingContext getBuildingContext() {
		return buildingContext;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> BasicType<T> resolveBasicType() {
		return getTypeConfiguration().getBasicTypeRegistry().resolveBasicType(
				this,
				this
		);
	}

	@Override
	public org.hibernate.type.converter.spi.AttributeConverterDefinition getAttributeConverterDefinition() {
		return null;
	}

	@Override
	public MutabilityPlan<T> getMutabilityPlan() {
		return null;
	}

	@Override
	public Comparator getComparator() {
		return null;
	}

	@Override
	public TemporalType getTemporalPrecision() {
		return null;
	}

	@Override
	public boolean isNationalized() {
		return false;
	}

	@Override
	public boolean isLob() {
		return false;
	}

	@Override
	public javax.persistence.EnumType getEnumeratedType() {
		return null;
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return buildingContext.getPreferredSqlTypeCodeForBoolean();
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return buildingContext.getBootstrapContext().getTypeConfiguration();
	}
}
