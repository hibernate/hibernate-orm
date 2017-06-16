/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.internal.util.compare.EqualsHelper;
import org.hibernate.metamodel.model.domain.spi.VersionSupport;
import org.hibernate.sql.exec.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * Redefines the Type contract in terms of "basic" or "value" types.  All Type methods are implemented
 * using delegation with the bundled SqlTypeDescriptor, JavaTypeDescriptor and AttributeConverter.
 *
 * @author Steve Ebersole
 *
 * @since 6.0
 */
public interface BasicType<T>
		extends Type<T>, ExpressableType<T>, javax.persistence.metamodel.BasicType<T>, BasicValuedExpressableType<T> {
	@Override
	BasicJavaDescriptor<T> getJavaTypeDescriptor();

	/**
	 * Get the SqlSelectionReader that can be used to read values of this type
	 * from JDBC ResultSets
	 */
	SqlSelectionReader<T> getSqlSelectionReader();

	ColumnDescriptor getColumnDescriptor();


	@Override
	default Classification getClassification() {
		return Classification.BASIC;
	}

	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	default boolean areEqual(T x, T y) throws HibernateException {
		return EqualsHelper.areEqual( x, y );
	}

	@Override
	default int getNumberOfJdbcParametersForRestriction() {
		return 1;
	}

	@Override
	default Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	default Optional<VersionSupport<T>> getVersionSupport() {
		return null;
	}
}
