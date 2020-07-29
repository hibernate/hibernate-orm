/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query;

import java.util.List;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.model.source.internal.hbm.HbmLocalMetadataBuildingContext;

/**
 * @author Steve Ebersole
 */
public class HbmResultSetMappingDefinitionBuilder {
	private final String registrationName;

	private NamedResultSetMappingDefinition.ResultMapping  rootEntityReturn;
	private NamedResultSetMappingDefinition.ResultMapping  rootCollectionReturn;
	private List<NamedResultSetMappingDefinition.ResultMapping> joinReturns;
	private List<HbmResultSetMappingDefinition.ScalarMappingDefinition> rootScalarReturns;

	public HbmResultSetMappingDefinitionBuilder(String queryRegistrationName) {
		this.registrationName = queryRegistrationName;
	}

	public String getRegistrationName() {
		return registrationName;
	}

	public HbmResultSetMappingDefinitionBuilder addReturn(JaxbHbmNativeQueryScalarReturnType returnMapping) {
		rootScalarReturns.add(
				new HbmResultSetMappingDefinition.ScalarMappingDefinition(
						returnMapping.getColumn(),
						returnMapping.getType()
				)
		);
		return this;
	}

	public HbmResultSetMappingDefinitionBuilder addReturn(JaxbHbmNativeQueryReturnType returnMapping) {
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
				&& ( rootEntityReturn != null || rootScalarReturns != null ) ) {
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

		return new HbmResultSetMappingDefinition(
				registrationName,
				rootEntityReturn,
				rootCollectionReturn,
				joinReturns,
				rootScalarReturns
		);
	}
}
