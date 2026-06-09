/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import jakarta.persistence.Cacheable;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SharedCacheMode;
import org.hibernate.MappingException;
import org.hibernate.AnnotationException;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.models.bind.internal.SecondaryTable;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.boot.models.bind.spi.BindingOptions;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.CacheRegion;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.JpaEventListener;
import org.hibernate.boot.models.categorize.spi.JpaEventListenerStyle;
import org.hibernate.boot.models.categorize.spi.NaturalIdCacheRegion;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jpa.boot.spi.CallbackDefinition;
import org.hibernate.jpa.boot.spi.EntityCallbackDefinition;
import org.hibernate.jpa.boot.spi.ListenerCallbackDefinition;
import org.hibernate.jpa.event.spi.CallbackType;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.IdentifiableTypeClass;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.TableOwner;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.models.ModelsException;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MethodDetails;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hibernate.boot.models.bind.ModelBindingLogging.MODEL_BINDING_LOGGER;
import static org.hibernate.internal.util.StringHelper.coalesce;

/// Binder for binding an entity type to a {@link PersistentClass}.
///
/// Construction intentionally creates only the local {@link PersistentClass} shell.
/// The coordinator then drives the public binding phases across all types in a
/// hierarchy.  This split keeps construction from accidentally depending on tables,
/// identifiers, or super-type keys that are produced by later phases.
/// The current phases for entity binders are:
///
/// 1. Construction - create the local {@link PersistentClass} shell.
/// 2. {@link #bindTypeSkeleton()} - assign stable entity names, register this
/// binder with {@link BindingState}, publish the mapping shell to the metadata
/// collector, and add the entity import.
/// 3. {@link #bindTables()} - create and attach primary and secondary table shells.
/// 4. {@link #bindSuperType()} - wire the mapping type to its resolved super
/// entity or mapped-superclass.
/// 5. {@link #bindEntityMetadata()} - bind entity-level metadata such as caching,
/// filters, and callbacks.
/// 6. {@link #bindIdentifier()} - bind the root identifier shape for the hierarchy.
/// 7. {@link #bindAssociationIdentifiers()} - resolve identifier attributes that
/// are themselves associations.
/// 8. {@link #bindMembers()} - bind the remaining currently coarse member phase:
/// discriminator, version, tenant id, and persistent attributes.
/// 9. {@link #bindCollectionIndexes()} - resolve collection indexes that depend
/// on fully-bound member state, such as property-derived map keys.
/// 10. {@link #bindAssociationTargets()} - resolve association target properties
/// for non-primary-key references.
/// 11. {@link #bindDerivedIdentifiers()} - resolve derived identifier
/// associations such as {@code @MapsId}.
/// 12. {@link #bindTableKeys()} - bind joined-subclass, secondary-table, and
/// association-table keys that depend on the root identifier shape and on joins
/// encountered while binding members.
/// 13. {@link #bindInverseAssociations()} - resolve inverse association values
/// from owning-side association mappings whose keys are now available.
/// 14. {@link #bindForeignKeys()} - create physical foreign-key constraints from
/// the association values and table keys prepared by earlier phases.
///
/// The implemented {@link TypeBindingPhase} contracts identify which phases this
/// binder participates in.  The coordinator owns the ordering while this class
/// documents what each phase makes available to later phases.
///
/// @since 9.0
/// @author Steve Ebersole
public class EntityTypeBinder extends IdentifiableTypeBinder
		implements TypeBindingPhase.TypeSkeleton,
				TypeBindingPhase.Tables,
				TypeBindingPhase.SuperType,
				TypeBindingPhase.EntityMetadata,
				TypeBindingPhase.Identifiers,
				TypeBindingPhase.AssociationIdentifiers,
				TypeBindingPhase.CollectionIndexes,
				TypeBindingPhase.AssociationTargets,
				TypeBindingPhase.DerivedIdentifiers,
				TypeBindingPhase.TableKeys,
				TypeBindingPhase.InverseAssociations,
				TypeBindingPhase.ForeignKeys,
				TypeBindingPhase.Members {
	private final PersistentClass binding;

	private final ModelBinders modelBinders;
	private IdentifierBinding identifierBinding;

	public EntityTypeBinder(
			EntityTypeMetadata type,
			IdentifiableTypeMetadata superType,
			EntityHierarchy.HierarchyRelation hierarchyRelation,
			ModelBinders modelBinders,
			BindingState state,
			BindingOptions options,
			BindingContext context) {
		super( type, superType, hierarchyRelation, state, options, context );
		this.modelBinders = modelBinders;

		this.binding = createBinding();
	}

	/// Finish the minimal type skeleton and publish it for other binders to resolve.
	///
	/// After this phase the entity has stable entity/import names and is registered
	/// with both {@link BindingState} and the metadata collector.  It should still
	/// not have tables, identifiers, attributes, or foreign keys bound here.
	public void bindTypeSkeleton() {
		final ClassDetails classDetails = getManagedType().getClassDetails();
		final Entity entityAnn = classDetails.getDirectAnnotationUsage( Entity.class );
		final String jpaEntityName = entityAnn.name();
		final String entityName;
		final String importName;

		if ( classDetails.getName() != null
				&& !classDetails.getName().equals( classDetails.getClassName() ) ) {
			// should indicate a dynamic model
			entityName = classDetails.getName();
		}
		else {
			entityName = classDetails.getClassName();
		}

		if ( StringHelper.isNotEmpty( jpaEntityName ) ) {
			importName = jpaEntityName;
		}
		else {
			importName = StringHelper.unqualifyEntityName( entityName );
		}

		binding.setClassName( classDetails.getClassName() );
		binding.setEntityName( entityName );
		binding.setJpaEntityName( importName );

		getBindingState().registerTypeBinder( getManagedType(), this );
		getBindingState().addImport( importName, entityName );
	}

	/// Bind table shells owned directly by this entity.
	///
	/// This phase attaches the primary table for roots, joined subclasses, and
	/// table-per-class subclasses, and creates secondary table joins.  It deliberately
	/// does not bind identifier columns or table foreign keys.
	public void bindTables() {
		if ( binding instanceof TableOwner ) {
			final var primaryTable = modelBinders.getTableBinder().bindPrimaryTable( getManagedType(), getHierarchyRelation() );
			final var table = primaryTable.binding();
			( (TableOwner) binding ).setTable( table );
		}

		final var secondaryTables = modelBinders.getTableBinder().bindSecondaryTables( this );
		secondaryTables.forEach( this::processSecondaryTable );
	}

	/// Wire the Hibernate mapping type to its resolved entity or mapped-superclass super type.
	///
	/// This phase runs after all type skeletons are registered, so superclass binders
	/// can be resolved without forcing table or member binding into construction.
	public void bindSuperType() {
		final IdentifiableTypeBinder superTypeBinder = getSuperTypeBinder();
		final EntityTypeBinder superEntityBinder = getSuperEntityBinder();
		if ( binding instanceof RootClass rootClass ) {
			assert superEntityBinder == null;

			if ( superTypeBinder != null ) {
				rootClass.setSuperMappedSuperclass( (MappedSuperclass) superTypeBinder.getTypeBinding() );
			}
		}
		else {
			final Subclass subclass = (Subclass) binding;

			if ( (superTypeBinder == superEntityBinder && superTypeBinder != null) ) {
				// the super is an entity
				subclass.setSuperclass( superEntityBinder.getTypeBinding() );
			}
			else if ( superTypeBinder != null ) {
				subclass.setSuperMappedSuperclass( (MappedSuperclass) superTypeBinder.getTypeBinding() );
			}
		}
	}

	/// Bind entity-level metadata that does not require member value binding.
	///
	/// Examples include cacheability, filters, and JPA callback definitions.
	public void bindEntityMetadata() {
		final ClassDetails classDetails = getManagedType().getClassDetails();
		processCaching( classDetails, getBindingState(), getBindingContext() );
		processFilters( classDetails, getBindingState(), getBindingContext() );
		processJpaEventListeners( getManagedType(), getBindingState(), getBindingContext() );
	}

	/// Bind the root identifier and retain its local binding state.
	///
	/// Only root entity binders participate in this phase.  Subclasses consume the
	/// completed root identifier shape in later key/FK phases.
	public void bindIdentifier() {
		if ( getHierarchyRelation() == EntityHierarchy.HierarchyRelation.ROOT ) {
			identifierBinding = IdentifierBinder.bindIdentifier(
					getManagedType(),
					(RootClass) getTypeBinding(),
					modelBinders,
					getBindingState(),
					getOptions(),
					getBindingContext()
			);
			getBindingState().addIdentifierBinding( getManagedType(), identifierBinding );
		}
	}

	public IdentifierBinding getIdentifierBinding() {
		return identifierBinding;
	}

	/// Resolve association-valued identifier attributes after all identifiers exist.
	public void bindAssociationIdentifiers() {
		new AssociationIdentifierBinder( this ).bindAssociationIdentifiers();
	}

	/// Resolve collection indexes that need all member properties to exist.
	///
	/// Property-derived map keys, for example {@code @MapKey(name = "code")}, are
	/// registered while binding the collection member but resolved here so the
	/// target entity's named property can be found regardless of type iteration
	/// order.
	public void bindCollectionIndexes() {
		getBindingState().forEachPropertyMapKeyBinding( (propertyMapKeyBinding) -> {
			if ( propertyMapKeyBinding.collection().getOwner() == getTypeBinding() ) {
				CollectionIndexBinder.bindPropertyMapKey( propertyMapKeyBinding, getBindingState() );
			}
		} );
	}

	/// Resolve non-primary-key association targets after all target members exist.
	public void bindAssociationTargets() {
		new AssociationTargetBinder( this ).bindAssociationTargets();
	}

	/// Resolve derived identifier associations after identifier and member values exist.
	public void bindDerivedIdentifiers() {
		new DerivedIdentifierBinder( this ).bindDerivedIdentifiers();
	}

	/// Bind table keys that depend on a completed root identifier shape.
	///
	/// Joined-subclass tables, secondary tables, and association tables use dependent
	/// key values that wrap the root identifier.  Keeping this in its own phase lets
	/// table and member binding create table shells before the dependent keys are
	/// resolved.
	public void bindTableKeys() {
		new TableKeyBinder( this ).bindTableKeys();
	}

	/// Resolve inverse associations owned by this entity.
	///
	/// This phase runs after all table keys have been created, so inverse
	/// associations can copy the owning-side key and element columns without
	/// depending on binder iteration order.
	public void bindInverseAssociations() {
		new InverseToOneAssociationBinder( this ).bindInverseAssociations();
		new InversePluralAssociationBinder( this ).bindInverseAssociations();
	}

	/// Create physical foreign-key constraints after values and keys are complete.
	public void bindForeignKeys() {
		new ForeignKeyBinder( this ).bindForeignKeys();
	}

	/// Bind discriminator, version, tenant id, and persistent attributes.
	///
	/// This is still a coarse phase today.  Later refactoring steps are expected to
	/// split foreign keys and attributes into more focused phases.
	public void bindMembers() {
		prepareBinding( modelBinders );
	}

	private void processJpaEventListeners(EntityTypeMetadata type, BindingState state, BindingContext context) {
		final List<JpaEventListener> listeners = type.getCompleteJpaEventListeners();
		if ( CollectionHelper.isEmpty( listeners ) ) {
			return;
		}

		listeners.forEach( (listener) -> {
			if ( listener.getStyle() == JpaEventListenerStyle.CALLBACK ) {
				processEntityCallbacks( listener );
			}
			else {
				assert listener.getStyle() == JpaEventListenerStyle.LISTENER;
				processListenerCallbacks( listener );
			}
		} );
	}

	private void processEntityCallbacks(JpaEventListener listener) {
		final Class<?> entityClass = listener.getCallbackClass().toJavaClass();
		processJpaEventCallbacks( entityClass, listener, JpaEventListenerStyle.CALLBACK, null );
	}

	private void processJpaEventCallbacks(
			Class<?> listenerClass,
			JpaEventListener listener,
			JpaEventListenerStyle style,
			Class<?> methodArgumentType) {
		assert style == JpaEventListenerStyle.CALLBACK || methodArgumentType != null;

		// todo : would be nicer to allow injecting them one at a time.
		//  		upstream is defined currently to accept a List
		final List<CallbackDefinition> callbackDefinitions = new ArrayList<>();

		final MethodDetails prePersistMethod = listener.getPrePersistMethod();
		if ( prePersistMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, prePersistMethod, methodArgumentType );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.PRE_PERSIST
			) );
		}

		final MethodDetails postPersistMethod = listener.getPostPersistMethod();
		if ( postPersistMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, postPersistMethod, methodArgumentType );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.POST_PERSIST
			) );
		}

		final MethodDetails preUpdateMethod = listener.getPreUpdateMethod();
		if ( preUpdateMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, preUpdateMethod, methodArgumentType );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.PRE_UPDATE
			) );
		}

		final MethodDetails postUpdateMethod = listener.getPostUpdateMethod();
		if ( postUpdateMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, postUpdateMethod, methodArgumentType );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.POST_UPDATE
			) );
		}

		final MethodDetails preRemoveMethod = listener.getPreRemoveMethod();
		if ( preRemoveMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, preRemoveMethod, methodArgumentType );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.PRE_REMOVE
			) );
		}

		final MethodDetails postRemoveMethod = listener.getPostRemoveMethod();
		if ( postRemoveMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, postRemoveMethod, methodArgumentType );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.POST_REMOVE
			) );
		}

		final MethodDetails postLoadMethod = listener.getPostLoadMethod();
		if ( postLoadMethod != null ) {
			final Method callbackMethod = findCallbackMethod( listenerClass, postLoadMethod, methodArgumentType );
			callbackDefinitions.add( createCallbackDefinition(
					listenerClass,
					callbackMethod,
					style,
					CallbackType.POST_LOAD
			) );
		}

		binding.addCallbackDefinitions( callbackDefinitions );
	}

	private static CallbackDefinition createCallbackDefinition(
			Class<?> listenerClass,
			Method callbackMethod,
			JpaEventListenerStyle style,
			CallbackType callbackType) {
		final CallbackDefinition callback;
		if ( style == JpaEventListenerStyle.CALLBACK ) {
			callback = new EntityCallbackDefinition( callbackMethod, callbackType );
		}
		else {
			callback = new ListenerCallbackDefinition( listenerClass, callbackMethod, callbackType );
		}
		return callback;
	}

	private void processListenerCallbacks(JpaEventListener listener) {
		final Class<?> listenerClass = listener.getCallbackClass().toJavaClass();
		processJpaEventCallbacks( listenerClass, listener, JpaEventListenerStyle.LISTENER, getManagedType().getClassDetails().toJavaClass() );
	}

	private Method findCallbackMethod(
			Class<?> callbackTarget,
			MethodDetails callbackMethod,
			Class<?> entityType) {
		try {
			if ( callbackMethod.getArgumentTypes().isEmpty() ) {
				return callbackTarget.getDeclaredMethod( callbackMethod.getName() );
			}
			else {
				final ClassDetails argClassDetails = callbackMethod.getArgumentTypes().get( 0 );
				// we don't
				return callbackTarget.getMethod( callbackMethod.getName(), argClassDetails.toJavaClass() );
			}
		}
		catch (NoSuchMethodException e) {
			final ModelsException modelsException = new ModelsException(
					String.format(
							Locale.ROOT,
							"Unable to locate callback method - %s.%s",
							callbackTarget.getName(),
							callbackMethod.getName()
					)
			);
			modelsException.addSuppressed( e );
			throw modelsException;
		}
	}

	@Override
	public PersistentClass getTypeBinding() {
		return binding;
	}

	@Override
	public Table getTable() {
		return binding.getTable();
	}

	@Override
	public EntityTypeMetadata getManagedType() {
		return (EntityTypeMetadata) super.getManagedType();
	}

	@Override
	public EntityTypeMetadata findSuperEntity() {
		return getManagedType();
	}

	private PersistentClass createBinding() {
		if ( getHierarchyRelation() == EntityHierarchy.HierarchyRelation.SUB ) {
			return createSubclass();
		}
		else {
			return new RootClass( getBindingState().getMetadataBuildingContext() );
		}
	}

	private PersistentClass createSubclass() {
		final IdentifiableTypeMetadata superType = getSuperType();

		final IdentifiableTypeBinder superTypeBinder = (IdentifiableTypeBinder) getBindingState().getTypeBinder( superType.getClassDetails() );
		final IdentifiableTypeClass superTypeBinding = superTypeBinder.getTypeBinding();

		// we have a few cases to handle here, complicated by how Hibernate has historically modeled
		// mapped-superclass in its PersistentClass model (aka, not well) which manifests in some
		// craziness over how these PersistentClass instantiations happen

		final InheritanceType inheritanceType = superType.getHierarchy().getInheritanceType();
		if ( inheritanceType == InheritanceType.JOINED ) {
			return createJoinedSubclass( superTypeBinding );
		}

		if ( inheritanceType == InheritanceType.TABLE_PER_CLASS ) {
			return createUnionSubclass( superTypeBinding );
		}

		assert inheritanceType == null || inheritanceType == InheritanceType.SINGLE_TABLE;
		return createSingleTableSubclass( superTypeBinding );
	}

	private UnionSubclass createUnionSubclass(IdentifiableTypeClass superTypeBinding) {
		if ( superTypeBinding instanceof PersistentClass superEntity ) {
			return new UnionSubclass( superEntity, getBindingState().getMetadataBuildingContext() );
		}
		else {
			assert superTypeBinding instanceof MappedSuperclass;

			final var superEntity = resolveSuperEntity( superTypeBinding );
			final var binding = new UnionSubclass(
					superEntity,
					getBindingState().getMetadataBuildingContext()
			);
			binding.setSuperMappedSuperclass( (MappedSuperclass) superTypeBinding );
			return binding;
		}
	}

	private JoinedSubclass createJoinedSubclass(IdentifiableTypeClass superTypeBinding) {
		final JoinedSubclass joinedSubclass;

		final var superEntityTypeBinding = getSuperEntityBinder().getTypeBinding();
		if ( superTypeBinding instanceof PersistentClass superEntity ) {
			joinedSubclass = new JoinedSubclass(
					superEntity,
					getBindingState().getMetadataBuildingContext()
			);
		}
		else {
			assert superTypeBinding instanceof MappedSuperclass;

			joinedSubclass = new JoinedSubclass(
					superEntityTypeBinding,
					getBindingState().getMetadataBuildingContext()
			);
			joinedSubclass.setSuperMappedSuperclass( (MappedSuperclass) superTypeBinding );
		}

		return joinedSubclass;
	}

	private JoinColumn resolveMatchingJoinColumnAnn(
			List<JoinColumn> inverseJoinColumnAnns,
			Column pkColumn,
			List<JoinColumn> joinColumnAnns) {
		int matchPosition = -1;
		for ( int j = 0; j < inverseJoinColumnAnns.size(); j++ ) {
			final var inverseJoinColumnAnn = inverseJoinColumnAnns.get( j );
			final String name = inverseJoinColumnAnn.name();
			if ( pkColumn.getName().equals( name ) ) {
				matchPosition = j;
				break;
			}
		}

		if ( matchPosition == -1 ) {
			throw new MappingException( "Unable to match primary key column [" + pkColumn.getName() + "] to any inverseJoinColumn - " + getManagedType().getEntityName() );
		}

		return joinColumnAnns.get( matchPosition );
	}

	private SingleTableSubclass createSingleTableSubclass(IdentifiableTypeClass superTypeBinding) {
		if ( superTypeBinding instanceof PersistentClass superEntity ) {
			return new SingleTableSubclass( superEntity, getBindingState().getMetadataBuildingContext() );
		}
		else {
			assert superTypeBinding instanceof MappedSuperclass;

			final PersistentClass superEntity = resolveSuperEntity( superTypeBinding );
			final SingleTableSubclass binding = new SingleTableSubclass(
					superEntity,
					getBindingState().getMetadataBuildingContext()
			);
			binding.setSuperMappedSuperclass( (MappedSuperclass) superTypeBinding );
			return binding;
		}
	}

	private PersistentClass resolveSuperEntity(IdentifiableTypeClass superTypeBinding) {
		if ( superTypeBinding instanceof PersistentClass ) {
			return (PersistentClass) superTypeBinding;
		}

		if ( superTypeBinding.getSuperType() != null ) {
			return resolveSuperEntity( superTypeBinding.getSuperType() );
		}

		throw new ModelsException( "Unable to resolve super PersistentClass for " + superTypeBinding );
	}

	@Override
	protected void prepareBinding(ModelBinders modelBinders) {
		if ( getHierarchyRelation() == EntityHierarchy.HierarchyRelation.ROOT ) {
			prepareRootEntityBinding( (RootClass) getTypeBinding(), modelBinders );
			if ( getManagedType().hasSubTypes() ) {
				bindDiscriminatorValue( getManagedType(), getTypeBinding(), modelBinders, getBindingState(), getOptions(), getBindingContext() );
			}
		}
		else {
			bindDiscriminatorValue( getManagedType(), getTypeBinding(), modelBinders, getBindingState(), getOptions(), getBindingContext() );
		}

		bindCacheable( getManagedType(), getTypeBinding(), modelBinders, getOptions(), getBindingState(), getBindingContext() );

		super.prepareBinding( modelBinders );
	}

	private static void bindCacheable(
			EntityTypeMetadata managedType,
			PersistentClass typeBinding,
			ModelBinders modelBinders,
			BindingOptions options,
			BindingState bindingState,
			BindingContext bindingContext) {
		final Cacheable cacheableAnn = managedType.getClassDetails().getDirectAnnotationUsage( Cacheable.class );
		final SharedCacheMode sharedCacheMode = bindingContext.getSharedCacheMode();
		typeBinding.setCached( isCacheable( sharedCacheMode, cacheableAnn ) );
	}

	private static boolean isCacheable(SharedCacheMode sharedCacheMode, Cacheable explicitCacheableAnn) {
		return switch ( sharedCacheMode ) {
			// all entities should be cached
			case ALL -> true;
			// Hibernate defaults to ENABLE_SELECTIVE, the only sensible setting
			// only entities with @Cacheable(true) should be cached
			case ENABLE_SELECTIVE, UNSPECIFIED -> explicitCacheableAnn != null && explicitCacheableAnn.value();
			// only entities with @Cacheable(false) should not be cached
			case DISABLE_SELECTIVE -> explicitCacheableAnn == null || explicitCacheableAnn.value();
			// treat both NONE and UNSPECIFIED the same
			default -> false;
		};
	}

	protected BasicValue getDiscriminatorMapping() {
		if ( binding instanceof RootClass rootClass ) {
			return (BasicValue) rootClass.getDiscriminator();
		}
		return getSuperEntityBinder().getDiscriminatorMapping();
	}

	private void bindDiscriminatorValue(
			EntityTypeMetadata managedType,
			PersistentClass typeBinding,
			ModelBinders modelBinders,
			BindingState bindingState,
			BindingOptions options,
			BindingContext bindingContext) {
		final BasicValue discriminatorMapping = getDiscriminatorMapping();
		if ( discriminatorMapping == null ) {
			return;
		}

		final DiscriminatorValue ann = managedType.getClassDetails().getDirectAnnotationUsage( DiscriminatorValue.class );
		if ( ann == null ) {
			final Type resolvedJavaType = discriminatorMapping.resolve().getRelationalJavaType().getJavaType();
			if ( resolvedJavaType == String.class ) {
				typeBinding.setDiscriminatorValue( typeBinding.getEntityName() );
			}
			else {
				typeBinding.setDiscriminatorValue( Integer.toString( typeBinding.getSubclassId() ) );
			}
		}
		else {
			typeBinding.setDiscriminatorValue( ann.value() );
		}
	}

	private void prepareRootEntityBinding(RootClass typeBinding, ModelBinders modelBinders) {
		// todo : possibly Hierarchy details - version, tenant-id, ...

		bindDiscriminator( getManagedType(), typeBinding, modelBinders, getOptions(), getBindingState(), getBindingContext() );
		bindVersion( getManagedType(), typeBinding, modelBinders, getOptions(), getBindingState(), getBindingContext() );
		bindTenantId( getManagedType(), typeBinding, modelBinders, getOptions(), getBindingState(), getBindingContext() );

		processSoftDelete( typeBinding.getIdentityTable(), typeBinding, getManagedType().getClassDetails() );
		processOptimisticLocking( typeBinding, getManagedType() );
		processCacheRegions( getManagedType(), typeBinding, getManagedType().getClassDetails() );
	}

	private static void bindDiscriminator(
			EntityTypeMetadata managedType,
			RootClass typeBinding,
			ModelBinders modelBinders,
			BindingOptions bindingOptions,
			BindingState bindingState,
			BindingContext bindingContext) {
		final InheritanceType inheritanceType = managedType.getHierarchy().getInheritanceType();
		final DiscriminatorColumn columnAnn = managedType.getClassDetails().getDirectAnnotationUsage( DiscriminatorColumn.class );
		final DiscriminatorFormula formulaAnn = managedType.getClassDetails().getDirectAnnotationUsage( DiscriminatorFormula.class );

		if ( columnAnn != null && formulaAnn != null ) {
			throw new MappingException( "Entity defined both @DiscriminatorColumn and @DiscriminatorFormula - " + typeBinding.getEntityName() );
		}

		if ( inheritanceType == InheritanceType.TABLE_PER_CLASS ) {
			// UnionSubclass cannot define a discriminator
			return;
		}

		if ( inheritanceType == InheritanceType.JOINED ) {
			// JoinedSubclass can define a discriminator in certain circumstances
			if ( bindingOptions.ignoreExplicitDiscriminatorsForJoinedInheritance() ) {
				if ( columnAnn != null || formulaAnn != null ) {
					MODEL_BINDING_LOGGER.debugf( "Skipping explicit discriminator for JOINED hierarchy due to configuration - " + typeBinding.getEntityName() );
				}
				return;
			}

			if ( !bindingOptions.createImplicitDiscriminatorsForJoinedInheritance() ) {
				if ( columnAnn == null && formulaAnn == null ) {
					return;
				}
			}
		}

		if ( inheritanceType == InheritanceType.SINGLE_TABLE ) {
			if ( !managedType.hasSubTypes() ) {
				return;
			}
		}

		final BasicValue value = new BasicValue( bindingState.getMetadataBuildingContext(), typeBinding.getIdentityTable() );
		typeBinding.setDiscriminator( value );

		final DiscriminatorType discriminatorType = ColumnBinder.bindDiscriminatorColumn(
				bindingContext,
				formulaAnn,
				value,
				columnAnn,
				bindingOptions,
				bindingState
		);

		final Class<?> discriminatorJavaType;
		switch ( discriminatorType ) {
			case STRING -> discriminatorJavaType = String.class;
			case CHAR -> discriminatorJavaType = char.class;
			case INTEGER -> discriminatorJavaType = int.class;
			default -> throw new IllegalStateException( "Unexpected DiscriminatorType - " + discriminatorType );
		}

		value.setImplicitJavaTypeAccess( typeConfiguration -> discriminatorJavaType );
	}

	private static void bindVersion(
			EntityTypeMetadata managedType,
			RootClass typeBinding,
			ModelBinders modelBinders,
			BindingOptions options,
			BindingState bindingState,
			BindingContext bindingContext) {
		final AttributeMetadata versionAttribute = managedType.getHierarchy().getVersionAttribute();
		if ( versionAttribute != null ) {
			VersionBinder.bindVersion( versionAttribute, managedType, typeBinding, options, bindingState, bindingContext );
		}
	}

	private static void bindTenantId(
			EntityTypeMetadata managedType,
			RootClass typeBinding,
			ModelBinders modelBinders,
			BindingOptions options,
			BindingState bindingState,
			BindingContext bindingContext) {
		final AttributeMetadata tenantIdAttribute = managedType.getHierarchy().getTenantIdAttribute();
		if ( tenantIdAttribute != null ) {
			TenantIdBinder.bindTenantId( tenantIdAttribute, managedType, typeBinding, options, bindingState, bindingContext );
		}

	}

	private void processSoftDelete(
			Table primaryTable,
			RootClass rootClass,
			ClassDetails classDetails) {
		final SoftDelete softDeleteConfig = getTypeBinding() instanceof RootClass
				? classDetails.getDirectAnnotationUsage( SoftDelete.class )
				: null;
		if ( softDeleteConfig == null ) {
			return;
		}

		final BasicValue softDeleteIndicatorValue = createSoftDeleteIndicatorValue( softDeleteConfig, primaryTable );
		final Column softDeleteIndicatorColumn = createSoftDeleteIndicatorColumn( softDeleteConfig, softDeleteIndicatorValue );
		primaryTable.addColumn( softDeleteIndicatorColumn );
		rootClass.enableSoftDelete( softDeleteIndicatorColumn, softDeleteConfig.strategy() );
	}

	private BasicValue createSoftDeleteIndicatorValue(
			SoftDelete softDeleteAnn,
			Table table) {
		assert softDeleteAnn != null;

		final ConverterDescriptor<?, ?> converterDescriptor = new RegisteredConversion(
				null,
				softDeleteAnn.converter(),
				false
		).getConverterDescriptor();

		final BasicValue softDeleteIndicatorValue = new BasicValue( getBindingState().getMetadataBuildingContext(), table );
		softDeleteIndicatorValue.makeSoftDelete( softDeleteAnn.strategy() );
		softDeleteIndicatorValue.setJpaAttributeConverterDescriptor( converterDescriptor );
		softDeleteIndicatorValue.setImplicitJavaTypeAccess( (typeConfiguration) -> (Class<?>) converterDescriptor.getRelationalValueResolvedType() );
		return softDeleteIndicatorValue;
	}

	private Column createSoftDeleteIndicatorColumn(
			SoftDelete softDeleteConfig,
			BasicValue softDeleteIndicatorValue) {
		final Column softDeleteColumn = new Column();

		applyColumnName( softDeleteColumn, softDeleteConfig, getBindingState(), getBindingContext() );

		softDeleteColumn.setLength( 1 );
		softDeleteColumn.setNullable( false );
		softDeleteColumn.setUnique( false );
		softDeleteColumn.setComment( "Soft-delete indicator" );

		softDeleteColumn.setValue( softDeleteIndicatorValue );
		softDeleteIndicatorValue.addColumn( softDeleteColumn );

		return softDeleteColumn;
	}

	private static void applyColumnName(
			Column softDeleteColumn,
			SoftDelete softDeleteConfig,
			BindingState state,
			BindingContext context) {
		final Database database = state.getMetadataBuildingContext().getMetadataCollector().getDatabase();
		final PhysicalNamingStrategy namingStrategy = context.getPhysicalNamingStrategy();
		final SoftDeleteType strategy = softDeleteConfig.strategy();
		final String logicalColumnName = coalesce(
				strategy.getDefaultColumnName(),
				softDeleteConfig.columnName()
		);
		final Identifier physicalColumnName = namingStrategy.toPhysicalColumnName(
				database.toIdentifier( logicalColumnName ),
				database.getJdbcEnvironment()
		);
		softDeleteColumn.setName( physicalColumnName.render( database.getDialect() ) );
	}

	private void processOptimisticLocking(
			RootClass rootEntity,
			EntityTypeMetadata source) {
		rootEntity.setOptimisticLockStyle( source.getHierarchy().getOptimisticLockStyle() );
	}

	private void processCacheRegions(
			EntityTypeMetadata source,
			RootClass rootClass,
			ClassDetails classDetails) {
		final EntityHierarchy hierarchy = source.getHierarchy();
		final CacheRegion cacheRegion = hierarchy.getCacheRegion();
		final NaturalIdCacheRegion naturalIdCacheRegion = hierarchy.getNaturalIdCacheRegion();

		if ( cacheRegion != null ) {
			rootClass.setCacheRegionName( cacheRegion.getRegionName() );
			rootClass.setCacheConcurrencyStrategy( cacheRegion.getAccessType().getExternalName() );
			rootClass.setLazyPropertiesCacheable( cacheRegion.isCacheLazyProperties() );
		}

		if ( naturalIdCacheRegion != null ) {
			rootClass.setNaturalIdCacheRegionName( naturalIdCacheRegion.getRegionName() );
		}
	}

	private void processCaching(ClassDetails classDetails, BindingState state, BindingContext context) {
		final var cacheableAnn = classDetails.getDirectAnnotationUsage( Cacheable.class );
		if ( cacheableAnn == null ) {
			return;
		}

		final boolean cacheable = cacheableAnn.value();
		binding.setCached( cacheable );
	}

	private void processFilters(ClassDetails classDetails, BindingState state, BindingContext context) {
		final Filter[] filters = classDetails.getRepeatedAnnotationUsages(
				Filter.class,
				context.getBootstrapContext().getModelsContext()
		);
		if ( filters.length == 0 ) {
			return;
		}

		for ( Filter filter : filters ) {
			final String filterName = filter.name();
			binding.addFilter(
					filterName,
					resolveFilterCondition( filterName, filter.condition(), state ),
					filter.deduceAliasInjectionPoints(),
					extractFilterAliasTableMap( filter ),
					extractFilterAliasEntityMap( filter )
			);
		}
	}

	private String resolveFilterCondition(String filterName, String condition, BindingState state) {
		if ( StringHelper.isNotBlank( condition ) ) {
			return condition;
		}

		final var filterDefinition = state.getFilterDefinition( filterName );
		if ( filterDefinition == null ) {
			throw new AnnotationException( "Entity '" + binding.getEntityName()
					+ "' has a '@Filter' for an undefined filter named '" + filterName + "'" );
		}

		final String defaultCondition = filterDefinition.getDefaultFilterCondition();
		if ( StringHelper.isBlank( defaultCondition ) ) {
			throw new AnnotationException( "Entity '" + binding.getEntityName()
					+ "' has a '@Filter' with no 'condition' and no default condition was given by the '@FilterDef' named '"
					+ filterName + "'" );
		}
		return defaultCondition;
	}

	private Map<String, String> extractFilterAliasTableMap(Filter filter) {
		final SqlFragmentAlias[] aliases = filter.aliases();
		if ( aliases.length == 0 ) {
			return null;
		}
		final Map<String,String> result = CollectionHelper.mapOfSize( aliases.length );
		for ( SqlFragmentAlias alias : aliases ) {
			if ( StringHelper.isNotEmpty( alias.table() ) ) {
				result.put( alias.alias(), alias.table() );
			}
		}
		return result.isEmpty() ? null : result;
	}

	private Map<String, String> extractFilterAliasEntityMap(Filter filter) {
		final SqlFragmentAlias[] aliases = filter.aliases();
		if ( aliases.length == 0 ) {
			return null;
		}
		final Map<String,String> result = CollectionHelper.mapOfSize( aliases.length );
		for ( SqlFragmentAlias alias : aliases ) {
			if ( alias.entity() != void.class ) {
				result.put( alias.alias(), alias.entity().getName() );
			}
		}
		return result.isEmpty() ? null : result;
	}

	private void processSecondaryTable(SecondaryTable secondaryTable) {
		final Join join = new Join();
		join.setTable( secondaryTable.binding() );
		join.setPersistentClass( binding );
		join.setOptional( secondaryTable.optional() );
		join.setInverse( !secondaryTable.owned() );
	}
}
