/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.SharedSessionContract;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.type.AnyType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * Singular, any-valued attribute
 *
 * @see org.hibernate.annotations.Any
 *
 * @author Steve Ebersole
 */
public class DiscriminatedAssociationAttributeMapping
		extends AbstractSingularAttributeMapping
		implements DiscriminatedAssociationModelPart {
	private final NavigableRole navigableRole;
	private final DiscriminatedAssociationMapping discriminatorMapping;

	public DiscriminatedAssociationAttributeMapping(
			NavigableRole attributeRole,
			JavaType<?> baseAssociationJtd,
			ManagedMappingType declaringType,
			int stateArrayPosition,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchTiming fetchTiming,
			PropertyAccess propertyAccess,
			Property bootProperty,
			AnyType anyType,
			Any bootValueMapping,
			MappingModelCreationProcess creationProcess) {
		super(
				bootProperty.getName(),
				stateArrayPosition,
				attributeMetadataAccess,
				fetchTiming,
				FetchStyle.SELECT,
				declaringType,
				propertyAccess,
				null
		);
		this.navigableRole = attributeRole;

		this.discriminatorMapping = DiscriminatedAssociationMapping.from(
				attributeRole,
				baseAssociationJtd,
				this,
				anyType,
				bootValueMapping,
				creationProcess
		);
	}

	@Override
	public BasicValuedModelPart getDiscriminatorPart() {
		return discriminatorMapping.getDiscriminatorPart();
	}

	@Override
	public BasicValuedModelPart getKeyPart() {
		return discriminatorMapping.getKeyPart();
	}

	@Override
	public EntityMappingType resolveDiscriminatorValue(Object discriminatorValue) {
		return discriminatorMapping.resolveDiscriminatorValueToEntityMapping( discriminatorValue );
	}

	public Object resolveDiscriminatorForEntityType(EntityMappingType entityMappingType) {
		return discriminatorMapping.resolveDiscriminatorValueToEntityMapping( entityMappingType );
	}

	@Override
	public String toString() {
		return "DiscriminatedAssociationAttributeMapping(" + navigableRole + ")@" + System.identityHashCode( this );
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		return discriminatorMapping.generateFetch(
				fetchParent,
				fetchablePath,
				fetchTiming,
				selected,
				resultVariable,
				creationState
		);
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void applySqlSelections(NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public void applySqlSelections(NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState, BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public MappingType getMappedType() {
		return discriminatorMapping;
	}

	@Override
	public int getNumberOfFetchables() {
		return 2;
	}

	@Override
	public int getJdbcTypeCount() {
		return getDiscriminatorPart().getJdbcTypeCount() + getKeyPart().getJdbcTypeCount();
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}

		final EntityMappingType concreteMappingType = determineConcreteType( value, session );
		final EntityIdentifierMapping identifierMapping = concreteMappingType.getIdentifierMapping();

		final Object discriminator = discriminatorMapping
				.getModelPart()
				.resolveDiscriminatorForEntityType( concreteMappingType );
		final Object identifier = identifierMapping.getIdentifier( value, session );

		return new Object[] {
				discriminatorMapping.getDiscriminatorPart().disassemble( discriminator, session ),
				identifierMapping.disassemble( identifier, session )
		};
	}

	private EntityMappingType determineConcreteType(Object entity, SharedSessionContractImplementor session) {
		final String entityName = session.bestGuessEntityName( entity );
		return session.getFactory()
				.getRuntimeMetamodels()
				.getEntityMappingType( entityName );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		discriminatorMapping.getDiscriminatorPart().forEachSelectable( offset, consumer );
		discriminatorMapping.getKeyPart().forEachSelectable( offset + 1, consumer );

		return 2;
	}

	@Override
	public int forEachJdbcType(IndexedConsumer<JdbcMapping> action) {
		action.accept( 0, discriminatorMapping.getDiscriminatorPart().getJdbcMapping() );
		action.accept( 1, discriminatorMapping.getKeyPart().getJdbcMapping() );
		return 2;
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		if ( value != null ) {
			if ( value.getClass().isArray() ) {
				final Object[] values = (Object[]) value;
				valuesConsumer.consume(
						offset,
						values[0],
						discriminatorMapping.getDiscriminatorPart().getJdbcMapping()
				);
				valuesConsumer.consume(
						offset + 1,
						values[1],
						discriminatorMapping.getKeyPart().getJdbcMapping()
				);
			}
			else {
				final EntityMappingType concreteMappingType = determineConcreteType( value, session );

				final Object discriminator = discriminatorMapping
						.getModelPart()
						.resolveDiscriminatorForEntityType( concreteMappingType );
				final Object disassembledDiscriminator = discriminatorMapping.getDiscriminatorPart().disassemble( discriminator, session );
				valuesConsumer.consume(
						offset,
						disassembledDiscriminator,
						discriminatorMapping.getDiscriminatorPart().getJdbcMapping()
				);

				final EntityIdentifierMapping identifierMapping = concreteMappingType.getIdentifierMapping();
				final Object identifier = identifierMapping.getIdentifier( value, session );
				final Object disassembledKey = discriminatorMapping.getKeyPart().disassemble( identifier, session );
				valuesConsumer.consume(
						offset + 1,
						disassembledKey,
						discriminatorMapping.getKeyPart().getJdbcMapping()
				);
			}
		}

		return 2;
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		final EntityMappingType concreteMappingType = determineConcreteType( domainValue, session );

		final Object discriminator = discriminatorMapping
				.getModelPart()
				.resolveDiscriminatorForEntityType( concreteMappingType );
		final Object disassembledDiscriminator = discriminatorMapping.getDiscriminatorPart().disassemble( discriminator, session );
		valueConsumer.consume( disassembledDiscriminator, discriminatorMapping.getDiscriminatorPart() );

		final EntityIdentifierMapping identifierMapping = concreteMappingType.getIdentifierMapping();
		final Object identifier = identifierMapping.getIdentifier( domainValue, session );
		final Object disassembledKey = discriminatorMapping.getKeyPart().disassemble( identifier, session );
		valueConsumer.consume( disassembledKey, discriminatorMapping.getKeyPart() );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		int span = getDiscriminatorPart().forEachJdbcType( offset, action );
		return span + getKeyPart().forEachJdbcType( offset + span, action );
	}

	@Override
	public void visitFetchables(Consumer<Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		fetchableConsumer.accept( getDiscriminatorPart() );
		fetchableConsumer.accept( getKeyPart() );
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return discriminatorMapping.findSubPart( name, treatTargetType );
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		consumer.accept( getDiscriminatorPart() );
		consumer.accept( getKeyPart() );
	}

	public static class MutabilityPlanImpl implements MutabilityPlan {
		// for now use the AnyType for consistency with write-operations
		private final AnyType anyType;

		public MutabilityPlanImpl(AnyType anyType) {
			this.anyType = anyType;
		}

		@Override
		public boolean isMutable() {
			return anyType.isMutable();
		}

		@Override
		public Object deepCopy(Object value) {
			return value;
		}

		@Override
		public Serializable disassemble(Object value, SharedSessionContract session) {
//			if ( value == null ) {
//				return null;
//			}
//			else {
//				return new AnyType.ObjectTypeCacheEntry(
//						persistenceContext.bestGuessEntityName( value ),
//						ForeignKeys.getEntityIdentifierIfNotUnsaved(
//								persistenceContext.bestGuessEntityName( value ),
//								value,
//								persistenceContext
//						)
//				);
//			}

			// this ^^ is what we want eventually, but for the time-being to ensure compatibility with
			// writing just reuse the AnyType

			final SharedSessionContractImplementor persistenceContext = (SharedSessionContractImplementor) session;
			return anyType.disassemble( value, persistenceContext, null );
		}

		@Override
		public Object assemble(Serializable cached, SharedSessionContract session) {
//			final AnyType.ObjectTypeCacheEntry e = (AnyType.ObjectTypeCacheEntry) cached;
//			return e == null ? null : session.internalLoad( e.entityName, e.id, eager, false );

			// again, what we want eventually ^^ versus what we should do now vv

			final SharedSessionContractImplementor persistenceContext = (SharedSessionContractImplementor) session;
			return anyType.assemble( cached, persistenceContext, null );
		}
	}
}
