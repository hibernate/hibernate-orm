/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.internal;

import javax.persistence.AttributeConverter;

import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.JpaAttributeConverterCreationContext;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.SqlTypeDescriptorIndicators;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * BasicMappingResolution handling HBM's support for
 * JPA AttributeConverter as a named type (using `converted::` prefix
 * for the type name).
 *
 * @author Steve Ebersole
 */
public class NamedConverterMapper implements BasicValueMapper {

	public static BasicValueMapper from(
			String name,
			BasicJavaDescriptor explicitJtd,
			SqlTypeDescriptor explicitStd,
			JpaAttributeConverterCreationContext converterCreationContext,
			MutabilityPlan explicitMutabilityPlan,
			SqlTypeDescriptorIndicators sqlTypeIndicators,
			MetadataBuildingContext metadataBuildingContext) {
		final StandardServiceRegistry serviceRegistry = metadataBuildingContext.getBootstrapContext().getServiceRegistry();
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );

		final TypeConfiguration typeConfiguration = metadataBuildingContext.getBootstrapContext().getTypeConfiguration();

		final String converterClassName = name.substring( ConverterDescriptor.TYPE_NAME_PREFIX.length() );
		final Class<? extends AttributeConverter> converterClass = classLoaderService.classForName( converterClassName );
		final ClassBasedConverterDescriptor converterDescriptor = new ClassBasedConverterDescriptor(
				converterClass,
				metadataBuildingContext.getBootstrapContext().getClassmateContext()
		);
		final JpaAttributeConverter converter = converterDescriptor.createJpaAttributeConverter( converterCreationContext );

		final BasicJavaDescriptor domainJtd = explicitJtd != null
				? explicitJtd
				: converter.getDomainJavaDescriptor();

		final BasicJavaDescriptor relationalJtd = converter.getRelationalJavaDescriptor();
		final SqlTypeDescriptor relationalStd = explicitStd != null
				? explicitStd
				: relationalJtd.getJdbcRecommendedSqlType( sqlTypeIndicators );

		return new NamedConverterMapper(
				name,
				domainJtd,
				relationalStd.getSqlExpressableType( relationalJtd, typeConfiguration ),
				converter,
				explicitMutabilityPlan != null
						? explicitMutabilityPlan
						: converter.getDomainJavaDescriptor().getMutabilityPlan()
		);
	}

	private final String name;

	private final BasicJavaDescriptor domainJtd;
	private final SqlExpressableType sqlExpressableType;
	private final JpaAttributeConverter jpaAttributeConverter;
	private final MutabilityPlan mutabilityPlan;

	public NamedConverterMapper(
			String name,
			BasicJavaDescriptor domainJtd,
			SqlExpressableType sqlExpressableType,
			JpaAttributeConverter jpaAttributeConverter,
			MutabilityPlan mutabilityPlan) {
		this.name = name;
		this.domainJtd = domainJtd;
		this.sqlExpressableType = sqlExpressableType;
		this.jpaAttributeConverter = jpaAttributeConverter;
		this.mutabilityPlan = mutabilityPlan;
	}

	@Override
	public BasicJavaDescriptor<?> getDomainJavaDescriptor() {
		return domainJtd;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return sqlExpressableType;
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return jpaAttributeConverter;
	}

	@Override
	public MutabilityPlan getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public String toString() {
		return "NamedConverterResolution(" + name + ')';
	}
}
