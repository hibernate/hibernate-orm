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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.metamodel.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.CollectionElementNature;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.EntityDiscriminator;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.binding.MetaAttribute;
import org.hibernate.metamodel.binding.SimpleSingularAttributeBinding;
import org.hibernate.metamodel.binding.SimpleValueBinding;
import org.hibernate.metamodel.binding.SingularAttributeBinding;
import org.hibernate.metamodel.binding.TypeDef;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.PluralAttribute;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.Identifier;
import org.hibernate.metamodel.relational.Schema;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.metamodel.relational.Tuple;
import org.hibernate.metamodel.source.LocalBindingContext;
import org.hibernate.metamodel.source.MetaAttributeContext;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.metamodel.source.hbm.Helper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityTuplizer;

/**
 * The common binder shared between annotations and {@code hbm.xml} processing.
 * <p/>
 * The API consists of {@link #Binder} and {@link #processEntityHierarchy}
 *
 * @author Steve Ebersole
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

		currentBindingContext = entitySource.getBindingContext();
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
			if ( currentInheritanceType == InheritanceType.SINGLE_TABLE ) {
				return makeDiscriminatedSubclassBinding( (SubclassEntitySource) entitySource, superEntityBinding );
			}
			else if ( currentInheritanceType == InheritanceType.JOINED ) {
				return makeJoinedSubclassBinding( (SubclassEntitySource) entitySource, superEntityBinding );
			}
			else if ( currentInheritanceType == InheritanceType.TABLE_PER_CLASS ) {
				return makeUnionedSubclassBinding( (SubclassEntitySource) entitySource, superEntityBinding );
			}
			else {
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
				null
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

		return entityBinding;
	}

	private EntityBinding makeDiscriminatedSubclassBinding(SubclassEntitySource entitySource, EntityBinding superEntityBinding) {
		final EntityBinding entityBinding = buildBasicEntityBinding( entitySource, superEntityBinding );

		entityBinding.setBaseTable( superEntityBinding.getBaseTable() );

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
			}
			case AGGREGATED_COMPOSITE: {
				// composite id with an actual component class
			}
			case COMPOSITE: {
				// what we used to term an "embedded composite identifier", which is not tobe confused with the JPA
				// term embedded. Specifically a composite id where there is no component class, though there may
				// be a @IdClass :/
			}
		}
	}

	private void bindSimpleIdentifier(SimpleIdentifierSource identifierSource, EntityBinding entityBinding) {
		final SimpleSingularAttributeBinding idAttributeBinding = doBasicSingularAttributeBindingCreation(
				identifierSource.getIdentifierAttributeSource(), entityBinding
		);

		entityBinding.getHierarchyDetails().getEntityIdentifier().setValueBinding( idAttributeBinding );
		entityBinding.getHierarchyDetails().getEntityIdentifier().setIdGenerator( identifierSource.getIdentifierGeneratorDescriptor() );

		final org.hibernate.metamodel.relational.Value relationalValue = idAttributeBinding.getValue();

		if ( SimpleValue.class.isInstance( relationalValue ) ) {
			if ( !Column.class.isInstance( relationalValue ) ) {
				// this should never ever happen..
				throw new AssertionFailure( "Simple-id was not a column." );
			}
			entityBinding.getBaseTable().getPrimaryKey().addColumn( Column.class.cast( relationalValue ) );
		}
		else {
			for ( SimpleValue subValue : ( (Tuple) relationalValue ).values() ) {
				if ( Column.class.isInstance( subValue ) ) {
					entityBinding.getBaseTable().getPrimaryKey().addColumn( Column.class.cast( subValue ) );
				}
			}
		}
	}

	private void bindVersion(EntityBinding entityBinding, RootEntitySource entitySource) {
		final SingularAttributeSource versioningAttributeSource = entitySource.getVersioningAttributeSource();
		if ( versioningAttributeSource == null ) {
			return;
		}

		SimpleSingularAttributeBinding attributeBinding = doBasicSingularAttributeBindingCreation(
				versioningAttributeSource, entityBinding
		);
		entityBinding.getHierarchyDetails().setVersioningAttributeBinding( attributeBinding );
	}

	private void bindDiscriminator(RootEntitySource entitySource, EntityBinding entityBinding) {
		final DiscriminatorSource discriminatorSource = entitySource.getDiscriminatorSource();
		if ( discriminatorSource == null ) {
			return;
		}

		SimpleSingularAttributeBinding attributeBinding = doBasicSingularAttributeBindingCreation(
				discriminatorSource, entityBinding
		);
		EntityDiscriminator discriminator = new EntityDiscriminator();
		discriminator.setValueBinding( attributeBinding );
		discriminator.setInserted( discriminatorSource.isInserted() );
		discriminator.setForced( discriminatorSource.isForced() );
		entityBinding.setEntityDiscriminator( discriminator );
	}

	private void bindDiscriminatorValue(SubclassEntitySource entitySource, EntityBinding entityBinding) {
		final String discriminatorValue = entitySource.getDiscriminatorValue();
		if ( discriminatorValue == null ) {
			return;
		}
		entityBinding.setDiscriminatorValue( discriminatorValue );
	}

	private void bindAttributes(AttributeSourceContainer attributeSourceContainer, EntityBinding entityBinding) {
		// todo : we really need the notion of a Stack here for the table from which the columns come for binding these attributes.
		// todo : adding the concept (interface) of a source of attribute metadata would allow reuse of this method for entity, component, unique-key, etc
		// for now, simply assume all columns come from the base table....

		for ( AttributeSource attributeSource : attributeSourceContainer.attributeSources() ) {
			if ( attributeSource.isSingular() ) {
				final SingularAttributeSource singularAttributeSource = (SingularAttributeSource) attributeSource;
				if ( singularAttributeSource.getNature() == SingularAttributeNature.COMPONENT ) {
					throw new NotYetImplementedException( "Component binding not yet implemented :(" );
				}
				else {
					doBasicSingularAttributeBindingCreation( singularAttributeSource, entityBinding );
				}
			}
			else {
				bindPersistentCollection( (PluralAttributeSource) attributeSource, entityBinding );
			}
		}
	}

	private void bindPersistentCollection(PluralAttributeSource attributeSource, EntityBinding entityBinding) {
		final AbstractPluralAttributeBinding pluralAttributeBinding;
		if ( attributeSource.getPluralAttributeNature() == PluralAttributeNature.BAG ) {
			final PluralAttribute pluralAttribute = entityBinding.getEntity()
					.locateOrCreateBag( attributeSource.getName() );
			pluralAttributeBinding = entityBinding.makeBagAttributeBinding(
					pluralAttribute,
					convert( attributeSource.getPluralAttributeElementNature() )
			);
		}
		else {
			// todo : implement other collection types
			throw new NotYetImplementedException( "Collections other than bag not yet implmented :(" );
		}

		doBasicAttributeBinding( attributeSource, pluralAttributeBinding );
	}

	private void doBasicAttributeBinding(AttributeSource attributeSource, AttributeBinding attributeBinding) {
		attributeBinding.setPropertyAccessorName( attributeSource.getPropertyAccessorName() );
		attributeBinding.setIncludedInOptimisticLocking( attributeSource.isIncludedInOptimisticLocking() );
	}

	private CollectionElementNature convert(PluralAttributeElementNature pluralAttributeElementNature) {
		return CollectionElementNature.valueOf( pluralAttributeElementNature.name() );
	}

	private SimpleSingularAttributeBinding doBasicSingularAttributeBindingCreation(
			SingularAttributeSource attributeSource,
			EntityBinding entityBinding) {
		final SingularAttribute attribute = attributeSource.isVirtualAttribute()
				? entityBinding.getEntity().locateOrCreateVirtualAttribute( attributeSource.getName() )
				: entityBinding.getEntity().locateOrCreateSingularAttribute( attributeSource.getName() );

		final SimpleSingularAttributeBinding attributeBinding;
		if ( attributeSource.getNature() == SingularAttributeNature.BASIC ) {
			attributeBinding = entityBinding.makeSimpleAttributeBinding( attribute );
			resolveTypeInformation( attributeSource.getTypeInformation(), attributeBinding );
		}
		else if ( attributeSource.getNature() == SingularAttributeNature.MANY_TO_ONE ) {
			attributeBinding = entityBinding.makeManyToOneAttributeBinding( attribute );
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
				buildMetaAttributeContext( attributeSource.metaAttributes(), entityBinding.getMetaAttributeContext() )
		);

		return attributeBinding;
	}

	private void resolveTypeInformation(ExplicitHibernateTypeSource typeSource, SimpleSingularAttributeBinding attributeBinding) {
		final Class<?> attributeJavaType = determineJavaType( attributeBinding.getAttribute() );
		if ( attributeJavaType != null ) {
			attributeBinding.getHibernateTypeDescriptor().setJavaTypeName( attributeJavaType.getName() );
			attributeBinding.getAttribute()
					.resolveType( currentBindingContext.makeJavaType( attributeJavaType.getName() ) );
		}

		final String explicitTypeName = typeSource.getName();
		if ( explicitTypeName != null ) {
			final TypeDef typeDef = currentBindingContext.getMetadataImplementor()
					.getTypeDefinition( explicitTypeName );
			if ( typeDef != null ) {
				attributeBinding.getHibernateTypeDescriptor().setExplicitTypeName( typeDef.getTypeClass() );
				attributeBinding.getHibernateTypeDescriptor().getTypeParameters().putAll( typeDef.getParameters() );
			}
			else {
				attributeBinding.getHibernateTypeDescriptor().setExplicitTypeName( explicitTypeName );
			}
			final Map<String, String> parameters = typeSource.getParameters();
			if ( parameters != null ) {
				attributeBinding.getHibernateTypeDescriptor().getTypeParameters().putAll( parameters );
			}
		}
		else {
			if ( attributeJavaType == null ) {
				// we will have problems later determining the Hibernate Type to use.  Should we throw an
				// exception now?  Might be better to get better contextual info
			}
		}
	}

	private Class<?> determineJavaType(final Attribute attribute) {
		try {
			final Class ownerClass = attribute.getAttributeContainer().getClassReference();
			AttributeJavaTypeDeterminerDelegate delegate = new AttributeJavaTypeDeterminerDelegate( attribute.getName() );
			BeanInfoHelper.visitBeanInfo( ownerClass, delegate );
			return delegate.javaType;
		}
		catch ( Exception ignore ) {
			// todo : log it?
		}
		return null;
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

	private void resolveToOneInformation(ToOneAttributeSource attributeSource, ManyToOneAttributeBinding attributeBinding) {
		final String referencedEntityName = attributeSource.getReferencedEntityName() != null
				? attributeSource.getReferencedEntityName()
				: attributeBinding.getAttribute().getSingularAttributeType().getClassName();
		attributeBinding.setReferencedEntityName( referencedEntityName );
		// todo : we should consider basing references on columns instead of property-ref, which would require a resolution (later) of property-ref to column names
		attributeBinding.setReferencedAttributeName( attributeSource.getReferencedEntityAttributeName() );

		attributeBinding.setCascadeStyles( attributeSource.getCascadeStyles() );
		attributeBinding.setFetchMode( attributeSource.getFetchMode() );
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
		final String schemaName = StringHelper.isEmpty( tableSource.getExplicitSchemaName() )
				? currentBindingContext.getMappingDefaults().getSchemaName()
				: currentBindingContext.getMetadataImplementor().getOptions().isGloballyQuotedIdentifiers()
				? StringHelper.quote( tableSource.getExplicitSchemaName() )
				: tableSource.getExplicitSchemaName();
		final String catalogName = StringHelper.isEmpty( tableSource.getExplicitCatalogName() )
				? currentBindingContext.getMappingDefaults().getCatalogName()
				: currentBindingContext.getMetadataImplementor().getOptions().isGloballyQuotedIdentifiers()
				? StringHelper.quote( tableSource.getExplicitCatalogName() )
				: tableSource.getExplicitCatalogName();

		String tableName = tableSource.getExplicitTableName();
		if ( StringHelper.isEmpty( tableName ) ) {
			tableName = currentBindingContext.getNamingStrategy()
					.classToTableName( entityBinding.getEntity().getClassName() );
		}
		else {
			tableName = currentBindingContext.getNamingStrategy().tableName( tableName );
		}
		if ( currentBindingContext.isGloballyQuotedIdentifiers() ) {
			tableName = StringHelper.quote( tableName );
		}

		final org.hibernate.metamodel.relational.Table table = currentBindingContext.getMetadataImplementor()
				.getDatabase()
				.getSchema( new Schema.Name( schemaName, catalogName ) )
				.locateOrCreateTable( Identifier.toIdentifier( tableName ) );

		entityBinding.setBaseTable( table );
	}

	private void bindSecondaryTables(EntitySource entitySource, EntityBinding entityBinding) {
		// todo : implement
	}

	private void bindTableUniqueConstraints(EntitySource entitySource, EntityBinding entityBinding) {
		// todo : implement
	}

	private void bindRelationalValues(
			RelationalValueSourceContainer relationalValueSourceContainer,
			SingularAttributeBinding attributeBinding) {

		List<SimpleValueBinding> valueBindings = new ArrayList<SimpleValueBinding>();

		if ( relationalValueSourceContainer.relationalValueSources().size() > 0 ) {
			for ( RelationalValueSource valueSource : relationalValueSourceContainer.relationalValueSources() ) {
				final TableSpecification table = attributeBinding.getEntityBinding()
						.getTable( valueSource.getContainingTableName() );

				if ( ColumnSource.class.isInstance( valueSource ) ) {
					final ColumnSource columnSource = ColumnSource.class.cast( valueSource );
					final Column column = table.locateOrCreateColumn( columnSource.getName() );
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
									table.locateOrCreateDerivedValue( ( (DerivedValueSource) valueSource ).getExpression() )
							)
					);
				}
			}
		}
		else {
			final String name = metadata.getOptions()
					.getNamingStrategy()
					.propertyToColumnName( attributeBinding.getAttribute().getName() );
			final SimpleValueBinding valueBinding = new SimpleValueBinding();
			valueBindings.add(
					new SimpleValueBinding(
							attributeBinding.getEntityBinding().getBaseTable().locateOrCreateColumn( name )
					)
			);
		}
		attributeBinding.setSimpleValueBindings( valueBindings );
	}

	private void processFetchProfiles(EntitySource entitySource, EntityBinding entityBinding) {
		// todo : process the entity-local fetch-profile declaration
	}

}
