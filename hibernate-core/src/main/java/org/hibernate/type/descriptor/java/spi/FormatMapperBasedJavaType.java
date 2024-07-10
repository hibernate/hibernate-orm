/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import java.io.Serializable;
import java.lang.reflect.Type;

import org.hibernate.Incubating;
import org.hibernate.SharedSessionContract;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractJavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Java type for {@link FormatMapper} based types i.e. {@link org.hibernate.type.SqlTypes#JSON}
 * or {@link org.hibernate.type.SqlTypes#SQLXML} mapped types.
 *
 * @author Christian Beikov
 * @author Yanming Zhou
 */
@Incubating
public abstract class FormatMapperBasedJavaType<T> extends AbstractJavaType<T> implements MutabilityPlan<T> {

	private final TypeConfiguration typeConfiguration;

	public FormatMapperBasedJavaType(
			Type type,
			MutabilityPlan<T> mutabilityPlan,
			TypeConfiguration typeConfiguration) {
		super( type, mutabilityPlan );
		this.typeConfiguration = typeConfiguration;
	}

	protected abstract FormatMapper getFormatMapper(TypeConfiguration typeConfiguration);

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		throw new JdbcTypeRecommendationException(
				"Could not determine recommended JdbcType for Java type '" + getTypeName() + "'"
		);
	}

	@Override
	public String toString(T value) {
		return getFormatMapper( typeConfiguration ).toString(
				value,
				this,
				typeConfiguration.getSessionFactory().getWrapperOptions()
		);
	}

	@Override
	public T fromString(CharSequence string) {
		return getFormatMapper( typeConfiguration ).fromString(
				string,
				this,
				typeConfiguration.getSessionFactory().getWrapperOptions()
		);
	}

	@Override
	public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
		if ( type.isAssignableFrom( getJavaTypeClass() ) ) {
			//noinspection unchecked
			return (X) value;
		}
		else if ( type == String.class ) {
			//noinspection unchecked
			return (X) getFormatMapper( typeConfiguration ).toString( value, this, options );
		}
		throw new UnsupportedOperationException(
				"Unwrap strategy not known for this Java type: " + getTypeName()
		);
	}

	@Override
	public <X> T wrap(X value, WrapperOptions options) {
		if ( getJavaTypeClass().isInstance( value ) ) {
			//noinspection unchecked
			return (T) value;
		}
		else if ( value instanceof String ) {
			return getFormatMapper( typeConfiguration ).fromString( (String) value, this, options );
		}
		throw new UnsupportedOperationException(
				"Wrap strategy not known for this Java type: " + getTypeName()
		);
	}

	@Override
	public MutabilityPlan<T> getMutabilityPlan() {
		final MutabilityPlan<T> mutabilityPlan = super.getMutabilityPlan();
		return mutabilityPlan == null ? this : mutabilityPlan;
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public T deepCopy(T value) {
		return value == null ? null : fromString( toString( value ) );
	}

	@Override
	public Serializable disassemble(T value, SharedSessionContract session) {
		return value == null ? null : toString( value );
	}

	@Override
	public T assemble(Serializable cached, SharedSessionContract session) {
		return cached == null ? null : fromString( (CharSequence) cached );
	}
}
