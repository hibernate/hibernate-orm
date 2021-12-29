/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query.results.internal;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.query.results.BootResultMappingLogging;
import org.hibernate.boot.query.results.ResultDescriptor;
import org.hibernate.query.internal.ResultMementoBasicStandard;
import org.hibernate.query.results.ResultSetMappingResolutionContext;
import org.hibernate.type.BasicType;

/**
 * @see JaxbHbmNativeQueryScalarReturnType
 *
 * @author Steve Ebersole
 */
public class HbmScalarDescriptor implements ResultDescriptor {
	private final String columnName;
	private final String hibernateTypeName;

	public HbmScalarDescriptor(String columnName, String hibernateTypeName) {
		this.columnName = columnName;
		this.hibernateTypeName = hibernateTypeName;

		BootResultMappingLogging.LOGGER.debugf(
				"Creating ScalarDescriptor (%s)",
				columnName
		);
	}

	public HbmScalarDescriptor(JaxbHbmNativeQueryScalarReturnType hbmScalarReturn) {
		this( hbmScalarReturn.getColumn(), hbmScalarReturn.getType() );
	}

	@Override
	public String toString() {
		return "HbmScalarDescriptor(" + columnName + ")";
	}

	@Override
	public ResultMementoBasicStandard resolve(ResultSetMappingResolutionContext resolutionContext) {
		BootResultMappingLogging.LOGGER.debugf(
				"Resolving HBM ScalarDescriptor into memento - %s",
				columnName
		);

		if ( hibernateTypeName != null ) {
			final BasicType<?> namedType = resolutionContext.getSessionFactory()
					.getTypeConfiguration()
					.getBasicTypeRegistry()
					.getRegisteredType( hibernateTypeName );

			if ( namedType == null ) {
				throw new IllegalArgumentException( "Could not resolve named type : " + hibernateTypeName );
			}

			return new ResultMementoBasicStandard( columnName, namedType, resolutionContext );
		}

		// todo (6.0) : column name may be optional in HBM - double check

		return new ResultMementoBasicStandard( columnName, null, resolutionContext );
	}
}
