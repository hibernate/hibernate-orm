/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.spi;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.model.source.internal.hbm.HbmLocalMetadataBuildingContext;

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
public interface HbmResultSetMappingDefinition extends NamedResultSetMappingDefinition {

	class Builder {
		private final String registrationName;

		private RootEntityMappingDefinition rootEntityReturn;
		private RootCollectionMappingDefinition rootCollectionReturn;
		private List<JoinMappingDefinition> joinReturns;
		private List<ScalarMappingDefinition> rootScalarReturns;

		public Builder(String queryRegistrationName) {
			this.registrationName = queryRegistrationName;
		}

		public String getRegistrationName() {
			return registrationName;
		}

		public Builder addReturn(JaxbHbmNativeQueryScalarReturnType returnMapping) {
			throw new NotYetImplementedFor6Exception();
		}

		public Builder addReturn(JaxbHbmNativeQueryReturnType returnMapping) {
			throw new NotYetImplementedFor6Exception();
		}

		public void addReturn(JaxbHbmNativeQueryJoinReturnType returnMapping) {
			throw new NotYetImplementedFor6Exception();
		}

		public void addReturn(JaxbHbmNativeQueryCollectionLoadReturnType returnMapping) {
			throw new NotYetImplementedFor6Exception();
		}

		public boolean hasAnyReturns() {
			return rootEntityReturn != null || rootCollectionReturn != null || rootScalarReturns != null;
		}

		public HbmResultSetMappingDefinition build(HbmLocalMetadataBuildingContext context) {
			if ( rootCollectionReturn != null
					&& ( rootEntityReturn != null ||  rootScalarReturns != null ) ) {
				throw new MappingException(
						"HBM return-collection ResultSet mapping cannot define an entity or scalar returns : " + registrationName,
						context.getOrigin()
				);
			}

			if ( joinReturns != null ) {
				if ( rootEntityReturn == null && rootCollectionReturn == null ) {
					throw new MappingException(
							"HBM return-join ResultSet mapping must be used in conjunction with root entity or collection return : " + registrationName,
							context.getOrigin()
					);
				}
			}

			throw new NotYetImplementedFor6Exception();
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// `hbm.xml` returns

	/**
	 * @see org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType
	 */
	interface RootEntityMappingDefinition {
		String getEntityName();

		String getSqlAlias();

		List<HbmPropertyMappingDefinition> getProperties();

		String getDiscriminatorColumnName();

		LockMode getLockMode();
	}

	/**
	 * @see org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryPropertyReturnType
	 */
	interface HbmPropertyMappingDefinition {
		String getPropertyPath();
		List<String> getColumnNames();
	}

	/**
	 * @see org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType
	 */
	interface JoinMappingDefinition {
		String getJoinedPropertyPath();

		String getSqlAlias();

		List<HbmPropertyMappingDefinition> getProperties();

		LockMode getLockMode();
	}

	/**
	 * @see org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType
	 */
	interface RootCollectionMappingDefinition {
		String getCollectionRole();

		String getSqlAlias();

		List<HbmPropertyMappingDefinition> getProperties();

		LockMode getLockMode();
	}

	/**
	 * @see org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType
	 */
	interface ScalarMappingDefinition {
		String getColumnName();
		String getTypeName();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA returns

	/**
	 * @see javax.persistence.ColumnResult
	 */
	interface JpaColumnResult {
	}
}
