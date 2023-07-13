/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.IncomparableComparator;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Uses object identity for {@code equals}/{@code hashCode} as we ensure that internally.
 *
 * @author Christian Beikov
 */
public class EntityJavaType<T> extends AbstractClassJavaType<T> {

	public EntityJavaType(Class<T> type, MutabilityPlan<T> mutabilityPlan) {
		super( type, mutabilityPlan , IncomparableComparator.INSTANCE );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		throw new JdbcTypeRecommendationException(
				"Could not determine recommended JdbcType for `" + getJavaType().getTypeName() + "`"
		);
	}

	@Override
	public int extractHashCode(T value) {
		return System.identityHashCode( value );
	}

	@Override
	public boolean areEqual(T one, T another) {
		return one == another;
	}

	@Override
	public String toString(T value) {
		return value.toString();
	}

	@Override
	public T fromString(CharSequence string) {
		throw new UnsupportedOperationException(
				"Conversion from String strategy not known for this Java type : " + getJavaType().getTypeName()
		);
	}

	@Override
	public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
		throw new UnsupportedOperationException(
				"Unwrap strategy not known for this Java type : " + getJavaType().getTypeName()
		);
	}

	@Override
	public <X> T wrap(X value, WrapperOptions options) {
		throw new UnsupportedOperationException(
				"Wrap strategy not known for this Java type : " + getJavaType().getTypeName()
		);
	}

	@Override
	public String toString() {
		return "EntityJavaType(" + getJavaType().getTypeName() + ")";
	}
}
