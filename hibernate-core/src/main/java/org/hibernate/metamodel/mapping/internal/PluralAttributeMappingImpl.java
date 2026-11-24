package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.MappingException;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.profile.internal.FetchProfileAffectee;
import org.hibernate.cascade.spi.CascadeStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.AuditMapping;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.AuxiliaryMapping;
import org.hibernate.metamodel.mapping.CollectionIdentifierDescriptor;
import org.hibernate.metamodel.mapping.CollectionMappingType;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SoftDeleteMapping;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.metamodel.mapping.TemporalMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.ordering.spi.OrderByFragment;
import org.hibernate.metamodel.mapping.ordering.OrderByFragmentTranslator;
import org.hibernate.metamodel.mapping.ordering.TranslationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationStrategy;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.action.queue.spi.decompose.collection.CollectionMutationTarget;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.metamodel.mapping.internal.CurrentState.TargetRead;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.expression.CaseSearchedExpression;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.creation.SqlAliasBase;
import org.hibernate.sql.ast.spi.creation.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.from.CollectionTableGroup;
import org.hibernate.sql.ast.spi.query.from.AuxiliaryTableReference;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.OneToManyTableGroup;
import org.hibernate.sql.ast.spi.query.from.PluralTableGroup;
import org.hibernate.sql.ast.spi.query.from.QueryPartTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.NullnessPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.predicate.Junction;
import org.hibernate.sql.ast.spi.query.predicate.PredicateCollector;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.collection.internal.CollectionDomainResult;
import org.hibernate.sql.results.graph.collection.internal.DelayedCollectionFetch;
import org.hibernate.sql.results.graph.collection.internal.EagerCollectionFetch;
import org.hibernate.sql.results.graph.collection.internal.SelectEagerCollectionFetch;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.BasicType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Locale.ROOT;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.internal.util.StringHelper.subStringNullIfEmpty;
import static org.hibernate.metamodel.mapping.internal.CurrentState.hasHistory;
import static org.hibernate.metamodel.mapping.internal.CurrentState.ownerRevision;
import static org.hibernate.query.sqm.ComparisonOperator.GREATER_THAN;


/**
 * @author Steve Ebersole
 */
public class PluralAttributeMappingImpl
		extends AbstractAttributeMapping
		implements PluralAttributeMapping, FetchProfileAffectee, FetchOptions {

	/**
	 * Allows callback after creation of the attribute mapping.
	 *
	 * Support for the {@linkplain CollectionPersister collection},
	 * {@linkplain CollectionPart element} and {@linkplain CollectionPart index}
	 * descriptors
	 */
	public interface Aware {
		/**
		 * Injects the created attribute mapping
		 */
		void injectAttributeMapping(PluralAttributeMapping attributeMapping);
	}

	private final CollectionMappingType<?> collectionMappingType;
	private final String referencedPropertyName;
	@Nullable private final String mapKeyPropertyName;

	private final CollectionPart elementDescriptor;
	@Nullable private final CollectionPart indexDescriptor;
	@Nullable private final CollectionIdentifierDescriptor identifierDescriptor;
	private final FetchTiming fetchTiming;
	private final FetchStyle fetchStyle;
	@Nullable
	private AuxiliaryMapping auxiliaryMapping;

	private final String bidirectionalAttributeName;

	private final CollectionPersister collectionDescriptor;
	@Nullable private final String separateCollectionTable;

	private final String sqlAliasStem;

	private final IndexMetadata indexMetadata;

	@SuppressWarnings("NullAway.Init") // Assigned during mapping model initialization.
	private ForeignKeyDescriptor fkDescriptor;

	@Nullable private OrderByFragment orderByFragment;
	@Nullable private OrderByFragment manyToManyOrderByFragment;

	public PluralAttributeMappingImpl(
			String attributeName,
			Collection bootDescriptor,
			PropertyAccess propertyAccess,
			AttributeMetadata attributeMetadata,
			CollectionMappingType<?> collectionMappingType,
			int stateArrayPosition,
			int fetchableIndex,
			CollectionPart elementDescriptor,
			@Nullable CollectionPart indexDescriptor,
			@Nullable CollectionIdentifierDescriptor identifierDescriptor,
			FetchTiming fetchTiming,
			FetchStyle fetchStyle,
			CascadeStyle cascadeStyle,
			ManagedMappingType declaringType,
			CollectionPersister collectionDescriptor,
			MappingModelCreationProcess creationProcess) {
		super( attributeName, fetchableIndex, declaringType, attributeMetadata, stateArrayPosition, propertyAccess );
		this.collectionMappingType = collectionMappingType;
		this.elementDescriptor = elementDescriptor;
		this.indexDescriptor = indexDescriptor;
		this.identifierDescriptor = identifierDescriptor;
		this.fetchTiming = fetchTiming;
		this.fetchStyle = fetchStyle;
		this.collectionDescriptor = collectionDescriptor;
		this.referencedPropertyName = bootDescriptor.getReferencedPropertyName();

		mapKeyPropertyName = bootDescriptor instanceof Map map ? map.getMapKeyPropertyName() : null;

		bidirectionalAttributeName = subStringNullIfEmpty( bootDescriptor.getMappedByProperty(), '.');

		sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( attributeName );

		separateCollectionTable = bootDescriptor.isOneToMany() ? null : collectionDescriptor.getTableName();

		final int baseIndex = bootDescriptor instanceof org.hibernate.mapping.List list ? list.getBaseIndex() : -1;
		indexMetadata = new IndexMetadata() {
			@Nullable
			@Override
			public CollectionPart getIndexDescriptor() {
				return indexDescriptor;
			}

			@Override
			public int getListIndexBase() {
				return baseIndex;
			}

			@Nullable
			@Override
			public String getIndexPropertyName() {
				return mapKeyPropertyName;
			}
		};

		auxiliaryMapping =
				bootDescriptor.getStateManagement()
						.createAuxiliaryMapping( this, bootDescriptor, creationProcess );
		if ( ( auxiliaryMapping instanceof TemporalMapping || auxiliaryMapping instanceof AuditMapping )
				&& ( bootDescriptor.getCustomSQLInsert() != null
					|| bootDescriptor.getCustomSQLUpdate() != null
					|| bootDescriptor.getCustomSQLDelete() != null
					|| bootDescriptor.getCustomSQLDeleteAll() != null ) ) {
			throw new UnsupportedMappingException( "Collection '" + bootDescriptor.getRole() + "' is "
					+ ( auxiliaryMapping instanceof AuditMapping ? "audited" : "temporal" )
					+ " and so may not be annotated '@SQLInsert', '@SQLUpdate', '@SQLDelete', or '@SQLDeleteAll'" );
		}

		injectAttributeMapping( elementDescriptor, indexDescriptor, collectionDescriptor, this );

		if ( elementDescriptor instanceof EntityCollectionPart elementMapping ) {
			validateTargetEntity( elementMapping, declaringType, attributeName, propertyAccess, creationProcess );
		}
	}

	/**
	 * @implNote This is check based on best effort.  If we are not able to resolve
	 * something needed for the check we simply short-circuit in the "affirmative".
	 * In testing, this mainly manifested in cases with embeddable inheritance
	 * given how the EmbeddableMappingType is built and an inability to locate
	 * subtype members.
	 */
	private static void validateTargetEntity(
			EntityCollectionPart elementPart,
			ManagedMappingType declaringType,
			String attributeName,
			PropertyAccess propertyAccess,
			MappingModelCreationProcess creationProcess) {
		final var representationStrategy = typeRepresentationStrategy( declaringType );
		if ( representationStrategy != null
				// nothing to check against with dynamic models
				&& representationStrategy.getMode() == RepresentationMode.POJO ) {
			final var attributeMemberDetails =
					getMemberDetails( attributeName, propertyAccess,
							declaringClassDetails( declaringType, creationProcess ) );
			if ( attributeMemberDetails != null ) {
				checkElementType( elementPart, declaringType, attributeName, attributeMemberDetails );
			}
			// else usually indicates the case of embeddable
			// inheritance mentioned in the @implNote
		}
	}

	private static ClassDetails declaringClassDetails(
			ManagedMappingType declaringType,
			MappingModelCreationProcess creationProcess) {
		return creationProcess.getCreationContext().getBootstrapContext()
				.getModelsContext().getClassDetailsRegistry()
				.resolveClassDetails( declaringType.getJavaType().getTypeName() );
	}

	private static void checkElementType(
			EntityCollectionPart elementPart,
			ManagedMappingType declaringType,
			String attributeName,
			MemberDetails attributeMemberDetails) {
		final var elementType =
				attributeMemberDetails.getElementType()
						.determineRawClass().toJavaClass();
		if ( !Object.class.equals( elementType ) ) {
			final var targetType = elementPart.getJavaType().getJavaTypeClass();
			if ( !elementType.isAssignableFrom( targetType ) ) {
				throw new MappingException(
						String.format(
								ROOT,
								"Plural attribute [%s.%s] was mapped with targetEntity=`%s`,"
										+ " but the attribute is declared as `%s`",
								castNonNull( declaringType.getNavigableRole() ).getFullPath(),
								attributeName,
								targetType.getName(),
								elementType.getName()
						)
				);
			}
		}
	}

	private static @Nullable MemberDetails getMemberDetails(
			String attributeName, PropertyAccess propertyAccess, ClassDetails declaringClassDetails) {
		final var member = propertyAccess.getGetter().getMember();
		if ( member instanceof Field ) {
			return locateField( declaringClassDetails, attributeName );
		}
		else if ( member instanceof Method method ) {
			return locateGetter( declaringClassDetails, method );
		}
		else {
			// we need access to the field or getter...
			return null;
		}
	}

	private static @Nullable ManagedTypeRepresentationStrategy typeRepresentationStrategy(ManagedMappingType declaringType) {
		if ( declaringType instanceof EntityMappingType declaringEntityType ) {
			return declaringEntityType.getRepresentationStrategy();
		}
		else if ( declaringType instanceof EmbeddableMappingType declaringEmbeddableType ) {
			return declaringEmbeddableType.getRepresentationStrategy();
		}
		else {
			// should never happen, but be lenient
			return null;
		}
	}

	/**
	 * Locate the corresponding field details.
	 *
	 * @return The field details, or {@code null} if we cannot locate it.
	 *
	 * @implNote See `implNote` on {@linkplain #validateTargetEntity} for details
	 * about why we return {@code null} instead of throwing an exception.
	 */
	@Nullable
	private static FieldDetails locateField(ClassDetails declaringClassDetails, String attributeName) {
		assert declaringClassDetails != null;
		var classDetails = declaringClassDetails;
		while ( classDetails != null && classDetails != ClassDetails.OBJECT_CLASS_DETAILS ) {
			final var fieldDetails = classDetails.findFieldByName( attributeName );
			if ( fieldDetails != null ) {
				return fieldDetails;
			}
			classDetails = classDetails.getSuperClass();
		}
		return null;
	}

	/**
	 * Locate the corresponding getter method details.
	 *
	 * @return The getter method details, or {@code null} if we cannot locate it.
	 *
	 * @implNote See `implNote` on {@linkplain #validateTargetEntity} for details
	 * about why we return {@code null} instead of throwing an exception.
	 */
	@Nullable
	private static MethodDetails locateGetter(ClassDetails declaringClassDetails, Method method) {
		assert declaringClassDetails != null;
		var classDetails = declaringClassDetails;
		while ( classDetails != null && classDetails != ClassDetails.OBJECT_CLASS_DETAILS ) {
			for ( int i = 0; i < classDetails.getMethods().size(); i++ ) {
				final var methodDetails = classDetails.getMethods().get(i);
				if ( methodDetails.getName().equals( method.getName() )
						&& methodDetails.getMethodKind() == MethodDetails.MethodKind.GETTER ) {
					return methodDetails;
				}
			}
			classDetails = classDetails.getSuperClass();
		}
		return null;
	}


	/**
	 * For Hibernate Reactive
	 */
	protected PluralAttributeMappingImpl(PluralAttributeMappingImpl original) {
		super( original );
		this.collectionMappingType = original.collectionMappingType;
		this.elementDescriptor = original.elementDescriptor;
		this.indexDescriptor = original.indexDescriptor;
		this.identifierDescriptor = original.identifierDescriptor;
		this.fetchTiming = original.fetchTiming;
		this.fetchStyle = original.fetchStyle;
		this.collectionDescriptor = original.collectionDescriptor;
		this.referencedPropertyName = original.referencedPropertyName;
		this.mapKeyPropertyName = original.mapKeyPropertyName;
		this.bidirectionalAttributeName = original.bidirectionalAttributeName;
		this.sqlAliasStem = original.sqlAliasStem;
		this.separateCollectionTable = original.separateCollectionTable;
		this.indexMetadata = original.indexMetadata;
		this.fkDescriptor = original.fkDescriptor;
		this.orderByFragment = original.orderByFragment;
		this.manyToManyOrderByFragment = original.manyToManyOrderByFragment;
		this.auxiliaryMapping = original.auxiliaryMapping;
		injectAttributeMapping( elementDescriptor, indexDescriptor, collectionDescriptor, this );
	}

	private static void injectAttributeMapping(
			CollectionPart elementDescriptor,
			@Nullable CollectionPart indexDescriptor,
			CollectionPersister collectionDescriptor,
			PluralAttributeMapping mapping) {
		if ( collectionDescriptor instanceof Aware aware ) {
			aware.injectAttributeMapping( mapping );
		}

		if ( elementDescriptor instanceof Aware aware ) {
			aware.injectAttributeMapping( mapping );
		}

		if ( indexDescriptor instanceof Aware aware ) {
			aware.injectAttributeMapping( mapping );
		}
	}

	@Override
	public boolean isBidirectionalAttributeName(@Nonnull NavigablePath fetchablePath, @Nonnull ToOneAttributeMapping modelPart) {
		return bidirectionalAttributeName == null
				// If the FK-target of the to-one mapping is the same as the FK-target of this one-to-many mapping,
				// and the FK-key refer to the same column then we say this is bidirectional,
				// given that this is only invoked for model parts of the collection elements
				? modelPart.getSideNature() == ForeignKeyDescriptor.Nature.KEY
						&& collectionDescriptor.isOneToMany()
						&& fkDescriptor.getTargetPart() == modelPart.getForeignKeyDescriptor().getTargetPart()
						&& areEqual( fkDescriptor.getKeyPart(), modelPart.getForeignKeyDescriptor().getKeyPart() )
				: fetchablePath.getLocalName().equals( bidirectionalAttributeName );
	}

	private boolean areEqual(ValuedModelPart part1, ValuedModelPart part2) {
		final int typeCount = part1.getJdbcTypeCount();
		if ( part2.getJdbcTypeCount() != typeCount ) {
			return false;
		}
		for ( int i = 0; i < typeCount; i++ ) {
			final var selectable1 = part1.getSelectable( i );
			final var selectable2 = part2.getSelectable( i );
			if ( selectable1.getJdbcMapping() != selectable2.getJdbcMapping()
				|| !selectable1.getContainingTableExpression().equals( selectable2.getContainingTableExpression() )
				|| !selectable1.getSelectionExpression().equals( selectable2.getSelectionExpression() ) ) {
				return false;
			}
		}
		return true;
	}

	public void finishInitialization(
			Property bootProperty,
			Collection bootDescriptor,
			@SuppressWarnings("unused")
			MappingModelCreationProcess creationProcess) {
		if ( auxiliaryMapping == null && bootDescriptor.isInverse() && collectionDescriptor.isManyToMany() ) {
			auxiliaryMapping = inverseAuxiliaryMapping( bootProperty, bootDescriptor.getMappedByProperty() );
		}
		final boolean hasOrder = bootDescriptor.getOrderBy() != null;
		final boolean hasManyToManyOrder = bootDescriptor.getManyToManyOrdering() != null;

		if ( hasOrder || hasManyToManyOrder ) {
			final TranslationContext context = collectionDescriptor::getFactory;

			if ( hasOrder ) {
				orderByFragment = OrderByFragmentTranslator.translate(
						bootDescriptor.getOrderBy(),
						this,
						context
				);
			}

			if ( hasManyToManyOrder ) {
				manyToManyOrderByFragment = OrderByFragmentTranslator.translate(
						bootDescriptor.getManyToManyOrdering(),
						this,
						context
				);
			}
		}
	}

	@Nonnull
	@Override
	public NavigableRole getNavigableRole() {
		return getCollectionDescriptor().getNavigableRole();
	}

	@Nonnull
	@Override
	public CollectionMappingType<?> getMappedType() {
		return collectionMappingType;
	}

	@Nonnull
	@Override
	public ForeignKeyDescriptor getKeyDescriptor() {
		return fkDescriptor;
	}

	@Nonnull
	@Override
	public CollectionPersister getCollectionDescriptor() {
		return collectionDescriptor;
	}

	@Nonnull
	@Override
	public CollectionPart getElementDescriptor() {
		return elementDescriptor;
	}

	@Nullable
	@Override
	public CollectionPart getIndexDescriptor() {
		return indexDescriptor;
	}

	@Nullable
	@Override
	public IndexMetadata getIndexMetadata() {
		return indexMetadata;
	}

	@Nullable
	@Override
	public CollectionIdentifierDescriptor getIdentifierDescriptor() {
		return identifierDescriptor;
	}

	@Nullable
	@Override
	public SoftDeleteMapping getSoftDeleteMapping() {
		return auxiliaryMapping instanceof SoftDeleteMapping softDeleteMapping
				? softDeleteMapping : null;
	}

	@Nonnull
	@Override
	public TableDetails getSoftDeleteTableDetails() {
		return ( (CollectionMutationTarget) getCollectionDescriptor() ).getCollectionTableMapping();
	}

	@Nullable
	@Override
	public TemporalMapping getTemporalMapping() {
		return auxiliaryMapping instanceof TemporalMapping temporalMapping
				? temporalMapping : null;
	}

	@Nullable
	@Override
	public AuditMapping getAuditMapping() {
		return auxiliaryMapping instanceof AuditMapping auditMapping
				? auditMapping : null;
	}

	@Nullable
	@Override
	public AuxiliaryMapping getAuxiliaryMapping() {
		return auxiliaryMapping;
	}


	@Nullable
	@Override
	public OrderByFragment getOrderByFragment() {
		return orderByFragment;
	}

	@Nullable
	@Override
	public OrderByFragment getManyToManyOrderByFragment() {
		return manyToManyOrderByFragment;
	}

	@Nullable
	@Override
	public String getSeparateCollectionTable() {
		return separateCollectionTable;
	}

	@Override
	public boolean containsTableReference(String tableExpression) {
		return tableExpression.equals( separateCollectionTable );
	}

	@Nullable
	@Override
	public Generator getGenerator() {
		// can never be a generated value
		return null;
	}

	@Override
	public String getFetchableName() {
		return castNonNull( getAttributeName() );
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public FetchStyle getStyle() {
		return fetchStyle;
	}

	@Override
	public FetchTiming getTiming() {
		return fetchTiming;
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return false;
	}

	/**
	 * The entity which is the given element or index of this collection, if it is
	 * an entity.
	 */
	private static @Nullable EntityMappingType associatedEntity(@Nullable CollectionPart part) {
		return part instanceof EntityCollectionPart entityPart
				? entityPart.getAssociatedEntityMappingType()
				: null;
	}

	/**
	 * The auxiliary mapping of the entity which is the given element or index of
	 * this collection, if any.
	 */
	private static @Nullable AuxiliaryMapping auxiliaryMapping(@Nullable CollectionPart part) {
		final var entity = associatedEntity( part );
		return entity == null ? null : entity.getAuxiliaryMapping();
	}

	/**
	 * The auxiliary mapping of the inverse side of a many-to-many association, which
	 * reads the rows belonging to the owning side, with the given name.
	 */
	private @Nullable AuxiliaryMapping inverseAuxiliaryMapping(Property bootProperty, @Nullable String mappedBy) {
		final var elementEntity = associatedEntity( elementDescriptor );
		if ( elementEntity == null || mappedBy == null || mappedBy.isEmpty() ) {
			return null;
		}
		final var owningAttribute =
				ManyToManyCollectionPart.resolveNamedTargetPart( mappedBy, elementEntity, collectionDescriptor )
						.asAttributeMapping();
		final var owningCollection = owningAttribute == null ? null : owningAttribute.asPluralAttributeMapping();
		final var owningMapping = owningCollection == null ? null : owningCollection.getAuxiliaryMapping();
		return owningMapping == null ? null : owningMapping.forInverseCollection( bootProperty );
	}

	/**
	 * Whether the collection, its element, or its index reads its history, in place
	 * of its current state, when it is read as of a revision of the owner.
	 */
	private boolean readsHistoryAtOwnerRevision(LoadQueryInfluencers influencers) {
		return readsHistoryAtOwnerRevision( auxiliaryMapping, influencers )
			|| readsHistoryAtOwnerRevision( auxiliaryMapping( elementDescriptor ), influencers )
			|| readsHistoryAtOwnerRevision( auxiliaryMapping( indexDescriptor ), influencers );
	}

	private static boolean readsHistoryAtOwnerRevision(
			@Nullable AuxiliaryMapping mapping, LoadQueryInfluencers influencers) {
		return mapping != null && mapping.readsHistoryAtOwnerRevision( influencers );
	}

	@Nonnull
	@Override
	public TableGroup createSubqueryRootTableGroup(
			@Nonnull NavigablePath navigablePath,
			@Nonnull PredicateConsumer predicateConsumer,
			@Nonnull SqlAstCreationState creationState) {
		final var parentPath = navigablePath.getParent();
		final var owner = parentPath == null ? null : creationState.getFromClauseAccess().findTableGroup( parentPath );
		// In an all-revisions query, the collection of an audit revision is the collection as of that revision
		final var ownerRevision = ownerRevision( navigablePath, creationState );
		final var targetRead = targetRead( owner, ownerRevision, creationState );
		final var tableGroup = targetRead.read( creationState.getLoadQueryInfluencers(), () -> createRootTableGroup(
				true,
				navigablePath,
				null,
				null,
				() -> predicateConsumer::applyPredicate,
				creationState
		) );
		final Consumer<Predicate> restrictions = predicateConsumer::applyPredicate;
		applyCollectionRestrictions( tableGroup, restrictions, restrictions, restrictions,
				ownerRevision, targetRead, false, creationState );
		return tableGroup;
	}

	/**
	 * How the tables of this collection, held by the given owner, are read: as current
	 * state for a current owner, and from the history tables of a temporal collection,
	 * element, or index of the collection of a revision.
	 */
	private TargetRead targetRead(
			@Nullable TableGroup owner,
			@Nullable Expression ownerRevision,
			SqlAstCreationState creationState) {
		return TargetRead.of(
				hasHistory( this ),
				owner,
				ownerRevision != null && readsHistoryAtOwnerRevision( creationState.getLoadQueryInfluencers() ),
				creationState
		);
	}

	@Override
	public void applyCollectionRestrictions(
			@Nonnull TableGroup tableGroup,
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull SqlAstCreationState creationState) {
		// A collection read as the root of a query has no owner there to be read as of
		applyCollectionRestrictions( tableGroup, predicateConsumer, predicateConsumer, predicateConsumer,
				null, TargetRead.QUERY, false, creationState );
	}

	private void applyCollectionRestrictions(
			TableGroup group, Consumer<Predicate> restrictions, Consumer<Predicate> auxiliaryRestrictions,
			Consumer<Predicate> ownerRestrictions, @Nullable Expression ownerRevision, TargetRead targetRead,
			boolean preserveCollection, SqlAstCreationState state) {
		final var influencers = state.getLoadQueryInfluencers();
		final var filters = influencers.getEnabledFilters();
		applyBaseRestrictions( restrictions, group, true, filters, false, null, state );
		applyBaseManyToManyRestrictions( restrictions, group, true, filters, null, state );
		final var pluralGroup = (PluralTableGroup) group;
		final var aliases = state.getSqlAliasBaseGenerator();
		targetRead.read( influencers, () -> {
			final var nullKey = nullMapKey( pluralGroup, influencers );
			final var keyEntity = associatedEntity( indexDescriptor );
			if ( keyEntity != null ) {
				// Map keys follow load-by-key filter semantics even in an HQL fetch join.
				final var keyGroup = pluralGroup.getIndexTableGroup();
				final var keyRestrictions = allowNullKey( nullKey, restrictions );
				keyEntity.applyBaseRestrictions( keyRestrictions, keyGroup, true, filters, true, null, state );
				if ( nullKey != null ) {
					// A native temporal table reference may itself hide the target of a non-null key.
					final var id = keyEntity.getIdentifierMapping().getSelectable( 0 );
					final var keyTable = keyGroup.resolveTableReference( id.getContainingTableExpression() );
					keyRestrictions.accept( new NullnessPredicate( new ColumnReference( keyTable, id ), true ) );
				}
			}
			applyAuxiliaryRestrictions( pluralGroup, auxiliaryRestrictions, influencers,
					aliases, ownerRevision, ownerRestrictions, nullKey );
			if ( preserveCollection && ownerRevision != null ) {
				final var revision = latestVersionStart( pluralGroup, influencers );
				if ( revision != null ) {
					// The joined versions coexist iff all are valid at the latest of their
					// starting revisions. This also holds when no owner row matches.
					applyAuxiliaryRestrictions( pluralGroup, predicate -> {}, influencers,
							aliases, revision, auxiliaryRestrictions, nullKey );
				}
			}
		} );
	}

	/** Returns a common revision to test if two or more versioned tables are joined. */
	private @Nullable Expression latestVersionStart(PluralTableGroup group, LoadQueryInfluencers influencers) {
		final var starts = new ArrayList<ColumnReference>( 3 );
		addVersionStart( starts, auxiliaryMapping, group, collectionDescriptor.getTableName(), influencers );
		addPartVersionStart( starts, elementDescriptor, group.getElementTableGroup(), influencers );
		addPartVersionStart( starts, indexDescriptor, group.getIndexTableGroup(), influencers );
		if ( starts.size() < 2 ) {
			return null;
		}
		Expression latest = starts.get( 0 );
		for ( int i = 1; i < starts.size(); i++ ) {
			final var start = starts.get( i );
			final var later = new CaseSearchedExpression( (BasicType<?>) start.getExpressionType() );
			later.when( new ComparisonPredicate( start, GREATER_THAN, latest ), start );
			later.otherwise( latest );
			latest = later;
		}
		return latest;
	}

	private static void addPartVersionStart(List<ColumnReference> starts, @Nullable CollectionPart part,
			@Nullable TableGroup group, LoadQueryInfluencers influencers) {
		final var entity = associatedEntity( part );
		if ( entity != null ) {
			addVersionStart( starts, entity.getAuxiliaryMapping(), castNonNull( group ),
					entity.getEntityPersister().getTableName(), influencers );
		}
	}

	private static void addVersionStart(List<ColumnReference> starts, @Nullable AuxiliaryMapping mapping,
			TableGroup group, String table, LoadQueryInfluencers influencers) {
		if ( mapping != null ) {
			final var start = mapping.getVersionStart( table, influencers );
			if ( start != null ) {
				starts.add( new ColumnReference( group.resolveTableReference( mapping.resolveTableName( table ) ), start ) );
			}
		}
	}

	private @Nullable Predicate nullMapKey(PluralTableGroup group, LoadQueryInfluencers influencers) {
		final var key = group instanceof OneToManyTableGroup ? nullableRestrictedMapKey( influencers ) : null;
		if ( key == null ) {
			return null;
		}
		final var table = group.getElementTableGroup().resolveTableReference( key.getKeyTable() );
		final var missing = new Junction( Junction.Nature.DISJUNCTION );
		key.visitKeySelectables( (i, column) ->
				missing.add( new NullnessPredicate( new ColumnReference( table, column ) ) ) );
		return missing;
	}

	private static Consumer<Predicate> allowNullKey(@Nullable Predicate nullKey, Consumer<Predicate> consumer) {
		if ( nullKey == null ) {
			return consumer;
		}
		return predicate -> {
			if ( predicate != null && !predicate.isEmpty() ) {
				consumer.accept( new Junction( Junction.Nature.DISJUNCTION, List.of( nullKey, predicate ), null ) );
			}
		};
	}

	@Override
	public void applyAuxiliaryRestrictions(
			@Nonnull TableGroup tableGroup,
			@Nonnull PredicateConsumer predicateConsumer,
			@Nonnull LoadQueryInfluencers influencers,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator) {
		final var group = (PluralTableGroup) tableGroup;
		applyAuxiliaryRestrictions( group, predicateConsumer::applyPredicate, influencers,
				sqlAliasBaseGenerator, null, predicateConsumer::applyPredicate, nullMapKey( group, influencers ) );
	}

	private void applyAuxiliaryRestrictions(
			PluralTableGroup group, Consumer<Predicate> restrictions, LoadQueryInfluencers influencers,
			SqlAliasBaseGenerator aliases, @Nullable Expression ownerRevision, Consumer<Predicate> ownerRestrictions,
			@Nullable Predicate nullKey) {
		applyPartRestriction( elementDescriptor, group.getElementTableGroup(), ownerRevision,
				restrictions, ownerRestrictions, aliases, influencers );
		applyPartRestriction( indexDescriptor, group.getIndexTableGroup(), ownerRevision,
				allowNullKey( nullKey, restrictions ), allowNullKey( nullKey, ownerRestrictions ), aliases, influencers );
		if ( auxiliaryMapping != null ) {
			auxiliaryMapping.applyAssociationRestrictions( this, group, ownerRevision,
					restrictions, ownerRestrictions, aliases, influencers );
		}
	}

	private static void applyPartRestriction(
			@Nullable CollectionPart part, @Nullable TableGroup group, @Nullable Expression ownerRevision,
			Consumer<Predicate> restrictions, Consumer<Predicate> ownerRestrictions,
			SqlAliasBaseGenerator aliases, LoadQueryInfluencers influencers) {
		final var entity = associatedEntity( part );
		final var auxiliary = entity == null ? null : entity.getAuxiliaryMapping();
		if ( auxiliary != null ) {
			auxiliary.applyAssociationRestrictions( entity, castNonNull( group ), ownerRevision,
					restrictions, ownerRestrictions, aliases, influencers );
		}
	}

	@Nonnull
	@Override
	public <T> DomainResult<T> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
		final var collectionTableGroup =
				creationState.getSqlAstCreationState().getFromClauseAccess()
						.getTableGroup( navigablePath );

		assert collectionTableGroup != null;

		// This is only used for collection initialization where we know the owner is available, so we mark it as visited
		// which will cause bidirectional to-one associations to be treated as such and avoid a join
		creationState.registerVisitedAssociationKey( fkDescriptor.getAssociationKey() );

		//noinspection unchecked
		return new CollectionDomainResult( navigablePath, this, resultVariable, tableGroup, creationState );
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		final var sqlAstCreationState = creationState.getSqlAstCreationState();

		final boolean added = creationState.registerVisitedAssociationKey( fkDescriptor.getAssociationKey() );

		try {
			if ( fetchTiming == FetchTiming.IMMEDIATE ) {
				if ( selected ) {
					final var collectionTableGroup = resolveCollectionTableGroup(
							fetchParent,
							fetchablePath,
							creationState,
							sqlAstCreationState
					);
					return buildEagerCollectionFetch(
							fetchablePath,
							this,
							collectionTableGroup,
							referencedPropertyName != null,
							fetchParent,
							creationState
					);
				}
				else {
					return createSelectEagerCollectionFetch(
							fetchParent,
							fetchablePath,
							creationState,
							sqlAstCreationState
					);
				}
			}

			if ( getCollectionDescriptor().getCollectionType().hasHolder() ) {
				return createSelectEagerCollectionFetch(
						fetchParent,
						fetchablePath,
						creationState,
						sqlAstCreationState
				);
			}

			return createDelayedCollectionFetch( fetchParent, fetchablePath, creationState, sqlAstCreationState );
		}
		finally {
			// This is only necessary because the association key is too general i.e. also matching FKs that other associations would match
			// and on top of this, we are not handling circular fetches for plural attributes yet
			if ( added ) {
				creationState.removeVisitedAssociationKey( fkDescriptor.getAssociationKey() );
			}
		}
	}

	/**
	 * For Hibernate Reactive
	 */
	protected Fetch buildDelayedCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			FetchParent fetchParent,
			DomainResult<?> collectionKeyResult,
			boolean unfetched) {
		return buildDelayedCollectionFetch(
				fetchedPath,
				fetchedAttribute,
				fetchParent,
				collectionKeyResult,
				unfetched,
				org.hibernate.engine.spi.FetchOptions.NONE
		);
	}

	protected Fetch buildDelayedCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			FetchParent fetchParent,
			@Nullable DomainResult<?> collectionKeyResult,
			boolean unfetched,
			org.hibernate.engine.spi.FetchOptions fetchOptions) {
		return new DelayedCollectionFetch(
				fetchedPath,
				fetchedAttribute,
				fetchParent,
				collectionKeyResult,
				unfetched,
				fetchOptions
		);
	}

	/**
	 * For Hibernate Reactive
	 */
	protected Fetch buildSelectEagerCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			DomainResult<?> collectionKeyDomainResult,
			FetchParent fetchParent) {
		return buildSelectEagerCollectionFetch(
				fetchedPath,
				fetchedAttribute,
				collectionKeyDomainResult,
				fetchParent,
				org.hibernate.engine.spi.FetchOptions.NONE
		);
	}

	protected Fetch buildSelectEagerCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			@Nullable DomainResult<?> collectionKeyDomainResult,
			FetchParent fetchParent,
			org.hibernate.engine.spi.FetchOptions fetchOptions) {
		return new SelectEagerCollectionFetch(
				fetchedPath,
				fetchedAttribute,
				collectionKeyDomainResult,
				fetchParent,
				fetchOptions
		);
	}

	/**
	 * For Hibernate Reactive
	 */
	protected Fetch buildEagerCollectionFetch(
			NavigablePath fetchedPath,
			PluralAttributeMapping fetchedAttribute,
			TableGroup collectionTableGroup,
			boolean needsCollectionKeyResult,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		return new EagerCollectionFetch(
				fetchedPath,
				fetchedAttribute,
				collectionTableGroup,
				needsCollectionKeyResult,
				fetchParent,
				creationState
		);
	}

	@Nullable
	@Override
	public Fetch resolveCircularFetch(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			DomainResultCreationState creationState) {
		if ( fetchTiming == FetchTiming.IMMEDIATE
				// if it's already visited
				&& creationState.isAssociationKeyVisited( fkDescriptor.getAssociationKey() ) ) {
			return createSelectEagerCollectionFetch(
					fetchParent,
					fetchablePath,
					creationState,
					creationState.getSqlAstCreationState()
			);
		}
		else {
			return null;
		}
	}

	private Fetch createSelectEagerCollectionFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			DomainResultCreationState creationState,
			SqlAstCreationState sqlAstCreationState) {
		return buildSelectEagerCollectionFetch( fetchablePath, this,
				collectionKeyDomainResult( fetchParent, fetchablePath, creationState, sqlAstCreationState ),
				fetchParent,
				creationState.getFetchOptions( fetchablePath ) );
	}

	private @Nullable DomainResult<?> collectionKeyDomainResult(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			DomainResultCreationState creationState,
			SqlAstCreationState sqlAstCreationState) {
		if ( referencedPropertyName == null ) {
			return null;
		}
		else {
			return getKeyDescriptor()
					.createTargetDomainResult(
							fetchablePath,
							sqlAstCreationState.getFromClauseAccess()
									.getTableGroup( fetchParent.getNavigablePath() ),
							fetchParent,
							creationState
					);
		}
	}

	private TableGroup resolveCollectionTableGroup(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			DomainResultCreationState creationState,
			SqlAstCreationState sqlAstCreationState) {
		final var fromClauseAccess = sqlAstCreationState.getFromClauseAccess();
		return fromClauseAccess.resolveTableGroup(
				fetchablePath,
				p -> {
					final var lhsTableGroup = fromClauseAccess.getTableGroup( fetchParent.getNavigablePath() );
					final var tableGroupJoin = createTableGroupJoin(
							fetchablePath,
							lhsTableGroup,
							null,
							null,
							SqlAstJoinType.LEFT,
							true,
							false,
							creationState.getSqlAstCreationState()
					);
					lhsTableGroup.addTableGroupJoin( tableGroupJoin );
					return tableGroupJoin.getJoinedGroup();
				}
		);
	}

	private Fetch createDelayedCollectionFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			DomainResultCreationState creationState,
			SqlAstCreationState sqlAstCreationState) {
		final DomainResult<?> collectionKeyDomainResult;
		// Lazy property. A null foreign key domain result will lead to
		// returning a domain result assembler that returns LazyPropertyInitializer.UNFETCHED_PROPERTY
		final var containingEntityMapping = findContainingEntityMapping();
		final boolean unfetched;
		if ( fetchParent.getReferencedMappingContainer() == containingEntityMapping
				&& containingEntityMapping.getEntityPersister().getPropertyLaziness()[getStateArrayPosition()] ) {
			collectionKeyDomainResult = null;
			unfetched = true;
		}
		else {
			if ( referencedPropertyName != null ) {
				collectionKeyDomainResult = getKeyDescriptor().createTargetDomainResult(
						fetchablePath,
						sqlAstCreationState.getFromClauseAccess().getTableGroup( fetchParent.getNavigablePath() ),
						fetchParent,
						creationState
				);
			}
			else {
				collectionKeyDomainResult = null;
			}
			unfetched = false;
		}
		return buildDelayedCollectionFetch(
				fetchablePath,
				this,
				fetchParent,
				collectionKeyDomainResult,
				unfetched,
				creationState.getFetchOptions( fetchablePath )
		);
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		return SqlAstJoinType.LEFT;
	}

	@Override
	public boolean isSimpleJoinPredicate(Predicate predicate) {
		return fkDescriptor.isSimpleJoinPredicate( predicate );
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAstCreationState creationState) {
		final var influencers = creationState.getLoadQueryInfluencers();
		final var keyPredicateCollector = new PredicateCollector();
		// In an all-revisions query, the collection of an audit revision is the collection as of that revision
		final var ownerRevision = ownerRevision( navigablePath, creationState );
		final var targetRead = targetRead( lhs, ownerRevision, creationState );
		final var tableGroup = targetRead.read( influencers, () -> createRootTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				explicitSqlAliasBase,
				requestedJoinType,
				fetched,
				addsPredicate,
				keyPredicateCollector::applyPredicate,
				creationState
		) );
		if ( preservesElements( requestedJoinType, navigablePath, creationState ) ) {
			return targetRead.read( influencers, () -> createElementPreservingJoin(
					navigablePath, (PluralTableGroup) tableGroup, keyPredicateCollector, creationState ) );
		}
		final var restrictionCollector = new PredicateCollector();
		final var auxiliaryRestrictionCollector = new PredicateCollector();
		final boolean preserveCollection = requestedJoinType == SqlAstJoinType.RIGHT || requestedJoinType == SqlAstJoinType.FULL;
		applyCollectionRestrictions( tableGroup, restrictionCollector, auxiliaryRestrictionCollector,
				preserveCollection ? keyPredicateCollector : auxiliaryRestrictionCollector,
				ownerRevision, targetRead, preserveCollection, creationState );

		if ( fetched ) {
			if ( orderByFragment != null ) {
				creationState.applyOrdering( tableGroup, orderByFragment );
			}

			if ( manyToManyOrderByFragment != null ) {
				creationState.applyOrdering( tableGroup, manyToManyOrderByFragment );
			}
		}

		final var restrictions = restrictionCollector.getPredicate();
		final var auxiliaryRestrictions = auxiliaryRestrictionCollector.getPredicate();
		final Predicate joinPredicate;
		if ( restrictions == null && auxiliaryRestrictions == null ) {
			joinPredicate = keyPredicateCollector.getPredicate();
		}
		else if ( preserveCollection ) {
			// Restrict the preserved operand before joining it to the owner.
			final var join = joinInsideCollection( tableGroup, creationState );
			join.applyPredicate( restrictions );
			join.applyPredicate( auxiliaryRestrictions );
			joinPredicate = keyPredicateCollector.getPredicate();
		}
		else {
			final var joinForRestrictions = determineJoinForRestrictions( tableGroup );
			if ( joinForRestrictions != null ) {
				joinForRestrictions.applyPredicate( restrictions );
				joinForRestrictions.applyPredicate( auxiliaryRestrictions );
				joinPredicate = keyPredicateCollector.getPredicate();
			}
			else if ( restrictions == null && hasElementOrIndexTableGroupJoin( tableGroup ) ) {
				final var junction = new Junction( Junction.Nature.CONJUNCTION );
				junction.add( keyPredicateCollector.getPredicate() );
				junction.add( new AuxiliaryRestrictions( auxiliaryRestrictions ) );
				joinPredicate = junction;
			}
			else {
				keyPredicateCollector.applyPredicate( restrictions );
				keyPredicateCollector.applyPredicate( auxiliaryRestrictions );
				joinPredicate = keyPredicateCollector.getPredicate();
			}
		}
		return new TableGroupJoin(
				navigablePath,
				determineSqlJoinType( lhs, requestedJoinType, fetched ),
				tableGroup,
				joinPredicate
		);
	}

	/**
	 * Whether a join of the given type preserves the entities which are the elements
	 * of this collection.
	 * <p>
	 * A right join of a many-to-many association returns every entity of the associated
	 * type, even one which belongs to no collection. So the collection table is joined
	 * to the owner by an inner join, and the table of the entity is then joined by the
	 * right join.
	 */
	private boolean preservesElements(
			@Nullable SqlAstJoinType joinType, NavigablePath navigablePath, SqlAstCreationState creationState) {
		return joinType == SqlAstJoinType.RIGHT
			&& !collectionDescriptor.isOneToMany()
			&& associatedEntity( elementDescriptor ) != null
			// the join of an entity or embeddable which is the key of a map follows the join of the element
			&& ( indexDescriptor == null || indexDescriptor.asBasicValuedModelPart() != null )
			// the collection of a revision is read together with the revision of its owner
			&& ownerRevision( navigablePath, creationState ) == null;
	}

	/**
	 * Create an inner join of the collection table, where the given table group has
	 * a right join of the table of the entity which is the element of the collection.
	 * <p>
	 * A row of the collection table is returned only with its owner, so an owner
	 * which an earlier join excludes brings back no row with no owner. A restriction
	 * on the collection table belongs to the right join, like the predicate of the
	 * join, where a row which it hides joins no owner to the entity. A restriction
	 * on the entity hides the entity itself, so it is applied before the entity is
	 * joined, since in the predicate of the join it would only leave the entity with
	 * no owner.
	 */
	private TableGroupJoin createElementPreservingJoin(
			NavigablePath navigablePath,
			PluralTableGroup tableGroup,
			PredicateCollector keyPredicateCollector,
			SqlAstCreationState creationState) {
		final var influencers = creationState.getLoadQueryInfluencers();
		final var filters = influencers.getEnabledFilters();
		final var aliases = creationState.getSqlAliasBaseGenerator();
		final var elementTableGroup = castNonNull( tableGroup.getElementTableGroup() );
		// the join of the table of the entity is rendered even if nothing refers to the entity
		elementTableGroup.getPrimaryTableReference();

		final Consumer<Predicate> rowRestrictions =
				castNonNull( tableGroup.findTableGroupJoin( elementTableGroup ) )::applyPredicate;
		applyBaseRestrictions( rowRestrictions, tableGroup, true, filters, false, null, creationState );
		if ( auxiliaryMapping != null ) {
			auxiliaryMapping.applyAssociationRestrictions( this, tableGroup, null,
					rowRestrictions, rowRestrictions, aliases, influencers );
		}

		final var elementRestrictionCollector = new PredicateCollector();
		final Consumer<Predicate> elementRestrictions = elementRestrictionCollector::applyPredicate;
		applyBaseManyToManyRestrictions( elementRestrictions, tableGroup, true, filters, null, creationState );
		applyPartRestriction( elementDescriptor, elementTableGroup, null,
				elementRestrictions, elementRestrictions, aliases, influencers );
		final var restrictions = elementRestrictionCollector.getPredicate();
		if ( restrictions != null ) {
			joinInsideCollection( elementTableGroup, creationState ).applyPredicate( restrictions );
		}

		return new TableGroupJoin( navigablePath, SqlAstJoinType.INNER, tableGroup, keyPredicateCollector.getPredicate() );
	}

	/**
	 * Determine the join of the element or index table to which the restrictions
	 * of the collection are applied, or {@code null} if they belong to the join of
	 * the collection table itself.
	 * <p>
	 * When the joins of the element and index tables are nested, every table is
	 * in scope of the predicate of the join of the collection table. Otherwise,
	 * a restriction which refers to the table of the element or index initialized
	 * the lazy table group of that table, and so it belongs to that join, or to a
	 * later one. A restriction which refers to the collection table alone stays
	 * on the join of the collection table, since a join of a table group which is
	 * never initialized is never rendered, and would take the restriction with it.
	 */
	private static @Nullable TableGroupJoin determineJoinForRestrictions(TableGroup collectionTableGroup) {
		if ( collectionTableGroup.getNestedTableGroupJoins().isEmpty() ) {
			final var tableGroupJoins = collectionTableGroup.getTableGroupJoins();
			for ( int i = tableGroupJoins.size() - 1; i >= 0; i-- ) {
				final var tableGroupJoin = tableGroupJoins.get( i );
				final var joinedGroup = tableGroupJoin.getJoinedGroup();
				if ( joinedGroup.isInitialized() && !joinedGroup.isVirtual() ) {
					return tableGroupJoin;
				}
			}
		}
		return null;
	}

	/**
	 * Whether the collection table group has a join of the table of an element
	 * or index, to which the restriction of an auxiliary mapping used to belong
	 * when the joins are not nested.
	 */
	private static boolean hasElementOrIndexTableGroupJoin(TableGroup collectionTableGroup) {
		final var tableGroupJoins =
				collectionTableGroup.getNestedTableGroupJoins().isEmpty()
						? collectionTableGroup.getTableGroupJoins()
						: collectionTableGroup.getNestedTableGroupJoins();
		return !tableGroupJoins.isEmpty()
			&& !tableGroupJoins.get( tableGroupJoins.size() - 1 ).getJoinedGroup().isVirtual();
	}

	/**
	 * Which joins of the collection a fetch may use is not to change. A join with
	 * only the restriction of an auxiliary mapping, for example soft delete, added
	 * to its key predicate was such a join when that restriction belonged to the
	 * join of the table of an element or index, and so it still is. Where there is
	 * no such table, for a collection of basic or embeddable values or a one-to-many
	 * association, the restriction always belonged to this join, and the join was
	 * never used for a fetch, just as a join with a where restriction or a filter
	 * never was, nor a join to which anything else was added.
	 */
	@Override
	public boolean isDefaultJoinPredicate(Predicate predicate) {
		return isSimpleJoinPredicate( predicate )
			|| predicate instanceof Junction junction
				&& junction.getNature() == Junction.Nature.CONJUNCTION
				&& junction.getPredicates().size() == 2
				&& isSimpleJoinPredicate( junction.getPredicates().get( 0 ) )
				&& junction.getPredicates().get( 1 ) instanceof AuxiliaryRestrictions;
	}

	/**
	 * The restrictions of the auxiliary mappings, in the predicate of a join of
	 * the collection table, kept apart from the key predicate and from anything
	 * added to the predicate later, so that they can be recognized.
	 */
	private static final class AuxiliaryRestrictions extends Junction {
		private AuxiliaryRestrictions(Predicate restrictions) {
			super( Nature.CONJUNCTION );
			add( restrictions );
		}
	}

	private TableGroupJoin joinInsideCollection(TableGroup group, SqlAstCreationState state) {
		final var nested = group.getNestedTableGroupJoins();
		for ( int i = nested.size() - 1; i >= 0; i-- ) {
			final var join = nested.get( i );
			if ( join.getJoinType() == SqlAstJoinType.INNER && join.getJoinedGroup().isInitialized()
					&& !join.getJoinedGroup().isVirtual() ) {
				return join;
			}
		}
		// A basic collection (or an uninitialized element join) has no inner
		// join on which to put the predicate. Joining a single row preserves multiplicity.
		final var factory = state.getCreationContext().getSessionFactory();
		final var path = group.getNavigablePath().append( "restriction" );
		final var alias = state.getSqlAliasBaseGenerator().createSqlAliasBase( "r" ).generateNewAlias();
		final var query = new QuerySpec( false );
		final var one = new QueryLiteral<>( 1, factory.getTypeConfiguration().getBasicTypeForJavaType( Integer.class ) );
		query.getSelectClause().addSqlSelection( new SqlSelectionImpl( one ) );
		final var singleRow = new QueryPartTableGroup( path, this, new SelectStatement( query ), alias,
				List.of( "n" ), false, true, factory );
		final var join = new TableGroupJoin( path, SqlAstJoinType.INNER, singleRow );
		group.addNestedTableGroupJoin( join );
		return join;
	}

	private boolean requiresOwnerPreservationOnFetch() {
		return requiresOwnerPreservation( auxiliaryMapping )
			|| requiresOwnerPreservation( auxiliaryMapping( elementDescriptor ) )
			|| requiresOwnerPreservation( auxiliaryMapping( indexDescriptor ) );
	}

	private static boolean requiresOwnerPreservation(@Nullable AuxiliaryMapping mapping) {
		return mapping != null && mapping.requiresOwnerPreservationOnCollectionFetch();
	}

	public SqlAstJoinType determineSqlJoinType(TableGroup lhs, @Nullable SqlAstJoinType requestedJoinType, boolean fetched) {
		if ( fetched && requestedJoinType != SqlAstJoinType.RIGHT && requestedJoinType != SqlAstJoinType.FULL
				&& requiresOwnerPreservationOnFetch() ) {
			return SqlAstJoinType.LEFT;
		}
		else if ( requestedJoinType == null ) {
			return fetched
					? getDefaultSqlAstJoinType( lhs )
					: SqlAstJoinType.INNER;
		}
		else {
			return requestedJoinType;
		}
	}

	@Override
	public TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType requestedJoinType,
			boolean fetched,
			@Nullable Consumer<Predicate> predicateConsumer,
			SqlAstCreationState creationState) {
		return targetRead( lhs, null, creationState ).read( creationState.getLoadQueryInfluencers(),
				() -> createRootTableGroupJoin( navigablePath, lhs, explicitSourceAlias, explicitSqlAliasBase,
						requestedJoinType, fetched, false, predicateConsumer, creationState ) );
	}

	private TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			@Nullable Consumer<Predicate> predicateConsumer,
			SqlAstCreationState creationState) {

		final var tableGroup =
				rootTableGroup(
						navigablePath,
						lhs,
						explicitSourceAlias,
						fetched,
						addsPredicate,
						creationState,
						determineSqlJoinType( lhs, requestedJoinType, fetched ),
						creationState.getSqlAliasBaseGenerator()
								.createSqlAliasBase( getSqlAliasStem() )
				);

		if ( predicateConsumer != null ) {
			predicateConsumer.accept( getKeyDescriptor()
					.generateJoinPredicate( lhs, tableGroup, creationState ) );
		}

		return tableGroup;
	}

	private boolean useCollectionTableGroup(SqlAstCreationState creationState) {
		return !getCollectionDescriptor().isOneToMany()
				|| auxiliaryMapping != null
					&& auxiliaryMapping.requiresSeparateMembershipTable( creationState.getLoadQueryInfluencers() );
	}

	private TableGroup rootTableGroup(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			boolean fetched,
			boolean addsPredicate,
			SqlAstCreationState creationState,
			SqlAstJoinType joinType,
			SqlAliasBase sqlAliasBase) {
		return useCollectionTableGroup( creationState )
				? createCollectionTableGroup(
						lhs.canUseInnerJoins()
						&& joinType == SqlAstJoinType.INNER,
						joinType,
						navigablePath,
						fetched,
						addsPredicate,
						explicitSourceAlias,
						sqlAliasBase,
						creationState
				)
				: createOneToManyTableGroup(
						lhs.canUseInnerJoins()
						&& joinType == SqlAstJoinType.INNER,
						joinType,
						navigablePath,
						fetched,
						addsPredicate,
						explicitSourceAlias,
						sqlAliasBase,
						creationState
				);
	}


	@Override
	public void setForeignKeyDescriptor(ForeignKeyDescriptor fkDescriptor) {
		this.fkDescriptor = fkDescriptor;
	}

	private TableGroup createOneToManyTableGroup(
			boolean canUseInnerJoins,
			SqlAstJoinType joinType,
			NavigablePath navigablePath,
			boolean fetched,
			boolean addsPredicate,
			@Nullable String sourceAlias,
			SqlAliasBase explicitSqlAliasBase,
			SqlAstCreationState creationState) {
		final var oneToManyCollectionPart = (OneToManyCollectionPart) elementDescriptor;
		final var sqlAliasBase = SqlAliasBase.from(
				explicitSqlAliasBase,
				sourceAlias,
				this,
				creationState.getSqlAliasBaseGenerator()
		);
		final var tableGroup = new OneToManyTableGroup(
				this,
				oneToManyCollectionPart.createAssociatedTableGroup(
						canUseInnerJoins,
						navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
						fetched,
						sourceAlias,
						sqlAliasBase,
						creationState
				),
				creationState.getCreationContext()
						// TODO: FIX ME
						.getSessionFactory()
		);
		if ( indexDescriptor instanceof TableGroupJoinProducer tableGroupJoinProducer ) {
			final var influencers = creationState.getLoadQueryInfluencers();
			final boolean nullableKey = nullableRestrictedMapKey( influencers ) != null;
			final boolean nested = nullableKey || isNestedJoin( joinType, addsPredicate, creationState );
			final var keyJoinType = nullableKey ? SqlAstJoinType.LEFT
					: nested && hasPartRestriction( indexDescriptor, influencers ) ? SqlAstJoinType.INNER : joinType;
			final var tableGroupJoin = tableGroupJoinProducer.createTableGroupJoin(
					navigablePath.append( CollectionPart.Nature.INDEX.getName() ),
					tableGroup,
					null,
					sqlAliasBase,
					keyJoinType,
					fetched,
					false,
					creationState
			);
			tableGroup.registerIndexTableGroup( tableGroupJoin, nested );
		}

		return tableGroup;
	}

	/** The child can belong to a one-to-many map even when its key association is null. */
	private @Nullable ForeignKeyDescriptor nullableRestrictedMapKey(LoadQueryInfluencers influencers) {
		if ( indexDescriptor instanceof ManyToManyCollectionPart index && hasPartRestriction( index, influencers ) ) {
			final var key = index.getForeignKeyDescriptor();
			for ( int i = 0; i < key.getJdbcTypeCount(); i++ ) {
				if ( key.getSelectable( i ).isNullable() ) {
					return key;
				}
			}
		}
		return null;
	}

	private static boolean hasPartRestriction(@Nullable CollectionPart part, LoadQueryInfluencers influencers) {
		final var entity = associatedEntity( part );
		if ( entity == null ) {
			return false;
		}
		final var auxiliary = entity.getAuxiliaryMapping();
		return auxiliary != null && ( auxiliary.hasRowRestriction( influencers ) || auxiliary.requiresTargetTableForKeyRead( influencers ) )
			|| entity.hasWhereRestrictions()
			|| part.getNature() == CollectionPart.Nature.INDEX && entity.isAffectedByEnabledFilters( influencers, true );
	}

	@Override
	public boolean hasRowRestrictions(@Nonnull LoadQueryInfluencers influencers) {
		return auxiliaryMapping != null && auxiliaryMapping.hasRowRestriction( influencers )
			|| hasPartRestriction( elementDescriptor, influencers )
			|| hasPartRestriction( indexDescriptor, influencers )
			|| PluralAttributeMapping.super.hasRowRestrictions( influencers );
	}

	private boolean isNestedJoin(SqlAstJoinType joinType, boolean addsPredicate, SqlAstCreationState creationState) {
		// For inner joins we never need join nesting
		return joinType != SqlAstJoinType.INNER
			// For outer joins we need nesting if there might be an on-condition that refers to the element table.
			// A restriction which hides an element hides the whole row of the collection table, along with the
			// key of the element or index which is read from it, and the index of a hidden element of a map.
			&& ( addsPredicate
					|| hasRowRestrictions( creationState.getLoadQueryInfluencers() ) );
	}

	private TableGroup createCollectionTableGroup(
			boolean canUseInnerJoins,
			SqlAstJoinType joinType,
			NavigablePath navigablePath,
			boolean fetched,
			boolean addsPredicate,
			@Nullable String sourceAlias,
			SqlAliasBase explicitSqlAliasBase,
			SqlAstCreationState creationState) {
		final var sqlAliasBase = SqlAliasBase.from(
				explicitSqlAliasBase,
				sourceAlias,
				this,
				creationState.getSqlAliasBaseGenerator()
		);
		final String tableName = collectionDescriptor.getTableName();
		final String alias = sqlAliasBase.generateNewAlias();
		final var collectionTableReference =
				collectionTableReference( creationState, tableName, alias );
		collectionTableReference.applyAuxiliaryTable( auxiliaryMapping,
				creationState.getLoadQueryInfluencers() );

		final var tableGroup = new CollectionTableGroup(
				canUseInnerJoins,
				navigablePath,
				this,
				fetched,
				sourceAlias,
				collectionTableReference,
				true,
				sqlAliasBase,
				s -> false,
				null,
				creationState.getCreationContext()
						// TODO: FIX ME
						.getSessionFactory()
		);

		// a join which preserves the elements has the element table joined to the collection table by the outer join
		final boolean nestedJoin = !preservesElements( joinType, navigablePath, creationState )
				&& isNestedJoin( joinType, addsPredicate, creationState );

		if ( elementDescriptor instanceof TableGroupJoinProducer tableGroupJoinProducer ) {
			final var tableGroupJoin = tableGroupJoinProducer.createTableGroupJoin(
					navigablePath.append( CollectionPart.Nature.ELEMENT.getName() ),
					tableGroup,
					null,
					sqlAliasBase,
					nestedJoin ? SqlAstJoinType.INNER : joinType,
					fetched,
					false,
					creationState
			);
			tableGroup.registerElementTableGroup( tableGroupJoin, nestedJoin );
		}

		if ( indexDescriptor instanceof TableGroupJoinProducer tableGroupJoinProducer ) {
			// an entity which is the key of a map mapped by @MapKey is an association
			// of the element, so it is joined from the element, whose table has the
			// foreign key
			final var elementTableGroup = tableGroup.getElementTableGroup();
			final boolean joinedFromElement = mapKeyPropertyName != null
					&& indexDescriptor instanceof EntityCollectionPart
					&& elementTableGroup != null;
			final var tableGroupJoin = tableGroupJoinProducer.createTableGroupJoin(
					navigablePath.append( CollectionPart.Nature.INDEX.getName() ),
					joinedFromElement ? elementTableGroup : tableGroup,
					null,
					sqlAliasBase,
					nestedJoin ? SqlAstJoinType.INNER : joinType,
					fetched,
					false,
					creationState
			);
			tableGroup.registerIndexTableGroup( tableGroupJoin, nestedJoin );
		}

		return tableGroup;
	}

	private NamedTableReference collectionTableReference(SqlAstCreationState creationState, String tableName, String alias) {
		return auxiliaryMapping != null && auxiliaryMapping.useAuxiliaryTable( creationState.getLoadQueryInfluencers() )
				? new AuxiliaryTableReference( auxiliaryMapping.resolveTableName( tableName ), tableName, alias, true )
				: new NamedTableReference( tableName, alias, true );
	}

	@Override
	public TableGroup createRootTableGroup(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			String explicitSourceAlias,
			SqlAliasBase explicitSqlAliasBase,
			Supplier<Consumer<Predicate>> additionalPredicateCollectorAccess,
			SqlAstCreationState creationState) {
		if ( !useCollectionTableGroup( creationState ) ) {
			return createOneToManyTableGroup(
					canUseInnerJoins,
					SqlAstJoinType.INNER,
					navigablePath,
					false,
					false,
					explicitSourceAlias,
					explicitSqlAliasBase,
					creationState
			);
		}
		else {
			return createCollectionTableGroup(
					canUseInnerJoins,
					SqlAstJoinType.INNER,
					navigablePath,
					false,
					false,
					explicitSourceAlias,
					explicitSqlAliasBase,
					creationState
			);
		}
	}

	@Override
	public int getBatchSize() {
		return getCollectionDescriptor().getBatchSize();
	}

	@Override
	public boolean isAffectedByEnabledFilters(@Nonnull LoadQueryInfluencers influencers, boolean onlyApplyForLoadByKeyFilters) {
		return getCollectionDescriptor().isAffectedByEnabledFilters( influencers, onlyApplyForLoadByKeyFilters );
	}

	@Override
	public boolean isAffectedByRowRestrictions(@Nonnull LoadQueryInfluencers influencers, boolean onlyLoadByKeyFilters) {
		return isAffectedByEnabledFilters( influencers, onlyLoadByKeyFilters )
			|| isAuxiliaryMappingAffected( auxiliaryMapping, influencers )
			|| isPartAffectedByRowRestrictions( elementDescriptor, influencers )
			|| isPartAffectedByRowRestrictions( indexDescriptor, influencers );
	}

	private static boolean isPartAffectedByRowRestrictions(@Nullable CollectionPart part, LoadQueryInfluencers influencers) {
		return isAuxiliaryMappingAffected( auxiliaryMapping( part ), influencers );
	}

	private static boolean isAuxiliaryMappingAffected(@Nullable AuxiliaryMapping mapping, LoadQueryInfluencers influencers) {
		return mapping != null && mapping.isAffectedByRowRestrictions( influencers );
	}

	@Override
	public boolean isAffectedByInfluencers(@Nonnull LoadQueryInfluencers influencers, boolean onlyLoadByKeyFilters) {
		return PluralAttributeMapping.super.isAffectedByInfluencers( influencers, onlyLoadByKeyFilters )
			|| auxiliaryMapping != null && auxiliaryMapping.isAffectedByInfluencers( influencers )
			|| isPartAffectedByInfluencers( elementDescriptor, influencers, onlyLoadByKeyFilters )
			|| isPartAffectedByInfluencers( indexDescriptor, influencers, onlyLoadByKeyFilters );
	}

	private static boolean isPartAffectedByInfluencers(
			@Nullable CollectionPart part, LoadQueryInfluencers influencers, boolean onlyLoadByKeyFilters) {
		final var entity = associatedEntity( part );
		return entity != null && entity.isAffectedByInfluencers( influencers, onlyLoadByKeyFilters );
	}

	@Override
	public boolean isAffectedByEntityGraph(@Nonnull LoadQueryInfluencers influencers) {
		return getCollectionDescriptor().isAffectedByEntityGraph( influencers );
	}

	@Override
	public void registerAffectingFetchProfile(String fetchProfileName) {
		if ( collectionDescriptor instanceof FetchProfileAffectee affectee ) {
			affectee.registerAffectingFetchProfile( fetchProfileName);
		}
	}

	@Override
	public boolean isAffectedByEnabledFetchProfiles(@Nonnull LoadQueryInfluencers influencers) {
		return getCollectionDescriptor().isAffectedByEnabledFetchProfiles( influencers );
	}

	@Nonnull
	@Override
	public String getRootPathName() {
		return getCollectionDescriptor().getRole();
	}

	@Nullable
	@Override
	public ModelPart findSubPart(@Nonnull String name, @Nullable EntityMappingType treatTargetType) {
		if ( elementDescriptor instanceof ModelPartContainer modelPartContainer ) {
			final var subPart = modelPartContainer.findSubPart( name, null );
			if ( subPart != null ) {
				return subPart;
			}
		}
		final var nature = CollectionPart.Nature.fromName( name );
		if ( nature != null ) {
			return switch ( nature ) {
				case ELEMENT -> elementDescriptor;
				case INDEX -> indexDescriptor;
				case ID -> identifierDescriptor;
			};
		}

		return null;
	}

	@Override
	public void forEachSubPart(@Nonnull IndexedConsumer<ModelPart> consumer, @Nullable EntityMappingType treatTarget) {
		consumer.accept( 0, elementDescriptor );

		int position = 1;
		if ( indexDescriptor != null ) {
			consumer.accept( position++, indexDescriptor );
		}

		if ( identifierDescriptor != null ) {
			consumer.accept( position+1, identifierDescriptor );
		}
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath, @Nonnull TableGroup tableGroup, @Nonnull DomainResultCreationState creationState) {
		elementDescriptor.applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		elementDescriptor.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			@Nullable Object domainValue,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValueBiConsumer<X, Y> valueConsumer,
			@Nullable SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitSubParts(@Nonnull Consumer<ModelPart> consumer, @Nullable EntityMappingType treatTargetType) {
		consumer.accept( elementDescriptor );
		if ( indexDescriptor != null ) {
			consumer.accept( indexDescriptor );
		}
	}

	@Nonnull
	@Override
	public String getContainingTableExpression() {
		return getKeyDescriptor().getKeyTable();
	}

	@Override
	public int getJdbcTypeCount() {
		return 0;
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping(int index) {
		throw new IndexOutOfBoundsException( index );
	}

	@Nonnull
	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		throw new IndexOutOfBoundsException( columnIndex );
	}

	@Override
	public int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		return 0;
	}

	@Nullable
	@Override
	public Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		return elementDescriptor.disassemble( value, session );
	}

	@Override
	public void addToCacheKey(@Nonnull MutableCacheKeyBuilder cacheKey, @Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		elementDescriptor.addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return elementDescriptor.forEachDisassembledJdbcValue( value, offset, x, y, valuesConsumer, session );
	}

	@Override
	public String toString() {
		return "PluralAttribute(" + getCollectionDescriptor().getRole() + ")";
	}
}
