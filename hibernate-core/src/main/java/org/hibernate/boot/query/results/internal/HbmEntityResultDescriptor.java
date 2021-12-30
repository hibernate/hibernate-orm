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
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.query.results.BootResultMappingLogging;
import org.hibernate.boot.query.results.HbmFetchDescriptor;
import org.hibernate.boot.query.results.HbmFetchParent;
import org.hibernate.boot.query.results.HbmResultSetMappingDescriptor;
import org.hibernate.boot.query.results.ResultDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.internal.FetchMementoBasicStandard;
import org.hibernate.query.internal.ResultMementoEntityStandard;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.named.FetchMementoBasic;
import org.hibernate.query.named.ResultMemento;
import org.hibernate.query.results.FetchParentMemento;
import org.hibernate.query.results.ResultSetMappingResolutionContext;

/**
 * @see JaxbHbmNativeQueryReturnType
 *
 * @author Steve Ebersole
 */
public class HbmEntityResultDescriptor implements ResultDescriptor, HbmFetchParent {
	private final String entityName;
	private final String tableAlias;
	private final String discriminatorColumnAlias;

	private final LockMode lockMode;

	private final List<HbmFetchDescriptor> propertyFetchDescriptors;

	private final Supplier<Map<String, Map<String, HbmJoinDescriptor>>> joinDescriptorsAccess;

	private final String registrationName;

	public HbmEntityResultDescriptor(
			JaxbHbmNativeQueryReturnType hbmEntityReturn,
			Supplier<Map<String, Map<String, HbmJoinDescriptor>>> joinDescriptorsAccess,
			String registrationName,
			MetadataBuildingContext context) {
		assert joinDescriptorsAccess != null;

		if ( hbmEntityReturn.getEntityName() == null ) {
			final String namedClass = hbmEntityReturn.getClazz();
			if ( namedClass == null ) {
				throw new MappingException( "Entity <return/> mapping did not specify entity name nor class - `" + registrationName + "`" );
			}

			String className = namedClass;

			final String implicitPackageName = context.getMappingDefaults().getImplicitPackageName();
			if ( implicitPackageName != null ) {
				final PersistentClass entityBinding = context.getMetadataCollector().getEntityBinding( namedClass );
				if ( entityBinding == null ) {
					final String packageQualified = implicitPackageName + "." + namedClass;
					final PersistentClass entityBinding2 = context.getMetadataCollector().getEntityBinding( packageQualified );
					if ( entityBinding2 != null ) {
						className = packageQualified;
					}
				}
			}
			this.entityName = className;
		}
		else {
			this.entityName = hbmEntityReturn.getEntityName();
		}

		this.tableAlias = hbmEntityReturn.getAlias();

		BootResultMappingLogging.LOGGER.debugf(
				"Creating EntityResultDescriptor (%s : %s) for ResultSet mapping - %s",
				tableAlias,
				entityName,
				registrationName
		);

		this.discriminatorColumnAlias = hbmEntityReturn.getReturnDiscriminator() == null
				? null
				: hbmEntityReturn.getReturnDiscriminator().getColumn();
		this.lockMode = hbmEntityReturn.getLockMode();
		this.joinDescriptorsAccess = joinDescriptorsAccess;
		this.registrationName = registrationName;

		this.propertyFetchDescriptors = HbmResultSetMappingDescriptor.extractPropertyFetchDescriptors(
				hbmEntityReturn.getReturnProperty(),
				this,
				registrationName,
				context
		);
	}

	public String getEntityName() {
		return entityName;
	}

	public String getTableAlias() {
		return tableAlias;
	}

	public String getDiscriminatorColumnAlias() {
		return discriminatorColumnAlias;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"HbmEntityResultDescriptor(%s : %s)",
				entityName,
				tableAlias
		);
	}

	@Override
	public ResultMemento resolve(ResultSetMappingResolutionContext resolutionContext) {
		BootResultMappingLogging.LOGGER.debugf(
				"Resolving HBM EntityResultDescriptor into memento - %s : %s (%s)",
				tableAlias,
				entityName,
				registrationName
		);

		final RuntimeMetamodels runtimeMetamodels = resolutionContext
				.getSessionFactory()
				.getRuntimeMetamodels();

		final EntityMappingType entityDescriptor = runtimeMetamodels.getEntityMappingType( runtimeMetamodels.getImportedName( entityName ) );
		final Map<String, Map<String, HbmJoinDescriptor>> joinDescriptors = joinDescriptorsAccess.get();
		if ( CollectionHelper.isNotEmpty( propertyFetchDescriptors ) || CollectionHelper.isNotEmpty( joinDescriptors ) ) {
			if ( tableAlias == null ) {
				throw new MappingException( "Entity <return/> mapping did not specify alias - `" + registrationName + "`" );
			}
		}
		HbmResultSetMappingDescriptor.applyFetchJoins( joinDescriptors, tableAlias, propertyFetchDescriptors );

		final NavigablePath entityPath = new NavigablePath( entityName );

		final FetchMementoBasic discriminatorMemento;
		if ( discriminatorColumnAlias == null ) {
			discriminatorMemento = null;
		}
		else {
			if ( entityDescriptor.getDiscriminatorMapping() == null ) {
				throw new MappingException(
						"Discriminator column mapping given for non-discriminated entity ["
								+ entityName + "] as part of resultset mapping [" + registrationName + "]"
				);
			}

			discriminatorMemento = new FetchMementoBasicStandard(
					entityPath.append( EntityDiscriminatorMapping.ROLE_NAME ),
					entityDescriptor.getDiscriminatorMapping(),
					discriminatorColumnAlias
			);
		}

		final Map<String, FetchMemento> fetchDescriptorMap = new HashMap<>();
		propertyFetchDescriptors.forEach(
				hbmFetchDescriptor -> fetchDescriptorMap.put(
						hbmFetchDescriptor.getFetchablePath(),
						hbmFetchDescriptor.resolve( resolutionContext )
				)
		);

		return new ResultMementoEntityStandard(
				tableAlias,
				entityDescriptor,
				lockMode,
				discriminatorMemento,
				fetchDescriptorMap
		);
	}

	private HbmFetchParentMemento thisAsParentMemento;

	@Override
	public FetchParentMemento resolveParentMemento(ResultSetMappingResolutionContext resolutionContext) {
		if ( thisAsParentMemento == null ) {
			final EntityMappingType entityDescriptor = resolutionContext
					.getSessionFactory()
					.getRuntimeMetamodels()
					.getEntityMappingType( entityName );
			thisAsParentMemento = new HbmFetchParentMemento(
					new NavigablePath( entityDescriptor.getEntityName() ),
					entityDescriptor
			);
		}

		return thisAsParentMemento;
	}
}
