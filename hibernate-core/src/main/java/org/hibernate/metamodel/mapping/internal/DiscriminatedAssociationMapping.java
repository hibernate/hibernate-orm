/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Selectable;
import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.DomainResultGraphNode;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.AnyType;
import org.hibernate.type.BasicType;
import org.hibernate.type.MetaType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Represents the "type" of an any-valued mapping
 *
 * @author Steve Ebersole
 */
public class DiscriminatedAssociationMapping implements MappingType, FetchOptions {

	public static DiscriminatedAssociationMapping from(
			NavigableRole containerRole,
			JavaType<?> baseAssociationJtd,
			DiscriminatedAssociationModelPart declaringModelPart,
			AnyType anyType,
			Any bootValueMapping,
			MappingModelCreationProcess creationProcess) {
		final SessionFactoryImplementor sessionFactory = creationProcess.getCreationContext().getSessionFactory();

		final Dialect dialect = sessionFactory.getSqlStringGenerationContext().getDialect();
		final String tableName = MappingModelCreationHelper.getTableIdentifierExpression(
				bootValueMapping.getTable(),
				creationProcess
		);

		assert bootValueMapping.getColumnSpan() == 2;
		final Iterator<Selectable> columnIterator = bootValueMapping.getColumnIterator();

		assert columnIterator.hasNext();
		final Selectable metaColumn = columnIterator.next();
		assert columnIterator.hasNext();
		final Selectable keyColumn = columnIterator.next();
		assert !columnIterator.hasNext();
		assert !metaColumn.isFormula();
		assert !keyColumn.isFormula();

		final AnyDiscriminatorPart discriminatorPart = new AnyDiscriminatorPart(
				containerRole.append( AnyDiscriminatorPart.ROLE_NAME),
				declaringModelPart,
				tableName,
				metaColumn.getText( dialect ),
				bootValueMapping.isNullable(),
				(MetaType) anyType.getDiscriminatorType()
		);


		final BasicType<?> keyType = (BasicType<?>) anyType.getIdentifierType();
		final BasicValuedModelPart keyPart = new AnyKeyPart(
				containerRole.append( AnyKeyPart.ROLE_NAME),
				declaringModelPart,
				tableName,
				keyColumn.getText( dialect ),
				bootValueMapping.isNullable(),
				keyType
		);

		return new DiscriminatedAssociationMapping(
				declaringModelPart,
				discriminatorPart,
				keyPart,
				baseAssociationJtd,
				bootValueMapping.isLazy()
						? FetchTiming.DELAYED
						: FetchTiming.IMMEDIATE,
				bootValueMapping.getMetaValues(),
				sessionFactory
		);
	}

	private final DiscriminatedAssociationModelPart modelPart;
	private final AnyDiscriminatorPart discriminatorPart;
	private final BasicValuedModelPart keyPart;

	private final JavaType<?> baseAssociationJtd;

	private final FetchTiming fetchTiming;

	private static class ValueMapping {
		private final Object discriminatorValue;
		private final EntityMappingType entityMapping;

		public ValueMapping(Object discriminatorValue, EntityMappingType entityMapping) {
			this.discriminatorValue = discriminatorValue;
			this.entityMapping = entityMapping;
		}
	}

	private final LinkedList<ValueMapping> discriminatorValueMappings = new LinkedList<>();

	public DiscriminatedAssociationMapping(
			DiscriminatedAssociationModelPart modelPart,
			AnyDiscriminatorPart discriminatorPart,
			BasicValuedModelPart keyPart,
			JavaType<?> baseAssociationJtd,
			FetchTiming fetchTiming,
			Map<Object,String> discriminatorValueMappings,
			SessionFactoryImplementor sessionFactory) {
		this.modelPart = modelPart;
		this.discriminatorPart = discriminatorPart;
		this.keyPart = keyPart;
		this.baseAssociationJtd = baseAssociationJtd;
		this.fetchTiming = fetchTiming;

		final RuntimeMetamodels runtimeMetamodels = sessionFactory.getRuntimeMetamodels();
		discriminatorValueMappings.forEach(
				(value, entityName) -> this.discriminatorValueMappings.add(
						new ValueMapping( value, runtimeMetamodels.getEntityMappingType( entityName ) )
				)
		);
	}

	public DiscriminatedAssociationModelPart getModelPart() {
		return modelPart;
	}

	public BasicValuedModelPart getDiscriminatorPart() {
		return discriminatorPart;
	}

	public BasicValuedModelPart getKeyPart() {
		return keyPart;
	}

	public Object resolveDiscriminatorValueToEntityMapping(EntityMappingType entityMappingType) {
		for ( int i = 0; i < discriminatorValueMappings.size(); i++ ) {
			final ValueMapping valueMapping = discriminatorValueMappings.get( i );
			if ( valueMapping.entityMapping.equals( entityMappingType ) ) {
				return valueMapping.discriminatorValue;
			}
		}

		return null;
	}

	public EntityMappingType resolveDiscriminatorValueToEntityMapping(Object discriminatorValue) {
		//noinspection ForLoopReplaceableByForEach
		for ( int i = 0; i < discriminatorValueMappings.size(); i++ ) {
			final ValueMapping valueMapping = discriminatorValueMappings.get( i );
			if ( valueMapping.discriminatorValue.equals( discriminatorValue ) ) {
				return valueMapping.entityMapping;
			}
		}

		return null;
	}

	public void forEachConcreteType(Consumer<EntityMappingType> consumer) {
		discriminatorValueMappings.forEach( valueMapping -> consumer.accept( valueMapping.entityMapping ) );
	}

	public EntityMappingType findConcrete(Function<EntityMappingType, EntityMappingType> matcher) {
		for ( ValueMapping discriminatorValueMapping : discriminatorValueMappings ) {
			final EntityMappingType matched = matcher.apply( discriminatorValueMapping.entityMapping );
			if ( matched != null ) {
				return matched;
			}
		}

		return null;
	}

	public <T> T fromConcrete(Function<EntityMappingType,T> matcher) {
		for ( ValueMapping discriminatorValueMapping : discriminatorValueMappings ) {
			final T matched = matcher.apply( discriminatorValueMapping.entityMapping );
			if ( matched != null ) {
				return matched;
			}
		}
		return null;
	}

	public ModelPart findSubPart(String name, EntityMappingType treatTarget) {
		if ( AnyDiscriminatorPart.ROLE_NAME.equals( name ) ) {
			return getDiscriminatorPart();
		}

		if ( AnyKeyPart.ROLE_NAME.equals( name ) ) {
			return getKeyPart();
		}

		if ( treatTarget != null ) {
			// make sure the treat-target is one of the mapped entities
			ensureMapped( treatTarget );

			return resolveAssociatedSubPart( name, treatTarget );
		}

		for ( ValueMapping discriminatorValueMapping : discriminatorValueMappings ) {
			try {
				final ModelPart subPart = resolveAssociatedSubPart( name, discriminatorValueMapping.entityMapping );
				if ( subPart != null ) {
					return subPart;
				}
			}
			catch (Exception e) {
				//noinspection UnnecessaryContinue
				continue;
			}
		}

		return null;
	}

	private ModelPart resolveAssociatedSubPart(String name, EntityMappingType entityMapping) {
		final EntityIdentifierMapping identifierMapping = entityMapping.getIdentifierMapping();

		if ( identifierMapping.getPartName().equals( name ) ) {
			return getKeyPart();
		}

		if ( identifierMapping instanceof SingleAttributeIdentifierMapping ) {
			final String idAttrName = ( (SingleAttributeIdentifierMapping) identifierMapping ).getAttributeName();
			if ( idAttrName.equals( name ) ) {
				return getKeyPart();
			}
		}

		return entityMapping.findSubPart( name );
	}

	private void ensureMapped(EntityMappingType treatTarget) {
		assert treatTarget != null;

		for ( ValueMapping mapping : discriminatorValueMappings ) {
			if ( mapping.entityMapping.equals( treatTarget ) ) {
				return;
			}
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Treat-target [`%s`] is not not an entity mapped by ANY value : %s",
						treatTarget.getEntityName(),
						modelPart.getNavigableRole()
				)
		);
	}

	public MappingType getPartMappingType() {
		return this;
	}

	public JavaType<?> getJavaTypeDescriptor() {
		return baseAssociationJtd;
	}

	@Override
	public JavaType<?> getMappedJavaTypeDescriptor() {
		return baseAssociationJtd;
	}

	@Override
	public FetchStyle getStyle() {
		return FetchStyle.SELECT;
	}

	@Override
	public FetchTiming getTiming() {
		return fetchTiming;
	}

	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new AnyValuedFetch(
				fetchablePath,
				baseAssociationJtd,
				modelPart,
				fetchTiming,
				fetchParent,
				creationState
		);
	}

	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new AnyValuedResult<>(
				navigablePath,
				baseAssociationJtd,
				modelPart,
				resultVariable
		);
	}

	private static abstract class AnyValuedResultGraphNode implements DomainResultGraphNode, FetchParent {
		private final NavigablePath navigablePath;

		private final DiscriminatedAssociationModelPart graphedPart;
		private final JavaType<?> baseAssociationJtd;

		private Fetch discriminatorValueFetch;
		private Fetch keyValueFetch;

		public AnyValuedResultGraphNode(
				NavigablePath navigablePath,
				DiscriminatedAssociationModelPart graphedPart,
				JavaType<?> baseAssociationJtd) {
			this.navigablePath = navigablePath;
			this.graphedPart = graphedPart;
			this.baseAssociationJtd = baseAssociationJtd;
		}

		protected void afterInitialize(DomainResultCreationState creationState) {
			final List<Fetch> fetches = creationState.visitFetches( this );
			assert fetches.size() == 2;

			discriminatorValueFetch = fetches.get( 0 );
			keyValueFetch = fetches.get( 1 );
		}

		public Fetch getDiscriminatorValueFetch() {
			return discriminatorValueFetch;
		}

		public Fetch getKeyValueFetch() {
			return keyValueFetch;
		}

		public JavaType<?> getBaseAssociationJtd() {
			return baseAssociationJtd;
		}

		@Override
		public NavigablePath getNavigablePath() {
			return navigablePath;
		}

		@Override
		public JavaType<?> getResultJavaTypeDescriptor() {
			return baseAssociationJtd;
		}

		@Override
		public boolean containsAnyNonScalarResults() {
			return true;
		}

		@Override
		public DiscriminatedAssociationModelPart getReferencedMappingContainer() {
			return graphedPart;
		}

		@Override
		public DiscriminatedAssociationModelPart getReferencedMappingType() {
			return graphedPart;
		}

		@Override
		public List<Fetch> getFetches() {
			return Arrays.asList( discriminatorValueFetch, keyValueFetch );
		}

		@Override
		public Fetch findFetch(Fetchable fetchable) {
			assert graphedPart.getDiscriminatorPart() == fetchable
					|| graphedPart.getKeyPart() == fetchable;

			if ( graphedPart.getDiscriminatorPart() == fetchable ) {
				return discriminatorValueFetch;
			}

			if ( graphedPart.getKeyPart() == fetchable ) {
				return keyValueFetch;
			}

			throw new IllegalArgumentException( "Given Fetchable [" + fetchable + "] did not match either discriminator nor key mapping" );
		}
	}

	private static class AnyValuedResult<T> extends AnyValuedResultGraphNode implements DomainResult<T> {
		private final String resultVariable;

		public AnyValuedResult(
				NavigablePath navigablePath,
				JavaType<?> baseAssociationJtd,
				DiscriminatedAssociationModelPart fetchedPart,
				String resultVariable) {
			super( navigablePath, fetchedPart, baseAssociationJtd );
			this.resultVariable = resultVariable;
		}

		@Override
		public String getResultVariable() {
			return resultVariable;
		}

		@Override
		public DomainResultAssembler<T> createResultAssembler(
				FetchParentAccess parentAccess,
				AssemblerCreationState creationState) {
			return new AnyResultAssembler<>(
					getNavigablePath(),
					getReferencedMappingContainer(),
					true,
					getDiscriminatorValueFetch().createAssembler( null, creationState ),
					getKeyValueFetch().createAssembler( null, creationState )
			);
		}
	}

	private static class AnyValuedFetch extends AnyValuedResultGraphNode implements Fetch {
		private final FetchTiming fetchTiming;
		private final FetchParent fetchParent;

		public AnyValuedFetch(
				NavigablePath navigablePath,
				JavaType<?> baseAssociationJtd,
				DiscriminatedAssociationModelPart fetchedPart,
				FetchTiming fetchTiming,
				FetchParent fetchParent,
				DomainResultCreationState creationState) {
			super( navigablePath, fetchedPart, baseAssociationJtd );
			this.fetchTiming = fetchTiming;
			this.fetchParent = fetchParent;

			afterInitialize( creationState );
		}

		@Override
		public FetchParent getFetchParent() {
			return fetchParent;
		}

		@Override
		public DiscriminatedAssociationModelPart getFetchedMapping() {
			return getReferencedMappingContainer();
		}

		@Override
		public FetchTiming getTiming() {
			return fetchTiming;
		}

		@Override
		public boolean hasTableGroup() {
			return false;
		}

		@Override
		public DomainResultAssembler<?> createAssembler(
				FetchParentAccess parentAccess,
				AssemblerCreationState creationState) {
			return new AnyResultAssembler<>(
					getNavigablePath(),
					getFetchedMapping(),
					fetchTiming == FetchTiming.IMMEDIATE,
					getDiscriminatorValueFetch().createAssembler( parentAccess, creationState ),
					getKeyValueFetch().createAssembler( parentAccess, creationState )
			);
		}
	}

	private static class AnyResultAssembler<T> implements DomainResultAssembler<T> {
		private final NavigablePath fetchedPath;

		private final DiscriminatedAssociationModelPart fetchedPart;

		private final boolean eager;

		private final DomainResultAssembler<?> discriminatorValueAssembler;
		private final DomainResultAssembler<?> keyValueAssembler;

		public AnyResultAssembler(
				NavigablePath fetchedPath,
				DiscriminatedAssociationModelPart fetchedPart,
				boolean eager,
				DomainResultAssembler<?> discriminatorValueAssembler,
				DomainResultAssembler<?> keyValueAssembler) {
			this.fetchedPath = fetchedPath;
			this.fetchedPart = fetchedPart;
			this.eager = eager;
			this.discriminatorValueAssembler = discriminatorValueAssembler;
			this.keyValueAssembler = keyValueAssembler;
		}

		@Override
		public T assemble(
				RowProcessingState rowProcessingState,
				JdbcValuesSourceProcessingOptions options) {
			final Object discriminatorValue = discriminatorValueAssembler.assemble( rowProcessingState, options );

			if ( discriminatorValue == null ) {
				// null association
				assert keyValueAssembler.assemble( rowProcessingState, options ) == null;
				return null;
			}

			final EntityMappingType entityMapping = fetchedPart.resolveDiscriminatorValue( discriminatorValue );

			final Object keyValue = keyValueAssembler.assemble( rowProcessingState, options );

			final SharedSessionContractImplementor session = rowProcessingState
					.getJdbcValuesSourceProcessingState()
					.getSession();

			//noinspection unchecked
			return (T) session.internalLoad(
					entityMapping.getEntityName(),
					keyValue,
					eager,
					// should not be null since we checked already.  null would indicate bad data (ala, not-found handling)
					false
			);
		}

		@Override
		public JavaType<T> getAssembledJavaTypeDescriptor() {
			//noinspection unchecked
			return (JavaType<T>) fetchedPart.getJavaTypeDescriptor();
		}

		@Override
		public String toString() {
			return "AnyResultAssembler( `" + fetchedPath + "` )";
		}
	}
}
