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
package org.hibernate.metamodel.internal.source;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.TruthValue;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.source.hbm.Helper;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicPluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityDiscriminator;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.IdGenerator;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.metamodel.spi.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.MetaAttribute;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SecondaryTable;
import org.hibernate.metamodel.spi.binding.SingularAssociationAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.domain.Attribute;
import org.hibernate.metamodel.spi.domain.Composite;
import org.hibernate.metamodel.spi.domain.Entity;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.DerivedValue;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.InLineView;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.UniqueKey;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.AttributeSourceContainer;
import org.hibernate.metamodel.spi.source.BasicPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.ColumnBindingDefaults;
import org.hibernate.metamodel.spi.source.ColumnSource;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.ConstraintSource;
import org.hibernate.metamodel.spi.source.DerivedValueSource;
import org.hibernate.metamodel.spi.source.DiscriminatorSource;
import org.hibernate.metamodel.spi.source.EntityHierarchy;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.InLineViewSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.MetadataImplementor;
import org.hibernate.metamodel.spi.source.Orderable;
import org.hibernate.metamodel.spi.source.PluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.PluralAttributeKeySource;
import org.hibernate.metamodel.spi.source.PluralAttributeNature;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.metamodel.spi.source.PrimaryKeyJoinColumnSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.RelationalValueSourceContainer;
import org.hibernate.metamodel.spi.source.RootEntitySource;
import org.hibernate.metamodel.spi.source.SecondaryTableSource;
import org.hibernate.metamodel.spi.source.SimpleIdentifierSource;
import org.hibernate.metamodel.spi.source.SingularAttributeNature;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;
import org.hibernate.metamodel.spi.source.Sortable;
import org.hibernate.metamodel.spi.source.SubclassEntityContainer;
import org.hibernate.metamodel.spi.source.SubclassEntitySource;
import org.hibernate.metamodel.spi.source.TableSource;
import org.hibernate.metamodel.spi.source.TableSpecificationSource;
import org.hibernate.metamodel.spi.source.ToOneAttributeSource;
import org.hibernate.metamodel.spi.source.UniqueConstraintSource;
import org.hibernate.metamodel.spi.source.VersionAttributeSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.config.spi.ConfigurationService;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.Type;

/**
 * The common binder shared between annotations and {@code hbm.xml} processing.
 * <p/>
 * The API consists of {@link #Binder( MetadataImplementor )} and {@link #processEntityHierarchies(Iterable)}
 *
 * @todo Really need to chop this up.  The class is doing so many different things right now.
 * @todo And really need to come up with consistent method naming.
 * @todo This class really should live in the o.h.metamodel.internal package
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class Binder {
	private final MetadataImplementor metadata;
	private final ArrayList<String> processedEntityNames = new ArrayList<String>();

	private HibernateTypeHelper typeHelper = new HibernateTypeHelper( this );

	private final ObjectNameNormalizer NAME_NORMALIZER = new ObjectNameNormalizer() {
		@Override
		protected boolean isUseQuotedIdentifiersGlobally() {
			return metadata.isGloballyQuotedIdentifiers();
		}

		@Override
		protected NamingStrategy getNamingStrategy() {
			return metadata.getNamingStrategy();
		}
	};

	private InheritanceType currentInheritanceType;
	private EntityMode currentHierarchyEntityMode;
	private LocalBindingContext currentBindingContext;
    private HashMap<String, EntitySource> sourcesByName = new HashMap<String, EntitySource>();

	public Binder( MetadataImplementor metadata ) {
		this.metadata = metadata;
	}

	MetadataImplementor getMetadata() {
		return metadata;
	}

	LocalBindingContext getCurrentBindingContext() {
		return currentBindingContext;
	}

	/**
	 * Process an entity hierarchy.
	 *
	 * @param entityHierarchies THe hierarchies to process.
	 */
	public void processEntityHierarchies( Iterable<? extends EntityHierarchy> entityHierarchies ) {
        // Index sources by name so we can find and resolve entities on the fly as references to them
        // are encountered (e.g., within associations)
        for ( EntityHierarchy hierarchy : entityHierarchies ) {
			mapSourcesByName( hierarchy.getRootEntitySource() );
		}

	    for ( EntityHierarchy hierarchy : entityHierarchies ) {
			processEntityHierarchy( hierarchy );
	    }
	}

	private void mapSourcesByName(EntitySource source) {
        sourcesByName.put( source.getEntityName(), source );
        for ( SubclassEntitySource subclassEntitySource : source.subclassEntitySources() ) {
			mapSourcesByName( subclassEntitySource );
		}
	}

	@SuppressWarnings( {"unchecked"})
	private void processEntityHierarchy(EntityHierarchy hierarchy) {
		final RootEntitySource rootEntitySource = hierarchy.getRootEntitySource();
		currentInheritanceType = hierarchy.getHierarchyInheritanceType();
		currentHierarchyEntityMode = rootEntitySource.getEntityMode();
		try {
			// create the binding
			final EntityBinding rootEntityBinding = createEntityBinding( rootEntitySource, null );

			// Create identifier generator for root entity
			Properties properties = new Properties();
			properties.putAll( metadata.getServiceRegistry().getService( ConfigurationService.class ).getSettings() );
			if ( !properties.contains( AvailableSettings.PREFER_POOLED_VALUES_LO ) ) {
				properties.put( AvailableSettings.PREFER_POOLED_VALUES_LO, "false" );
			}
			if ( !properties.contains( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER ) ) {
				properties.put( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER, NAME_NORMALIZER );
			}
			rootEntityBinding.getHierarchyDetails()
					.getEntityIdentifier()
					.createIdentifierGenerator( metadata.getIdentifierGeneratorFactory(), properties );

			if ( currentInheritanceType != InheritanceType.NO_INHERITANCE ) {
				processHierarchySubEntities( rootEntitySource, rootEntityBinding );
			}
		}
		finally {
			currentHierarchyEntityMode = null;
			currentInheritanceType = null;
		}
	}

	private void processHierarchySubEntities(
			SubclassEntityContainer subclassEntitySource,
			EntityBinding superEntityBinding) {
		for ( SubclassEntitySource subEntity : subclassEntitySource.subclassEntitySources() ) {
			// create the current entity's binding....
			final EntityBinding entityBinding = createEntityBinding( subEntity, superEntityBinding );
			// then drill down into its sub-class entities
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

		Deque<TableSpecification> tableStack = new ArrayDeque<TableSpecification>(  );
		tableStack.add( entityBinding.getPrimaryTable() );
		bindAttributes( entitySource, entityBinding, tableStack );

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

	private void bindSimpleIdentifier(
			SimpleIdentifierSource identifierSource,
			EntityBinding entityBinding) {
		final BasicAttributeBinding idAttributeBinding = bindBasicAttribute(
				identifierSource.getIdentifierAttributeSource(),
				entityBinding
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

		final List<RelationalValueBinding> relationalValueBindings = idAttributeBinding.getRelationalValueBindings();

		if ( relationalValueBindings.size() == 1 ) {
			final Value value = relationalValueBindings.get( 0 ).getValue();
			if ( !Column.class.isInstance( value ) ) {
				// this should never ever happen..
				throw new AssertionFailure( "Simple-id was not a column." );
			}
			entityBinding.getPrimaryTable().getPrimaryKey().addColumn( Column.class.cast( value ) );
		}
		else {
			for ( RelationalValueBinding valueBinding : relationalValueBindings ) {
				final Value value = valueBinding.getValue();
				if ( Column.class.isInstance( value ) ) {
					entityBinding.getPrimaryTable().getPrimaryKey().addColumn( Column.class.cast( value ) );
				}
			}
		}
	}

	private void bindVersion(EntityBinding entityBinding, RootEntitySource entitySource) {
		final VersionAttributeSource versioningAttributeSource = entitySource.getVersioningAttributeSource();
		if ( versioningAttributeSource == null ) {
			return;
		}

		BasicAttributeBinding attributeBinding = bindBasicAttribute( versioningAttributeSource, entityBinding );
		entityBinding.getHierarchyDetails().getEntityVersion().setVersioningAttributeBinding( attributeBinding );
		entityBinding.getHierarchyDetails().getEntityVersion().setUnsavedValue( versioningAttributeSource.getUnsavedValue() );
	}

	public static final ColumnBindingDefaults DISCRIMINATOR_COLUMN_BINDING_DEFAULTS = new ColumnBindingDefaults() {
		@Override
		public boolean areValuesIncludedInInsertByDefault() {
			return true;
		}

		@Override
		public boolean areValuesIncludedInUpdateByDefault() {
			return false;
		}

		@Override
		public boolean areValuesNullableByDefault() {
			return false;
		}
	};

	private void bindDiscriminator(RootEntitySource entitySource, EntityBinding entityBinding) {
		final DiscriminatorSource discriminatorSource = entitySource.getDiscriminatorSource();
		if ( discriminatorSource == null ) {
			return;
		}

		TableSpecification table = entityBinding.locateTable( discriminatorSource.getDiscriminatorRelationalValueSource().getContainingTableName() );
		Value relationalValue;
		if ( ColumnSource.class.isInstance( discriminatorSource.getDiscriminatorRelationalValueSource() ) ) {
			relationalValue = makeColumn(
					( ColumnSource) discriminatorSource.getDiscriminatorRelationalValueSource(),
					DISCRIMINATOR_COLUMN_BINDING_DEFAULTS,
					table,
					currentBindingContext.getMappingDefaults().getDiscriminatorColumnName(), 
					false
			);
		}
		else {
			relationalValue = makeDerivedValue( (DerivedValueSource) discriminatorSource.getDiscriminatorRelationalValueSource(), table );
		}
		EntityDiscriminator discriminator = new EntityDiscriminator(
				relationalValue,
				discriminatorSource.isInserted(),
				discriminatorSource.isForced()
		);

		discriminator.getExplicitHibernateTypeDescriptor().setExplicitTypeName(
				discriminatorSource.getExplicitHibernateTypeName() != null
						? discriminatorSource.getExplicitHibernateTypeName()
						: "string"
		);

		entityBinding.getHierarchyDetails().setEntityDiscriminator( discriminator );
		entityBinding.setDiscriminatorMatchValue( entitySource.getDiscriminatorMatchValue() );

		Type resolvedType = typeHelper.determineHibernateTypeFromDescriptor( discriminator.getExplicitHibernateTypeDescriptor() );
		if ( resolvedType != null ) {
			typeHelper.pushHibernateTypeInformationDown( resolvedType, relationalValue );
		}
	}

	private void bindDiscriminatorValue(SubclassEntitySource entitySource, EntityBinding entityBinding) {
		final String discriminatorValue = entitySource.getDiscriminatorMatchValue();
		if ( discriminatorValue == null ) {
			return;
		}
		entityBinding.setDiscriminatorMatchValue( discriminatorValue );
	}

	private void bindAttributes(
			AttributeSourceContainer attributeSourceContainer,
			AttributeBindingContainer attributeBindingContainer,
			Deque<TableSpecification> tableStack) {
		// todo : we really need the notion of a Stack here for the table from which the columns come for binding these attributes.
		// todo : adding the concept (interface) of a source of attribute metadata would allow reuse of this method for entity, component, unique-key, etc
		// for now, simply assume all columns come from the base table....

		for ( AttributeSource attributeSource : attributeSourceContainer.attributeSources() ) {
			if ( attributeSource.isSingular() ) {
				bindSingularAttribute( (SingularAttributeSource) attributeSource, attributeBindingContainer, tableStack );
			}
			else {
				bindPersistentCollection( (PluralAttributeSource) attributeSource, attributeBindingContainer, tableStack );
			}
		}
	}

	private void bindSingularAttribute(
			SingularAttributeSource attributeSource,
			AttributeBindingContainer attributeBindingContainer,
			Deque<TableSpecification> tableStack) {
		if ( attributeSource.getNature() == SingularAttributeNature.BASIC ) {
			bindBasicAttribute( attributeSource, attributeBindingContainer );
		}
		else if ( attributeSource.getNature() == SingularAttributeNature.COMPONENT ) {
			bindComponentAttribute( (ComponentAttributeSource) attributeSource, attributeBindingContainer, tableStack );
		}
		else if ( attributeSource.getNature() == SingularAttributeNature.ANY ) {
			throw new NotYetImplementedException( "Handling of ANY mappings not yet implemented" );
		}
		else {
			bindToOneAttribute( (ToOneAttributeSource) attributeSource, attributeBindingContainer );
		}
	}

	private SingularAttributeBinding bindToOneAttribute(
			ToOneAttributeSource attributeSource,
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

		final List<RelationalValueBinding> relationalValueBindings = createSimpleRelationalValues(
				attributeSource,
				attributeBindingContainer,
				attribute,
				attributeBindingContainer
						.seekEntityBinding()
						.getPrimaryTable()
		);
		final String propertyAccessorName = Helper.getPropertyAccessorName(
				attributeSource.getPropertyAccessorName(),
				false,
				currentBindingContext.getMappingDefaults().getPropertyAccessorName()
		);
		final MetaAttributeContext metaAttributeContext = buildMetaAttributeContext(
				attributeSource.metaAttributes(),
				attributeBindingContainer.getMetaAttributeContext()
		);

		final SingularAssociationAttributeBinding attributeBinding;
		if ( attributeSource.getNature() == SingularAttributeNature.ONE_TO_ONE ) {
			throw new NotYetImplementedException( "Handling of one-to-one mappings not yet implemented" );
		}
		else if ( attributeSource.getNature() == SingularAttributeNature.MANY_TO_ONE ) {
			attributeBinding = attributeBindingContainer.makeManyToOneAttributeBinding(
					attribute,
					propertyAccessorName,
					attributeSource.isIncludedInOptimisticLocking(),
					attributeSource.isLazy(),
					metaAttributeContext,
					attributeSource.getReferencedEntityName(),
					attributeSource.getReferencedEntityAttributeName(),
					relationalValueBindings
			);
			typeHelper.bindSingularAttributeTypeInformation( attributeSource.getTypeInformation(), attributeBinding );
			resolveToOneInformation(
					attributeSource,
					(ManyToOneAttributeBinding) attributeBinding
			);
			final String referencedEntityName = attributeBinding.getReferencedEntityName();
			EntityBinding referencedEntityBinding = getEntityBinding( referencedEntityName );
			if ( referencedEntityBinding == null ) {
				EntitySource source = sourcesByName.get( referencedEntityName );
				createEntityBinding( source, referencedEntityBinding );
			}
			AttributeBinding referencedAttrBinding = attributeSource.getReferencedEntityAttributeName() != null
					? referencedEntityBinding.locateAttributeBinding( attributeSource.getReferencedEntityAttributeName() )
					: referencedEntityBinding.getHierarchyDetails().getEntityIdentifier().getValueBinding();
			attributeBinding.resolveReference( referencedAttrBinding );
			referencedAttrBinding.addEntityReferencingAttributeBinding( attributeBinding );
		}
		else {
			throw new NotYetImplementedException();
		}

		return attributeBinding;
	}

	private ForeignKey resolveForeignKey(
			ToOneAttributeSource attributeSource,
			AttributeBindingContainer attributeBindingContainer,
			Attribute attribute) {
		final String explicitForeignKeyName = attributeSource.getForeignKeyName();

		// foreign key columns should all come from the same table...
		final TableSpecification sourceTable = attributeBindingContainer.seekEntityBinding().locateTable(
				attributeSource.relationalValueSources().get( 0 ).getContainingTableName()
		);

		ForeignKey foreignKey = sourceTable.locateForeignKey( explicitForeignKeyName );
		if ( foreignKey != null ) {
			// todo : maybe validate columns?
			return foreignKey;
		}

		final List<Value> sourceColumns = new ArrayList<Value>();
		for ( RelationalValueSource relationalValueSource : attributeSource.relationalValueSources() ) {
			sourceColumns.add(
					makeRelationalValue(
							sourceTable,
							relationalValueSource,
							attributeSource,
							attribute
					)
			);
		}

		// todo : pretty sure this is one of the places where the "chasing" approach implemented by john breaks down
		//		even assuming the best case that the target entity and its attributes are fully resolved,
		//		what about the self-referential (Employee<->Manager) case?
		final EntityBinding referencedEntityBinding = metadata.getEntityBinding( attributeSource.getReferencedEntityName() );
		if ( referencedEntityBinding == null ) {
			throw new MappingException(
					"Unable to locate reference entity binding for association : " + attributeSource,
					currentBindingContext.getOrigin()
			);
		}

		// todo : try to look up the reverse FK based on the tables/columns...

		final TableSpecification targetTable;
		final List<Value> targetColumns = new ArrayList<Value>();
		if ( StringHelper.isEmpty( attributeSource.getReferencedEntityAttributeName() ) ) {
			List<RelationalValueBinding> valueBindings = referencedEntityBinding.getHierarchyDetails()
					.getEntityIdentifier()
					.getValueBinding()
					.getRelationalValueBindings();
			targetTable = valueBindings.get( 0 ).getValue().getTable();
			for ( RelationalValueBinding binding : valueBindings ) {
				targetColumns.add( binding.getValue() );
			}
		}
		else {
			// todo : this is very different for JPA which uses @JoinColumn and hbm which uses property-ref
			//		we need a scheme that works for both.
			// 		the processing here uses hbm/property-ref approach
			//
			// the referenced property could conceivably be either a component or a basic
			final SingularAttributeBinding referencedAttributeBinding = (SingularAttributeBinding) referencedEntityBinding.locateAttributeBinding(
					attributeSource.getReferencedEntityAttributeName()
			);
			if ( CompositeAttributeBinding.class.isInstance( referencedAttributeBinding ) ) {
				collectValues( (CompositeAttributeBinding) referencedAttributeBinding, targetColumns );
			}
			else {
				for ( RelationalValueBinding valueBinding :( (BasicAttributeBinding) referencedAttributeBinding ).getRelationalValueBindings() ) {
					targetColumns.add( valueBinding.getValue() );
				}
			}
			targetTable = targetColumns.get( 0 ).getTable();
		}

		if ( sourceColumns.size() != targetColumns.size() ) {
			throw new MappingException(
					"Encountered mismatch in number of columns in foreign key",
					currentBindingContext.getOrigin()
			);
		}
		foreignKey = sourceTable.createForeignKey( targetTable, explicitForeignKeyName );
		for ( int i = 0; i < sourceColumns.size(); i++ ) {
			// todo : handle special case of formulas in key mappings...
			foreignKey.addColumnMapping( (Column) sourceColumns.get(i), (Column) targetColumns.get(i) );
		}

		return foreignKey;
	}

	private void collectValues(CompositeAttributeBinding compositeAttributeBinding, List<Value> targetColumns) {
		for ( AttributeBinding attributeBinding : compositeAttributeBinding.attributeBindings() ) {
			if ( BasicAttributeBinding.class.isInstance( attributeBinding ) ) {
				for ( RelationalValueBinding valueBinding :( (BasicAttributeBinding) attributeBinding ).getRelationalValueBindings() ) {
					targetColumns.add( valueBinding.getValue() );
				}
			}
			else if ( ManyToOneAttributeBinding.class.isInstance( attributeBinding ) ) {
				for ( RelationalValueBinding valueBinding :( (BasicAttributeBinding) attributeBinding ).getRelationalValueBindings() ) {
					targetColumns.add( valueBinding.getValue() );
				}
			}
			else if ( CompositeAttributeBinding.class.isInstance( attributeBinding ) ) {
				collectValues( (CompositeAttributeBinding) attributeBinding, targetColumns );
			}
		}
	}

	private BasicAttributeBinding bindBasicAttribute(
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

		final List<RelationalValueBinding> relationalValueBindings = createSimpleRelationalValues(
				attributeSource,
				attributeBindingContainer,
				attribute,
				attributeBindingContainer
						.seekEntityBinding()
						.getPrimaryTable()
		);
		final String propertyAccessorName = Helper.getPropertyAccessorName(
				attributeSource.getPropertyAccessorName(),
				false,
				currentBindingContext.getMappingDefaults().getPropertyAccessorName()
		);
		final MetaAttributeContext metaAttributeContext = buildMetaAttributeContext(
				attributeSource.metaAttributes(),
				attributeBindingContainer.getMetaAttributeContext()
		);

		final BasicAttributeBinding attributeBinding = attributeBindingContainer.makeBasicAttributeBinding(
				attribute,
				relationalValueBindings,
				propertyAccessorName,
				attributeSource.isIncludedInOptimisticLocking(),
				attributeSource.isLazy(),
				metaAttributeContext,
				attributeSource.getGeneration()
		);
		typeHelper.bindSingularAttributeTypeInformation( attributeSource.getTypeInformation(), attributeBinding );
		return attributeBinding;
	}

	private void bindComponentAttribute(
			ComponentAttributeSource attributeSource,
			AttributeBindingContainer container,
			Deque<TableSpecification> tableStack) {
		final String attributeName = attributeSource.getName();
		SingularAttribute attribute = container.getAttributeContainer().locateCompositeAttribute( attributeName );
		final Composite composite;
		if ( attribute == null ) {
			composite = new Composite(
					attributeSource.getPath(),
					attributeSource.getClassName(),
					attributeSource.getClassReference(),
					null // composition inheritance not YET supported
			);
			attribute = container.getAttributeContainer().createCompositeAttribute( attributeName, composite );
		}
		else {
			composite = (Composite) attribute.getSingularAttributeType();
		}

		final String propertyAccessorName = Helper.getPropertyAccessorName(
				attributeSource.getPropertyAccessorName(),
				false,
				currentBindingContext.getMappingDefaults().getPropertyAccessorName()
		);
		final MetaAttributeContext metaAttributeContext = buildMetaAttributeContext(
				attributeSource.metaAttributes(),
				container.getMetaAttributeContext()
		);

		final SingularAttribute parentReferenceAttribute;
		if ( StringHelper.isNotEmpty( attributeSource.getParentReferenceAttributeName() ) ) {
			parentReferenceAttribute = composite.createSingularAttribute( attributeSource.getParentReferenceAttributeName() );
		}
		else {
			parentReferenceAttribute = null;
		}

		CompositeAttributeBinding compositeAttributeBinding = container.makeComponentAttributeBinding(
				attribute,
				parentReferenceAttribute,
				propertyAccessorName,
				attributeSource.isIncludedInOptimisticLocking(),
				attributeSource.isLazy(),
				metaAttributeContext
		);

		bindAttributes( attributeSource, compositeAttributeBinding, tableStack );
	}

	private void bindPersistentCollection(
			PluralAttributeSource attributeSource,
			AttributeBindingContainer attributeBindingContainer,
			Deque<TableSpecification> tableStack) {
		final PluralAttribute existingAttribute = attributeBindingContainer.getAttributeContainer()
				.locatePluralAttribute( attributeSource.getName() );
		final AbstractPluralAttributeBinding pluralAttributeBinding;

		final String propertyAccessorName = Helper.getPropertyAccessorName(
				attributeSource.getPropertyAccessorName(),
				false,
				currentBindingContext.getMappingDefaults().getPropertyAccessorName()
		);
		final MetaAttributeContext metaAttributeContext = buildMetaAttributeContext(
				attributeSource.metaAttributes(),
				attributeBindingContainer.getMetaAttributeContext()
		);
		if ( attributeSource.getPluralAttributeNature() == PluralAttributeNature.BAG ) {
			final PluralAttribute attribute = existingAttribute != null
					? existingAttribute
					: attributeBindingContainer.getAttributeContainer().createBag( attributeSource.getName() );
			pluralAttributeBinding = attributeBindingContainer.makeBagAttributeBinding(
					attribute,
					convert( attributeSource.getElementSource().getNature() ),
					propertyAccessorName,
					attributeSource.isIncludedInOptimisticLocking(),
					false,
					metaAttributeContext
			);
		}
		else if ( attributeSource.getPluralAttributeNature() == PluralAttributeNature.SET ) {
			final PluralAttribute attribute = existingAttribute != null
					? existingAttribute
					: attributeBindingContainer.getAttributeContainer().createSet( attributeSource.getName() );
			final Comparator comparator = null;
			pluralAttributeBinding = attributeBindingContainer.makeSetAttributeBinding(
					attribute,
					convert( attributeSource.getElementSource().getNature() ),
					propertyAccessorName,
					attributeSource.isIncludedInOptimisticLocking(),
					false,
					metaAttributeContext,
					comparator
			);
		}
		else {
			// todo : implement other collection types
			throw new NotYetImplementedException( "Collections other than bag and set not yet implemented :(" );
		}

		doBasicPluralAttributeBinding( attributeSource, pluralAttributeBinding );

		bindCollectionTable( attributeSource, pluralAttributeBinding );
		bindSortingAndOrdering( attributeSource, pluralAttributeBinding );

		bindCollectionKey( attributeSource, pluralAttributeBinding, tableStack );
		bindCollectionElement( attributeSource, pluralAttributeBinding );
		bindCollectionIndex( attributeSource, pluralAttributeBinding );

		metadata.addCollection( pluralAttributeBinding );
	}

	private void doBasicPluralAttributeBinding(PluralAttributeSource source, AbstractPluralAttributeBinding binding) {
		binding.setFetchTiming( source.getFetchTiming() );
		binding.setFetchStyle( source.getFetchStyle() );

		binding.setCaching( source.getCaching() );

		binding.getHibernateTypeDescriptor().setJavaTypeName(
				source.getPluralAttributeNature().reportedJavaType().getName()
		);
		binding.getHibernateTypeDescriptor().setExplicitTypeName( source.getTypeInformation().getName() );
		binding.getHibernateTypeDescriptor().getTypeParameters().putAll( source.getTypeInformation().getParameters() );

		if ( StringHelper.isNotEmpty( source.getCustomPersisterClassName() ) ) {
			binding.setExplicitPersisterClass(
					currentBindingContext.<CollectionPersister>locateClassByName( source.getCustomPersisterClassName() )
			);
		}

		binding.setCustomLoaderName( source.getCustomLoaderName() );
		binding.setCustomSqlInsert( source.getCustomSqlInsert() );
		binding.setCustomSqlUpdate( source.getCustomSqlUpdate() );
		binding.setCustomSqlDelete( source.getCustomSqlDelete() );
		binding.setCustomSqlDeleteAll( source.getCustomSqlDeleteAll() );

		binding.setWhere( source.getWhere() );

//		doBasicAttributeBinding( source, binding );
	}

//	private CollectionLaziness interpretLaziness(String laziness) {
//		if ( laziness == null ) {
//			laziness = Boolean.toString( metadata.getMappingDefaults().areAssociationsLazy() );
//		}
//
//		if ( "extra".equals( laziness ) ) {
//			return CollectionLaziness.EXTRA;
//		}
//		else if ( "false".equals( laziness ) ) {
//			return CollectionLaziness.NOT;
//		}
//		else if ( "true".equals( laziness ) ) {
//			return CollectionLaziness.LAZY;
//		}
//
//		throw new MappingException(
//				String.format( "Unexpected collection laziness value %s", laziness ),
//				currentBindingContext.getOrigin()
//		);
//	}

	private void bindCollectionTable(
			final PluralAttributeSource attributeSource,
			final AbstractPluralAttributeBinding pluralAttributeBinding) {
		if ( attributeSource.getElementSource().getNature() == org.hibernate.metamodel.spi.source.PluralAttributeElementNature.ONE_TO_MANY ) {
			return;
		}

		TableSpecificationSource tableSpecificationSource = attributeSource.getCollectionTableSpecificationSource();
		if ( TableSource.class.isInstance( tableSpecificationSource ) ) {
			Table collectionTable = createTable(
					(TableSource) tableSpecificationSource,
					new InferredNamingStrategy() {
						@Override
						public String inferredTableName() {
							final EntityBinding owner = pluralAttributeBinding.getContainer().seekEntityBinding();
							final String ownerTableLogicalName = Table.class.isInstance( owner.getPrimaryTable() )
									? Table.class.cast( owner.getPrimaryTable() ).getTableName().getName()
									: null;
							return currentBindingContext.getNamingStrategy().collectionTableName(
									owner.getEntity().getName(),
									ownerTableLogicalName,
									null,	// todo : here
									null,	// todo : and here
									pluralAttributeBinding.getContainer().getPathBase() + '.' + attributeSource.getName()
							);
						}
					}
			);
			pluralAttributeBinding.setCollectionTable( collectionTable );
		}
		else {
			pluralAttributeBinding.setCollectionTable( createInLineView( (InLineViewSource) tableSpecificationSource ) );
		}

		if ( StringHelper.isNotEmpty( attributeSource.getCollectionTableComment() ) ) {
			pluralAttributeBinding.getCollectionTable().addComment( attributeSource.getCollectionTableComment() );
		}

		if ( StringHelper.isNotEmpty( attributeSource.getCollectionTableCheck() ) ) {
			pluralAttributeBinding.getCollectionTable().addCheckConstraint( attributeSource.getCollectionTableCheck() );
		}
	}

	private void bindCollectionKey(
		PluralAttributeSource attributeSource,
		AbstractPluralAttributeBinding pluralAttributeBinding,
		Deque<TableSpecification> tableStack) {

		final PluralAttributeKeySource keySource = attributeSource.getKeySource();
		
		String foreignKeyName = (
				StringHelper.isNotEmpty( keySource.getExplicitForeignKeyName() ) ?
						quoteIdentifier( keySource.getExplicitForeignKeyName() ) :
						null // TODO: is null FK name allowd (is there a default?) 
		);
		pluralAttributeBinding.getPluralAttributeKeyBinding().prepareForeignKey(
				foreignKeyName,
				tableStack.peekLast()
		);
		final ForeignKey foreignKey = pluralAttributeBinding.getPluralAttributeKeyBinding().getForeignKey();

		foreignKey.setDeleteRule( keySource.getOnDeleteAction() );

		if ( keySource.getReferencedEntityAttributeName() == null ) {
			bindCollectionKeyTargetingPrimaryKey( attributeSource.getKeySource(), pluralAttributeBinding );
		}
		else {
			bindCollectionKeyTargetingPropertyRef( attributeSource.getKeySource(), pluralAttributeBinding );
		}

		HibernateTypeDescriptor targetTypeDescriptor = pluralAttributeBinding.getHibernateTypeDescriptor();

	}

	private void bindCollectionKeyTargetingPrimaryKey(
			PluralAttributeKeySource keySource,
			AbstractPluralAttributeBinding pluralAttributeBinding) {

		for ( RelationalValueSource valueSource : keySource.getValueSources() ) {
			if ( ColumnSource.class.isInstance( valueSource ) ) {
				final Column column = makeColumn(
						ColumnSource.class.cast( valueSource ),
						COLL_KEY_COLUMN_BINDING_DEFAULTS,
						pluralAttributeBinding.getCollectionTable(),
						pluralAttributeBinding.getAttribute().getName(),
						true
				);
				pluralAttributeBinding.getPluralAttributeKeyBinding().getForeignKey().addColumn( column );
			}
			else {
				// TODO: deal with formulas???
			}
		}
	}

	private void bindCollectionKeyTargetingPropertyRef(
			PluralAttributeKeySource keySource,
			AbstractPluralAttributeBinding pluralAttributeBinding) {
		final EntityBinding ownerEntityBinding = pluralAttributeBinding.getContainer().seekEntityBinding();
		final AttributeBinding referencedAttributeBinding = ownerEntityBinding.locateAttributeBinding(
				keySource.getReferencedEntityAttributeName()
		);
		final ForeignKey foreignKey = pluralAttributeBinding.getPluralAttributeKeyBinding().getForeignKey();
		if ( ! referencedAttributeBinding.getAttribute().isSingular() ) {
			throw new MappingException(
					String.format(
							"Collection (%s) property-ref is a plural attribute (%s); must be singular.",
							pluralAttributeBinding.getAttribute().getRole(),
							referencedAttributeBinding
					),
					currentBindingContext.getOrigin()
			);
		}
		Iterator<RelationalValueBinding> targetValueBindings = 
				( (SingularAttributeBinding) referencedAttributeBinding ).getRelationalValueBindings().iterator();
		for ( RelationalValueSource valueSource : keySource.getValueSources() ) {
			if ( ! targetValueBindings.hasNext() ) {
				throw new MappingException(
						String.format(
								"More collection key source columns than target columns for collection: %s",
								pluralAttributeBinding.getAttribute().getRole()
						),
						currentBindingContext.getOrigin()
				);
			}
			Value targetValue = targetValueBindings.next().getValue();
			if ( ColumnSource.class.isInstance( valueSource ) ) {
				final ColumnSource columnSource = ColumnSource.class.cast( valueSource );
				final Column column = makeColumn(
						columnSource,
						COLL_KEY_COLUMN_BINDING_DEFAULTS,
						pluralAttributeBinding.getCollectionTable(),
						pluralAttributeBinding.getAttribute().getName(),
						true
				);
				if ( targetValue != null && ! Column.class.isInstance( targetValue ) ) {
					throw new MappingException(
							String.format(
									"Type mismatch between collection key source and target; collection: %s; source column (%s) corresponds with target derived value (%s).",
									pluralAttributeBinding.getAttribute().getRole(),
									columnSource.getName(),
									DerivedValue.class.cast( targetValue ).getExpression()
							),
							currentBindingContext.getOrigin()
					);
				}
				foreignKey.addColumnMapping( column, Column.class.cast( targetValue ) );
			}
			else {
				// TODO: deal with formulas???
			}
		}
		if ( targetValueBindings != null && targetValueBindings.hasNext() ) {
			throw new MappingException(
					String.format(
							"More collection key target columns than source columns for collection: %s",
							pluralAttributeBinding.getAttribute().getRole()
					),
					currentBindingContext.getOrigin()
			);
		}
	}
	private static final ColumnBindingDefaults COLL_KEY_COLUMN_BINDING_DEFAULTS = new ColumnBindingDefaults() {
		@Override
		public boolean areValuesIncludedInInsertByDefault() {
			return true;
		}

		@Override
		public boolean areValuesIncludedInUpdateByDefault() {
			return false;
		}

		@Override
		public boolean areValuesNullableByDefault() {
			return false;
		}
	};

	private void bindCollectionElement(
			PluralAttributeSource attributeSource,
			AbstractPluralAttributeBinding pluralAttributeBinding) {
		final PluralAttributeElementSource elementSource = attributeSource.getElementSource();
		if ( elementSource.getNature() == org.hibernate.metamodel.spi.source.PluralAttributeElementNature.BASIC ) {
			final BasicPluralAttributeElementSource basicElementSource =
					(BasicPluralAttributeElementSource) elementSource;
			final BasicPluralAttributeElementBinding basicCollectionElement =
					(BasicPluralAttributeElementBinding) pluralAttributeBinding.getPluralAttributeElementBinding();
			bindBasicPluralElementRelationalValues(
					basicElementSource,
					basicCollectionElement
			);
			typeHelper.bindPluralAttributeTypeInformation(
					attributeSource,
					pluralAttributeBinding
			);
			return;
		}

// todo : handle cascades
//		final Cascadeable cascadeable = (Cascadeable) binding.getPluralAttributeElementBinding();
//		cascadeable.setCascadeStyles( source.getCascadeStyles() );

		// todo : implement
		throw new NotYetImplementedException(
				String.format(
						"Support for collection elements of type %s not yet implemented",
						elementSource.getNature()
				)
		);
	}

	private void bindBasicPluralElementRelationalValues(
			RelationalValueSourceContainer relationalValueSourceContainer,
			BasicPluralAttributeElementBinding elementBinding) {
		elementBinding.setRelationalValueBindings(
				createSimpleRelationalValues(
						relationalValueSourceContainer,
						elementBinding.getPluralAttributeBinding().getContainer(),
						elementBinding.getPluralAttributeBinding().getAttribute(),
						elementBinding.getPluralAttributeBinding().getCollectionTable()
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

	private PluralAttributeElementNature convert(org.hibernate.metamodel.spi.source.PluralAttributeElementNature pluralAttributeElementNature) {
		return PluralAttributeElementNature.valueOf( pluralAttributeElementNature.name() );
	}

	private EntityBinding getEntityBinding(String entityName) {
	    // Check if binding has already been created
        EntityBinding binding = metadata.getEntityBinding( entityName );
        if ( binding == null ) {
            // Find appropriate source to create binding
            EntitySource source = sourcesByName.get( entityName );
            // Get super entity binding (creating it if necessary using recursive call to this method)
            EntityBinding superBinding = source instanceof SubclassEntitySource
                ? getEntityBinding( ( ( SubclassEntitySource ) source ).superclassEntitySource().getEntityName() )
                : null;
            // Create entity binding
            binding = createEntityBinding( source, superBinding );
            // Create entity binding's sub-entity bindings
            processHierarchySubEntities( source, binding );
        }
        return binding;
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

	private void bindPrimaryTable(EntitySource entitySource, final EntityBinding entityBinding) {
		final TableSpecificationSource tableSpecificationSource = entitySource.getPrimaryTable();
		if ( TableSource.class.isInstance( tableSpecificationSource ) ) {
			final Table table = createTable(
					(TableSource) tableSpecificationSource,
					new InferredNamingStrategy() {
						@Override
						public String inferredTableName() {
							return currentBindingContext.getNamingStrategy()
									.classToTableName( entityBinding.getEntity().getClassName() );
						}
					}
			);
			entityBinding.setPrimaryTable( table );
			// todo : ugh!
			entityBinding.setPrimaryTableName( table.getTableName().getName() );
		}
		else {
			entityBinding.setPrimaryTable( createInLineView( (InLineViewSource) tableSpecificationSource ) );
		}
	}

	private InLineView createInLineView(InLineViewSource inLineViewSource) {
		final Schema.Name databaseSchemaName = Helper.determineDatabaseSchemaName(
				inLineViewSource.getExplicitSchemaName(),
				inLineViewSource.getExplicitCatalogName(),
				currentBindingContext
		);
		final Identifier logicalName = Identifier.toIdentifier( inLineViewSource.getLogicalName() );
		return currentBindingContext.getMetadataImplementor()
				.getDatabase()
				.locateSchema( databaseSchemaName )
				.createInLineView( logicalName, inLineViewSource.getSelectStatement() );
	}

	private static interface InferredNamingStrategy {
		public String inferredTableName();
	}

	private Table createTable(TableSource tableSource, InferredNamingStrategy namingStrategy) {
		String explicitTableNameString = tableSource.getExplicitTableName();
		if ( explicitTableNameString == null ) {
			explicitTableNameString = namingStrategy.inferredTableName();
		}
		explicitTableNameString = quoteIdentifier( explicitTableNameString );
		final Identifier logicalName = Identifier.toIdentifier( explicitTableNameString );

		explicitTableNameString = currentBindingContext.getNamingStrategy().tableName( explicitTableNameString );
		explicitTableNameString = quoteIdentifier( explicitTableNameString );
		final Identifier physicalName = Identifier.toIdentifier( explicitTableNameString );

		final Schema.Name databaseSchemaName = Helper.determineDatabaseSchemaName(
				tableSource.getExplicitSchemaName(),
				tableSource.getExplicitCatalogName(),
				currentBindingContext
		);

		Table table = currentBindingContext.getMetadataImplementor()
				.getDatabase()
				.locateSchema( databaseSchemaName )
				.locateTable( logicalName );
		if ( table == null ) {
			table = currentBindingContext.getMetadataImplementor()
				.getDatabase()
				.locateSchema( databaseSchemaName )
				.createTable( logicalName, physicalName );
		}
		return table;
	}

	private void bindSecondaryTables(EntitySource entitySource, EntityBinding entityBinding) {
		final TableSpecification primaryEntityTable = entityBinding.getPrimaryTable();

		for ( SecondaryTableSource secondaryTableSource : entitySource.getSecondaryTables() ) {
			final TableSpecification secondaryTable;
			final TableSpecificationSource source = secondaryTableSource.getTableSource();
			if ( TableSource.class.isInstance( source ) ) {
				secondaryTable = createTable(
						(TableSource) source,
						new InferredNamingStrategy() {
							@Override
							public String inferredTableName() {
								throw new MappingException(
										"Secondary table must specify explicit name",
										currentBindingContext.getOrigin()
								);
							}
						}
				);
			}
			else {
				secondaryTable = createInLineView( (InLineViewSource) source );
			}

			// todo : really need a concept like SecondaryTableSource in the binding model as well
			//		so that EntityBinding can know the proper foreign key to use to build SQL statements.

			ForeignKey foreignKey = null;
			if ( secondaryTableSource.getForeignKeyName() != null ) {
				foreignKey = secondaryTable.locateForeignKey( secondaryTableSource.getForeignKeyName() );
				if ( foreignKey == null ) {
					foreignKey = secondaryTable.createForeignKey(
							primaryEntityTable,
							secondaryTableSource.getForeignKeyName()
					);
				}
			}
			else {
				// for now lets assume we have to create it, but eventually we should look through the
				//		candidate foreign keys referencing primary table also...
				foreignKey = secondaryTable.createForeignKey( primaryEntityTable, null );
			}

			for ( PrimaryKeyJoinColumnSource joinColumnSource : secondaryTableSource.getJoinColumns() ) {
				// todo : currently we only support columns here, not formulas
				// todo : apply naming strategy to infer missing column name
				Column fkColumn = secondaryTable.locateColumn( joinColumnSource.getColumnName() );
				if ( fkColumn == null ) {
					fkColumn = secondaryTable.createColumn( joinColumnSource.getColumnName() );
					if ( joinColumnSource.getColumnDefinition() != null ) {
						fkColumn.setSqlType( joinColumnSource.getColumnDefinition() );
					}
				}
				if ( joinColumnSource.getReferencedColumnName() != null ) {
					final Column referencedColumn = primaryEntityTable.locateColumn(
							joinColumnSource.getReferencedColumnName()
					);
					foreignKey.addColumnMapping( fkColumn, referencedColumn );
				}
				else {
					foreignKey.addColumn( fkColumn );
				}
			}

			entityBinding.addSecondaryTable( new SecondaryTable( secondaryTable, foreignKey ) );
		}
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

	private List<RelationalValueBinding> createSimpleRelationalValues(
			RelationalValueSourceContainer relationalValueSourceContainer,
			AttributeBindingContainer attributeBindingContainer,
			Attribute attribute,
			TableSpecification defaultTable) {

		List<RelationalValueBinding> valueBindings = new ArrayList<RelationalValueBinding>();

		if ( !relationalValueSourceContainer.relationalValueSources().isEmpty() ) {
			for ( RelationalValueSource valueSource : relationalValueSourceContainer.relationalValueSources() ) {
				TableSpecification resolvedTable = defaultTable;
				if ( valueSource.getContainingTableName() != null ) {
					resolvedTable = attributeBindingContainer
							.seekEntityBinding()
							.locateTable( valueSource.getContainingTableName() );
				}
				if ( ColumnSource.class.isInstance( valueSource ) ) {
					final ColumnSource columnSource = ColumnSource.class.cast( valueSource );
					final Column column = 
							makeColumn(
									columnSource, 
									relationalValueSourceContainer,
									resolvedTable,
									attribute.getName(), 
									true 
							);
					valueBindings.add(
							new RelationalValueBinding(
									column,
									decode( columnSource.isIncludedInInsert(), relationalValueSourceContainer.areValuesIncludedInInsertByDefault() ),
									decode(
											columnSource.isIncludedInUpdate(),
											relationalValueSourceContainer.areValuesIncludedInUpdateByDefault()
									)
							)
					);
				}
				else {
					valueBindings.add(
							new RelationalValueBinding(
									makeDerivedValue( ( (DerivedValueSource) valueSource ), resolvedTable )
							)
					);
				}
			}
		}
		else {
			String name = metadata.getOptions()
					.getNamingStrategy()
					.propertyToColumnName( attribute.getName() );
			name = quoteIdentifier( name );
			Column column = defaultTable.locateOrCreateColumn( name );
			column.setNullable( relationalValueSourceContainer.areValuesNullableByDefault() );
			valueBindings.add( new RelationalValueBinding( column ) );
		}
		return valueBindings;
	}

	private String quoteIdentifier(String string) {
		return currentBindingContext.isGloballyQuotedIdentifiers() ? StringHelper.quote( string ) : string;
	}

	private Value makeRelationalValue(
			TableSpecification table,
			RelationalValueSource valueSource,
			ColumnBindingDefaults columnBindingDefaults,
			Attribute attribute) {
		if ( ColumnSource.class.isInstance( valueSource ) ) {
			final ColumnSource columnSource = ColumnSource.class.cast( valueSource );
			return makeColumn(
					columnSource,
					columnBindingDefaults, 
					table, 
					attribute.getName(),
					true
			);
		}
		else {
			return makeDerivedValue( (DerivedValueSource) valueSource, table );
		}
	}

	private Column makeColumn(
			ColumnSource columnSource,
			ColumnBindingDefaults columnBindingDefaults,
			TableSpecification table,
			String defaultName, 
			boolean isDefaultAttributeName) {
		if ( columnSource.getName() == null && defaultName == null ) {
			throw new MappingException(
					"Cannot resolve name for column because the no name was specified and default name is null.",
					currentBindingContext.getOrigin()
			);
		}
		String name;
		if ( columnSource.getName() != null ) {
			name = metadata.getOptions().getNamingStrategy().columnName( columnSource.getName() );
		}
		else if ( isDefaultAttributeName ) {
			name = metadata.getOptions().getNamingStrategy().propertyToColumnName( defaultName );
		}
		else {
			name = metadata.getOptions().getNamingStrategy().columnName( defaultName );
		}
		String resolvedColumnName = quoteIdentifier( name );
		final Column column = table.locateOrCreateColumn( resolvedColumnName );
		column.setNullable(
				decode( columnSource.isNullable(), columnBindingDefaults.areValuesNullableByDefault() )
		);
		column.setDefaultValue( columnSource.getDefaultValue() );
		column.setSqlType( columnSource.getSqlType() );
		column.setSize( columnSource.getSize() );
		column.setJdbcDataType( columnSource.getDatatype() );
		column.setReadFragment( columnSource.getReadFragment() );
		column.setWriteFragment( columnSource.getWriteFragment() );
		column.setUnique( columnSource.isUnique() );
		column.setCheckCondition( columnSource.getCheckCondition() );
		column.setComment( columnSource.getComment() );
		return column;
	}

	private boolean decode(TruthValue truthValue, boolean defaultValue) {
		switch ( truthValue ) {
			case FALSE: {
				return false;
			}
			case TRUE: {
				return true;
			}
			default: {
				return defaultValue;
			}
		}
	}

	private DerivedValue makeDerivedValue(DerivedValueSource derivedValueSource, TableSpecification table) {
		return table.locateOrCreateDerivedValue( derivedValueSource.getExpression() );
	}

	private void processFetchProfiles(EntitySource entitySource, EntityBinding entityBinding) {
		// todo : process the entity-local fetch-profile declaration
	}

}
