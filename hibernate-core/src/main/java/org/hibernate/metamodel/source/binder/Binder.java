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
import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.binding.MetaAttribute;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.binding.TypeDef;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.domain.SingularAttribute;
import org.hibernate.metamodel.relational.Column;
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
 * @author Steve Ebersole
 */
public class Binder {
	private final MetadataImplementor metadata;
	private final List<String> processedEntityNames;

	private InheritanceType currentInheritanceType;
	private LocalBindingContext currentBindingContext;

	public Binder(MetadataImplementor metadata, List<String> processedEntityNames) {
		this.metadata = metadata;
		this.processedEntityNames = processedEntityNames;
	}

	public void processEntityHierarchy(EntityHierarchy entityHierarchy) {
		currentInheritanceType = entityHierarchy.getHierarchyInheritanceType();
		EntityBinding rootEntityBinding = createEntityBinding( entityHierarchy.getRootEntitySource(), null );
		if ( currentInheritanceType != InheritanceType.NO_INHERITANCE ) {
			processHierarchySubEntities( entityHierarchy.getRootEntitySource(), rootEntityBinding );
		}
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

		bindTableUniqueConstraints( entityBinding );

		return entityBinding;
	}

	private EntityBinding createBasicEntityBinding(EntitySource entitySource, EntityBinding superEntityBinding) {
		if ( superEntityBinding == null ) {
			return makeRootEntityBinding( (RootEntitySource) entitySource );
		}
		else {
			if ( currentInheritanceType == InheritanceType.SINGLE_TABLE ) {
				return makeDiscriminatedSubclassBinding( entitySource, superEntityBinding );
			}
			else if ( currentInheritanceType == InheritanceType.JOINED ) {
				return makeJoinedSubclassBinding( entitySource, superEntityBinding );
			}
			else if ( currentInheritanceType == InheritanceType.TABLE_PER_CLASS ) {
				return makeUnionedSubclassBinding( entitySource, superEntityBinding );
			}
			else {
				// extreme internal error!
				throw new AssertionFailure( "Internal condition failure" );
			}
		}
	}

	private EntityBinding makeRootEntityBinding(RootEntitySource entitySource) {
		final EntityBinding entityBinding = new EntityBinding();
		entityBinding.setInheritanceType( currentInheritanceType );
		entityBinding.setRoot( true );

		final EntityMode entityMode = entitySource.getEntityMode();
		entityBinding.setEntityMode( entityMode );

		final String entityName = entitySource.getEntityName();
		final String className = entityMode == EntityMode.POJO ? entitySource.getClassName() : null;

		final Entity entity = new Entity(
				entityName,
				className,
				currentBindingContext.makeClassReference( className ),
				null
		);
		entityBinding.setEntity( entity );

		performBasicEntityBind( entitySource, entityBinding );

		bindPrimaryTable( entityBinding, entitySource );

		bindIdentifier( entitySource, entityBinding );
		bindVersion( entityBinding, entitySource );
		bindDiscriminator( entitySource, entityBinding );

		entityBinding.setMutable( entitySource.isMutable() );
		entityBinding.setExplicitPolymorphism( entitySource.isExplicitPolymorphism() );
		entityBinding.setWhereFilter( entitySource.getWhere() );
		entityBinding.setRowId( entitySource.getRowId() );
		entityBinding.setOptimisticLockStyle( entitySource.getOptimisticLockStyle() );
		entityBinding.setCaching( entitySource.getCaching() );

		return entityBinding;
	}

	private EntityBinding makeDiscriminatedSubclassBinding(EntitySource entitySource, EntityBinding superEntityBinding) {
		return null;  //To change body of created methods use File | Settings | File Templates.
	}

	private EntityBinding makeJoinedSubclassBinding(EntitySource entitySource, EntityBinding superEntityBinding) {
		return null;  //To change body of created methods use File | Settings | File Templates.
	}

	private EntityBinding makeUnionedSubclassBinding(EntitySource entitySource, EntityBinding superEntityBinding) {
		return null;  //To change body of created methods use File | Settings | File Templates.
	}

	private void performBasicEntityBind(EntitySource entitySource, EntityBinding entityBinding) {
		entityBinding.setJpaEntityName( entitySource.getJpaEntityName() );

		if ( entityBinding.getEntityMode() == EntityMode.POJO ) {
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
			entityBinding.setCustomEntityTuplizerClass( currentBindingContext.<EntityTuplizer>locateClassByName( customTuplizerClassName ) );
		}

		final String customPersisterClassName = entitySource.getCustomPersisterClassName();
		if ( customPersisterClassName != null ) {
			entityBinding.setCustomEntityPersisterClass( currentBindingContext.<EntityPersister>locateClassByName( customPersisterClassName ) );
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
	}

	private void bindIdentifier(RootEntitySource entitySource, EntityBinding entityBinding) {
		if ( entitySource.getIdentifierSource() == null ) {
			throw new AssertionFailure( "Expecting identifier information on root entity descriptor" );
		}
		switch ( entitySource.getIdentifierSource().getNature() ) {
			case SIMPLE: {
				bindSimpleIdentifier( (SimpleIdentifierSource) entitySource.getIdentifierSource(), entityBinding );
			}
			case AGGREGATED_COMPOSITE: {

			}
			case COMPOSITE: {
			}
		}
	}

	private void bindSimpleIdentifier(SimpleIdentifierSource identifierSource, EntityBinding entityBinding) {
		final SimpleAttributeBinding idAttributeBinding = doBasicSingularAttributeBindingCreation(
				identifierSource.getIdentifierAttributeSource(), entityBinding
		);

		entityBinding.getEntityIdentifier().setValueBinding( idAttributeBinding );
		entityBinding.getEntityIdentifier().setIdGenerator( identifierSource.getIdentifierGeneratorDescriptor() );

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

		SimpleAttributeBinding attributeBinding = doBasicSingularAttributeBindingCreation(
				versioningAttributeSource, entityBinding
		);
		entityBinding.setVersionBinding( attributeBinding );
	}

	private void bindDiscriminator(RootEntitySource entitySource, EntityBinding entityBinding) {
		//To change body of created methods use File | Settings | File Templates.
	}

	private void bindAttributes(EntitySource entitySource, EntityBinding entityBinding) {
		for ( AttributeSource attributeSource : entitySource.attributeSources() ) {
			if ( attributeSource.isSingular() ) {
				doBasicSingularAttributeBindingCreation( (SingularAttributeSource) attributeSource, entityBinding );
			}
			// todo : components and collections
		}
	}

	private SimpleAttributeBinding doBasicSingularAttributeBindingCreation(
			SingularAttributeSource attributeSource,
			EntityBinding entityBinding) {
		final SingularAttribute attribute = attributeSource.isVirtualAttribute()
				? entityBinding.getEntity().locateOrCreateVirtualAttribute( attributeSource.getName() )
				: entityBinding.getEntity().locateOrCreateSingularAttribute( attributeSource.getName() );

		final SimpleAttributeBinding attributeBinding;
		if ( attributeSource.getNature() == SingularAttributeNature.BASIC ) {
			attributeBinding = entityBinding.makeSimpleAttributeBinding( attribute );
			resolveTypeInformation( attributeSource.getTypeInformation(), attributeBinding );
		}
		else if ( attributeSource.getNature() == SingularAttributeNature.MANY_TO_ONE ) {
			attributeBinding = entityBinding.makeManyToOneAttributeBinding( attribute );
			resolveTypeInformation( attributeSource.getTypeInformation(), attributeBinding );
			resolveToOneReferenceInformation( (ToOneAttributeSource) attributeSource, (ManyToOneAttributeBinding) attributeBinding );
		}
		else {
			throw new NotYetImplementedException();
		}

		attributeBinding.setInsertable( attributeSource.isInsertable() );
		attributeBinding.setUpdatable( attributeSource.isUpdatable() );
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

		final org.hibernate.metamodel.relational.Value relationalValue = makeValue( attributeSource, attributeBinding );
		attributeBinding.setValue( relationalValue );

		attributeBinding.setMetaAttributeContext(
				buildMetaAttributeContext( attributeSource.metaAttributes(), entityBinding.getMetaAttributeContext() )
		);

		return attributeBinding;
	}

	private void resolveTypeInformation(ExplicitHibernateTypeSource typeSource, SimpleAttributeBinding attributeBinding) {
		final Class<?> attributeJavaType = determineJavaType( attributeBinding.getAttribute() );
		if ( attributeJavaType != null ) {
			attributeBinding.getHibernateTypeDescriptor().setJavaTypeName( attributeJavaType.getName() );
			attributeBinding.getAttribute().resolveType( currentBindingContext.makeJavaType( attributeJavaType.getName() ) );
		}

		final String explicitTypeName = typeSource.getName();
		if ( explicitTypeName != null ) {
			final TypeDef typeDef = currentBindingContext.getMetadataImplementor().getTypeDefinition( explicitTypeName );
			if ( typeDef != null ) {
				attributeBinding.getHibernateTypeDescriptor().setExplicitTypeName( typeDef.getTypeClass() );
				attributeBinding.getHibernateTypeDescriptor().getTypeParameters().putAll( typeDef.getParameters() );
			}
			else {
				attributeBinding.getHibernateTypeDescriptor().setExplicitTypeName( explicitTypeName );
			}
			final Map<String,String> parameters = typeSource.getParameters();
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

	private void resolveToOneReferenceInformation(ToOneAttributeSource attributeSource, ManyToOneAttributeBinding attributeBinding) {
		final String referencedEntityName = attributeSource.getReferencedEntityName() != null
				? attributeSource.getReferencedEntityName()
				: attributeBinding.getAttribute().getSingularAttributeType().getClassName();
		attributeBinding.setReferencedEntityName( referencedEntityName );
		attributeBinding.setReferencedAttributeName( attributeSource.getReferencedEntityAttributeName() );
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

	private org.hibernate.metamodel.relational.Value makeValue(
			RelationValueMetadataSource relationValueMetadataSource,
			SimpleAttributeBinding attributeBinding) {

		// todo : to be completely correct, we need to know which table the value belongs to.
		// 		There is a note about this somewhere else with ideas on the subject.
		//		For now, just use the entity's base table.
		final TableSpecification table = attributeBinding.getEntityBinding().getBaseTable();

		if ( relationValueMetadataSource.relationalValueSources().size() > 0 ) {
			List<SimpleValue> values = new ArrayList<SimpleValue>();
			for ( RelationalValueSource valueSource : relationValueMetadataSource.relationalValueSources() ) {
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
					values.add( column );
				}
				else {
					values.add( table.locateOrCreateDerivedValue( ( (DerivedValueSource) valueSource ).getExpression() ) );
				}
			}
			if ( values.size() == 1 ) {
				return values.get( 0 );
			}
			Tuple tuple = new Tuple( table, null );
			for ( SimpleValue value : values ) {
				tuple.addValue( value );
			}
			return tuple;
		}
		else {
			// assume a column named based on the NamingStrategy
			final String name = metadata.getOptions()
					.getNamingStrategy()
					.propertyToColumnName( attributeBinding.getAttribute().getName() );
			return table.locateOrCreateColumn( name );
		}
	}

}
