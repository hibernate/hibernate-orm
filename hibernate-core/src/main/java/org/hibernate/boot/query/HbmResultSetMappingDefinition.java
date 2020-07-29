/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.Builders;
import org.hibernate.query.results.ScalarResultBuilder;
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
public class HbmResultSetMappingDefinition implements NamedResultSetMappingDefinition {
	// todo (6.0) : see note on org.hibernate.boot.query.SqlResultSetMappingDefinition

	private final String registrationName;

	private final ResultMapping rootEntityReturn;
	private final ResultMapping  rootCollectionReturn;
	private final List<ResultMapping> joinReturns;
	private final List<ScalarMappingDefinition> scalarResultMappings;

	public HbmResultSetMappingDefinition(
			String registrationName,
			ResultMapping  rootEntityReturn,
			ResultMapping  rootCollectionReturn,
			List<ResultMapping> joinReturns,
			List<ScalarMappingDefinition> scalarResultMappings) {
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
	public NamedResultSetMappingMemento resolve(SessionFactoryImplementor factory) {
		final List<ScalarResultBuilder> scalarResultBuilders;
		if ( scalarResultMappings == null || scalarResultMappings.isEmpty() ) {
			scalarResultBuilders = null;
		}
		else {
			scalarResultBuilders = new ArrayList<>( scalarResultMappings.size() );
			scalarResultMappings.forEach(
					resultMapping -> {
						scalarResultBuilders.add( resultMapping.resolve( factory ) );
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
	public interface RootEntityMappingDefinition extends ResultMapping {
		String getEntityName();

		String getSqlAlias();

		List<HbmPropertyMappingDefinition> getProperties();

		String getDiscriminatorColumnName();

		LockMode getLockMode();
	}

	/**
	 * @see org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryPropertyReturnType
	 */
	public interface HbmPropertyMappingDefinition extends ResultMapping {
		String getPropertyPath();
		List<String> getColumnNames();
	}

	/**
	 * @see org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType
	 */
	public interface JoinMappingDefinition extends ResultMapping {
		String getJoinedPropertyPath();

		String getSqlAlias();

		List<HbmPropertyMappingDefinition> getProperties();

		LockMode getLockMode();
	}

	/**
	 * @see org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType
	 */
	public interface RootCollectionMappingDefinition extends ResultMapping {
		String getCollectionRole();

		String getSqlAlias();

		List<HbmPropertyMappingDefinition> getProperties();

		LockMode getLockMode();
	}

	/**
	 * @see org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType
	 */
	public static class ScalarMappingDefinition implements ResultMapping {
		private final String columnName;
		private final String hibernateTypeName;

		public ScalarMappingDefinition(String columnName, String hibernateTypeName) {
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
		public ScalarResultBuilder resolve(SessionFactoryImplementor factory) {
			if ( hibernateTypeName != null ) {
				final BasicType<?> namedType = factory.getTypeConfiguration()
						.getBasicTypeRegistry()
						.getRegisteredType( hibernateTypeName );

				if ( namedType == null ) {
					throw new IllegalArgumentException( "Could not resolve named type : " + hibernateTypeName );
				}

				return Builders.scalar( columnName );
			}

			// todo (6.0) : column name may be optional in HBM - double check

			return Builders.scalar( columnName );
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
