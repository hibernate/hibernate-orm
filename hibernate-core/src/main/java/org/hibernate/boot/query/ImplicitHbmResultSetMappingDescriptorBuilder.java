/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

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
import org.hibernate.boot.query.HbmResultSetMappingDescriptor.HbmFetchParent;
import org.hibernate.boot.query.HbmResultSetMappingDescriptor.JoinDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;

import static org.hibernate.boot.query.BootQueryLogging.BOOT_QUERY_LOGGER;
import static org.hibernate.boot.query.HbmResultSetMappingDescriptor.*;

/**
 * Builder for implicit ResultSet mapping defined inline as part of a named native query
 *
 * @author Steve Ebersole
 */
public class ImplicitHbmResultSetMappingDescriptorBuilder {

	private final String registrationName;
	private final MetadataBuildingContext metadataBuildingContext;

	private final List<ResultDescriptor> resultDescriptors;

	private Map<String, Map<String, JoinDescriptor>> joinDescriptors;
	private Map<String, HbmFetchParent> fetchParentByAlias;

	private boolean foundEntityReturn;
	private boolean foundCollectionReturn;


	public ImplicitHbmResultSetMappingDescriptorBuilder(String queryRegistrationName, MetadataBuildingContext metadataBuildingContext) {
		this.registrationName = queryRegistrationName;

		BOOT_QUERY_LOGGER.tracef(
				"Creating implicit HbmResultSetMappingDescriptor for named-native-query : %s",
				registrationName
		);

		this.metadataBuildingContext = metadataBuildingContext;

		this.resultDescriptors = new ArrayList<>();
	}

	public ImplicitHbmResultSetMappingDescriptorBuilder(String queryRegistrationName, int numberOfReturns, MetadataBuildingContext metadataBuildingContext) {
		this.registrationName = queryRegistrationName;
		this.metadataBuildingContext = metadataBuildingContext;

		this.resultDescriptors = new ArrayList<>( numberOfReturns );
	}

	public String getRegistrationName() {
		return registrationName;
	}

	public boolean hasAnyReturns() {
		return ! resultDescriptors.isEmpty();
	}

	public ImplicitHbmResultSetMappingDescriptorBuilder addReturn(JaxbHbmNativeQueryScalarReturnType returnMapping) {
		resultDescriptors.add(
				new ScalarDescriptor(
						returnMapping.getColumn(),
						returnMapping.getType()
				)
		);
		return this;
	}

	public ImplicitHbmResultSetMappingDescriptorBuilder addReturn(JaxbHbmNativeQueryReturnType returnMapping) {
		foundEntityReturn = true;
		final EntityResultDescriptor resultDescriptor = new EntityResultDescriptor(
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

	public ImplicitHbmResultSetMappingDescriptorBuilder addReturn(JaxbHbmNativeQueryJoinReturnType returnMapping) {
		if ( joinDescriptors == null ) {
			joinDescriptors = new HashMap<>();
		}

		if ( fetchParentByAlias == null ) {
			fetchParentByAlias = new HashMap<>();
		}

		collectJoinFetch(
				returnMapping,
				joinDescriptors,
				fetchParentByAlias,
				registrationName,
				metadataBuildingContext
		);
		return this;
	}

	public ImplicitHbmResultSetMappingDescriptorBuilder addReturn(JaxbHbmNativeQueryCollectionLoadReturnType returnMapping) {
		foundCollectionReturn = true;
		final CollectionResultDescriptor resultDescriptor = new CollectionResultDescriptor(
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
