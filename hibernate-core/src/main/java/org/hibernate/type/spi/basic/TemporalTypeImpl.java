/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.basic;

import java.sql.Types;
import java.util.Comparator;
import javax.persistence.AttributeConverter;

import org.hibernate.type.spi.descriptor.TypeDescriptorRegistryAccess;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.descriptor.java.TemporalTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class TemporalTypeImpl<T,D> extends BasicTypeImpl<T,D> implements TemporalType<T> {
	private final javax.persistence.TemporalType precision;

	public TemporalTypeImpl(
			TemporalTypeDescriptor<T> domainJavaType,
			SqlTypeDescriptor sqlType,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator) {
		super( domainJavaType, sqlType, mutabilityPlan, comparator );
		this.precision = domainJavaType.getPrecision();
	}

	public TemporalTypeImpl(
			TemporalTypeDescriptor<T> domainJavaType,
			SqlTypeDescriptor sqlType,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator,
			javax.persistence.TemporalType precision) {
		super( domainJavaType, sqlType, mutabilityPlan, comparator );
		this.precision = precision;
	}

	public TemporalTypeImpl(
			TemporalTypeDescriptor<T> domainJavaType,
			SqlTypeDescriptor sqlType,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator,
			AttributeConverter<T, D> attributeConverter,
			JavaTypeDescriptor intermediateJavaType,
			javax.persistence.TemporalType precision) {
		super( domainJavaType, sqlType, mutabilityPlan, comparator, attributeConverter, intermediateJavaType );
		this.precision = precision;
	}

	public TemporalTypeImpl(
			TemporalTypeDescriptor<T> javaTypeDescriptor,
			SqlTypeDescriptor sqlTypeDescriptor,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator,
			AttributeConverter<T,D> attributeConverter,
			JavaTypeDescriptor intermediateJavaType) {
		this( javaTypeDescriptor, sqlTypeDescriptor, mutabilityPlan, comparator, attributeConverter, intermediateJavaType, javaTypeDescriptor.getPrecision() );
	}

	@Override
	public TemporalTypeDescriptor<T> getJavaTypeDescriptor() {
		return (TemporalTypeDescriptor<T>) super.getJavaTypeDescriptor();
	}

	@Override
	public javax.persistence.TemporalType getPrecision() {
		return precision;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> TemporalType<X> resolveTypeForPrecision(javax.persistence.TemporalType precision, BasicTypeRegistry basicTypeRegistry) {
		if ( precision == getPrecision() ) {
			return (TemporalType<X>) this;
		}

		final TemporalTypeDescriptor<X> treatedJavaTypeDescriptor = getJavaTypeDescriptor().resolveTypeForPrecision(
				precision,
				basicTypeRegistry.getTypeDescriptorRegistryAccess()
		);
		final SqlTypeDescriptor treatedSqlTypeDescriptor = determineSqlTypeDescriptor(
				precision,
				basicTypeRegistry.getTypeDescriptorRegistryAccess()
		);
		final AttributeConverterDefinition converterDefinition;

		if ( getAttributeConverter() == null ) {
			converterDefinition = null;
		}
		else {
			converterDefinition = new AttributeConverterDefinition() {
				@Override
				public AttributeConverter getAttributeConverter() {
					return TemporalTypeImpl.this.getAttributeConverter();
				}

				@Override
				public Class<?> getDomainType() {
					return TemporalTypeImpl.this.getJavaTypeDescriptor().getJavaTypeClass();
				}

				@Override
				public Class<?> getJdbcType() {
					return getColumnMapping().getSqlTypeDescriptor().getJdbcRecommendedJavaTypeMapping(
							basicTypeRegistry.getTypeDescriptorRegistryAccess().getTypeConfiguration() ).getJavaTypeClass();
				}
			};
		}

		return (TemporalType<X>) basicTypeRegistry.resolveBasicType(
				new BasicTypeParameters<X>() {
					@Override
					public JavaTypeDescriptor<X> getJavaTypeDescriptor() {
						return treatedJavaTypeDescriptor;
					}

					@Override
					public SqlTypeDescriptor getSqlTypeDescriptor() {
						return treatedSqlTypeDescriptor;
					}

					@Override
					public AttributeConverterDefinition getAttributeConverterDefinition() {
						return converterDefinition;
					}

					@Override
					public MutabilityPlan<X> getMutabilityPlan() {
						return getJavaTypeDescriptor().getMutabilityPlan();
					}

					@Override
					public Comparator<X> getComparator() {
						return getJavaTypeDescriptor().getComparator();
					}

					@Override
					public javax.persistence.TemporalType getTemporalPrecision() {
						return precision;
					}
				},
				basicTypeRegistry.getBaseJdbcRecommendedSqlTypeMappingContext()
		);
	}

	private SqlTypeDescriptor determineSqlTypeDescriptor(
			javax.persistence.TemporalType precision,
			TypeDescriptorRegistryAccess typeDescriptorRegistryAccess) {
		switch ( precision ) {
			case TIMESTAMP: {
				return typeDescriptorRegistryAccess.getSqlTypeDescriptorRegistry().getDescriptor( Types.TIMESTAMP );
			}
			case DATE: {
				return typeDescriptorRegistryAccess.getSqlTypeDescriptorRegistry().getDescriptor( Types.DATE );
			}
			case TIME: {
				return typeDescriptorRegistryAccess.getSqlTypeDescriptorRegistry().getDescriptor( Types.TIME );
			}
			default: {
				throw new IllegalArgumentException( "Unexpected javax.persistence.TemporalType [" + precision + "]; expecting TIMESTAMP, DATE or TIME" );
			}
		}
	}
}
