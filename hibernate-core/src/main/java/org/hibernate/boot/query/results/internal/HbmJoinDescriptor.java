/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query.results.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType;
import org.hibernate.boot.query.results.BootResultMappingLogging;
import org.hibernate.boot.query.results.HbmFetchDescriptor;
import org.hibernate.boot.query.results.HbmFetchParent;
import org.hibernate.boot.query.results.HbmResultSetMappingDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.internal.FetchMementoHbmStandard;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.named.ResultMemento;
import org.hibernate.query.results.FetchParentMemento;
import org.hibernate.query.results.ResultSetMappingResolutionContext;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;

/**
 * @see JaxbHbmNativeQueryJoinReturnType
 *
 * @author Steve Ebersole
 */
public class HbmJoinDescriptor implements HbmFetchDescriptor, HbmFetchParent {
	private final String ownerTableAlias;
	private final String tableAlias;
	private final String propertyPath;
	private final LockMode lockMode;
	private final List<HbmFetchDescriptor> propertyFetchDescriptors;
	private final Supplier<Map<String, Map<String, HbmJoinDescriptor>>> joinDescriptorsAccess;
	private final Supplier<Map<String, HbmFetchParent>> fetchParentByAliasAccess;

	public HbmJoinDescriptor(
			JaxbHbmNativeQueryJoinReturnType hbmJoinReturn,
			Supplier<Map<String, Map<String, HbmJoinDescriptor>>> joinDescriptorsAccess,
			Supplier<Map<String, HbmFetchParent>> fetchParentByAliasAccess,
			String registrationName,
			MetadataBuildingContext context) {
		this.joinDescriptorsAccess = joinDescriptorsAccess;
		this.fetchParentByAliasAccess = fetchParentByAliasAccess;
		final String fullPropertyPath = hbmJoinReturn.getProperty();
		final int firstDot = fullPropertyPath.indexOf( '.' );
		if ( firstDot < 1 ) {
			throw new MappingException(
					"Illegal <return-join/> property attribute: `" + fullPropertyPath + "`.  Should"
							+ "be in the form `{ownerAlias.joinedPropertyPath}`"
			);
		}

		this.ownerTableAlias = fullPropertyPath.substring( 0, firstDot );

		this.propertyPath = fullPropertyPath.substring( firstDot + 1 );
		this.tableAlias = hbmJoinReturn.getAlias();
		if ( tableAlias == null ) {
			throw new MappingException(
					"<return-join/> did not specify alias [" + ownerTableAlias + "." + propertyPath + "]"
			);
		}

		this.lockMode = hbmJoinReturn.getLockMode();
		this.propertyFetchDescriptors = HbmResultSetMappingDescriptor.extractPropertyFetchDescriptors(
				hbmJoinReturn.getReturnProperty(),
				this,
				registrationName,
				context
		);
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"HbmJoinDescriptor(%s.%s : %s)",
				ownerTableAlias,
				propertyPath,
				tableAlias
		);
	}

	@Override
	public String getFetchablePath() {
		return propertyPath;
	}

	private FetchMementoHbmStandard memento;
	private HbmFetchParentMemento thisAsParentMemento;

	@Override
	public FetchMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
		BootResultMappingLogging.LOGGER.debugf(
				"Resolving HBM JoinDescriptor into memento - %s : %s . %s",
				tableAlias,
				ownerTableAlias,
				propertyPath
		);

		if ( memento == null ) {
			final HbmFetchParentMemento thisAsParentMemento = resolveParentMemento( resolutionContext );

			HbmResultSetMappingDescriptor.applyFetchJoins( joinDescriptorsAccess.get(), tableAlias, propertyFetchDescriptors );

			final Map<String, FetchMemento> fetchDescriptorMap = new HashMap<>();
			propertyFetchDescriptors.forEach(
					hbmFetchDescriptor -> fetchDescriptorMap.put(
							hbmFetchDescriptor.getFetchablePath(),
							hbmFetchDescriptor.resolve( resolutionContext )
					)
			);
			memento = new FetchMementoHbmStandard(
					thisAsParentMemento.getNavigablePath(),
					ownerTableAlias,
					tableAlias,
					lockMode,
					thisAsParentMemento,
					fetchDescriptorMap,
					(Fetchable) thisAsParentMemento.getFetchableContainer()
			);
		}

		return memento;
	}

	@Override
	public HbmFetchParentMemento resolveParentMemento(ResultSetMappingResolutionContext resolutionContext) {
		if ( thisAsParentMemento == null ) {
			final HbmFetchParent hbmFetchParent = fetchParentByAliasAccess.get().get( ownerTableAlias );
			if ( hbmFetchParent == null ) {
				throw new MappingException(
						"Could not locate join-return owner by alias [" + ownerTableAlias + "] for join path [" + propertyPath + "]"
				);
			}

			final FetchParentMemento ownerMemento = hbmFetchParent.resolveParentMemento( resolutionContext );

			final String[] parts = propertyPath.split( "\\." );
			NavigablePath navigablePath = ownerMemento.getNavigablePath().append( parts[ 0 ] );
			FetchableContainer fetchable = (FetchableContainer) ownerMemento.getFetchableContainer()
					.findSubPart( parts[ 0 ], null );

			for ( int i = 1; i < parts.length; i++ ) {
				navigablePath = navigablePath.append( parts[ i ] );
				fetchable = (FetchableContainer) fetchable.findSubPart( parts[ i ], null );
			}

			thisAsParentMemento = new HbmFetchParentMemento( navigablePath, fetchable );
		}

		return thisAsParentMemento;
	}

	@Override
	public ResultMemento asResultMemento(
			NavigablePath path,
			ResultSetMappingResolutionContext resolutionContext) {
		throw new UnsupportedOperationException();
	}
}
