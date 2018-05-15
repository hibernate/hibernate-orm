/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.sql.spi;

import org.hibernate.sql.JdbcValueBinder;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Abstract SqlTypeDescriptor that defines templated support for handling
 * JdbcValueMapper resolution.
 *
 * @see TypeConfiguration#resolveJdbcValueMapper
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTemplateSqlTypeDescriptor implements SqlTypeDescriptor {
	/**
	 * Delegates to {@link TypeConfiguration#resolveJdbcValueMapper}
	 *
	 * @implSpec Defined as final since the whole point of this base class is to support
	 * this method via templating.  Subclasses should instead implement {@link #createBinder}
	 * and {@link #createExtractor}.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public final SqlExpressableType getSqlExpressableType(
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.resolveJdbcValueMapper(
				this,
				javaTypeDescriptor,
				jtd -> {
					final JdbcValueBinder binder = createBinder( jtd, typeConfiguration );
					final JdbcValueExtractor extractor = createExtractor( jtd, typeConfiguration );

					return new StandardSqlExpressableTypeImpl( jtd, this, extractor, binder );
				}
		);
	}

	/**
	 * Called from {@link SqlTypeDescriptor#getSqlExpressableType} when needing to create the mapper.
	 *
	 * @implNote The value returned from here will be the {@link SqlExpressableType#getJdbcValueBinder()}
	 * for the mapper returned from {@link SqlTypeDescriptor#getSqlExpressableType}
	 */
	@SuppressWarnings("WeakerAccess")
	protected abstract <X> JdbcValueBinder<X> createBinder(
			BasicJavaDescriptor<X> javaTypeDescriptor,
			TypeConfiguration typeConfiguration);

	/**
	 * Called from {@link SqlTypeDescriptor#getSqlExpressableType} when needing to create the mapper.
	 *
	 * @implNote The value returned from here will be the {@link SqlExpressableType#getJdbcValueExtractor}
	 * for the mapper returned from {@link SqlTypeDescriptor#getSqlExpressableType}
	 */
	@SuppressWarnings("WeakerAccess")
	protected abstract <X> JdbcValueExtractor<X> createExtractor(
			BasicJavaDescriptor<X> javaTypeDescriptor,
			TypeConfiguration typeConfiguration);
}
