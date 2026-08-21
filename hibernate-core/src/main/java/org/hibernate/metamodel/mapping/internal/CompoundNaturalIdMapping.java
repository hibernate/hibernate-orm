/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.loader.ast.internal.CompoundNaturalIdLoader;
import org.hibernate.loader.ast.internal.MultiNaturalIdLoaderInPredicate;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.hibernate.property.access.spi.GetterMethodImpl;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.hibernate.internal.util.StringHelper.decapitalize;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.Collections.emptyMap;

/**
 * Multi-attribute NaturalIdMapping implementation
 */
public class CompoundNaturalIdMapping extends AbstractNaturalIdMapping implements MappingType, FetchableContainer {

	private final List<SingularAttributeMapping> attributes;
	private final ValueNormalizer valueNormalizer;

	private List<JdbcMapping> jdbcMappings;
	/*
		This value is used to determine the size of the array used to create the ImmutableFetchList (see org.hibernate.sql.results.graph.internal.ImmutableFetchList#Builder)
		The Fetch is inserted into the array at a position corresponding to its Fetchable key value.
	 */
	private final int maxFetchableKeyIndex;

	private final SessionFactoryImplementor sessionFactory;

	public CompoundNaturalIdMapping(
			EntityMappingType declaringType,
			ClassDetails naturalIdClass,
			List<SingularAttributeMapping> attributes,
			MappingModelCreationProcess creationProcess) {
		super( declaringType, isMutable( attributes ) );
		this.valueNormalizer = createValueNormalizer( naturalIdClass, attributes, creationProcess );
		this.attributes = attributes;

		int maxIndex = 0;
		for ( var attribute : attributes ) {
			if ( attribute.getFetchableKey() > maxIndex ) {
				maxIndex = attribute.getFetchableKey();
			}
		}
		this.maxFetchableKeyIndex = maxIndex + 1;

		creationProcess.registerInitializationCallback(
				"Determine compound natural-id JDBC mappings ( " + declaringType.getEntityName() + ")",
				() -> {
					final List<JdbcMapping> jdbcMappings = new ArrayList<>();
					attributes.forEach( attribute -> attribute.forEachJdbcType(
							(index, jdbcMapping) -> jdbcMappings.add( jdbcMapping )
					) );
					this.jdbcMappings = jdbcMappings;
					return true;
				}
		);

		this.sessionFactory = creationProcess.getCreationContext().getSessionFactory();
	}

	private static boolean isMutable(List<SingularAttributeMapping> attributes) {
		for ( int i = 0; i < attributes.size(); i++ ) {
			if ( attributes.get( i ).getAttributeMetadata().isUpdatable() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	@Nullable
	public Class<?> getNaturalIdClass() {
		return valueNormalizer.getIdClassType();
	}

	@Override
	public Object[] extractNaturalIdFromEntityState(Object[] state) {
		if ( state == null ) {
			return null;
		}
		else if ( state.length == attributes.size() ) {
			return state;
		}
		else {
			final var values = new Object[attributes.size()];
			for ( int i = 0; i <= attributes.size() - 1; i++ ) {
				final var attributeMapping = attributes.get( i );
				values[i] = state[attributeMapping.getStateArrayPosition()];
			}
			return values;
		}
	}

	@Override
	public Object[] extractNaturalIdFromEntity(Object entity) {
		final var values = new Object[attributes.size()];
		for ( int i = 0; i < attributes.size(); i++ ) {
			values[i] = attributes.get( i ).getPropertyAccess().getGetter().get( entity );
		}
		return values;
	}

	@Override
	public Object[] normalizeInput(Object incoming) {
		sessionFactory.getStatistics().normalizeNaturalId( getDeclaringType().getEntityName() );

		if ( incoming instanceof Object[] array ) {
			// already normalized
			return array;
		}
		else {
			return valueNormalizer.normalize( incoming );
		}
	}

	@Override
	public boolean isNormalized(Object incoming) {
		return incoming instanceof Object[];
	}

	@Override
	public void validateInternalForm(Object naturalIdValue) {
		if ( naturalIdValue != null ) {
			// should be an array, with a size equal to the number of attributes making up this compound natural-id
			if ( naturalIdValue instanceof Object[] values ) {
				if ( values.length != attributes.size() ) {
					throw new IllegalArgumentException(
							"Natural-id value [" + naturalIdValue + "] did not contain the expected number of elements ["
							+ attributes.size() + "]"
					);
				}
			}
			else {
				throw new IllegalArgumentException(
						"Natural-id value [" + naturalIdValue + "] was not an array as expected" );
			}
		}
	}

	@Override
	public int calculateHashCode(Object value) {
		if ( value == null ) {
			return 0;
		}
		else {
			final var values = (Object[]) value;
			int hashcode = 0;
			for ( int i = 0; i < attributes.size(); i++ ) {
				final Object o = values[i];
				if ( o != null ) {
					hashcode = 27 * hashcode
							+ ((JavaType) attributes.get( i ).getExpressibleJavaType()).extractHashCode( o );
				}
			}
			return hashcode;
		}
	}

	@Override
	public void verifyFlushState(Object id, Object[] currentState, Object[] loadedState, SharedSessionContractImplementor session) {
		if ( !isMutable() ) {
			final var persistenceContext = session.getPersistenceContextInternal();
			final var persister = getDeclaringType().getEntityPersister();

			final Object[] naturalId = extractNaturalIdFromEntityState( currentState );
			final Object snapshot = loadedState == null
					? persistenceContext.getNaturalIdSnapshot( id, persister )
					: persister.getNaturalIdMapping().extractNaturalIdFromEntityState( loadedState );
			final Object[] previousNaturalId = (Object[]) snapshot;
			assert naturalId.length == getNaturalIdAttributes().size();
			assert previousNaturalId.length == naturalId.length;

			for ( int i = 0; i < getNaturalIdAttributes().size(); i++ ) {
				final var attributeMapping = getNaturalIdAttributes().get( i );
				if ( !attributeMapping.getAttributeMetadata().isUpdatable() ) {
					final Object currentValue = naturalId[i];
					final Object previousValue = previousNaturalId[i];
					if ( !attributeMapping.areEqual( currentValue, previousValue, session ) ) {
						throw new HibernateException(
								String.format(
										"An immutable attribute [%s] within compound natural identifier of entity %s was altered from `%s` to `%s`",
										attributeMapping.getAttributeName(),
										persister.getEntityName(),
										previousValue,
										currentValue
								)
						);
					}
				}
				// else property is updatable (mutable), there is nothing to check
			}
		}
		// otherwise the natural id is mutable (!immutable), no need to do the checks
	}

	@Override
	public boolean areEqual(Object one, Object other, SharedSessionContractImplementor session) {
		final var oneArray = (Object[]) one;
		final var otherArray = (Object[]) other;
		final var naturalIdAttributes = getNaturalIdAttributes();
		for ( int i = 0; i < naturalIdAttributes.size(); i++ ) {
			if ( !naturalIdAttributes.get( i ).areEqual( oneArray[i], otherArray[i], session ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public List<SingularAttributeMapping> getNaturalIdAttributes() {
		return attributes;
	}

	@Override
	public NaturalIdLoader<?> makeLoader(EntityMappingType entityDescriptor) {
		return new CompoundNaturalIdLoader<>( this, entityDescriptor );
	}

	@Override
	public MultiNaturalIdLoader<?> makeMultiLoader(EntityMappingType entityDescriptor) {
		return new MultiNaturalIdLoaderInPredicate<>( entityDescriptor );
	}

	@Override
	public MappingType getPartMappingType() {
		return this;
	}

	@Override
	public JavaType<?> getJavaType() {
		// the JavaType is the entity itself
		return getDeclaringType().getJavaType();
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return getJavaType();
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		for ( var attributeMapping : attributes ) {
			if ( attributeMapping.hasPartitionedSelectionMapping() ) {
				return true;
			}
		}
		return false;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ModelPart

	@Override
	public <T> DomainResult<T> createDomainResult(NavigablePath navigablePath, TableGroup tableGroup, String resultVariable, DomainResultCreationState creationState) {
		assert navigablePath.getLocalName().equals( NaturalIdMapping.PART_NAME );

		final var objectArrayJavaType =
				creationState.getSqlAstCreationState().getCreationContext()
						.getTypeConfiguration().getJavaTypeRegistry()
						.resolveDescriptor( Object[].class );

		// register the table group under `...{natural-id}` as well
		creationState.getSqlAstCreationState().getFromClauseAccess()
				.resolveTableGroup( navigablePath, np -> tableGroup );

		return (DomainResult<T>) new DomainResultImpl(
				navigablePath,
				this,
				objectArrayJavaType,
				resultVariable,
				creationState
		);
	}

	@Override
	public void applySqlSelections(NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {
		for ( int i = 0; i < attributes.size(); i++ ) {
			attributes.get( i ).applySqlSelections( navigablePath, tableGroup, creationState );
		}
	}

	@Override
	public void applySqlSelections(NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState, BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		for ( int i = 0; i < attributes.size(); i++ ) {
			attributes.get( i ).applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
		}
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		int span = 0;
		if ( domainValue == null ) {
			for ( int i = 0; i < attributes.size(); i++ ) {
				span += attributes.get( i )
						.breakDownJdbcValues( null, offset + span, x, y, valueConsumer, session );
			}
		}
		else if ( domainValue instanceof Object[] values ) {
			assert values.length == attributes.size();
			for ( int i = 0; i < attributes.size(); i++ ) {
				span += attributes.get( i ).breakDownJdbcValues(
						values[i],
						offset + span,
						x,
						y,
						valueConsumer,
						session
				);
			}
		}
		else {
			throw new AssertionFailure( "Unexpected domain value type" );
		}
		return span;
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		int span = 0;
		for ( int i = 0; i < attributes.size(); i++ ) {
			span += attributes.get( i ).forEachSelectable( span + offset, consumer );
		}
		return span;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Bindable

	@Override
	public int getJdbcTypeCount() {
		return jdbcMappings.size();
	}

	@Override
	public JdbcMapping getJdbcMapping(int index) {
		return jdbcMappings.get( index );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		int span = 0;
		for ( ; span < jdbcMappings.size(); span++ ) {
			action.accept( span + offset, jdbcMappings.get( span ) );
		}
		return span;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		else if ( value instanceof Object[] incoming ) {
			assert incoming.length == attributes.size();
			final var outgoing = new Object[incoming.length];
			for ( int i = 0; i < attributes.size(); i++ ) {
				outgoing[i] = attributes.get( i ).disassemble( incoming[i], session );
			}
			return outgoing;
		}
		else {
			throw new AssertionFailure( "Unexpected value" );
		}
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			for ( int i = 0; i < attributes.size(); i++ ) {
				attributes.get( i ).addToCacheKey( cacheKey, null, session );
			}
		}
		else if ( value instanceof Object[] values ) {
			assert values.length == attributes.size();
			for ( int i = 0; i < attributes.size(); i++ ) {
				attributes.get( i ).addToCacheKey( cacheKey, values[i], session );
			}
		}
		else {
			throw new AssertionFailure( "Unexpected value" );
		}
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		int span = 0;
		if ( value == null ) {
			for ( int i = 0; i < attributes.size(); i++ ) {
				final var attribute = attributes.get( i );
				span += attribute.forEachDisassembledJdbcValue(
						null,
						span + offset,
						x,
						y,
						valuesConsumer,
						session
				);
			}
		}
		else if ( value instanceof Object[] incoming ) {
			assert incoming.length == attributes.size();
			for ( int i = 0; i < attributes.size(); i++ ) {
				final var attribute = attributes.get( i );
				span += attribute.forEachDisassembledJdbcValue(
						incoming[i],
						span + offset,
						x,
						y,
						valuesConsumer,
						session
				);
			}
		}
		else {
			throw new AssertionFailure( "Unexpected value" );
		}
		return span;
	}

	@Override
	public <X, Y> int forEachJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		int span = 0;
		if ( value == null ) {
			for ( int i = 0; i < attributes.size(); i++ ) {
				final var attribute = attributes.get( i );
				span += attribute.forEachJdbcValue( null, span + offset, x, y, valuesConsumer, session );
			}
		}
		else if ( value instanceof Object[] incoming ) {
			assert incoming.length == attributes.size();
			for ( int i = 0; i < attributes.size(); i++ ) {
				final var attribute = attributes.get( i );
				span += attribute.forEachJdbcValue( incoming[i], span + offset, x, y, valuesConsumer, session );
			}
		}
		else {
			throw new AssertionFailure( "Unexpected value" );
		}
		return span;
	}

	@Override
	public int getNumberOfFetchables() {
		return attributes.size();
	}

	@Override
	public Fetchable getFetchable(int position) {
		return attributes.get( position );
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		for ( int i = 0; i < attributes.size(); i++ ) {
			if ( name.equals( attributes.get( i ).getAttributeName() ) ) {
				return attributes.get( i );
			}
		}
		return null;
	}

	@Override
	public void forEachSubPart(IndexedConsumer<ModelPart> consumer, EntityMappingType treatTarget) {
		for ( int i = 0; i < attributes.size(); i++ ) {
			consumer.accept( i, attributes.get( i ) );
		}
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		attributes.forEach( consumer );
	}

	@Override
	public int getNumberOfFetchableKeys() {
		return maxFetchableKeyIndex;
	}

	public static class DomainResultImpl implements DomainResult<Object[]>, FetchParent {
		private final NavigablePath navigablePath;
		private final CompoundNaturalIdMapping naturalIdMapping;
		private final JavaType<Object[]> arrayJtd;

		private final ImmutableFetchList fetches;
		private final boolean hasJoinFetches;
		private final boolean containsCollectionFetches;

		private final String resultVariable;

		public DomainResultImpl(
				NavigablePath navigablePath,
				CompoundNaturalIdMapping naturalIdMapping,
				JavaType<Object[]> arrayJtd,
				String resultVariable,
				DomainResultCreationState creationState) {
			this.navigablePath = navigablePath;
			this.naturalIdMapping = naturalIdMapping;
			this.arrayJtd = arrayJtd;
			this.resultVariable = resultVariable;
			this.fetches = creationState.visitFetches( this );
			this.hasJoinFetches = this.fetches.hasJoinFetches();
			this.containsCollectionFetches = this.fetches.containsCollectionFetches();
		}

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// DomainResult

		@Override
		public String getResultVariable() {
			return resultVariable;
		}

		@Override
		public DomainResultAssembler<Object[]> createResultAssembler(
				InitializerParent<?> parent,
				AssemblerCreationState creationState) {
			return new AssemblerImpl( fetches, arrayJtd, creationState );
		}

		@Override
		public JavaType<Object[]> getResultJavaType() {
			return arrayJtd;
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// FetchParent

		@Override
		public Initializer<?> createInitializer(InitializerParent<?> parent, AssemblerCreationState creationState) {
			throw new UnsupportedOperationException( "Compound natural id mappings should not use an initializer" );
		}

		@Override
		public FetchableContainer getReferencedMappingContainer() {
			return getReferencedMappingType();
		}

		@Override
		public FetchableContainer getReferencedMappingType() {
			return naturalIdMapping;
		}

		@Override
		public NavigablePath getNavigablePath() {
			return navigablePath;
		}

		@Override
		public ImmutableFetchList getFetches() {
			return fetches;
		}

		@Override
		public Fetch findFetch(Fetchable fetchable) {
			assert fetchable != null;
			return fetches.get( fetchable );
		}

		@Override
		public boolean hasJoinFetches() {
			return hasJoinFetches;
		}

		@Override
		public boolean containsCollectionFetches() {
			return containsCollectionFetches;
		}
	}

	private static class AssemblerImpl implements DomainResultAssembler<Object[]> {
		private final JavaType<Object[]> jtd;
		private final DomainResultAssembler<?>[] subAssemblers;

		private AssemblerImpl(ImmutableFetchList fetches, JavaType<Object[]> jtd, AssemblerCreationState creationState) {
			this.jtd = jtd;
			this.subAssemblers = new DomainResultAssembler[fetches.size()];
			int i = 0;
			for ( Fetch fetch : fetches ) {
				subAssemblers[i++] = fetch.createAssembler( null, creationState );
			}
		}

		@Override
		public Object[] assemble(RowProcessingState rowProcessingState) {
			final var result = new Object[subAssemblers.length];
			for ( int i = 0; i < subAssemblers.length; i++ ) {
				result[i] = subAssemblers[i].assemble( rowProcessingState );
			}
			return result;
		}

		@Override
		public void resolveState(RowProcessingState rowProcessingState) {
			for ( var subAssembler : subAssemblers ) {
				subAssembler.resolveState( rowProcessingState );
			}
		}

		@Override
		public <X> void forEachResultAssembler(BiConsumer<Initializer<?>, X> consumer, X arg) {
			for ( var subAssembler : subAssemblers ) {
				final var initializer = subAssembler.getInitializer();
				// In case of natural id mapping selection every initializer is a "result initializer",
				// regardless of what Initializer#isResultInitializer reports
				if ( initializer != null ) {
					consumer.accept( initializer, arg );
				}
			}
		}

		@Override
		public JavaType<Object[]> getAssembledJavaType() {
			return jtd;
		}
	}

	interface ValueNormalizer {
		boolean isInstance(Object value);
		Object[] normalize(Object value);
		Class<?> getIdClassType();
	}

	private static <T> ValueNormalizer createValueNormalizer(
			ClassDetails naturalIdClassDetails,
			List<SingularAttributeMapping> keyAttributes,
			MappingModelCreationProcess creationProcess) {
		if ( naturalIdClassDetails == null ) {
			return new ValueNormalizerSupport( keyAttributes );
		}

		final var modelsContext =
				creationProcess.getCreationContext().getBootstrapContext()
						.getModelsContext();

		var naturalIdClass = naturalIdClassDetails.toJavaClass( modelsContext.getClassLoading(), modelsContext );
		var naturalIdClassComponents = extractComponents( naturalIdClass );
		var naturalIdClassGetterAccess = createNaturalIdClassGetterAccess( naturalIdClass );

		final List<AttributeMapper<Object, T>> attributeMappers = new ArrayList<>();
		keyAttributes.forEach( keyAttribute -> {
			// find the matching MemberDetails on the `naturalIdClass`...
			final var extractor = resolveMatchingExtractor(
					naturalIdClass,
					keyAttribute,
					naturalIdClassGetterAccess,
					naturalIdClassComponents,
					modelsContext
			);
			// todo (natural-id-class) : atm there is functionally no difference
			//		between BasicAttributeMapperImpl and ToOneAttributeMapperImpl.
			//		ideally we'd eventually support usage of the associated key entity's
			//		id and then there would.  see the note in ToOneAttributeMapperImpl#extractFrom
			final var attrMapper =
					keyAttribute instanceof ToOneAttributeMapping
							? new ToOneAttributeMapperImpl<T>( keyAttribute, extractor )
							: new BasicAttributeMapperImpl<T>( keyAttribute, extractor );
			attributeMappers.add( attrMapper );
		} );

		//noinspection unchecked,rawtypes
		return new KeyClassNormalizer( keyAttributes, naturalIdClass, attributeMappers );
	}

	static class ValueNormalizerSupport implements ValueNormalizer {
		private final List<SingularAttributeMapping> naturalIdAttributes;

		public ValueNormalizerSupport(List<SingularAttributeMapping> naturalIdAttributes) {
			this.naturalIdAttributes = naturalIdAttributes;
		}

		@Override
		public boolean isInstance(Object value) {
			return value instanceof Map;
		}

		@Override
		public Object[] normalize(Object incoming) {
			if ( !isInstance( incoming ) ) {
				throw new UnsupportedMappingException( "Could not normalize compound natural id value: " + incoming );
			}
			final var values = new Object[naturalIdAttributes.size()];
			//noinspection unchecked
			final Map<String,Object> valuesMap = (Map<String,Object>) incoming;
			for ( int i = 0; i < naturalIdAttributes.size(); i++ ) {
				values[i] = valuesMap.get( naturalIdAttributes.get( i ).getAttributeName() );
			}
			return values;
		}

		@Override
		public Class<?> getIdClassType() {
			return null;
		}
	}

	/// Responsible for decomposing a value of the NaturalIdClass into the internal array format
	static class KeyClassNormalizer<T> extends ValueNormalizerSupport {
		private final Class<T> idClassType;
		private final List<AttributeMapper<Object, T>> idClassAttributeMappers;

		public KeyClassNormalizer(
				List<SingularAttributeMapping> naturalIdAttributes,
				Class<T> idClassType,
				List<AttributeMapper<Object, T>> idClassAttributeMappers) {
			super( naturalIdAttributes );
			this.idClassType = idClassType;
			this.idClassAttributeMappers = idClassAttributeMappers;
		}

		@Override
		public Class<?> getIdClassType() {
			return idClassType;
		}

		@Override
		public Object[] normalize(Object value) {
			if ( idClassType.isInstance( value ) ) {
				return doNormalize( idClassType.cast( value ) );
			}

			return super.normalize( value );
		}

		public Object[] doNormalize(T idClassValue) {
			final var result = new Object[idClassAttributeMappers.size()];
			for ( int i = 0; i < idClassAttributeMappers.size(); i++ ) {
				var value = idClassAttributeMappers.get( i ).extractFrom( idClassValue );
				result[i] = value;
			}
			return result;
		}

		public boolean isInstance(Object value) {
			return idClassType.isInstance( value ) || super.isInstance( value );
		}
	}

	private static <T> Function<String, Method> createNaturalIdClassGetterAccess(Class<T> naturalIdClass) {
		return new Function<>() {
			private Map<String,Method> getterMethods;
			@Override
			public Method apply(String name) {
				if ( getterMethods == null ) {
					getterMethods = extractGetterMethods( naturalIdClass );
				}
				return getterMethods.get( name );
			}
		};
	}

	private static <T> Getter resolveMatchingExtractor(
			Class<T> naturalIdClass,
			AttributeMapping keyAttribute,
			Function<String, Method> getterMethodAccess,
			Map<String, RecordComponent> naturalIdClassComponents,
			ModelsContext modelsContext) {
		// first, if the `naturalIdClass` is a record, look for a component
		final String keyName = keyAttribute.getAttributeName();

		if ( naturalIdClass.isRecord() ) {
			final var component = naturalIdClassComponents.get( keyName );
			if ( component != null ) {
				return new GetterMethodImpl( naturalIdClass, keyName, component.getAccessor() );
			}
		}

		// next look for a getter method
		final var getterMethod = getterMethodAccess.apply( keyName );
		if ( getterMethod != null ) {
			return new GetterMethodImpl( naturalIdClass, keyName, getterMethod );
		}

		// lastly, look for a field
		try {
			return new GetterFieldImpl( naturalIdClass, keyName,
					naturalIdClass.getDeclaredField( keyName ) );
		}
		catch (NoSuchFieldException ignore) {
		}

		throw new MappingException( "Unable to find NaturalIdClass accessor for natural id attribute: " + keyName );
	}

	private static <T> Map<String, Method> extractGetterMethods(Class<T> naturalIdClass) {
		final Map<String, Method> result = new HashMap<>();
		for ( var declaredMethod : naturalIdClass.getDeclaredMethods() ) {
			if ( declaredMethod.getParameterCount() == 0
				&& declaredMethod.getReturnType() != void.class
				&& !isStatic( declaredMethod.getModifiers() ) ) {
				var methodName = declaredMethod.getName();
				if ( methodName.startsWith( "is" ) ) {
					result.put( decapitalize( methodName.substring( 2 ) ),
							declaredMethod );
				}
				else if ( methodName.startsWith( "get" ) ) {
					result.put( decapitalize( methodName.substring( 3 ) ),
							declaredMethod );
				}
			}
		}
		return result;
	}

	private static Map<String, RecordComponent> extractComponents(Class<?> naturalIdClass) {
		if ( !naturalIdClass.isRecord() ) {
			return emptyMap();
		}

		final var recordComponents = naturalIdClass.getRecordComponents();
		final Map<String, RecordComponent> result = new HashMap<>();
		for ( RecordComponent recordComponent : recordComponents ) {
			result.put( recordComponent.getName(), recordComponent );
		}
		return result;
	}

	public interface AttributeMapper<V, T> {
		V extractFrom(T keyValue);
	}

	/// AttributeMapper for both basic and embedded values
	public record BasicAttributeMapperImpl<T>(AttributeMapping entityAttribute, Getter keyClassExtractor)
			implements AttributeMapper<Object, T> {
		@Override
		public Object extractFrom(T keyValue) {
			return keyClassExtractor.get( keyValue );
		}
	}

	/// AttributeMapper for to-one values
	public record ToOneAttributeMapperImpl<T>(AttributeMapping entityAttribute, Getter keyClassExtractor)
			implements AttributeMapper<Object, T> {
		@Override
		public Object extractFrom(T keyValue) {
			// todo (natural-id-class) : handle "key -> to-one" resolutions
			//		this requires some contract changes though to pass Session
			//		to be able to resolve key -> entity for the to-one.
			//		+
			/// 	the other difficulty is handling "derived id" structures
			//
			//		see `NaturalIdMapping#normalizeInput`
			return keyClassExtractor.get( keyValue );
		}
	}
}
