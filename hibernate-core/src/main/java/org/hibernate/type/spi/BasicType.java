/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.util.Objects;
import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.spi.BasicTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.results.spi.SqlSelectionReader;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Redefines the Type contract in terms of "basic" or "value" types which is
 * a mapping from a Java type (JavaTypeDescriptor) to a single SQL type
 * (SqlTypeDescriptor).
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
@Incubating( since = "6.0" )
public interface BasicType<T>
		extends Type<T>, BasicValuedExpressableType<T>, BasicTypeDescriptor<T> {
	@Override
	BasicJavaDescriptor<T> getJavaTypeDescriptor();

	/**
	 * The descriptor of the SQL type part of this basic-type
	 */
	SqlTypeDescriptor getSqlTypeDescriptor();

	/**
	 * Get the SqlSelectionReader that can be used to read values of this type
	 * from JDBC ResultSets
	 */
	SqlSelectionReader<T> getSqlSelectionReader();

	default SqlExpressableType getSqlExpressableType(TypeConfiguration typeConfiguration) {
		return getSqlTypeDescriptor().getSqlExpressableType(
				getJavaTypeDescriptor(),
				typeConfiguration
		);
	}

	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	default boolean areEqual(T x, T y) throws HibernateException {
		return Objects.equals( x, y );
	}

	@Override
	default Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	default Optional<VersionSupport<T>> getVersionSupport() {
		return Optional.empty();
	}
}
