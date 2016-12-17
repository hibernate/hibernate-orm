/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.basic;

import java.io.Serializable;
import java.sql.Types;
import java.util.Comparator;
import javax.persistence.AttributeConverter;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.spi.TemporalType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.basic.AbstractBasicTypeImpl;
import org.hibernate.type.spi.basic.BasicTypeParameters;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.descriptor.java.TemporalTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTemporalTypeImpl<T> extends AbstractBasicTypeImpl<T> implements TemporalType<T> {

	@Override
	@SuppressWarnings("unchecked")
	public <X> TemporalType<X> resolveTypeForPrecision(javax.persistence.TemporalType precision, TypeConfiguration typeConfiguration) {
		if ( precision == getPrecision() ) {
			return (TemporalType<X>) this;
		}

		final TemporalTypeDescriptor<X> treatedJavaTypeDescriptor = getJavaTypeDescriptor().resolveTypeForPrecision(
				precision,
				typeConfiguration
		);
		final SqlTypeDescriptor treatedSqlTypeDescriptor = determineSqlTypeDescriptor(
				precision,
				typeConfiguration
		);

		final AttributeConverterDefinition converterDefinition;

		if ( getAttributeConverterDefinition() == null ) {
			converterDefinition = null;
		}
		else {
			converterDefinition = new AttributeConverterDefinition() {
				@Override
				public AttributeConverter getAttributeConverter() {
					return getAttributeConverterDefinition().getAttributeConverter();
				}

				@Override
				public JavaTypeDescriptor<?> getDomainType() {
					return getJavaTypeDescriptor();
				}

				@Override
				public JavaTypeDescriptor<?> getJdbcType() {
					return getColumnMappings()[0].getSqlTypeDescriptor().getJdbcRecommendedJavaTypeMapping(
							typeConfiguration.getBasicTypeRegistry().getTypeDescriptorRegistryAccess().getTypeConfiguration()
					);
				}
			};
		}

		return (TemporalType<X>) typeConfiguration.getBasicTypeRegistry().resolveBasicType(
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
				typeConfiguration.getBasicTypeRegistry().getBaseJdbcRecommendedSqlTypeMappingContext()
		);
	}

	private SqlTypeDescriptor determineSqlTypeDescriptor(
			javax.persistence.TemporalType precision,
			TypeConfiguration typeConfiguration) {
		switch ( precision ) {
			case TIMESTAMP: {
				return typeConfiguration.getSqlTypeDescriptorRegistry().getDescriptor( Types.TIMESTAMP );
			}
			case DATE: {
				return typeConfiguration.getSqlTypeDescriptorRegistry().getDescriptor( Types.DATE );
			}
			case TIME: {
				return typeConfiguration.getSqlTypeDescriptorRegistry().getDescriptor( Types.TIME );
			}
			default: {
				throw new IllegalArgumentException( "Unexpected javax.persistence.TemporalType [" + precision + "]; expecting TIMESTAMP, DATE or TIME" );
			}
		}
	}

	@Override
	public boolean isDirty(Object old, Object current, SharedSessionContractImplementor session)
			throws HibernateException {
		return isDirty( old, current );

	}

	@Override
	public boolean isDirty(
			Object oldState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		return checkable[0] && isDirty( oldState, currentState );
	}

	protected final boolean isDirty(Object old, Object current) {
		return !getJavaTypeDescriptor().areEqual( (T) old, (T) current );
	}

	@Override
	public boolean isModified(
			Object dbState, Object currentState, boolean[] checkable, SharedSessionContractImplementor session)
			throws HibernateException {
		return false;
	}

	@Override
	public Object assemble(Serializable cached, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		return getMutabilityPlan().assemble( cached );
	}

	@Override
	public Serializable disassemble(T value, SharedSessionContractImplementor session, Object owner)
			throws HibernateException {
		return getMutabilityPlan().disassemble( value );
	}
}
