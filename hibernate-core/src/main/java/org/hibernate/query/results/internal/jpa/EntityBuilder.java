/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.jpa;

import jakarta.persistence.sql.EmbeddedMapping;
import jakarta.persistence.sql.EntityMapping;
import jakarta.persistence.sql.FieldMapping;
import jakarta.persistence.sql.MemberMapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.results.internal.complete.CompleteFetchBuilderBasicPart;
import org.hibernate.query.results.internal.complete.CompleteFetchBuilderEmbeddableValuedModelPart;
import org.hibernate.query.results.spi.FetchBuilder;
import org.hibernate.query.results.spi.FetchBuilderBasicValued;
import org.hibernate.query.results.spi.ResultBuilder;
import org.hibernate.query.results.spi.ResultBuilderEntityValued;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/// Support for [jakarta.persistence.sql.EntityMapping].
///
/// @see EntityResultImpl
///
/// @author Steve Ebersole
public class EntityBuilder<T> extends AbstractMappingElementBuilder<T> implements ResultBuilderEntityValued {
	private final EntityPersister entityDescriptor;
	private final NavigablePath rootPath;
	private final FetchBuilder identifierFetchBuilder;
	private final FetchBuilderBasicValued discriminatorFetchBuilder;
	private final Map<String,FetchBuilder> attributeFetchBuilders = new HashMap<>();

	public EntityBuilder(EntityMapping<T> entityMapping, SessionFactoryImplementor sessionFactory) {
		super( entityMapping.getAlias(), entityMapping.getJavaType(), sessionFactory );

		this.entityDescriptor = sessionFactory.getMappingMetamodel().getEntityDescriptor( entityMapping.entityClass() );
		this.rootPath = new NavigablePath( entityDescriptor.getRootPathName() );

		if ( StringHelper.isBlank( entityMapping.discriminatorColumn() ) ) {
			discriminatorFetchBuilder = null;
		}
		else {
			var discriminatorDescriptor = entityDescriptor.getDiscriminatorMapping();
			discriminatorFetchBuilder = new CompleteFetchBuilderBasicPart(
					rootPath.append( discriminatorDescriptor.getPartName() ),
					discriminatorDescriptor,
					entityMapping.discriminatorColumn()
			);
		}

		final IdentifierFetchHandler identifierFetchHandler = buildIdentifierFetchHandler(
				entityDescriptor,
				rootPath
		);
		for ( int i = 0; i < entityMapping.fields().length; i++ ) {
			final MemberMapping<?> memberMapping = entityMapping.fields()[i];
			if ( memberMapping instanceof FieldMapping<?,?> basicMapping ) {
				if ( !identifierFetchHandler.handleMember( basicMapping ) ) {
					final BasicValuedModelPart modelPart = entityDescriptor
							.findSubPart( basicMapping.name() )
							.asBasicValuedModelPart();
					attributeFetchBuilders.put( basicMapping.name(), new CompleteFetchBuilderBasicPart(
							rootPath.append( basicMapping.name() ),
							modelPart,
							basicMapping.columnName()
					) );
				}
			}
			else {
				final EmbeddedMapping<?,?> embeddedMapping = (EmbeddedMapping<?, ?>) memberMapping;
				final EmbeddableValuedModelPart attributeMapping =
						(EmbeddableValuedModelPart) entityDescriptor.findSubPart( embeddedMapping.name() );
				if ( !identifierFetchHandler.handleMember( embeddedMapping ) ) {
					attributeFetchBuilders.put( embeddedMapping.name(), new CompleteFetchBuilderEmbeddableValuedModelPart(
							rootPath.append( attributeMapping.getPartName() ),
							attributeMapping,
							extractColumnNames( embeddedMapping )
					) );
				}
			}
		}

		this.identifierFetchBuilder = identifierFetchHandler.buildFetchBuilder();
	}

	private static List<String> extractColumnNames(EmbeddedMapping<?, ?> embeddedMapping) {
		final List<String> names = new ArrayList<>();
		collectColumnNames( embeddedMapping, names::add );
		return names;
	}

	private IdentifierFetchHandler buildIdentifierFetchHandler(EntityPersister entityDescriptor, NavigablePath rootPath) {
		if ( entityDescriptor.getIdentifierMapping() instanceof NonAggregatedIdentifierMapping complexId ) {
			return new ComplexIdentifierFetchHandler( entityDescriptor, complexId, rootPath );
		}
		else {
			return new SimpleIdentifierFetchHandler(
					entityDescriptor,
					(SingleAttributeIdentifierMapping) entityDescriptor.getIdentifierMapping(),
					rootPath
			);
		}
	}

	/// Used to help deal with [MemberMapping] as either an id or attribute.
	private interface IdentifierFetchHandler {
		boolean handleMember(FieldMapping<?,?> memberMapping);
		boolean handleMember(EmbeddedMapping<?,?> memberMapping);
		FetchBuilder buildFetchBuilder();
	}

	private static class SimpleIdentifierFetchHandler implements IdentifierFetchHandler {
		private final EntityMappingType entityDescriptor;
		private final SingleAttributeIdentifierMapping idMapping;
		private final NavigablePath rootPath;
		private MemberMapping<?> memberMapping;

		public SimpleIdentifierFetchHandler(
				EntityMappingType entityDescriptor,
				SingleAttributeIdentifierMapping idMapping,
				NavigablePath rootPath) {
			this.entityDescriptor = entityDescriptor;
			this.idMapping = idMapping;
			this.rootPath = rootPath;
		}

		@Override
		public boolean handleMember(FieldMapping<?, ?> memberMapping) {
			final boolean isId = idMapping.getAttributeName().equals( memberMapping.name() );
			if ( isId ) {
				setIdMapping( memberMapping );
			}
			return isId;
		}

		@Override
		public boolean handleMember(EmbeddedMapping<?, ?> memberMapping) {
			final boolean isId = idMapping.getAttributeName().equals( memberMapping.name() );
			if ( isId ) {
				setIdMapping( memberMapping );
			}
			return isId;
		}

		private void setIdMapping(MemberMapping<?> memberMapping) {
			if ( this.memberMapping != null ) {
				throw new IllegalStateException( "Multiple MemberMapping defined for simple identifier mapping : " + entityDescriptor.getEntityName() );
			}
			this.memberMapping = memberMapping;
		}

		@Override
		public FetchBuilder buildFetchBuilder() {
			if ( memberMapping instanceof FieldMapping<?,?> basicMapping ) {
				return new CompleteFetchBuilderBasicPart(
						rootPath.append( basicMapping.name() ),
						idMapping.asBasicValuedModelPart(),
						basicMapping.columnName()
				);
			}
			else {
				final var embeddedMapping = (EmbeddedMapping<?,?>) memberMapping;
				return new CompleteFetchBuilderEmbeddableValuedModelPart(
						rootPath.append( embeddedMapping.name() ),
						idMapping.asEmbeddedAttributeMapping(),
						extractColumnNames( embeddedMapping )
				);
			}
		}
	}

	private static class ComplexIdentifierFetchHandler implements IdentifierFetchHandler {
		private final Set<String> idAttributeNames;
		private final List<String> columnNames;
		private final NonAggregatedIdentifierMapping idMapping;
		private final NavigablePath rootPath;

		public ComplexIdentifierFetchHandler(
				EntityPersister entityDescriptor,
				NonAggregatedIdentifierMapping idMapping,
				NavigablePath rootPath) {
			this.idMapping = idMapping;
			this.rootPath = rootPath;

			idAttributeNames = new HashSet<>();
			idMapping.getVirtualIdEmbeddable().forEachAttributeMapping( (attr) -> idAttributeNames.add( attr.getAttributeName() ) );

			columnNames = CollectionHelper.arrayList( idMapping.getJdbcTypeCount() );
		}

		@Override
		public boolean handleMember(FieldMapping<?, ?> memberMapping) {
			final boolean isId = idAttributeNames.contains( memberMapping.name() );
			if ( isId ) {
				addId( memberMapping );
			}
			return isId;
		}

		@Override
		public boolean handleMember(EmbeddedMapping<?, ?> memberMapping) {
			return false;
		}

		private void addId(MemberMapping<?> memberMapping) {
			collectColumnNames( memberMapping, columnNames::add );
		}

		@Override
		public FetchBuilder buildFetchBuilder() {
			return new CompleteFetchBuilderEmbeddableValuedModelPart(
					rootPath.append( idMapping.getPartName() ),
					idMapping,
					columnNames
			);
		}
	}

	private static void collectColumnNames(MemberMapping<?> memberMapping, Consumer<String> columnNameConsumer) {
		if ( memberMapping instanceof FieldMapping<?,?> basicMapping ) {
			columnNameConsumer.accept( basicMapping.columnName() );
		}
		else {
			var embeddedMapping = (EmbeddedMapping<?, ?>) memberMapping;
			for ( MemberMapping<?> embeddedMemberMapping : embeddedMapping.fields() ) {
				collectColumnNames( embeddedMemberMapping, columnNameConsumer );
			}
		}
	}

	@Override
	public EntityResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState creationState) {
		applyTableGroup( resultPosition, creationState );

		return new EntityResultImpl<>(
				entityDescriptor,
				javaType,
				rootPath,
				identifierFetchBuilder,
				discriminatorFetchBuilder,
				attributeFetchBuilders,
				jdbcResultsMetadata,
				creationState
		);
	}

	private void applyTableGroup(int resultPosition, DomainResultCreationState creationState) {
		final String implicitAlias = entityDescriptor.getSqlAliasStem() + resultPosition;
		final var sqlAliasBase = creationState.getSqlAliasBaseManager().createSqlAliasBase( implicitAlias );

		// we just want it added to the registry
		creationState.getSqlAstCreationState().getFromClauseAccess().resolveTableGroup(
				rootPath,
				path -> entityDescriptor.createRootTableGroup(
						// since this is only used for result set mappings, the canUseInnerJoins value is irrelevant.
						true,
						rootPath,
						implicitAlias,
						sqlAliasBase,
						null,
						creationState.getSqlAstCreationState()
				)
		);
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}

}
