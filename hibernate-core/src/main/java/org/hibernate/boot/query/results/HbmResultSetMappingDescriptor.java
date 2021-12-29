/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.query.results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryPropertyReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmResultSetMappingType;
import org.hibernate.boot.query.results.internal.HbmCollectionResultDescriptor;
import org.hibernate.boot.query.results.internal.HbmEntityResultDescriptor;
import org.hibernate.boot.query.results.internal.HbmJoinDescriptor;
import org.hibernate.boot.query.results.internal.HbmPropertyFetchDescriptor;
import org.hibernate.boot.query.results.internal.HbmScalarDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.internal.NamedResultSetMappingMementoImpl;
import org.hibernate.query.results.ResultSetMappingResolutionContext;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.named.ResultMemento;

/**
 * Boot-time descriptor of a result-set mapping as defined in an `hbm.xml` file
 * either implicitly ({@code <sql-query/>}) or explicitly ({@code <resultset/>})
 *
 * @author Steve Ebersole
 */
public class HbmResultSetMappingDescriptor implements NamedResultSetMappingDescriptor {

	private final String registrationName;
	private final List<ResultDescriptor> resultDescriptors;
	private final Map<String, Map<String, HbmJoinDescriptor>> joinDescriptors;
	private final Map<String, HbmFetchParent> fetchParentByAlias;


	/**
	 * Constructor for an explicit `<resultset/>` mapping.
	 */
	public HbmResultSetMappingDescriptor(
			JaxbHbmResultSetMappingType hbmResultSetMapping,
			MetadataBuildingContext context) {
		this.registrationName = hbmResultSetMapping.getName();

		BootResultMappingLogging.LOGGER.debugf(
				"Creating explicit HbmResultSetMappingDescriptor : %s",
				registrationName
		);

		final List<?> hbmValueMappingsSource = hbmResultSetMapping.getValueMappingSources();
		final List<ResultDescriptor> localResultDescriptors = CollectionHelper.arrayList( hbmValueMappingsSource.size() );
		this.joinDescriptors = new HashMap<>();
		this.fetchParentByAlias = new HashMap<>();

		boolean foundCollectionReturn = false;

		//noinspection ForLoopReplaceableByForEach
		for ( int i = 0; i < hbmValueMappingsSource.size(); i++ ) {
			final Object hbmValueMapping = hbmValueMappingsSource.get( i );

			if ( hbmValueMapping == null ) {
				throw new IllegalStateException(
						"ValueMappingSources contained null reference(s)"
				);
			}

			if ( hbmValueMapping instanceof JaxbHbmNativeQueryReturnType ) {
				final JaxbHbmNativeQueryReturnType hbmEntityReturn = (JaxbHbmNativeQueryReturnType) hbmValueMapping;

				final HbmEntityResultDescriptor entityResultDescriptor = new HbmEntityResultDescriptor(
						hbmEntityReturn,
						() -> joinDescriptors,
						registrationName,
						context
				);
				localResultDescriptors.add( entityResultDescriptor );
				fetchParentByAlias.put( entityResultDescriptor.getTableAlias(), entityResultDescriptor );
			}
			else if ( hbmValueMapping instanceof JaxbHbmNativeQueryCollectionLoadReturnType ) {
				final JaxbHbmNativeQueryCollectionLoadReturnType hbmCollectionReturn = (JaxbHbmNativeQueryCollectionLoadReturnType) hbmValueMapping;
				foundCollectionReturn = true;

				final HbmCollectionResultDescriptor collectionResultDescriptor = new HbmCollectionResultDescriptor(
						hbmCollectionReturn,
						() -> joinDescriptors,
						registrationName,
						context
				);
				localResultDescriptors.add( collectionResultDescriptor );
				fetchParentByAlias.put( collectionResultDescriptor.getTableAlias(), collectionResultDescriptor );
			}
			else if ( hbmValueMapping instanceof JaxbHbmNativeQueryJoinReturnType ) {
				final JaxbHbmNativeQueryJoinReturnType jaxbHbmJoinReturn = (JaxbHbmNativeQueryJoinReturnType) hbmValueMapping;

				applyJoinFetch( jaxbHbmJoinReturn, joinDescriptors, fetchParentByAlias, registrationName, context );
			}
			else if ( hbmValueMapping instanceof JaxbHbmNativeQueryScalarReturnType ) {
				final JaxbHbmNativeQueryScalarReturnType hbmScalarReturn = (JaxbHbmNativeQueryScalarReturnType) hbmValueMapping;

				localResultDescriptors.add( new HbmScalarDescriptor( hbmScalarReturn ) );
			}
			else {
				throw new IllegalArgumentException(
						"Unknown NativeQueryReturn type : " + hbmValueMapping.getClass().getName()
				);
			}
		}

		if ( foundCollectionReturn && localResultDescriptors.size() > 1 ) {
			throw new MappingException(
					"Cannot combine other returns with a collection return (" + registrationName + ")"
			);
		}

		this.resultDescriptors = localResultDescriptors;
	}

	public static void applyJoinFetch(
			JaxbHbmNativeQueryJoinReturnType jaxbHbmJoin,
			Map<String, Map<String, HbmJoinDescriptor>> joinDescriptors,
			Map<String, HbmFetchParent> fetchParentByAlias,
			String registrationName,
			MetadataBuildingContext context) {
		// property path is in the form `{ownerAlias}.{joinedPath}`.  Split it into the 2 parts
		final String fullPropertyPath = jaxbHbmJoin.getProperty();
		final int firstDot = fullPropertyPath.indexOf( '.' );
		if ( firstDot < 1 ) {
			throw new MappingException(
					"Illegal <return-join/> property attribute: `" + fullPropertyPath + "`.  Should"
					+ "be in the form `{ownerAlias.joinedPropertyPath}` (" + registrationName + ")"
			);
		}

		final String ownerTableAlias = fullPropertyPath.substring( 0, firstDot );
		final String propertyPath = fullPropertyPath.substring( firstDot + 1 );
		final String tableAlias = jaxbHbmJoin.getAlias();

		Map<String, HbmJoinDescriptor> joinDescriptorsForAlias = joinDescriptors.get( ownerTableAlias );
		//noinspection Java8MapApi
		if ( joinDescriptorsForAlias == null ) {
			joinDescriptorsForAlias = new HashMap<>();
			joinDescriptors.put( ownerTableAlias, joinDescriptorsForAlias );
		}

		final HbmJoinDescriptor existing = joinDescriptorsForAlias.get( propertyPath );
		if ( existing != null ) {
			throw new MappingException(
					"Property join specified twice for join-return `" + ownerTableAlias + "." + propertyPath
							+ "` (" + registrationName + ")"
			);
		}

		final HbmJoinDescriptor joinDescriptor = new HbmJoinDescriptor(
				jaxbHbmJoin,
				() -> joinDescriptors,
				() -> fetchParentByAlias,
				registrationName,
				context
		);
		joinDescriptorsForAlias.put( propertyPath, joinDescriptor );
		fetchParentByAlias.put( tableAlias, joinDescriptor );
	}


	/**
	 * Constructor for an implicit {@code <sql-query/>} result-set mapping.
	 */
	public HbmResultSetMappingDescriptor(
			String registrationName,
			List<ResultDescriptor> resultDescriptors,
			Map<String, Map<String, HbmJoinDescriptor>> joinDescriptors,
			Map<String,HbmFetchParent> fetchParentsByAlias) {
		BootResultMappingLogging.LOGGER.debugf(
				"Creating implicit HbmResultSetMappingDescriptor : %s",
				registrationName
		);

		this.registrationName = registrationName;
		this.resultDescriptors = resultDescriptors;
		this.joinDescriptors = joinDescriptors;

		assert fetchParentsByAlias != null;
		assert joinDescriptors != null;

		this.fetchParentByAlias = fetchParentsByAlias;


//		resultDescriptors.forEach(
//				resultDescriptor -> {
//					if ( resultDescriptor instanceof EntityResultDescriptor ) {
//						final EntityResultDescriptor entityResultDescriptor = (EntityResultDescriptor) resultDescriptor;
//						fetchParentByAlias.put( entityResultDescriptor.tableAlias, entityResultDescriptor );
//					}
//					else if ( resultDescriptor instanceof CollectionResultDescriptor ) {
//						final CollectionResultDescriptor collectionResultDescriptor = (CollectionResultDescriptor) resultDescriptor;
//						fetchParentByAlias.put( collectionResultDescriptor.tableAlias, collectionResultDescriptor );
//					}
//				}
//		);
//
//		joinDescriptors.forEach(
//				(ownerAlias, joinsForOwner) -> joinsForOwner.forEach(
//						(path, joinDescriptor) -> {
//							final HbmFetchParent existing = fetchParentByAlias.get( path );
//							if ( existing != null ) {
//								throw new MappingException(
//										"Found 2 return descriptors with the same alias: ["
//												+ joinDescriptor + "] & [" + existing + "]"
//								);
//							}
//
//							fetchParentByAlias.put( joinDescriptor.tableAlias, joinDescriptor );
//						}
//				)
//		);
	}

	@Override
	public String getRegistrationName() {
		return registrationName;
	}

	@Override
	public NamedResultSetMappingMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
		BootResultMappingLogging.LOGGER.debugf(
				"Resolving HbmResultSetMappingDescriptor into memento for [%s]",
				registrationName
		);

		final List<ResultMemento> resultMementos = new ArrayList<>( resultDescriptors.size() );
		resultDescriptors.forEach(
				(descriptor) -> resultMementos.add( descriptor.resolve( resolutionContext ) )
		);

		return new NamedResultSetMappingMementoImpl( registrationName, resultMementos );
	}


	public static List<HbmFetchDescriptor> extractPropertyFetchDescriptors(
			List<JaxbHbmNativeQueryPropertyReturnType> hbmReturnProperties,
			HbmFetchParent fetchParent,
			String registrationName,
			MetadataBuildingContext context) {
		final List<HbmFetchDescriptor> propertyFetchDescriptors = new ArrayList<>( hbmReturnProperties.size() );

		hbmReturnProperties.forEach(
				(propertyReturn) -> {
					propertyFetchDescriptors.add(
							new HbmPropertyFetchDescriptor( propertyReturn, fetchParent, registrationName, context )
					);
				}
		);

		return propertyFetchDescriptors;
	}

	public static void applyFetchJoins(
			Supplier<Map<String, Map<String, HbmJoinDescriptor>>> joinDescriptorsAccess,
			String tableAlias,
			List<HbmFetchDescriptor> propertyFetchDescriptors) {
		final Map<String, Map<String, HbmJoinDescriptor>> joinDescriptors = joinDescriptorsAccess.get();

		if ( joinDescriptors == null ) {
			return;
		}

		final Map<String, HbmJoinDescriptor> ownerJoinDescriptors = joinDescriptors.get( tableAlias );
		if ( ownerJoinDescriptors != null ) {
			final Set<String> processedFetchableNames = new HashSet<>();
			ownerJoinDescriptors.forEach(
					(fetchableName, joinDescriptor) -> {
						final boolean added = processedFetchableNames.add( fetchableName );

						if ( added ) {
							propertyFetchDescriptors.add( joinDescriptor );
						}
						else {
							// the fetch is most likely more complete of a mapping so replace the original
							for ( int i = 0; i < propertyFetchDescriptors.size(); i++ ) {
								final HbmFetchDescriptor propertyFetchDescriptor = propertyFetchDescriptors.get( i );
								if ( propertyFetchDescriptor.getFetchablePath().equals( fetchableName ) ) {
									propertyFetchDescriptors.set( i, joinDescriptor );
								}
							}
						}
					}
			);
		}
	}

}
