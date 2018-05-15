/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cfg;

import javax.persistence.EnumType;
import javax.persistence.TemporalType;

import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.domain.ResolutionContext;
import org.hibernate.boot.model.type.spi.BasicTypeResolver;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.BasicTypeParameters;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public abstract class BasicTypeResolverSupport<T>
		implements BasicTypeResolver, JdbcRecommendedSqlTypeMappingContext, BasicTypeParameters {
	private final MetadataBuildingContext buildingContext;

	private BasicJavaDescriptor javaTypeDescriptor;
	private SqlTypeDescriptor sqlTypeDescriptor;

	private BasicType basicType;

	public BasicTypeResolverSupport(MetadataBuildingContext buildingContext) {
		this.buildingContext = buildingContext;
	}

	protected MetadataBuildingContext getBuildingContext() {
		return buildingContext;
	}

	@Override
	@SuppressWarnings("unchecked")
	public BasicType<T> resolveBasicType(ResolutionContext context) {
		if ( basicType == null ) {
			resolveJavaAndSqlTypeDescriptors();
			basicType = getTypeConfiguration().getBasicTypeRegistry().resolveBasicType(
					this,
					this
			);
		}
		return basicType;
	}

	@Override
	public BasicJavaDescriptor getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	protected void setJavaTypeDescriptor(BasicJavaDescriptor javaTypeDescriptor) {
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}

	protected void setSqlTypeDescriptor(SqlTypeDescriptor sqlTypeDescriptor) {
		this.sqlTypeDescriptor = sqlTypeDescriptor;
	}

	protected void resolveJavaAndSqlTypeDescriptors() {
		final TypeConfiguration typeConfiguration = getBuildingContext().getBootstrapContext().getTypeConfiguration();
		final JavaTypeDescriptorRegistry jtdRegistry = typeConfiguration.getJavaTypeDescriptorRegistry();

		sqlTypeDescriptor = getExplicitSqlTypeDescriptor();

		if ( getAttributeConverterDescriptor() != null ) {
			final Class<?> domainJavaType = getAttributeConverterDescriptor().getDomainValueResolvedType().getErasedType();
			javaTypeDescriptor = (BasicJavaDescriptor) jtdRegistry.getOrMakeJavaDescriptor( domainJavaType );

			if ( sqlTypeDescriptor == null ) {
				final Class<?> relationalJavaType = getAttributeConverterDescriptor().getRelationalValueResolvedType()
						.getErasedType();
				final JavaTypeDescriptor<?> relationalJavaDescriptor = jtdRegistry.getOrMakeJavaDescriptor(
						relationalJavaType );
				sqlTypeDescriptor = relationalJavaDescriptor.getJdbcRecommendedSqlType( this );
			}
		}
		else {
			javaTypeDescriptor = (BasicJavaDescriptor) jtdRegistry.getOrMakeJavaDescriptor( getReflectedValueJavaType() );
		}

		if ( sqlTypeDescriptor == null ) {
			sqlTypeDescriptor = javaTypeDescriptor.getJdbcRecommendedSqlType( this );
		}
	}

	protected abstract Class<T> getReflectedValueJavaType();

	@Override
	public ConverterDescriptor getAttributeConverterDescriptor() {
		return null;
	}

	@Override
	public TemporalType getTemporalPrecision() {
		return getTemporalType();
	}

	@Override
	public TemporalType getTemporalType() {
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
	public EnumType getEnumeratedType() {
		return getEnumType();
	}

	public EnumType getEnumType() {
		return buildingContext.getBuildingOptions().getImplicitEnumType();
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
