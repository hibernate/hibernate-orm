/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.jpa;

import jakarta.annotation.Nonnull;
import jakarta.persistence.sql.EmbeddedMapping;
import jakarta.persistence.sql.EntityMapping;
import jakarta.persistence.sql.FieldMapping;
import jakarta.persistence.sql.MemberMapping;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/// Support for [jakarta.persistence.sql.EntityMapping].
///
/// @see EntityResultImpl
///
/// @author Steve Ebersole
public class EntityBuilder<T> extends AbstractMappingElementBuilder<T> implements ResultBuilderEntityValued {
	private final EntityPersister entityDescriptor;
	private final NavigablePath rootPath;
	private final LockMode lockMode;
	private final FetchBuilder identifierFetchBuilder;
	private final FetchBuilderBasicValued discriminatorFetchBuilder;
	private final Map<String,FetchBuilder> attributeFetchBuilders = new HashMap<>();

	public EntityBuilder(EntityMapping<T> entityMapping, SessionFactoryImplementor sessionFactory) {
		super( entityMapping.getAlias(), entityMapping.getJavaType(), sessionFactory );

		entityDescriptor =
				sessionFactory.getMappingMetamodel()
						.getEntityDescriptor( entityMapping.entityClass() );
		rootPath = new NavigablePath( entityDescriptor.getRootPathName() );
		lockMode = LockMode.fromJpaLockMode( entityMapping.lockMode() );

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

		final var identifierFetchHandler = buildIdentifierFetchHandler();
		for ( int i = 0; i < entityMapping.fields().length; i++ ) {
			final var memberMapping = entityMapping.fields()[i];
			if ( memberMapping instanceof FieldMapping<?,?> basicMapping ) {
				if ( !identifierFetchHandler.handleMember( basicMapping ) ) {
					attributeFetchBuilders.put(
							basicMapping.name(),
							new CompleteFetchBuilderBasicPart(
									rootPath.append( basicMapping.name() ),
									entityDescriptor.findSubPart( basicMapping.name() )
											.asBasicValuedModelPart(),
									basicMapping.columnName()
							)
					);
				}
			}
			else if ( memberMapping instanceof EmbeddedMapping<?,?> embeddedMapping ) {
				final var attributeMapping =
								entityDescriptor.findSubPart( embeddedMapping.name() );
				if ( !identifierFetchHandler.handleMember( embeddedMapping ) ) {
					if ( attributeMapping instanceof EmbeddableValuedModelPart modelPart ) {
						attributeFetchBuilders.put(
								embeddedMapping.name(),
								new CompleteFetchBuilderEmbeddableValuedModelPart(
										rootPath.append( attributeMapping.getPartName() ),
										modelPart,
										extractColumnNames( embeddedMapping, modelPart )
								)
						);
					}
					else {
						throw new IllegalArgumentException( "Not an embedded attribute: "
								+ attributeMapping.getNavigableRole().getFullPath() );
					}
				}
			}
			else {
				throw new IllegalArgumentException( "Unrecognized member mapping type: "
							+ memberMapping.getClass().getName() );
			}
		}

		this.identifierFetchBuilder = identifierFetchHandler.buildFetchBuilder();
	}

	private static List<String> extractColumnNames(
			EmbeddedMapping<?, ?> embeddedMapping,
			EmbeddableValuedModelPart modelPart) {
		final var columnNames = new String[modelPart.getJdbcTypeCount()];
		collectColumnNames( embeddedMapping, modelPart.getEmbeddableTypeDescriptor(), 0, columnNames );
		for ( int i = 0; i < columnNames.length; i++ ) {
			if ( columnNames[i] == null ) {
				throw new IllegalArgumentException(
						"No column name specified for embedded attribute selectable: "
								+ modelPart.getSelectable( i ).getSelectableName()
				);
			}
		}
		return List.of( columnNames );
	}

	private static void collectColumnNames(
			EmbeddedMapping<?, ?> embeddedMapping,
			ManagedMappingType embeddableMappingType,
			int offset,
			String[] columnNames) {
		for ( var memberMapping : embeddedMapping.fields() ) {
			final String memberName = memberName( memberMapping );
			final var attributeMapping = embeddableMappingType.findAttributeMapping( memberName );
			if ( attributeMapping == null ) {
				throw new IllegalArgumentException(
						"Embedded result mapping specified unknown attribute: " + memberName
				);
			}
			populateColumnNames(
					offset + getAttributeOffset( embeddableMappingType, memberName ),
					columnNames,
					memberMapping,
					memberName,
					attributeMapping
			);
		}
	}

	private static void populateColumnNames(
			int offset,
			String[] columnNames,
			MemberMapping<?> memberMapping,
			String memberName,
			AttributeMapping attributeMapping) {
		if ( memberMapping instanceof FieldMapping<?, ?> basicMapping ) {
			columnNames[offset] = basicMapping.columnName();
		}
		else if ( memberMapping instanceof EmbeddedMapping<?, ?> nestedMapping ) {
			if ( !(attributeMapping instanceof EmbeddableValuedModelPart nestedModelPart) ) {
				throw new IllegalArgumentException(
						"Embedded result mapping specified non-embedded attribute: " + memberName
				);
			}
			collectColumnNames(
					nestedMapping,
					nestedModelPart.getEmbeddableTypeDescriptor(),
					offset,
					columnNames
			);
		}
	}

	@Nonnull
	private static String memberName(MemberMapping<?> memberMapping) {
		if ( memberMapping instanceof FieldMapping<?, ?> basicMapping ) {
			return basicMapping.name();
		}
		else if ( memberMapping instanceof EmbeddedMapping<?, ?> nestedMapping ) {
			return nestedMapping.name();
		}
		else {
			throw new IllegalArgumentException( "Unrecognized member mapping type: "
												+ memberMapping.getClass().getName());
		}
	}

	private static int getAttributeOffset(ManagedMappingType embeddableMappingType, String attributeName) {
		int offset = 0;
		for ( int i = 0; i < embeddableMappingType.getNumberOfAttributeMappings(); i++ ) {
			final var attributeMapping = embeddableMappingType.getAttributeMapping( i );
			if ( attributeMapping.getAttributeName().equals( attributeName ) ) {
				return offset;
			}
			offset += attributeMapping.getJdbcTypeCount();
		}
		throw new IllegalArgumentException( "Embedded result mapping specified unknown attribute: " + attributeName );
	}

	private IdentifierFetchHandler buildIdentifierFetchHandler() {
		final var identifierMapping = entityDescriptor.getIdentifierMapping();
		if ( identifierMapping instanceof NonAggregatedIdentifierMapping complexId ) {
			return new ComplexIdentifierFetchHandler( complexId, rootPath );
		}
		else if ( identifierMapping instanceof SingleAttributeIdentifierMapping simpleId ) {
			return new SimpleIdentifierFetchHandler( entityDescriptor, simpleId, rootPath );
		}
		else {
			throw new IllegalArgumentException( "Unrecognized identifier mapping type: "
											+ identifierMapping.getClass().getName() );
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
			else if ( memberMapping instanceof EmbeddedMapping<?,?> embeddedMapping ) {
				return new CompleteFetchBuilderEmbeddableValuedModelPart(
						rootPath.append( embeddedMapping.name() ),
						idMapping.asEmbeddedAttributeMapping(),
						extractColumnNames( embeddedMapping, idMapping.asEmbeddedAttributeMapping() )
				);
			}
			else {
				throw new IllegalArgumentException( "Unrecognized member mapping type: "
							+ memberMapping.getClass().getName() );
			}
		}
	}

	private static class ComplexIdentifierFetchHandler implements IdentifierFetchHandler {
		private final Set<String> idAttributeNames;
		private final List<String> columnNames;
		private final NonAggregatedIdentifierMapping idMapping;
		private final NavigablePath rootPath;

		public ComplexIdentifierFetchHandler(
				NonAggregatedIdentifierMapping idMapping,
				NavigablePath rootPath) {
			this.idMapping = idMapping;
			this.rootPath = rootPath;

			idAttributeNames = new HashSet<>();
			idMapping.getVirtualIdEmbeddable()
					.forEachAttributeMapping( attr -> idAttributeNames.add( attr.getAttributeName() ) );

			columnNames = arrayList( idMapping.getJdbcTypeCount() );
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
		else if ( memberMapping instanceof EmbeddedMapping<?,?> embeddedMapping ) {
			for ( var embeddedMemberMapping : embeddedMapping.fields() ) {
				collectColumnNames( embeddedMemberMapping, columnNameConsumer );
			}
		}
	}

	@Override
	public EntityResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState creationState) {
		final String resultAlias = applyTableGroup( resultPosition, creationState );

		return new EntityResultImpl<>(
				entityDescriptor,
				javaType,
				rootPath,
				resultAlias,
				lockMode,
				identifierFetchBuilder,
				discriminatorFetchBuilder,
				attributeFetchBuilders,
				jdbcResultsMetadata,
				creationState
		);
	}

	private String applyTableGroup(int resultPosition, DomainResultCreationState creationState) {
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
		return implicitAlias;
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}

}
