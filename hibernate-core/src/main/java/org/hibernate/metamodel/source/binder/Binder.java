/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.binder;

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.metamodel.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.AttributeBindingContainer;
import org.hibernate.metamodel.binding.BasicAttributeBinding;
import org.hibernate.metamodel.binding.BasicCollectionElement;
import org.hibernate.metamodel.binding.CollectionElementNature;
import org.hibernate.metamodel.binding.CollectionLaziness;
import org.hibernate.metamodel.binding.ComponentAttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.EntityDiscriminator;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.binding.MetaAttribute;
import org.hibernate.metamodel.binding.SimpleValueBinding;
import org.hibernate.metamodel.binding.SingularAttributeBinding;
import org.hibernate.metamodel.binding.TypeDef;
import org.hibernate.metamodel.domain.Component;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.PluralAttribute;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.DerivedValue;
import org.hibernate.metamodel.relational.Identifier;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Table;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.relational.Tuple;
import org.hibernate.metamodel.relational.UniqueKey;
import org.hibernate.metamodel.source.LocalBindingContext;
import org.hibernate.metamodel.source.MappingException;
import org.hibernate.metamodel.source.MetaAttributeContext;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.metamodel.source.hbm.Helper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityTuplizer;

/**
 * The common binder shared between annotations and {@code hbm.xml} processing.
 * <p/>
 * The API consists of {@link #Binder(MetadataImplementor, List)} and {@link #processEntityHierarchy(EntityHierarchy)}
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class Binder {
	private final MetadataImplementor metadata;
	private final List<String> processedEntityNames;

	private InheritanceType currentInheritanceType;
	private EntityMode currentHierarchyEntityMode;
	private LocalBindingContext currentBindingContext;

	public Binder(MetadataImplementor metadata, List<String> processedEntityNames) {
		this.metadata = metadata;
		this.processedEntityNames = processedEntityNames;
	}

	/**
	 * Process an entity hierarchy.
	 *
	 * @param entityHierarchy THe hierarchy to process.
	 */
	public void processEntityHierarchy(EntityHierarchy entityHierarchy) {
		currentInheritanceType = entityHierarchy.getHierarchyInheritanceType();
		EntityBinding rootEntityBinding = createEntityBinding( entityHierarchy.getRootEntitySource(), null );
		if ( currentInheritanceType != InheritanceType.NO_INHERITANCE ) {
			processHierarchySubEntities( entityHierarchy.getRootEntitySource(), rootEntityBinding );
		}
		currentHierarchyEntityMode = null;
	}

	private void processHierarchySubEntities(SubclassEntityContainer subclassEntitySource, EntityBinding superEntityBinding) {
		for ( SubclassEntitySource subEntity : subclassEntitySource.subclassEntitySources() ) {
			EntityBinding entityBinding = createEntityBinding( subEntity, superEntityBinding );
			processHierarchySubEntities( subEntity, entityBinding );
		}
	}


	// Entities ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private EntityBinding createEntityBinding(EntitySource entitySource, EntityBinding superEntityBinding) {
		if ( processedEntityNames.contains( entitySource.getEntityName() ) ) {
			return metadata.getEntityBinding( entitySource.getEntityName() );
		}

		currentBindingContext = entitySource.getLocalBindingContext();
		try {
			final EntityBinding entityBinding = doCreateEntityBinding( entitySource, superEntityBinding );

			metadata.addEntity( entityBinding );
			processedEntityNames.add( entityBinding.getEntity().getName() );

			processFetchProfiles( entitySource, entityBinding );

			return entityBinding;
		}
		finally {
			currentBindingContext = null;
		}
	}

	private EntityBinding doCreateEntityBinding(EntitySource entitySource, EntityBinding superEntityBinding) {
		final EntityBinding entityBinding = createBasicEntityBinding( entitySource, superEntityBinding );

		bindSecondaryTables( entitySource, entityBinding );
		bindAttributes( entitySource, entityBinding );

		bindTableUniqueConstraints( entitySource, entityBinding );

		return entityBinding;
	}

	private EntityBinding createBasicEntityBinding(EntitySource entitySource, EntityBinding superEntityBinding) {
		if ( superEntityBinding == null ) {
			return makeRootEntityBinding( (RootEntitySource) entitySource );
		}
		else {
			switch ( currentInheritanceType ) {
				case SINGLE_TABLE:
					return makeDiscriminatedSubclassBinding( (SubclassEntitySource) entitySource, superEntityBinding );
				case JOINED:
					return makeJoinedSubclassBinding( (SubclassEntitySource) entitySource, superEntityBinding );
				case TABLE_PER_CLASS:
					return makeUnionedSubclassBinding( (SubclassEntitySource) entitySource, superEntityBinding );
				default:
					// extreme internal error!
					throw new AssertionFailure( "Internal condition failure" );
			}
		}
	}

	private EntityBinding makeRootEntityBinding(RootEntitySource entitySource) {
		currentHierarchyEntityMode = entitySource.getEntityMode();

		final EntityBinding entityBinding = buildBasicEntityBinding( entitySource, null );

		bindPrimaryTable( entitySource, entityBinding );

		bindIdentifier( entitySource, entityBinding );
		bindVersion( entityBinding, entitySource );
		bindDiscriminator( entitySource, entityBinding );

		entityBinding.getHierarchyDetails().setCaching( entitySource.getCaching() );
		entityBinding.getHierarchyDetails().setExplicitPolymorphism( entitySource.isExplicitPolymorphism() );
		entityBinding.getHierarchyDetails().setOptimisticLockStyle( entitySource.getOptimisticLockStyle() );

		entityBinding.setMutable( entitySource.isMutable() );
		entityBinding.setWhereFilter( entitySource.getWhere() );
		entityBinding.setRowId( entitySource.getRowId() );

		return entityBinding;
	}

	private EntityBinding buildBasicEntityBinding(EntitySource entitySource, EntityBinding superEntityBinding) {
		final EntityBinding entityBinding = superEntityBinding == null
				? new EntityBinding( currentInheritanceType, currentHierarchyEntityMode )
				: new EntityBinding( superEntityBinding );

		final String entityName = entitySource.getEntityName();
		final String className = currentHierarchyEntityMode == EntityMode.POJO ? entitySource.getClassName() : null;

		final Entity entity = new Entity(
				entityName,
				className,
				currentBindingContext.makeClassReference( className ),
				superEntityBinding == null ? null : superEntityBinding.getEntity()
		);
		entityBinding.setEntity( entity );

		entityBinding.setJpaEntityName( entitySource.getJpaEntityName() );

		if ( currentHierarchyEntityMode == EntityMode.POJO ) {
			final String proxy = entitySource.getProxy();
			if ( proxy != null ) {
				entityBinding.setProxyInterfaceType(
						currentBindingContext.makeClassReference(
								currentBindingContext.qualifyClassName( proxy )
						)
				);
				entityBinding.setLazy( true );
			}
			else if ( entitySource.isLazy() ) {
				entityBinding.setProxyInterfaceType( entityBinding.getEntity().getClassReferenceUnresolved() );
				entityBinding.setLazy( true );
			}
		}
		else {
			entityBinding.setProxyInterfaceType( null );
			entityBinding.setLazy( entitySource.isLazy() );
		}

		final String customTuplizerClassName = entitySource.getCustomTuplizerClassName();
		if ( customTuplizerClassName != null ) {
			entityBinding.setCustomEntityTuplizerClass(
					currentBindingContext.<EntityTuplizer>locateClassByName(
							customTuplizerClassName
					)
			);
		}

		final String customPersisterClassName = entitySource.getCustomPersisterClassName();
		if ( customPersisterClassName != null ) {
			entityBinding.setCustomEntityPersisterClass(
					currentBindingContext.<EntityPersister>locateClassByName(
							customPersisterClassName
					)
			);
		}

		entityBinding.setMetaAttributeContext( buildMetaAttributeContext( entitySource ) );

		entityBinding.setDynamicUpdate( entitySource.isDynamicUpdate() );
		entityBinding.setDynamicInsert( entitySource.isDynamicInsert() );
		entityBinding.setBatchSize( entitySource.getBatchSize() );
		entityBinding.setSelectBeforeUpdate( entitySource.isSelectBeforeUpdate() );
		entityBinding.setAbstract( entitySource.isAbstract() );

		entityBinding.setCustomLoaderName( entitySource.getCustomLoaderName() );
		entityBinding.setCustomInsert( entitySource.getCustomSqlInsert() );
		entityBinding.setCustomUpdate( entitySource.getCustomSqlUpdate() );
		entityBinding.setCustomDelete( entitySource.getCustomSqlDelete() );

		if ( entitySource.getSynchronizedTableNames() != null ) {
			entityBinding.addSynchronizedTableNames( entitySource.getSynchronizedTableNames() );
		}

		entityBinding.setJpaCallbackClasses(entitySource.getJpaCallbackClasses());

		return entityBinding;
	}

	private EntityBinding makeDiscriminatedSubclassBinding(SubclassEntitySource entitySource, EntityBinding superEntityBinding) {
		final EntityBinding entityBinding = buildBasicEntityBinding( entitySource, superEntityBinding );

		entityBinding.setPrimaryTable( superEntityBinding.getPrimaryTable() );
		entityBinding.setPrimaryTableName( superEntityBinding.getPrimaryTableName() );
		bindDiscriminatorValue( entitySource, entityBinding );

		return entityBinding;
	}

	private EntityBinding makeJoinedSubclassBinding(SubclassEntitySource entitySource, EntityBinding superEntityBinding) {
		final EntityBinding entityBinding = buildBasicEntityBinding( entitySource, superEntityBinding );

		bindPrimaryTable( entitySource, entityBinding );

		// todo : join

		return entityBinding;
	}

	private EntityBinding makeUnionedSubclassBinding(SubclassEntitySource entitySource, EntityBinding superEntityBinding) {
		final EntityBinding entityBinding = buildBasicEntityBinding( entitySource, superEntityBinding );

		bindPrimaryTable( entitySource, entityBinding );

		// todo : ??

		return entityBinding;
	}

	// Attributes ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void bindIdentifier(RootEntitySource entitySource, EntityBinding entityBinding) {
		if ( entitySource.getIdentifierSource() == null ) {
			throw new AssertionFailure( "Expecting identifier information on root entity descriptor" );
		}
		switch ( entitySource.getIdentifierSource().getNature() ) {
			case SIMPLE: {
				bindSimpleIdentifier( (SimpleIdentifierSource) entitySource.getIdentifierSource(), entityBinding );
				break;
			}
			case AGGREGATED_COMPOSITE: {
				// composite id with an actual component class
			    break;
			}
			case COMPOSITE: {
				// what we used to term an "embedded composite identifier", which is not tobe confused with the JPA
				// term embedded. Specifically a composite id where there is no component class, though there may
				// be a @IdClass :/
			    break;
			}
		}
	}

	private void bindSimpleIdentifier(SimpleIdentifierSource identifierSource, EntityBinding entityBinding) {
		final BasicAttributeBinding idAttributeBinding = doBasicSingularAttributeBindingCreation(
				identifierSource.getIdentifierAttributeSource(), entityBinding
		);

		entityBinding.getHierarchyDetails().getEntityIdentifier().setValueBinding( idAttributeBinding );
		IdGenerator generator = identifierSource.getIdentifierGeneratorDescriptor();
		if ( generator == null ) {
			Map<String, String> params = new HashMap<String, String>();
			params.put( IdentifierGenerator.ENTITY_NAME, entityBinding.getEntity().getName() );
			generator = new IdGenerator( "default_assign_identity_generator", "assigned", params );
		}
		entityBinding.getHierarchyDetails()
				.getEntityIdentifier()
				.setIdGenerator( generator );

		final org.hibernate.metamodel.relational.Value relationalValue = idAttributeBinding.getValue();

		if ( SimpleValue.class.isInstance( relationalValue ) ) {
			if ( !Column.class.isInstance( relationalValue ) ) {
				// this should never ever happen..
				throw new AssertionFailure( "Simple-id was not a column." );
			}
			entityBinding.getPrimaryTable().getPrimaryKey().addColumn( Column.class.cast( relationalValue ) );
		}
		else {
			for ( SimpleValue subValue : ( (Tuple) relationalValue ).values() ) {
				if ( Column.class.isInstance( subValue ) ) {
					entityBinding.getPrimaryTable().getPrimaryKey().addColumn( Column.class.cast( subValue ) );
				}
			}
		}
	}

	private void bindVersion(EntityBinding entityBinding, RootEntitySource entitySource) {
		final SingularAttributeSource versioningAttributeSource = entitySource.getVersioningAttributeSource();
		if ( versioningAttributeSource == null ) {
			return;
		}

		BasicAttributeBinding attributeBinding = doBasicSingularAttributeBindingCreation(
				versioningAttributeSource, entityBinding
		);
		entityBinding.getHierarchyDetails().setVersioningAttributeBinding( attributeBinding );
	}

	private void bindDiscriminator(RootEntitySource entitySource, EntityBinding entityBinding) {
		final DiscriminatorSource discriminatorSource = entitySource.getDiscriminatorSource();
		if ( discriminatorSource == null ) {
			return;
		}

		EntityDiscriminator discriminator = new EntityDiscriminator();
		SimpleValue relationalValue = makeSimpleValue(
				entityBinding,
				discriminatorSource.getDiscriminatorRelationalValueSource()
		);
		discriminator.setBoundValue( relationalValue );

		discriminator.getExplicitHibernateTypeDescriptor().setExplicitTypeName(
				discriminatorSource.getExplicitHibernateTypeName() != null
						? discriminatorSource.getExplicitHibernateTypeName()
						: "string"
		);

		discriminator.setInserted( discriminatorSource.isInserted() );
		discriminator.setForced( discriminatorSource.isForced() );

		entityBinding.getHierarchyDetails().setEntityDiscriminator( discriminator );
		entityBinding.setDiscriminatorMatchValue( entitySource.getDiscriminatorMatchValue() );
	}

	private void bindDiscriminatorValue(SubclassEntitySource entitySource, EntityBinding entityBinding) {
		final String discriminatorValue = entitySource.getDiscriminatorMatchValue();
		if ( discriminatorValue == null ) {
			return;
		}
		entityBinding.setDiscriminatorMatchValue( discriminatorValue );
	}

	private void bindAttributes(AttributeSourceContainer attributeSourceContainer, AttributeBindingContainer attributeBindingContainer) {
		// todo : we really need the notion of a Stack here for the table from which the columns come for binding these attributes.
		// todo : adding the concept (interface) of a source of attribute metadata would allow reuse of this method for entity, component, unique-key, etc
		// for now, simply assume all columns come from the base table....

		for ( AttributeSource attributeSource : attributeSourceContainer.attributeSources() ) {
			if ( attributeSource.isSingular() ) {
				final SingularAttributeSource singularAttributeSource = (SingularAttributeSource) attributeSource;
				if ( singularAttributeSource.getNature() == SingularAttributeNature.COMPONENT ) {
					bindComponent( (ComponentAttributeSource) singularAttributeSource, attributeBindingContainer );
				}
				else {
					doBasicSingularAttributeBindingCreation( singularAttributeSource, attributeBindingContainer );
				}
			}
			else {
				bindPersistentCollection( (PluralAttributeSource) attributeSource, attributeBindingContainer );
			}
		}
	}

	private void bindComponent(ComponentAttributeSource attributeSource, AttributeBindingContainer container) {
		final String attributeName = attributeSource.getName();
		SingularAttribute attribute = container.getAttributeContainer().locateComponentAttribute( attributeName );
		if ( attribute == null ) {
			final Component component = new Component(
					attributeSource.getPath(),
					attributeSource.getClassName(),
					attributeSource.getClassReference(),
					null // component inheritance not YET supported
			);
			attribute = container.getAttributeContainer().createComponentAttribute( attributeName, component );
		}
		ComponentAttributeBinding componentAttributeBinding = container.makeComponentAttributeBinding( attribute );

		if ( StringHelper.isNotEmpty( attributeSource.getParentReferenceAttributeName() ) ) {
			final SingularAttribute parentReferenceAttribute =
					componentAttributeBinding.getComponent()
							.createSingularAttribute( attributeSource.getParentReferenceAttributeName() );
			componentAttributeBinding.setParentReference( parentReferenceAttribute );
		}

		componentAttributeBinding.setMetaAttributeContext(
				buildMetaAttributeContext( attributeSource.metaAttributes(), container.getMetaAttributeContext() )
		);

		bindAttributes( attributeSource, componentAttributeBinding );
	}

	private void bindPersistentCollection(PluralAttributeSource attributeSource, AttributeBindingContainer attributeBindingContainer) {
		final PluralAttribute existingAttribute = attributeBindingContainer.getAttributeContainer()
				.locatePluralAttribute( attributeSource.getName() );
		final AbstractPluralAttributeBinding pluralAttributeBinding;

		if ( attributeSource.getPluralAttributeNature() == PluralAttributeNature.BAG ) {
			final PluralAttribute attribute = existingAttribute != null
					? existingAttribute
					: attributeBindingContainer.getAttributeContainer().createBag( attributeSource.getName() );
			pluralAttributeBinding = attributeBindingContainer.makeBagAttributeBinding(
					attribute,
					convert( attributeSource.getElementSource().getNature() )
			);
		}
		else if ( attributeSource.getPluralAttributeNature() == PluralAttributeNature.SET ) {
			final PluralAttribute attribute = existingAttribute != null
					? existingAttribute
					: attributeBindingContainer.getAttributeContainer().createSet( attributeSource.getName() );
			pluralAttributeBinding = attributeBindingContainer.makeSetAttributeBinding(
					attribute,
					convert( attributeSource.getElementSource().getNature() )
			);
		}
		else {
			// todo : implement other collection types
			throw new NotYetImplementedException( "Collections other than bag and set not yet implemented :(" );
		}

		doBasicPluralAttributeBinding( attributeSource, pluralAttributeBinding );

		bindCollectionTable( attributeSource, pluralAttributeBinding );
		bindSortingAndOrdering( attributeSource, pluralAttributeBinding );

		bindCollectionKey( attributeSource, pluralAttributeBinding );
		bindCollectionElement( attributeSource, pluralAttributeBinding );
		bindCollectionIndex( attributeSource, pluralAttributeBinding );

		metadata.addCollection( pluralAttributeBinding );
	}

	private void doBasicPluralAttributeBinding(PluralAttributeSource source, AbstractPluralAttributeBinding binding) {
		binding.setFetchTiming( source.getFetchTiming() );
		binding.setFetchStyle( source.getFetchStyle() );
		binding.setCascadeStyles( source.getCascadeStyles() );

		binding.setCaching( source.getCaching() );

		binding.getHibernateTypeDescriptor().setJavaTypeName(
				source.getPluralAttributeNature().reportedJavaType().getName()
		);
		binding.getHibernateTypeDescriptor().setExplicitTypeName( source.getTypeInformation().getName() );
		binding.getHibernateTypeDescriptor().getTypeParameters().putAll( source.getTypeInformation().getParameters() );

		if ( StringHelper.isNotEmpty( source.getCustomPersisterClassName() ) ) {
			binding.setCollectionPersisterClass(
					currentBindingContext.<CollectionPersister>locateClassByName( source.getCustomPersisterClassName() )
			);
		}

		if ( source.getCustomPersisterClassName() != null ) {
			binding.setCollectionPersisterClass(
					metadata.<CollectionPersister>locateClassByName( source.getCustomPersisterClassName() )
			);
		}

		binding.setCustomLoaderName( source.getCustomLoaderName() );
		binding.setCustomSqlInsert( source.getCustomSqlInsert() );
		binding.setCustomSqlUpdate( source.getCustomSqlUpdate() );
		binding.setCustomSqlDelete( source.getCustomSqlDelete() );
		binding.setCustomSqlDeleteAll( source.getCustomSqlDeleteAll() );

		binding.setMetaAttributeContext(
				buildMetaAttributeContext(
						source.metaAttributes(),
						binding.getContainer().getMetaAttributeContext()
				)
		);

		doBasicAttributeBinding( source, binding );
	}

	private CollectionLaziness interpretLaziness(String laziness) {
		if ( laziness == null ) {
			laziness = Boolean.toString( metadata.getMappingDefaults().areAssociationsLazy() );
		}

		if ( "extra".equals( laziness ) ) {
			return CollectionLaziness.EXTRA;
		}
		else if ( "false".equals( laziness ) ) {
			return CollectionLaziness.NOT;
		}
		else if ( "true".equals( laziness ) ) {
			return CollectionLaziness.LAZY;
		}

		throw new MappingException(
				String.format( "Unexpected collection laziness value %s", laziness ),
				currentBindingContext.getOrigin()
		);
	}

	private void bindCollectionTable(
			PluralAttributeSource attributeSource,
			AbstractPluralAttributeBinding pluralAttributeBinding) {
		if ( attributeSource.getElementSource().getNature() == PluralAttributeElementNature.ONE_TO_MANY ) {
			return;
		}

		final Schema.Name schemaName = Helper.determineDatabaseSchemaName(
				attributeSource.getExplicitSchemaName(),
				attributeSource.getExplicitCatalogName(),
				currentBindingContext
		);
		final Schema schema = metadata.getDatabase().locateSchema( schemaName );

		final String tableName = attributeSource.getExplicitCollectionTableName();
		if ( StringHelper.isNotEmpty( tableName ) ) {
			final Identifier tableIdentifier = Identifier.toIdentifier(
					currentBindingContext.getNamingStrategy().tableName( tableName )
			);
			Table collectionTable = schema.locateTable( tableIdentifier );
			if ( collectionTable == null ) {
				collectionTable = schema.createTable( tableIdentifier );
			}
			pluralAttributeBinding.setCollectionTable( collectionTable );
		}
		else {
			// todo : not sure wel have all the needed info here in all cases, specifically when needing to know the "other side"
			final EntityBinding owner = pluralAttributeBinding.getContainer().seekEntityBinding();
			final String ownerTableLogicalName = Table.class.isInstance( owner.getPrimaryTable() )
					? Table.class.cast( owner.getPrimaryTable() ).getTableName().getName()
					: null;
			String collectionTableName = currentBindingContext.getNamingStrategy().collectionTableName(
					owner.getEntity().getName(),
					ownerTableLogicalName,
					null,	// todo : here
					null,	// todo : and here
					pluralAttributeBinding.getContainer().getPathBase() + '.' + attributeSource.getName()
			);
			collectionTableName = quoteIdentifier( collectionTableName );
			pluralAttributeBinding.setCollectionTable(
					schema.locateOrCreateTable(
							Identifier.toIdentifier(
									collectionTableName
							)
					)
			);
		}

		if ( StringHelper.isNotEmpty( attributeSource.getCollectionTableComment() ) ) {
			pluralAttributeBinding.getCollectionTable().addComment( attributeSource.getCollectionTableComment() );
		}

		if ( StringHelper.isNotEmpty( attributeSource.getCollectionTableCheck() ) ) {
			pluralAttributeBinding.getCollectionTable().addCheckConstraint( attributeSource.getCollectionTableCheck() );
		}

		pluralAttributeBinding.setWhere( attributeSource.getWhere() );
	}

	private void bindCollectionKey(
			PluralAttributeSource attributeSource,
			AbstractPluralAttributeBinding pluralAttributeBinding) {
		pluralAttributeBinding.getCollectionKey().prepareForeignKey(
				attributeSource.getKeySource().getExplicitForeignKeyName(),
				null  // todo : handle secondary table names
		);
		pluralAttributeBinding.getCollectionKey().getForeignKey().setDeleteRule(
				attributeSource.getKeySource().getOnDeleteAction()
		);
		// todo : need to bind "relational values", account for property-ref
	}

	private void bindCollectionElement(
			PluralAttributeSource attributeSource,
			AbstractPluralAttributeBinding pluralAttributeBinding) {
		final PluralAttributeElementSource elementSource = attributeSource.getElementSource();
		if ( elementSource.getNature() == PluralAttributeElementNature.BASIC ) {
			final BasicPluralAttributeElementSource basicElementSource = (BasicPluralAttributeElementSource) elementSource;
			final BasicCollectionElement basicCollectionElement = (BasicCollectionElement) pluralAttributeBinding.getCollectionElement();
			resolveTypeInformation(
					basicElementSource.getExplicitHibernateTypeSource(),
					pluralAttributeBinding.getAttribute(),
					basicCollectionElement
			);
			// todo : temp
			return;
		}
		// todo : implement
		throw new NotYetImplementedException(
				String.format(
						"Support for collection elements of type %s not yet implemented",
						elementSource.getNature()
				)
		);
	}

	private void bindCollectionIndex(
			PluralAttributeSource attributeSource,
			AbstractPluralAttributeBinding pluralAttributeBinding) {
		if ( attributeSource.getPluralAttributeNature() != PluralAttributeNature.LIST
				&& attributeSource.getPluralAttributeNature() != PluralAttributeNature.MAP ) {
			return;
		}

		// todo : implement
		throw new NotYetImplementedException();
	}

	private void bindSortingAndOrdering(
			PluralAttributeSource attributeSource,
			AbstractPluralAttributeBinding pluralAttributeBinding) {
		if ( Sortable.class.isInstance( attributeSource ) ) {
			final Sortable sortable = Sortable.class.cast( attributeSource );
			if ( sortable.isSorted() ) {
				// todo : handle setting comparator

				// and then return because sorting and ordering are mutually exclusive
				return;
			}
		}

		if ( Orderable.class.isInstance( attributeSource ) ) {
			final Orderable orderable = Orderable.class.cast( attributeSource );
			if ( orderable.isOrdered() ) {
				// todo : handle setting ordering
			}
		}
	}

	private void doBasicAttributeBinding(AttributeSource attributeSource, AttributeBinding attributeBinding) {
		attributeBinding.setPropertyAccessorName( attributeSource.getPropertyAccessorName() );
		attributeBinding.setIncludedInOptimisticLocking( attributeSource.isIncludedInOptimisticLocking() );
	}

	private CollectionElementNature convert(PluralAttributeElementNature pluralAttributeElementNature) {
		return CollectionElementNature.valueOf( pluralAttributeElementNature.name() );
	}

	private BasicAttributeBinding doBasicSingularAttributeBindingCreation(
			SingularAttributeSource attributeSource,
			AttributeBindingContainer attributeBindingContainer) {
		final SingularAttribute existingAttribute = attributeBindingContainer.getAttributeContainer()
				.locateSingularAttribute( attributeSource.getName() );
		final SingularAttribute attribute;
		if ( existingAttribute != null ) {
			attribute = existingAttribute;
		}
		else if ( attributeSource.isVirtualAttribute() ) {
			attribute = attributeBindingContainer.getAttributeContainer().createVirtualSingularAttribute(
					attributeSource.getName()
			);
		}
		else {
			attribute = attributeBindingContainer.getAttributeContainer()
					.createSingularAttribute( attributeSource.getName() );
		}

		final BasicAttributeBinding attributeBinding;
		if ( attributeSource.getNature() == SingularAttributeNature.BASIC ) {
			attributeBinding = attributeBindingContainer.makeBasicAttributeBinding( attribute );
			resolveTypeInformation( attributeSource.getTypeInformation(), attributeBinding );
		}
		else if ( attributeSource.getNature() == SingularAttributeNature.MANY_TO_ONE ) {
			attributeBinding = attributeBindingContainer.makeManyToOneAttributeBinding( attribute );
			resolveTypeInformation( attributeSource.getTypeInformation(), attributeBinding );
			resolveToOneInformation(
					(ToOneAttributeSource) attributeSource,
					(ManyToOneAttributeBinding) attributeBinding
			);
		}
		else {
			throw new NotYetImplementedException();
		}

		attributeBinding.setGeneration( attributeSource.getGeneration() );
		attributeBinding.setLazy( attributeSource.isLazy() );
		attributeBinding.setIncludedInOptimisticLocking( attributeSource.isIncludedInOptimisticLocking() );

		attributeBinding.setPropertyAccessorName(
				Helper.getPropertyAccessorName(
						attributeSource.getPropertyAccessorName(),
						false,
						currentBindingContext.getMappingDefaults().getPropertyAccessorName()
				)
		);

		bindRelationalValues( attributeSource, attributeBinding );

		attributeBinding.setMetaAttributeContext(
				buildMetaAttributeContext(
						attributeSource.metaAttributes(),
						attributeBindingContainer.getMetaAttributeContext()
				)
		);

		return attributeBinding;
	}

	private void resolveTypeInformation(ExplicitHibernateTypeSource typeSource, BasicAttributeBinding attributeBinding) {
		final Class<?> attributeJavaType = determineJavaType( attributeBinding.getAttribute() );
		if ( attributeJavaType != null ) {
			attributeBinding.getAttribute()
					.resolveType( currentBindingContext.makeJavaType( attributeJavaType.getName() ) );
		}

		resolveTypeInformation( typeSource, attributeBinding.getHibernateTypeDescriptor(), attributeJavaType );
	}

	private void resolveTypeInformation(
			ExplicitHibernateTypeSource typeSource,
			PluralAttribute attribute,
			BasicCollectionElement collectionElement) {
		final Class<?> attributeJavaType = determineJavaType( attribute );
		resolveTypeInformation( typeSource, collectionElement.getHibernateTypeDescriptor(), attributeJavaType );
	}

	private void resolveTypeInformation(
			ExplicitHibernateTypeSource typeSource,
			HibernateTypeDescriptor hibernateTypeDescriptor,
			Class<?> discoveredJavaType) {
		if ( discoveredJavaType != null ) {
			hibernateTypeDescriptor.setJavaTypeName( discoveredJavaType.getName() );
		}

		final String explicitTypeName = typeSource.getName();
		if ( explicitTypeName != null ) {
			final TypeDef typeDef = currentBindingContext.getMetadataImplementor()
					.getTypeDefinition( explicitTypeName );
			if ( typeDef != null ) {
				hibernateTypeDescriptor.setExplicitTypeName( typeDef.getTypeClass() );
				hibernateTypeDescriptor.getTypeParameters().putAll( typeDef.getParameters() );
			}
			else {
				hibernateTypeDescriptor.setExplicitTypeName( explicitTypeName );
			}
			final Map<String, String> parameters = typeSource.getParameters();
			if ( parameters != null ) {
				hibernateTypeDescriptor.getTypeParameters().putAll( parameters );
			}
		}
		else {
			if ( discoveredJavaType == null ) {
				// we will have problems later determining the Hibernate Type to use.  Should we throw an
				// exception now?  Might be better to get better contextual info
			}
		}
	}

	/**
	 * @param attribute the domain attribute
	 *
	 * @return Returns the Java type of the attribute using reflection or {@code null} if the type cannot be discovered
	 */
	private Class<?> determineJavaType(final SingularAttribute attribute) {
		try {
			final Class<?> ownerClass = attribute.getAttributeContainer().getClassReference();
			AttributeJavaTypeDeterminerDelegate delegate = new AttributeJavaTypeDeterminerDelegate( attribute.getName() );
			BeanInfoHelper.visitBeanInfo( ownerClass, delegate );
			return delegate.javaType;
		}
		catch ( Exception ignore ) {
			// todo : log it?
		}
		return null;
	}

	private Class<?> determineJavaType(PluralAttribute attribute) {
		try {
			final Class<?> ownerClass = attribute.getAttributeContainer().getClassReference();
			PluralAttributeJavaTypeDeterminerDelegate delegate = new PluralAttributeJavaTypeDeterminerDelegate(
					ownerClass,
					attribute.getName()
			);
			BeanInfoHelper.visitBeanInfo( ownerClass, delegate );
			return delegate.javaType;
		}
		catch ( Exception ignore ) {
			// todo : log it?
		}
		return null;
	}

	private class PluralAttributeJavaTypeDeterminerDelegate implements BeanInfoHelper.BeanInfoDelegate {
		private final Class<?> ownerClass;
		private final String attributeName;

		private Class<?> javaType = null;

		private PluralAttributeJavaTypeDeterminerDelegate(Class<?> ownerClass, String attributeName) {
			this.ownerClass = ownerClass;
			this.attributeName = attributeName;
		}

		@Override
		public void processBeanInfo(BeanInfo beanInfo) throws Exception {
			for ( PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors() ) {
				if ( propertyDescriptor.getName().equals( attributeName ) ) {
					javaType = extractCollectionComponentType( beanInfo, propertyDescriptor );
					break;
				}
			}
		}

		@SuppressWarnings( { "unchecked" })
		private Class<?> extractCollectionComponentType(BeanInfo beanInfo, PropertyDescriptor propertyDescriptor) {
			final java.lang.reflect.Type collectionAttributeType;
			if ( propertyDescriptor.getReadMethod() != null ) {
				collectionAttributeType = propertyDescriptor.getReadMethod().getGenericReturnType();
			}
			else if ( propertyDescriptor.getWriteMethod() != null ) {
				collectionAttributeType = propertyDescriptor.getWriteMethod().getGenericParameterTypes()[0];
			}
			else {
				// we need to look for the field and look at it...
				try {
					collectionAttributeType = ownerClass.getField( propertyDescriptor.getName() ).getGenericType();
				}
				catch ( Exception e ) {
					return null;
				}
			}

			if ( ParameterizedType.class.isInstance( collectionAttributeType ) ) {
				final java.lang.reflect.Type[] types = ( (ParameterizedType) collectionAttributeType ).getActualTypeArguments();
				if ( types == null ) {
					return null;
				}
				else if ( types.length == 1 ) {
					return (Class<?>) types[0];
				}
				else if ( types.length == 2 ) {
					// Map<K,V>
					return (Class<?>) types[1];
				}
			}
			return null;
		}
	}

	private void resolveToOneInformation(ToOneAttributeSource attributeSource, ManyToOneAttributeBinding attributeBinding) {
		final String referencedEntityName = attributeSource.getReferencedEntityName() != null
				? attributeSource.getReferencedEntityName()
				: attributeBinding.getAttribute().getSingularAttributeType().getClassName();
		attributeBinding.setReferencedEntityName( referencedEntityName );
		// todo : we should consider basing references on columns instead of property-ref, which would require a resolution (later) of property-ref to column names
		attributeBinding.setReferencedAttributeName( attributeSource.getReferencedEntityAttributeName() );

		attributeBinding.setCascadeStyles( attributeSource.getCascadeStyles() );
		attributeBinding.setFetchTiming( attributeSource.getFetchTiming() );
		attributeBinding.setFetchStyle( attributeSource.getFetchStyle() );
	}

	private MetaAttributeContext buildMetaAttributeContext(EntitySource entitySource) {
		return buildMetaAttributeContext(
				entitySource.metaAttributes(),
				true,
				currentBindingContext.getMetadataImplementor().getGlobalMetaAttributeContext()
		);
	}

	private static MetaAttributeContext buildMetaAttributeContext(
			Iterable<MetaAttributeSource> metaAttributeSources,
			MetaAttributeContext parentContext) {
		return buildMetaAttributeContext( metaAttributeSources, false, parentContext );
	}

	private static MetaAttributeContext buildMetaAttributeContext(
			Iterable<MetaAttributeSource> metaAttributeSources,
			boolean onlyInheritable,
			MetaAttributeContext parentContext) {
		final MetaAttributeContext subContext = new MetaAttributeContext( parentContext );

		for ( MetaAttributeSource metaAttributeSource : metaAttributeSources ) {
			if ( onlyInheritable & !metaAttributeSource.isInheritable() ) {
				continue;
			}

			final String name = metaAttributeSource.getName();
			final MetaAttribute inheritedMetaAttribute = parentContext.getMetaAttribute( name );
			MetaAttribute metaAttribute = subContext.getLocalMetaAttribute( name );
			if ( metaAttribute == null || metaAttribute == inheritedMetaAttribute ) {
				metaAttribute = new MetaAttribute( name );
				subContext.add( metaAttribute );
			}
			metaAttribute.addValue( metaAttributeSource.getValue() );
		}

		return subContext;
	}

	// Relational ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void bindPrimaryTable(EntitySource entitySource, EntityBinding entityBinding) {
		final TableSource tableSource = entitySource.getPrimaryTable();
		final Table table = createTable( entityBinding, tableSource );
		entityBinding.setPrimaryTable( table );
		entityBinding.setPrimaryTableName( table.getTableName().getName() );
	}

	private void bindSecondaryTables(EntitySource entitySource, EntityBinding entityBinding) {
		for ( TableSource secondaryTableSource : entitySource.getSecondaryTables() ) {
			final Table table = createTable( entityBinding, secondaryTableSource );
			entityBinding.addSecondaryTable( secondaryTableSource.getLogicalName(), table );
		}
	}

	private Table createTable(EntityBinding entityBinding, TableSource tableSource) {
		String tableName = tableSource.getExplicitTableName();
		if ( StringHelper.isEmpty( tableName ) ) {
			tableName = currentBindingContext.getNamingStrategy()
					.classToTableName( entityBinding.getEntity().getClassName() );
		}
		else {
			tableName = currentBindingContext.getNamingStrategy().tableName( tableName );
		}
		tableName = quoteIdentifier( tableName );

		final Schema.Name databaseSchemaName = Helper.determineDatabaseSchemaName(
				tableSource.getExplicitSchemaName(),
				tableSource.getExplicitCatalogName(),
				currentBindingContext
		);
		return currentBindingContext.getMetadataImplementor()
				.getDatabase()
				.locateSchema( databaseSchemaName )
				.locateOrCreateTable( Identifier.toIdentifier( tableName ) );
	}

	private void bindTableUniqueConstraints(EntitySource entitySource, EntityBinding entityBinding) {
		for ( ConstraintSource constraintSource : entitySource.getConstraints() ) {
			if ( constraintSource instanceof UniqueConstraintSource ) {
				TableSpecification table = entityBinding.locateTable( constraintSource.getTableName() );
				if ( table == null ) {
					// throw exception !?
				}
				String constraintName = constraintSource.name();
				if ( constraintName == null ) {
					// create a default name
				}

				UniqueKey uniqueKey = table.getOrCreateUniqueKey( constraintName );
				for ( String columnName : constraintSource.columnNames() ) {
					uniqueKey.addColumn( table.locateOrCreateColumn( quoteIdentifier( columnName ) ) );
				}
			}
		}
	}

	private void bindRelationalValues(
			RelationalValueSourceContainer relationalValueSourceContainer,
			SingularAttributeBinding attributeBinding) {

		List<SimpleValueBinding> valueBindings = new ArrayList<SimpleValueBinding>();

		if ( !relationalValueSourceContainer.relationalValueSources().isEmpty() ) {
			for ( RelationalValueSource valueSource : relationalValueSourceContainer.relationalValueSources() ) {
				final TableSpecification table = attributeBinding.getContainer()
						.seekEntityBinding()
						.locateTable( valueSource.getContainingTableName() );

				if ( ColumnSource.class.isInstance( valueSource ) ) {
					final ColumnSource columnSource = ColumnSource.class.cast( valueSource );
					final Column column = makeColumn( (ColumnSource) valueSource, table );
					valueBindings.add(
							new SimpleValueBinding(
									column,
									columnSource.isIncludedInInsert(),
									columnSource.isIncludedInUpdate()
							)
					);
				}
				else {
					valueBindings.add(
							new SimpleValueBinding(
									makeDerivedValue( ( (DerivedValueSource) valueSource ), table )
							)
					);
				}
			}
		}
		else {
			String name = metadata.getOptions()
					.getNamingStrategy()
					.propertyToColumnName( attributeBinding.getAttribute().getName() );
			name = quoteIdentifier( name );
			Column column = attributeBinding.getContainer()
									.seekEntityBinding()
									.getPrimaryTable()
									.locateOrCreateColumn( name );
			column.setNullable( relationalValueSourceContainer.areValuesNullableByDefault() );
			valueBindings.add(
					new SimpleValueBinding(
							column,
							relationalValueSourceContainer.areValuesIncludedInInsertByDefault(),
							relationalValueSourceContainer.areValuesIncludedInUpdateByDefault()
					)
			);
		}
		attributeBinding.setSimpleValueBindings( valueBindings );
	}

	private String quoteIdentifier(String identifier) {
		return currentBindingContext.isGloballyQuotedIdentifiers() ? StringHelper.quote( identifier ) : identifier;
	}

	private SimpleValue makeSimpleValue(
			EntityBinding entityBinding,
			RelationalValueSource valueSource) {
		final TableSpecification table = entityBinding.locateTable( valueSource.getContainingTableName() );

		if ( ColumnSource.class.isInstance( valueSource ) ) {
			return makeColumn( (ColumnSource) valueSource, table );
		}
		else {
			return makeDerivedValue( (DerivedValueSource) valueSource, table );
		}
	}

	private Column makeColumn(ColumnSource columnSource, TableSpecification table) {
		String name = columnSource.getName();
		name = metadata.getOptions().getNamingStrategy().columnName( name );
		name = quoteIdentifier( name );
		final Column column = table.locateOrCreateColumn( name );
		column.setNullable( columnSource.isNullable() );
		column.setDefaultValue( columnSource.getDefaultValue() );
		column.setSqlType( columnSource.getSqlType() );
		column.setSize( columnSource.getSize() );
		column.setDatatype( columnSource.getDatatype() );
		column.setReadFragment( columnSource.getReadFragment() );
		column.setWriteFragment( columnSource.getWriteFragment() );
		column.setUnique( columnSource.isUnique() );
		column.setCheckCondition( columnSource.getCheckCondition() );
		column.setComment( columnSource.getComment() );
		return column;
	}

	private DerivedValue makeDerivedValue(DerivedValueSource derivedValueSource, TableSpecification table) {
		return table.locateOrCreateDerivedValue( derivedValueSource.getExpression() );
	}

	private void processFetchProfiles(EntitySource entitySource, EntityBinding entityBinding) {
		// todo : process the entity-local fetch-profile declaration
	}

	private static class AttributeJavaTypeDeterminerDelegate implements BeanInfoHelper.BeanInfoDelegate {
		private final String attributeName;
		private Class<?> javaType = null;

		private AttributeJavaTypeDeterminerDelegate(String attributeName) {
			this.attributeName = attributeName;
		}

		@Override
		public void processBeanInfo(BeanInfo beanInfo) throws Exception {
			for ( PropertyDescriptor propertyDescriptor : beanInfo.getPropertyDescriptors() ) {
				if ( propertyDescriptor.getName().equals( attributeName ) ) {
					javaType = propertyDescriptor.getPropertyType();
					break;
				}
			}
		}
	}
}
