/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query.results.internal;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType;
import org.hibernate.boot.query.results.BootResultMappingLogging;
import org.hibernate.boot.query.results.HbmFetchDescriptor;
import org.hibernate.boot.query.results.HbmFetchParent;
import org.hibernate.boot.query.results.HbmResultSetMappingDescriptor;
import org.hibernate.boot.query.results.ResultDescriptor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.internal.ResultMementoCollectionStandard;
import org.hibernate.query.named.ResultMemento;
import org.hibernate.query.results.FetchParentMemento;
import org.hibernate.query.results.ResultSetMappingResolutionContext;

/**
 * @see JaxbHbmNativeQueryCollectionLoadReturnType
 *
 * @author Steve Ebersole
 */
public class HbmCollectionResultDescriptor implements ResultDescriptor, HbmFetchParent {
	private final NavigablePath collectionPath;
	private final String tableAlias;
	private final LockMode lockMode;
	private final Supplier<Map<String, Map<String, HbmJoinDescriptor>>> joinDescriptorsAccess;
	private final List<HbmFetchDescriptor> propertyFetchDescriptors;

	private ResultMemento memento;
	private FetchParentMemento thisAsParentMemento;

	public HbmCollectionResultDescriptor(
			JaxbHbmNativeQueryCollectionLoadReturnType hbmCollectionReturn,
			Supplier<Map<String, Map<String, HbmJoinDescriptor>>> joinDescriptorsAccess,
			String registrationName,
			MetadataBuildingContext context) {
		final String role = hbmCollectionReturn.getRole();
		final int dotIndex = role.indexOf( '.' );
		final String entityName = role.substring( 0, dotIndex );
		final InFlightMetadataCollector metadataCollector = context.getMetadataCollector();
		final String fullEntityName = metadataCollector.getImports().get( entityName );
		this.collectionPath = new NavigablePath(
				fullEntityName + "." + role.substring( dotIndex + 1 )
		);
		this.tableAlias = hbmCollectionReturn.getAlias();
		if ( tableAlias == null ) {
			throw new MappingException(
					"<return-collection/> did not specify alias [" + collectionPath.getFullPath() + "]"
			);
		}

		BootResultMappingLogging.LOGGER.debugf(
				"Creating CollectionResultDescriptor (%s : %s)",
				tableAlias,
				collectionPath
		);

		this.lockMode = hbmCollectionReturn.getLockMode();
		this.joinDescriptorsAccess = joinDescriptorsAccess;

		this.propertyFetchDescriptors = HbmResultSetMappingDescriptor.extractPropertyFetchDescriptors(
				hbmCollectionReturn.getReturnProperty(),
				this,
				registrationName,
				context
		);
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"HbmCollectionResultDescriptor(%s : %s)",
				collectionPath.getFullPath(),
				tableAlias
		);
	}

	public NavigablePath getCollectionPath() {
		return collectionPath;
	}

	public String getTableAlias() {
		return tableAlias;
	}

	@Override
	public ResultMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
		BootResultMappingLogging.LOGGER.debugf(
				"Resolving HBM CollectionResultDescriptor into memento - %s : %s",
				tableAlias,
				collectionPath
		);

		if ( memento == null ) {
			HbmResultSetMappingDescriptor.applyFetchJoins( joinDescriptorsAccess, tableAlias, propertyFetchDescriptors );

			final FetchParentMemento thisAsParentMemento = resolveParentMemento( resolutionContext );

			memento = new ResultMementoCollectionStandard(
					tableAlias,
					thisAsParentMemento.getNavigablePath(),
					(PluralAttributeMapping) thisAsParentMemento.getFetchableContainer()
			);
		}

		return memento;
	}

	@Override
	public FetchParentMemento resolveParentMemento(ResultSetMappingResolutionContext resolutionContext) {
		if ( thisAsParentMemento == null ) {
			final CollectionPersister collectionDescriptor = resolutionContext.getSessionFactory()
					.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getCollectionDescriptor( collectionPath.getFullPath() );

			thisAsParentMemento = new HbmFetchParentMemento( collectionPath, collectionDescriptor.getAttributeMapping() );
		}

		return thisAsParentMemento;
	}
}
