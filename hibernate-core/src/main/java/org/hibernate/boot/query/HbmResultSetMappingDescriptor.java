/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.internal.ResultMementoBasicStandard;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.type.BasicType;

/**
 * Boot-time descriptor of a result-set-mapping as defined in an `hbm.xml` file
 * either implicitly or explicitly
 *
 * @see org.hibernate.Session#createNativeQuery(String, String)
 * @see org.hibernate.Session#createStoredProcedureCall(String, String[])
 * @see org.hibernate.Session#createStoredProcedureCall(String, String[])
 *
 * @author Steve Ebersole
 */
public class HbmResultSetMappingDescriptor implements NamedResultSetMappingDescriptor {
	// todo (6.0) : see note on org.hibernate.boot.query.SqlResultSetMappingDefinition

	private final String registrationName;

	private final ResultDescriptor rootEntityReturn;
	private final ResultDescriptor rootCollectionReturn;
	private final List<ResultDescriptor> joinReturns;
	private final List<ScalarDescriptor> scalarResultMappings;

	public HbmResultSetMappingDescriptor(
			String registrationName,
			ResultDescriptor rootEntityReturn,
			ResultDescriptor rootCollectionReturn,
			List<ResultDescriptor> joinReturns,
			List<ScalarDescriptor> scalarResultMappings) {
		this.registrationName = registrationName;

		this.rootEntityReturn = rootEntityReturn;
		this.rootCollectionReturn = rootCollectionReturn;
		this.joinReturns = joinReturns;
		this.scalarResultMappings = scalarResultMappings;
	}

	@Override
	public String getRegistrationName() {
		return registrationName;
	}

	@Override
	public NamedResultSetMappingMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
		final List<ResultMementoBasicStandard> scalarResultMementos;
		if ( scalarResultMappings == null || scalarResultMappings.isEmpty() ) {
			scalarResultMementos = Collections.emptyList();
		}
		else {
			scalarResultMementos = new ArrayList<>( scalarResultMappings.size() );
			scalarResultMappings.forEach(
					resultMapping -> {
						scalarResultMementos.add( resultMapping.resolve( resolutionContext ) );
					}
			);
		}

		throw new NotYetImplementedFor6Exception( getClass() );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// `hbm.xml` returns

	/**
	 * @see org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType
	 */
	public interface RootEntityDescriptor extends ResultDescriptor {
		String getEntityName();

		String getSqlAlias();

		List<HbmPropertyDescriptor> getProperties();

		String getDiscriminatorColumnName();

		LockMode getLockMode();
	}

	/**
	 * @see org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryPropertyReturnType
	 */
	public interface HbmPropertyDescriptor extends ResultDescriptor {
		String getPropertyPath();
		List<String> getColumnNames();
	}

	/**
	 * @see org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType
	 */
	public interface JoinDescriptor extends ResultDescriptor {
		String getJoinedPropertyPath();

		String getSqlAlias();

		List<HbmPropertyDescriptor> getProperties();

		LockMode getLockMode();
	}

	/**
	 * @see org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType
	 */
	public interface RootCollectionDescriptor extends ResultDescriptor {
		String getCollectionRole();

		String getSqlAlias();

		List<HbmPropertyDescriptor> getProperties();

		LockMode getLockMode();
	}

	/**
	 * @see org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType
	 */
	public static class ScalarDescriptor implements ResultDescriptor {
		private final String columnName;
		private final String hibernateTypeName;

		public ScalarDescriptor(String columnName, String hibernateTypeName) {
			this.columnName = columnName;
			this.hibernateTypeName = hibernateTypeName;
		}

		public String getColumnName() {
			return columnName;
		}

		public String getHibernateTypeName() {
			return hibernateTypeName;
		}

		@Override
		public ResultMementoBasicStandard resolve(ResultSetMappingResolutionContext resolutionContext) {
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA returns

	/**
	 * @see javax.persistence.ColumnResult
	 */
	interface JpaColumnResult {
	}
}
