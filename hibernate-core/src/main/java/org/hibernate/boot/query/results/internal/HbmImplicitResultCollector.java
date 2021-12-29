/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query.results.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.model.source.internal.hbm.HbmLocalMetadataBuildingContext;
import org.hibernate.boot.query.BootQueryLogging;
import org.hibernate.boot.query.results.HbmResultSetMappingDescriptor;
import org.hibernate.boot.query.results.HbmFetchParent;
import org.hibernate.boot.query.results.ResultDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;

import static org.hibernate.boot.query.results.HbmResultSetMappingDescriptor.*;

/**
 * Builder for implicit ResultSet mapping defined inline as part of a named native query
 *
 * @author Steve Ebersole
 */
public class HbmImplicitResultCollector {

	private final String registrationName;
	private final MetadataBuildingContext metadataBuildingContext;

	private final List<ResultDescriptor> resultDescriptors;

	private Map<String, Map<String, HbmJoinDescriptor>> joinDescriptors;
	private Map<String, HbmFetchParent> fetchParentByAlias;

	private boolean foundEntityReturn;
	private boolean foundCollectionReturn;


	public HbmImplicitResultCollector(String queryRegistrationName, MetadataBuildingContext metadataBuildingContext) {
		this.registrationName = queryRegistrationName;

		BootQueryLogging.LOGGER.debugf(
				"Creating implicit HbmResultSetMappingDescriptor for named-native-query : %s",
				registrationName
		);

		this.metadataBuildingContext = metadataBuildingContext;

		this.resultDescriptors = new ArrayList<>();
	}

	public String getRegistrationName() {
		return registrationName;
	}

	public boolean hasAnyReturns() {
		return ! resultDescriptors.isEmpty();
	}

	public HbmImplicitResultCollector addReturn(JaxbHbmNativeQueryScalarReturnType rtn) {
		resultDescriptors.add( new HbmScalarDescriptor( rtn.getColumn(), rtn.getType() ) );
		return this;
	}

	public HbmImplicitResultCollector addReturn(JaxbHbmNativeQueryReturnType rtn) {
		foundEntityReturn = true;
		final HbmEntityResultDescriptor resultDescriptor = new HbmEntityResultDescriptor(
				rtn,
				() -> joinDescriptors,
				registrationName,
				metadataBuildingContext
		);

		resultDescriptors.add( resultDescriptor );

		if ( fetchParentByAlias == null ) {
			fetchParentByAlias = new HashMap<>();
		}
		fetchParentByAlias.put( rtn.getAlias(), resultDescriptor );

		return this;
	}

	public HbmImplicitResultCollector addReturn(JaxbHbmNativeQueryJoinReturnType returnMapping) {
		if ( joinDescriptors == null ) {
			joinDescriptors = new HashMap<>();
		}

		if ( fetchParentByAlias == null ) {
			fetchParentByAlias = new HashMap<>();
		}

		applyJoinFetch(
				returnMapping,
				joinDescriptors,
				fetchParentByAlias,
				registrationName,
				metadataBuildingContext
		);
		return this;
	}

	public HbmImplicitResultCollector addReturn(JaxbHbmNativeQueryCollectionLoadReturnType returnMapping) {
		foundCollectionReturn = true;
		final HbmCollectionResultDescriptor resultDescriptor = new HbmCollectionResultDescriptor(
				returnMapping,
				() -> joinDescriptors,
				registrationName,
				metadataBuildingContext
		);

		resultDescriptors.add( resultDescriptor );

		if ( fetchParentByAlias == null ) {
			fetchParentByAlias = new HashMap<>();
		}
		fetchParentByAlias.put( returnMapping.getAlias(), resultDescriptor );

		return this;
	}

	public HbmResultSetMappingDescriptor build(HbmLocalMetadataBuildingContext context) {
		if ( foundCollectionReturn && resultDescriptors.size() > 1 ) {
			throw new MappingException(
					"HBM return-collection ResultSet mapping cannot define entity or scalar returns : " + registrationName,
					context.getOrigin()
			);
		}

		if ( joinDescriptors != null ) {
			if ( ! foundEntityReturn && ! foundCollectionReturn ) {
				throw new MappingException(
						"HBM return-join ResultSet mapping must be used in conjunction with root entity or collection return : " + registrationName,
						context.getOrigin()
				);
			}
		}

		return new HbmResultSetMappingDescriptor(
				registrationName,
				resultDescriptors,
				joinDescriptors != null
						? joinDescriptors
						: Collections.emptyMap(),
				fetchParentByAlias != null
						? fetchParentByAlias
						: Collections.emptyMap()
		);
	}
}
