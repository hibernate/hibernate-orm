/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.basic;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.VersionSupport;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractBasicTypeImpl<T> implements BasicType<T> {

	@Override
	public VersionSupport<T> getVersionSupport() {
		return null;
	}

	@Override
	public String getTypeName() {
		// todo : improve this to account for converters, etc
		return getJavaTypeDescriptor().getJavaTypeClass().getName();
	}

	@Override
	@SuppressWarnings("unchecked")
	public T hydrate(
			ResultSet rs,
			String[] names,
			SharedSessionContractImplementor session,
			Object owner) throws HibernateException, SQLException {
		final AttributeConverterDefinition converterDefinition = getAttributeConverterDefinition();

		if ( converterDefinition == null ) {
			return getColumnMapping().getSqlTypeDescriptor().getExtractor( getJavaTypeDescriptor() ).extract(
					rs,
					names[0],
					session
			);
		}
		else {
			final Object databaseValue = getColumnMapping().getSqlTypeDescriptor().getExtractor( converterDefinition.getJdbcType() ).extract(
					rs,
					names[0],
					session
			);

			return (T) converterDefinition.getAttributeConverter().convertToEntityAttribute( databaseValue );
		}
	}

	@Override
	public int getColumnSpan() {
		return 1;
	}
}
