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
package org.hibernate.metamodel.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jboss.logging.Logger;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.TruthValue;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SyntheticAttributeHelper;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.internal.HibernateTypeHelper.ReflectedCollectionJavaTypes;
import org.hibernate.metamodel.internal.source.hbm.ListAttributeSource;
import org.hibernate.metamodel.internal.source.hbm.MapAttributeSource;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.BackRefAttributeBinding;
import org.hibernate.metamodel.spi.binding.BagBinding;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicPluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.BasicPluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityDiscriminator;
import org.hibernate.metamodel.spi.binding.EntityVersion;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.IdGenerator;
import org.hibernate.metamodel.spi.binding.IndexedPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.metamodel.spi.binding.ListBinding;
import org.hibernate.metamodel.spi.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.MapBinding;
import org.hibernate.metamodel.spi.binding.MetaAttribute;
import org.hibernate.metamodel.spi.binding.OneToManyPluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementNature;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeKeyBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SecondaryTable;
import org.hibernate.metamodel.spi.binding.SetBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.binding.TypeDefinition;
import org.hibernate.metamodel.spi.domain.Attribute;
import org.hibernate.metamodel.spi.domain.Composite;
import org.hibernate.metamodel.spi.domain.Entity;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.AbstractValue;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.DerivedValue;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.JdbcDataType;
import org.hibernate.metamodel.spi.relational.PrimaryKey;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.UniqueKey;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.AggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.AttributeSourceContainer;
import org.hibernate.metamodel.spi.source.BasicPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.ColumnSource;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.ConstraintSource;
import org.hibernate.metamodel.spi.source.DerivedValueSource;
import org.hibernate.metamodel.spi.source.DiscriminatorSource;
import org.hibernate.metamodel.spi.source.EntityHierarchy;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.ForeignKeyContributingSource;
import org.hibernate.metamodel.spi.source.IdentifierSource;
import org.hibernate.metamodel.spi.source.InLineViewSource;
import org.hibernate.metamodel.spi.source.IndexedPluralAttributeSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MappingDefaults;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.MetadataImplementor;
import org.hibernate.metamodel.spi.source.MultiTenancySource;
import org.hibernate.metamodel.spi.source.NonAggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.spi.source.OneToManyPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.Orderable;
import org.hibernate.metamodel.spi.source.PluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.PluralAttributeKeySource;
import org.hibernate.metamodel.spi.source.PluralAttributeNature;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.RelationalValueSourceContainer;
import org.hibernate.metamodel.spi.source.RootEntitySource;
import org.hibernate.metamodel.spi.source.SecondaryTableSource;
import org.hibernate.metamodel.spi.source.SimpleIdentifierSource;
import org.hibernate.metamodel.spi.source.SingularAttributeNature;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;
import org.hibernate.metamodel.spi.source.Sortable;
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
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import static org.hibernate.engine.spi.SyntheticAttributeHelper.SYNTHETIC_COMPOSITE_ID_ATTRIBUTE_NAME;

/**
 * The common binder shared between annotations and {@code hbm.xml} processing.
 * <p/>
 * The API consists of {@link #Binder(MetadataImplementor, IdentifierGeneratorFactory)} and {@link #bindEntities(Iterable)}
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Gail Badner
 */
public class Binder {
	private static final Logger log = Logger.getLogger( Binder.class );

	private final MetadataImplementor metadata;
	private final IdentifierGeneratorFactory identifierGeneratorFactory;
	private final ObjectNameNormalizer nameNormalizer;
	private final HashMap< String, EntitySource > entitySourcesByName = new HashMap< String, EntitySource >();
	private final HashMap< RootEntitySource, EntityHierarchy > entityHierarchiesByRootEntitySource =
			new HashMap< RootEntitySource, EntityHierarchy >();
	private final HashMap< String, AttributeSource > attributeSourcesByName = new HashMap< String, AttributeSource >();
	private final LinkedList< LocalBindingContext > bindingContexts = new LinkedList< LocalBindingContext >();
	private final LinkedList< InheritanceType > inheritanceTypes = new LinkedList< InheritanceType >();

	private final LinkedList< EntityMode > entityModes = new LinkedList< EntityMode >();

	private final HibernateTypeHelper typeHelper; // todo: refactor helper and remove redundant methods in this class

	public Binder( final MetadataImplementor metadata, final IdentifierGeneratorFactory identifierGeneratorFactory ) {
		this.metadata = metadata;
		this.identifierGeneratorFactory = identifierGeneratorFactory;
		nameNormalizer = new ObjectNameNormalizer() {

			@Override
			protected NamingStrategy getNamingStrategy() {
				return metadata.getNamingStrategy();
			}

			@Override
			protected boolean isUseQuotedIdentifiersGlobally() {
				return metadata.isGloballyQuotedIdentifiers();
			}
		};
		typeHelper = new HibernateTypeHelper( this, metadata );
	}

	private AttributeBinding attributeBinding( final String entityName, final String attributeName ) {
		// Check if binding has already been created
		EntityBinding entityBinding = entityBinding( entityName );
		AttributeSource attributeSource = attributeSourcesByName.get( attributeSourcesByNameKey( entityName, attributeName ) );
		bindAttribute( entityBinding, attributeSource );
		return entityBinding.locateAttributeBinding( attributeName );
	}

	private String attributeSourcesByNameKey( final String entityName, final String attributeName ) {
		return entityName + "." + attributeName;
	}

	// Top-level attributes handling ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void bindAttributes(
			final AttributeBindingContainer attributeBindingContainer,
			final AttributeSourceContainer attributeSourceContainer ) {
		for ( final AttributeSource attributeSource : attributeSourceContainer.attributeSources() ) {
			bindAttribute( attributeBindingContainer, attributeSource );
		}
	}

	private AttributeBinding bindAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final AttributeSource attributeSource ) {
		// Return existing binding if available
		final String attributeName = attributeSource.getName();
		final AttributeBinding attributeBinding = attributeBindingContainer.locateAttributeBinding( attributeName );
		if ( attributeBinding != null ) {
			return attributeBinding;
		}

		if ( attributeSource.isSingular() ) {
			return bindSingularAttribute( attributeBindingContainer, ( SingularAttributeSource ) attributeSource );
		} else {
			return bindPluralAttribute( attributeBindingContainer, ( PluralAttributeSource ) attributeSource );
		}
	}

	// Singular attributes ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private SingularAttributeBinding bindSingularAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeSource attributeSource ) {
		final SingularAttributeNature nature = attributeSource.getNature();
		final SingularAttribute attribute =
				attributeBindingContainer.getAttributeContainer().locateSingularAttribute( attributeSource.getName() );
		if ( nature == SingularAttributeNature.BASIC ) {
			return bindBasicAttribute( attributeBindingContainer, attributeSource, attribute );
		}
		if ( nature == SingularAttributeNature.MANY_TO_ONE ) {
			return bindManyToOneAttribute( attributeBindingContainer, ( ToOneAttributeSource ) attributeSource, attribute );
		}
		if ( nature == SingularAttributeNature.COMPONENT ) {
			return bindComponentAttribute( attributeBindingContainer, ( ComponentAttributeSource ) attributeSource, attribute );
		}
		throw new NotYetImplementedException( nature.toString() );
	}

	private BasicAttributeBinding bindBasicAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeSource attributeSource,
			SingularAttribute attribute ) {
		if ( attribute == null ) {
			attribute = createSingularAttribute( attributeBindingContainer, attributeSource );
		}
		final List< RelationalValueBinding > relationalValueBindings =
				bindValues(
						attributeBindingContainer,
						attributeSource,
						attribute,
						attributeBindingContainer.seekEntityBinding().getPrimaryTable() );
		final BasicAttributeBinding attributeBinding =
				attributeBindingContainer.makeBasicAttributeBinding(
						attribute,
						relationalValueBindings,
						propertyAccessorName( attributeSource ),
						attributeSource.isIncludedInOptimisticLocking(),
						attributeSource.isLazy(),
						createMetaAttributeContext( attributeBindingContainer, attributeSource ),
						attributeSource.getGeneration() );
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();
		bindHibernateTypeDescriptor(
				attributeBinding.getHibernateTypeDescriptor(),
				attributeSource.getTypeInformation(),
				createSingularAttributeJavaType( attributeBinding.getAttribute() ) );
		Type resolvedType = heuristicType( hibernateTypeDescriptor );
		bindHibernateResolvedType( attributeBinding.getHibernateTypeDescriptor(), resolvedType );
		bindJdbcDataType( resolvedType, ( AbstractValue ) relationalValueBindings.get( 0 ).getValue() );
		attributeBinding.getAttribute().resolveType( bindingContext().makeJavaType( hibernateTypeDescriptor.getJavaTypeName() ) );
		return attributeBinding;
	}

	private CompositeAttributeBinding bindComponentAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final ComponentAttributeSource attributeSource,
			SingularAttribute attribute ) {
		Composite composite;
		if ( attribute == null ) {
			composite =
					new Composite(
							attributeSource.getPath(),
							attributeSource.getClassName(),
							attributeSource.getClassReference(),
							null );
			attribute =
					attributeBindingContainer.getAttributeContainer().createCompositeAttribute(
							attributeSource.getName(),
							composite );
		} else {
			composite = ( Composite ) attribute.getSingularAttributeType();
		}

		final SingularAttribute referencingAttribute;
		if ( StringHelper.isEmpty( attributeSource.getParentReferenceAttributeName() ) ) {
			referencingAttribute = null;
		} else {
			referencingAttribute = composite.createSingularAttribute( attributeSource.getParentReferenceAttributeName() );
		}
		final CompositeAttributeBinding attributeBinding =
				attributeBindingContainer.makeComponentAttributeBinding(
						attribute,
						referencingAttribute,
						propertyAccessorName( attributeSource ),
						attributeSource.isIncludedInOptimisticLocking(),
						attributeSource.isLazy(),
						createMetaAttributeContext( attributeBindingContainer, attributeSource ) );
		bindAttributes( attributeBinding, attributeSource );
		return attributeBinding;
	}

	private ManyToOneAttributeBinding bindManyToOneAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final ToOneAttributeSource attributeSource,
			SingularAttribute attribute ) {
		if ( attribute == null ) {
			attribute = createSingularAttribute( attributeBindingContainer, attributeSource );
		}
		// TODO: figure out which table is used (could be secondary table...)
		TableSpecification table = attributeBindingContainer.seekEntityBinding().getPrimaryTable();
		final List< RelationalValueBinding > relationalValueBindings =
				bindValues( attributeBindingContainer, attributeSource, attribute, table );

		org.hibernate.internal.util.Value< Class< ? >> referencedJavaTypeValue = createSingularAttributeJavaType( attribute );
		final String referencedEntityName =
				attributeSource.getReferencedEntityName() != null
						? attributeSource.getReferencedEntityName()
						: referencedJavaTypeValue.getValue().getName();
		final EntityBinding referencedEntityBinding = entityBinding( referencedEntityName );

		// Foreign key...
		final ForeignKeyContributingSource.JoinColumnResolutionDelegate resolutionDelegate =
				attributeSource.getForeignKeyTargetColumnResolutionDelegate();
		final ForeignKeyContributingSource.JoinColumnResolutionContext resolutionContext;
		if ( resolutionDelegate != null ) {
			resolutionContext = new JoinColumnResolutionContext(referencedEntityBinding, attributeSource);
		} else {
			resolutionContext = null;
		}

		ForeignKey foreignKey = createOrLocateForeignKey(
				attributeSource.getExplicitForeignKeyName(),
				table,
				referencedEntityBinding.getPrimaryTable()
		);

		final AttributeBinding referencedAttributeBinding = determineReferencedAttributeBinding(
				resolutionDelegate,
				resolutionContext,
				referencedEntityBinding
		);
		if ( !referencedAttributeBinding.getAttribute().isSingular() ) {
			throw bindingContext().makeMappingException(
					String.format(
							"Attribute [%s] defined foreign key referencing non-singular attribute [%s]",
							attributeSource.getName(),
							referencedAttributeBinding.getAttribute().getName() ) );
		}

		// todo : currently a chicken-egg problem here between creating the attribute binding and binding its FK values...

		final ManyToOneAttributeBinding attributeBinding =
				attributeBindingContainer.makeManyToOneAttributeBinding(
						attribute,
						propertyAccessorName( attributeSource ),
						attributeSource.isIncludedInOptimisticLocking(),
						attributeSource.isLazy(),
						createMetaAttributeContext( attributeBindingContainer, attributeSource ),
						( SingularAttributeBinding ) referencedAttributeBinding,
						relationalValueBindings );

		bindForeignKeyColumns( foreignKey, attributeBinding.getRelationalValueBindings(), resolutionDelegate, resolutionContext );

		// Type resolution...
		if ( !attribute.isTypeResolved() ) {
			attribute.resolveType( referencedEntityBinding.getEntity() );
		}
		final boolean isRefToPk =
				referencedEntityBinding.getHierarchyDetails().getEntityIdentifier().isIdentifierAttributeBinding(
						referencedAttributeBinding );
		final String uniqueKeyAttributeName = isRefToPk ? null : referencedAttributeBinding.getAttribute().getName();
		Type resolvedType =
				metadata.getTypeResolver().getTypeFactory().manyToOne(
						attributeBinding.getReferencedEntityName(),
						uniqueKeyAttributeName,
						attributeBinding.getFetchTiming() != FetchTiming.IMMEDIATE,
						attributeBinding.getFetchTiming() == FetchTiming.DELAYED,
						true, //TODO: is isEmbedded() obsolete?
						false, //TODO: should be attributeBinding.isIgnoreNotFound(),
						false //TODO: determine if isLogicalOneToOne
				);
		bindHibernateTypeDescriptor(
				attributeBinding.getHibernateTypeDescriptor(),
				attributeSource.getTypeInformation(),
				referencedJavaTypeValue );
		bindHibernateResolvedType( attributeBinding.getHibernateTypeDescriptor(), resolvedType );
		bindJdbcDataType( resolvedType, ( AbstractValue ) relationalValueBindings.get( 0 ).getValue() );

		attributeBinding.setCascadeStyles( attributeSource.getCascadeStyles() );
		attributeBinding.setFetchTiming( attributeSource.getFetchTiming() );
		attributeBinding.setFetchStyle( attributeSource.getFetchStyle() );

		return attributeBinding;
	}

	// Plural attributes ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private AbstractPluralAttributeBinding bindPluralAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final PluralAttributeSource attributeSource ) {
		final PluralAttributeNature nature = attributeSource.getPluralAttributeNature();
		final PluralAttribute attribute =
				attributeBindingContainer.getAttributeContainer().locatePluralAttribute( attributeSource.getName() );
		final AbstractPluralAttributeBinding attributeBinding;
		final Type resolvedType;
		if ( nature == PluralAttributeNature.BAG ) {
			attributeBinding = bindBagAttribute( attributeBindingContainer, attributeSource, attribute );
			resolvedType = resolveBagType( ( BagBinding ) attributeBinding );
		} else if ( nature == PluralAttributeNature.SET ) {
			attributeBinding = bindSetAttribute( attributeBindingContainer, attributeSource, attribute );
			resolvedType = resolveSetType( ( SetBinding ) attributeBinding );
		} else if ( nature == PluralAttributeNature.LIST ) {
			attributeBinding = bindListAttribute( attributeBindingContainer, ( ListAttributeSource ) attributeSource, attribute );
			resolvedType = resolveListType( ( ListBinding ) attributeBinding );
		} else if ( nature == PluralAttributeNature.MAP ) {
			attributeBinding = bindMapAttribute( attributeBindingContainer, ( MapAttributeSource ) attributeSource, attribute );
			resolvedType = resolveMapType( ( MapBinding ) attributeBinding );
		} else {
			throw new NotYetImplementedException( nature.toString() );
		}

		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();
		ReflectedCollectionJavaTypes reflectedCollectionJavaTypes = typeHelper.getReflectedCollectionJavaTypes( attributeBinding );
		bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				attributeSource.getTypeInformation(),
				defaultCollectionJavaTypeName( reflectedCollectionJavaTypes, attributeSource ) );
		bindHibernateResolvedType( hibernateTypeDescriptor, resolvedType );
		// Note: Collection types do not have a relational model
		attributeBinding.setFetchTiming( attributeSource.getFetchTiming() );
		attributeBinding.setFetchStyle( attributeSource.getFetchStyle() );
		attributeBinding.setCaching( attributeSource.getCaching() );
		if ( StringHelper.isNotEmpty( attributeSource.getCustomPersisterClassName() ) ) {
			attributeBinding.setExplicitPersisterClass( bindingContexts.peek().< CollectionPersister >locateClassByName(
					attributeSource.getCustomPersisterClassName() ) );
		}
		attributeBinding.setCustomLoaderName( attributeSource.getCustomLoaderName() );
		attributeBinding.setCustomSqlInsert( attributeSource.getCustomSqlInsert() );
		attributeBinding.setCustomSqlUpdate( attributeSource.getCustomSqlUpdate() );
		attributeBinding.setCustomSqlDelete( attributeSource.getCustomSqlDelete() );
		attributeBinding.setCustomSqlDeleteAll( attributeSource.getCustomSqlDeleteAll() );
		attributeBinding.setWhere( attributeSource.getWhere() );

		bindSortingAndOrdering( attributeBinding, attributeSource );

		if ( attributeSource.getElementSource().getNature() == org.hibernate.metamodel.spi.source.PluralAttributeElementNature.BASIC ) {
			bindBasicCollectionKey( attributeBinding, attributeSource );
			bindBasicCollectionElement(
					( BasicPluralAttributeElementBinding ) attributeBinding.getPluralAttributeElementBinding(),
					( BasicPluralAttributeElementSource ) attributeSource.getElementSource(),
					defaultCollectionElementJavaTypeName( reflectedCollectionJavaTypes ) );
		}
		else if ( attributeSource.getElementSource().getNature() == org.hibernate.metamodel.spi.source.PluralAttributeElementNature.ONE_TO_MANY ) {
			final OneToManyPluralAttributeElementSource elementSource =
					(OneToManyPluralAttributeElementSource) attributeSource.getElementSource();
			final String defaultElementJavaTypeName = defaultCollectionElementJavaTypeName( reflectedCollectionJavaTypes );
			String referencedEntityName =
					elementSource.getReferencedEntityName() != null ?
							elementSource.getReferencedEntityName() :
							defaultElementJavaTypeName;
			if  ( referencedEntityName == null ) {
				throw new MappingException(
						String.format( "The mapping for the entity associated with one-to-many attribute (%s) is undefined.",
								createAttributePath( attributeBinding )
						),
						bindingContexts.peek().getOrigin()
				);
			}
			EntityBinding referencedEntityBinding = entityBinding( referencedEntityName );
 			bindOneToManyCollectionKey( attributeBinding, attributeSource, referencedEntityBinding );
			bindOneToManyCollectionElement(
					(OneToManyPluralAttributeElementBinding) attributeBinding.getPluralAttributeElementBinding(),
					(OneToManyPluralAttributeElementSource) attributeSource.getElementSource(),
					referencedEntityBinding,
					defaultElementJavaTypeName
			);
		}
		else {
			throw new NotYetImplementedException( String.format(
					"Support for collection elements of type %s not yet implemented",
					attributeSource.getElementSource().getNature() ) );
		}

		if ( attributeSource instanceof IndexedPluralAttributeSource ) {
			bindCollectionIndex(
					( IndexedPluralAttributeBinding ) attributeBinding,
					( ( IndexedPluralAttributeSource ) attributeSource ).getIndexSource(),
					defaultCollectionIndexJavaTypeName( reflectedCollectionJavaTypes ) );
		}

		bindCollectionTablePrimaryKey( attributeBinding, attributeSource, reflectedCollectionJavaTypes );
		metadata.addCollection( attributeBinding );
		return attributeBinding;
	}

	private AbstractPluralAttributeBinding bindBagAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final PluralAttributeSource attributeSource,
			PluralAttribute attribute ) {
		if ( attribute == null ) {
			attribute = attributeBindingContainer.getAttributeContainer().createBag( attributeSource.getName() );
		}
		return attributeBindingContainer.makeBagAttributeBinding(
				attribute,
				pluralAttributeElementNature( attributeSource ),
				pluralAttributeKeyBinding( attributeBindingContainer, attributeSource ),
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				false,
				createMetaAttributeContext( attributeBindingContainer, attributeSource ) );
	}

	private AbstractPluralAttributeBinding bindSetAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final PluralAttributeSource attributeSource,
			PluralAttribute attribute ) {
		if ( attribute == null ) {
			attribute = attributeBindingContainer.getAttributeContainer().createSet( attributeSource.getName() );
		}
		return attributeBindingContainer.makeSetAttributeBinding(
				attribute,
				pluralAttributeElementNature( attributeSource ),
				pluralAttributeKeyBinding( attributeBindingContainer, attributeSource ),
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				false,
				createMetaAttributeContext( attributeBindingContainer, attributeSource )
		);
	}

	private AbstractPluralAttributeBinding bindListAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final ListAttributeSource attributeSource,
			PluralAttribute attribute ) {
		if ( attribute == null ) {
			attribute = attributeBindingContainer.getAttributeContainer().createList( attributeSource.getName() );
		}
		return attributeBindingContainer.makeListAttributeBinding(
				attribute,
				pluralAttributeElementNature( attributeSource ),
				pluralAttributeKeyBinding( attributeBindingContainer, attributeSource ),
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				false,
				createMetaAttributeContext( attributeBindingContainer, attributeSource ),
				attributeSource.getIndexSource().base()
		);
	}

	private AbstractPluralAttributeBinding bindMapAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final MapAttributeSource attributeSource,
			PluralAttribute attribute ) {
		if ( attribute == null ) {
			attribute = attributeBindingContainer.getAttributeContainer().createMap( attributeSource.getName() );
		}
		return attributeBindingContainer.makeMapAttributeBinding(
				attribute,
				pluralAttributeElementNature( attributeSource ),
				pluralAttributeKeyBinding( attributeBindingContainer, attributeSource ),
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				false,

				createMetaAttributeContext( attributeBindingContainer, attributeSource )
		);
	}

	private void bindBasicCollectionElement(
			final BasicPluralAttributeElementBinding elementBinding,
			final BasicPluralAttributeElementSource elementSource,
			final String defaultElementJavaTypeName ) {
		bindBasicPluralElementRelationalValues( elementSource, elementBinding );
		bindHibernateTypeDescriptor(
				elementBinding.getHibernateTypeDescriptor(),
				elementSource.getExplicitHibernateTypeSource(),
				defaultElementJavaTypeName );
		Type resolvedElementType = heuristicType( elementBinding.getHibernateTypeDescriptor() );
		bindHibernateResolvedType( elementBinding.getHibernateTypeDescriptor(), resolvedElementType );
		bindJdbcDataType( resolvedElementType, ( AbstractValue ) elementBinding.getRelationalValueBindings().get( 0 ).getValue() );
	}

	private void bindOneToManyCollectionElement(
			final OneToManyPluralAttributeElementBinding elementBinding,
			final OneToManyPluralAttributeElementSource elementSource,
			final EntityBinding referencedEntityBinding,
			final String defaultElementJavaTypeName) {

		//referencedEntityBinding.getHierarchyDetails().getEntityIdentifier().getValueBinding().addEntityReferencingAttributeBinding(  );

		elementBinding.setRelationalValueBindings(
			referencedEntityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding().getRelationalValueBindings()
		);
		final HibernateTypeDescriptor hibernateTypeDescriptor = elementBinding.getHibernateTypeDescriptor();
		bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				referencedEntityBinding.getEntity().getName(),
				null,
				defaultElementJavaTypeName
		);

		Type resolvedElementType = metadata.getTypeResolver().getTypeFactory().manyToOne(
				referencedEntityBinding.getEntity().getName(),
				null,
				false,
				false,
				true,
				false, //TODO: should be attributeBinding.isIgnoreNotFound(),
				false
		);
		bindHibernateResolvedType( elementBinding.getHibernateTypeDescriptor(), resolvedElementType );
		// no need to bind JDBC data types because element is referenced EntityBinding's ID
		elementBinding.setCascadeStyles( elementSource.getCascadeStyles() );
	}

	private void bindBasicSetElementTablePrimaryKey(final PluralAttributeBinding attributeBinding) {
		final PrimaryKey primaryKey = attributeBinding.getPluralAttributeKeyBinding().getCollectionTable().getPrimaryKey();
		final ForeignKey foreignKey = attributeBinding.getPluralAttributeKeyBinding().getForeignKey();
		final BasicPluralAttributeElementBinding elementBinding =
				( BasicPluralAttributeElementBinding ) attributeBinding.getPluralAttributeElementBinding();
		if ( elementBinding.getPluralAttributeElementNature() != PluralAttributeElementNature.BASIC ) {
			throw new MappingException( String.format(
					"Expected a SetBinding with an element of nature PluralAttributeElementNature.BASIC; instead was %s",
					elementBinding.getPluralAttributeElementNature() ), bindingContexts.peek().getOrigin() );
		}
		for ( final Column foreignKeyColumn : foreignKey.getSourceColumns() ) {
			primaryKey.addColumn( foreignKeyColumn );
		}
		for ( final RelationalValueBinding elementValueBinding : elementBinding.getRelationalValueBindings() ) {
			if ( elementValueBinding.getValue() instanceof Column && !elementValueBinding.isNullable() ) {
				primaryKey.addColumn( ( Column ) elementValueBinding.getValue() );
			}
		}
		if ( primaryKey.getColumnSpan() == foreignKey.getColumnSpan() ) {
			// for backward compatibility, allow a set with no not-null
			// element columns, using all columns in the row locater SQL
			// todo: create an implicit not null constraint on all cols?
		}
	}

	private void bindBasicPluralElementRelationalValues(
			final RelationalValueSourceContainer relationalValueSourceContainer,
			final BasicPluralAttributeElementBinding elementBinding ) {
		elementBinding.setRelationalValueBindings( bindValues(
				elementBinding.getPluralAttributeBinding().getContainer(),
				relationalValueSourceContainer,
				elementBinding.getPluralAttributeBinding().getAttribute(),
				elementBinding.getPluralAttributeBinding().getPluralAttributeKeyBinding().getCollectionTable() ) );
	}

	private void bindCollectionIndex(
			final IndexedPluralAttributeBinding attributeBinding,
			final PluralAttributeIndexSource attributeSource,
			final String defaultIndexJavaTypeName ) {
		IndexedPluralAttributeBinding indexedAttributeBinding = attributeBinding;
		final BasicPluralAttributeIndexBinding indexBinding =
				( BasicPluralAttributeIndexBinding ) indexedAttributeBinding.getPluralAttributeIndexBinding();
		indexBinding.setIndexRelationalValue( bindValues(
				indexedAttributeBinding.getContainer(),
				attributeSource,
				indexedAttributeBinding.getAttribute(),
				indexedAttributeBinding.getPluralAttributeKeyBinding().getCollectionTable() ).get( 0 ).getValue() );
		bindHibernateTypeDescriptor(
				indexBinding.getHibernateTypeDescriptor(),
				attributeSource.explicitHibernateTypeSource(),
				defaultIndexJavaTypeName );
		Type resolvedElementType = heuristicType( indexBinding.getHibernateTypeDescriptor() );
		bindHibernateResolvedType( indexBinding.getHibernateTypeDescriptor(), resolvedElementType );
		bindJdbcDataType( resolvedElementType, (AbstractValue) indexBinding.getIndexRelationalValue() );
	}

	private void bindBasicCollectionKey(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeSource attributeSource) {
		if ( attributeSource.getElementSource().getNature() != org.hibernate.metamodel.spi.source.PluralAttributeElementNature.BASIC ) {
			throw new AssertionFailure(
					String.format(
							"Expected basic attribute binding; instead got {%s}",
							attributeSource.getElementSource().getNature()
					)
			);
		}
		attributeBinding.getPluralAttributeKeyBinding().setInverse( false );
		TableSpecification collectionTable = createCollectionTable( attributeBinding, attributeSource );
		if ( StringHelper.isNotEmpty( attributeSource.getCollectionTableComment() ) ) {
			collectionTable.addComment( attributeSource.getCollectionTableComment() );
		}
		if ( StringHelper.isNotEmpty( attributeSource.getCollectionTableCheck() ) ) {
			collectionTable.addCheckConstraint( attributeSource.getCollectionTableCheck() );
		}
		bindCollectionTableForeignKey(
				attributeBinding,
				attributeSource.getKeySource(),
				collectionTable
		);
	}

	private void bindOneToManyCollectionKey(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeSource attributeSource,
			final EntityBinding referencedEntityBinding) {
		if ( attributeSource.getElementSource().getNature() != org.hibernate.metamodel.spi.source.PluralAttributeElementNature.ONE_TO_MANY ) {
			throw new AssertionFailure(
					String.format(
							"Expected one-to-many attribute binding; instead got {%s}",
							attributeSource.getElementSource().getNature()
					)
			);
		}
		if ( attributeSource.getCollectionTableSpecificationSource() != null ) {
				// TODO: Need to look up the table to be able to create the foreign key
				throw new NotYetImplementedException( "one-to-many using a join table is not supported yet." );
		}
		attributeBinding.getPluralAttributeKeyBinding().setInverse( attributeSource.isInverse() );
		if ( !attributeSource.isInverse() && !isNullable( attributeSource.getKeySource() ) ) {
			TableSpecification collectionTable = referencedEntityBinding.getPrimaryTable();
			bindCollectionTableForeignKey( attributeBinding, attributeSource.getKeySource(), collectionTable );
			// Create the synthetic attribute
			SingularAttribute syntheticAttribute = referencedEntityBinding.getEntity().createSyntheticSingularAttribute(
					SyntheticAttributeHelper.createBackRefAttributeName( attributeBinding.getAttribute().getRole() )
			);
			// Create the backref attribute binding.
			BackRefAttributeBinding backRefAttributeBinding =
					referencedEntityBinding.makeBackRefAttributeBinding( syntheticAttribute, attributeBinding );
			final HibernateTypeDescriptor keyTypeDescriptor = attributeBinding.getPluralAttributeKeyBinding().getHibernateTypeDescriptor();
			final HibernateTypeDescriptor hibernateTypeDescriptor = backRefAttributeBinding.getHibernateTypeDescriptor();
			hibernateTypeDescriptor.setJavaTypeName( keyTypeDescriptor.getJavaTypeName() );
			hibernateTypeDescriptor.setExplicitTypeName( keyTypeDescriptor.getExplicitTypeName() );
			hibernateTypeDescriptor.setToOne( keyTypeDescriptor.isToOne() );
			hibernateTypeDescriptor.getTypeParameters().putAll( keyTypeDescriptor.getTypeParameters() );
			hibernateTypeDescriptor.setResolvedTypeMapping( keyTypeDescriptor.getResolvedTypeMapping() );
			backRefAttributeBinding.getAttribute().resolveType(
					attributeBinding.getPluralAttributeKeyBinding().getReferencedAttributeBinding().getAttribute().getSingularAttributeType()
			);
		}
	}

	private boolean isNullable(RelationalValueSourceContainer valueSourceContainer) {
		if ( valueSourceContainer.relationalValueSources().isEmpty() ) {
			return valueSourceContainer.areValuesNullableByDefault();
		}
		else {
			// return true if *any* values are nullable
			for ( RelationalValueSource relationalValueSource : valueSourceContainer.relationalValueSources() ) {
				if ( relationalValueSource.getNature() == RelationalValueSource.Nature.COLUMN ) {
					ColumnSource columnSource = (ColumnSource) relationalValueSource;
					if ( toBoolean( columnSource.isNullable(), valueSourceContainer.areValuesNullableByDefault() ) ) {
						return true;
					}
				}
			}
			return false;
		}
	}

	void bindCollectionTableForeignKey(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeKeySource keySource,
			TableSpecification collectionTable) {

		final AttributeBindingContainer attributeBindingContainer = attributeBinding.getContainer();
		final PluralAttributeKeyBinding keyBinding = attributeBinding.getPluralAttributeKeyBinding();

		final ForeignKeyContributingSource.JoinColumnResolutionDelegate resolutionDelegate =
				keySource.getForeignKeyTargetColumnResolutionDelegate();
		final ForeignKeyContributingSource.JoinColumnResolutionContext resolutionContext;
		if ( resolutionDelegate != null ) {
			resolutionContext = new ForeignKeyContributingSource.JoinColumnResolutionContext() {
				@Override
				public List< Value > resolveRelationalValuesForAttribute( String attributeName ) {
					final AttributeBinding referencedAttributeBinding =
							attributeBindingContainer.locateAttributeBinding( attributeName );
					if ( !referencedAttributeBinding.getAttribute().isSingular() ) {
						throw bindingContext().makeMappingException(
								String.format(
										"Many-to-one attribute [%s] named plural attribute as property-ref [%s]",
										attributeBinding.getAttribute().getName(),
										attributeName
								)
						);
					}
					List< Value > values = new ArrayList< Value >();
					SingularAttributeBinding referencedAttributeBindingAsSingular =
							( SingularAttributeBinding ) referencedAttributeBinding;
					for ( RelationalValueBinding valueBinding : referencedAttributeBindingAsSingular.getRelationalValueBindings() ) {
						values.add( valueBinding.getValue() );
					}
					return values;
				}

				@Override
				public Column resolveColumn(
						String logicalColumnName,
						String logicalTableName,
						String logicalSchemaName,
						String logicalCatalogName ) {
					return metadata.getDatabase().getSchema( logicalSchemaName, logicalCatalogName ).locateTable(
							Identifier.toIdentifier( logicalTableName ) ).locateColumn( logicalColumnName );
				}
			};
		} else {
			resolutionContext = null;
		}

		// todo: is null FK name allowed (is there a default?)
		final String foreignKeyName =
				StringHelper.isEmpty( keySource.getExplicitForeignKeyName() )
						? null
						: quotedIdentifier( keySource.getExplicitForeignKeyName() );

		// TODO: deal with secondary tables...
		final TableSpecification table = attributeBinding.getContainer().seekEntityBinding().getPrimaryTable();
		keyBinding.prepareForeignKey( foreignKeyName, collectionTable, table );
		keyBinding.getForeignKey().setDeleteRule( keySource.getOnDeleteAction() );
		bindForeignKeyColumns(
				keyBinding.getForeignKey(),
				bindValues( attributeBinding.getContainer(), keySource, attributeBinding.getAttribute(), collectionTable ),
				resolutionDelegate,
				resolutionContext );

		final HibernateTypeDescriptor pluralAttributeKeyTypeDescriptor = keyBinding.getHibernateTypeDescriptor();
		final HibernateTypeDescriptor referencedTypeDescriptor =
				keyBinding.getReferencedAttributeBinding().getHibernateTypeDescriptor();
		pluralAttributeKeyTypeDescriptor.setExplicitTypeName( referencedTypeDescriptor.getExplicitTypeName() );
		pluralAttributeKeyTypeDescriptor.setJavaTypeName( referencedTypeDescriptor.getJavaTypeName() );
		// TODO: not sure about the following...
		pluralAttributeKeyTypeDescriptor.setToOne( referencedTypeDescriptor.isToOne() );
		pluralAttributeKeyTypeDescriptor.getTypeParameters().putAll( referencedTypeDescriptor.getTypeParameters() );
		final Type resolvedKeyType = referencedTypeDescriptor.getResolvedTypeMapping();
		pluralAttributeKeyTypeDescriptor.setResolvedTypeMapping( resolvedKeyType );

		Iterator< Column > fkColumnIterator = keyBinding.getForeignKey().getSourceColumns().iterator();
		if ( resolvedKeyType.isComponentType() ) {
			ComponentType componentType = ( ComponentType ) resolvedKeyType;
			for ( Type subType : componentType.getSubtypes() ) {
				bindJdbcDataType( subType, fkColumnIterator.next() );
			}
		} else {
			bindJdbcDataType( resolvedKeyType, fkColumnIterator.next() );
		}
	}

	private void bindCollectionTablePrimaryKey(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeSource attributeSource,
			final HibernateTypeHelper.ReflectedCollectionJavaTypes reflectedCollectionJavaTypes ) {
		PluralAttributeNature pluralAttributeNature = attributeSource.getPluralAttributeNature();
		if ( attributeSource.getElementSource().getNature() == org.hibernate.metamodel.spi.source.PluralAttributeElementNature.ONE_TO_MANY
				|| pluralAttributeNature == PluralAttributeNature.BAG ) {
			return;
		}
		if ( attributeBinding.getPluralAttributeElementBinding().getPluralAttributeElementNature() == PluralAttributeElementNature.BASIC ) {
			if ( pluralAttributeNature == PluralAttributeNature.SET ) {
				bindBasicSetElementTablePrimaryKey( attributeBinding );
			} else if ( pluralAttributeNature == PluralAttributeNature.LIST || pluralAttributeNature == PluralAttributeNature.MAP ) {
				bindIndexedTablePrimaryKey( ( IndexedPluralAttributeBinding ) attributeBinding );
			} else {
				throw new NotYetImplementedException( "Only Sets with basic elements are supported so far." );
			}
		}
	}

	private void bindDiscriminator( final EntityBinding rootEntityBinding, final RootEntitySource rootEntitySource ) {
		final DiscriminatorSource discriminatorSource = rootEntitySource.getDiscriminatorSource();
		if ( discriminatorSource == null ) {
			return;
		}
		final RelationalValueSource valueSource = discriminatorSource.getDiscriminatorRelationalValueSource();
		final TableSpecification table = rootEntityBinding.locateTable( valueSource.getContainingTableName() );
		AbstractValue value;
		if ( valueSource instanceof ColumnSource ) {
			value =
					createColumn(
							table,
							( ColumnSource ) valueSource,
							bindingContexts.peek().getMappingDefaults().getDiscriminatorColumnName(),
							false,
							false );
		} else {
			value = table.locateOrCreateDerivedValue( ( ( DerivedValueSource ) valueSource ).getExpression() );
		}
		final EntityDiscriminator discriminator =
				new EntityDiscriminator( value, discriminatorSource.isInserted(), discriminatorSource.isForced() );
		rootEntityBinding.getHierarchyDetails().setEntityDiscriminator( discriminator );
		rootEntityBinding.setDiscriminatorMatchValue( rootEntitySource.getDiscriminatorMatchValue() );
		// Configure discriminator hibernate type
		final String typeName =
				discriminatorSource.getExplicitHibernateTypeName() != null
						? discriminatorSource.getExplicitHibernateTypeName()
						: "string";
		final HibernateTypeDescriptor hibernateTypeDescriptor = discriminator.getExplicitHibernateTypeDescriptor();
		hibernateTypeDescriptor.setExplicitTypeName( typeName );
		Type resolvedType = heuristicType( hibernateTypeDescriptor );
		bindHibernateResolvedType( hibernateTypeDescriptor, resolvedType );
		bindJdbcDataType( resolvedType, value );
	}

	private void bindMultiTenancy(EntityBinding rootEntityBinding, RootEntitySource rootEntitySource) {
		final MultiTenancySource multiTenancySource = rootEntitySource.getMultiTenancySource();
		if ( multiTenancySource == null ) {
			return;
		}

		// if (1) the strategy is discriminator based and (2) the entity is not shared, we need to either (a) extract
		// the user supplied tenant discriminator value mapping or (b) generate an implicit one
		final boolean needsTenantIdentifierValueMapping =
				MultiTenancyStrategy.DISCRIMINATOR == metadata.getOptions().getMultiTenancyStrategy()
				&& ! multiTenancySource.isShared();

		if ( needsTenantIdentifierValueMapping ) {
			// NOTE : the table for tenant identifier/discriminator is always the primary table
			final Value tenantDiscriminatorValue;
			final RelationalValueSource valueSource = multiTenancySource.getRelationalValueSource();
			if ( valueSource == null ) {
				// user supplied no explicit information, so use implicit mapping with default name
				tenantDiscriminatorValue = rootEntityBinding.getPrimaryTable().locateOrCreateColumn( "tenant_id" );
			}
			else {
				tenantDiscriminatorValue = buildRelationValue( valueSource, rootEntityBinding.getPrimaryTable() );
			}
			rootEntityBinding.getHierarchyDetails().getTenantDiscrimination().setDiscriminatorValue( tenantDiscriminatorValue );
		}

		rootEntityBinding.getHierarchyDetails().getTenantDiscrimination().setShared( multiTenancySource.isShared() );
		rootEntityBinding.getHierarchyDetails().getTenantDiscrimination().setUseParameterBinding( multiTenancySource.bindAsParameter() );
	}

	private Value buildRelationValue(RelationalValueSource valueSource, TableSpecification table) {
		if ( valueSource instanceof ColumnSource ) {
			return createColumn(
					table,
					( ColumnSource ) valueSource,
					bindingContexts.peek().getMappingDefaults().getDiscriminatorColumnName(),
					false,
					false
			);
		}
		else {
			return table.locateOrCreateDerivedValue( ( ( DerivedValueSource ) valueSource ).getExpression() );
		}
	}

	private EntityBinding bindEntities( final EntityHierarchy entityHierarchy ) {
		final RootEntitySource rootEntitySource = entityHierarchy.getRootEntitySource();
		// Return existing binding if available
		EntityBinding rootEntityBinding = metadata.getEntityBinding( rootEntitySource.getEntityName() );
		if ( rootEntityBinding != null ) {
			return rootEntityBinding;
		}
		// Save inheritance type and entity mode that will apply to entire hierarchy
		inheritanceTypes.push( entityHierarchy.getHierarchyInheritanceType() );
		entityModes.push( rootEntitySource.getEntityMode() );
		final LocalBindingContext bindingContext = rootEntitySource.getLocalBindingContext();
		bindingContexts.push( bindingContext );
		try {
			// Create root entity binding
			rootEntityBinding = createEntityBinding( rootEntitySource, null );
			// Create/Bind root-specific information
			bindIdentifier( rootEntityBinding, rootEntitySource.getIdentifierSource() );
			bindVersion( rootEntityBinding, rootEntitySource.getVersioningAttributeSource() );
			bindDiscriminator( rootEntityBinding, rootEntitySource );
			createIdentifierGenerator( rootEntityBinding );
			bindMultiTenancy( rootEntityBinding, rootEntitySource );
			rootEntityBinding.getHierarchyDetails().setCaching( rootEntitySource.getCaching() );
			rootEntityBinding.getHierarchyDetails().setExplicitPolymorphism( rootEntitySource.isExplicitPolymorphism() );
			rootEntityBinding.getHierarchyDetails().setOptimisticLockStyle( rootEntitySource.getOptimisticLockStyle() );
			rootEntityBinding.setMutable( rootEntitySource.isMutable() );
			rootEntityBinding.setWhereFilter( rootEntitySource.getWhere() );
			rootEntityBinding.setRowId( rootEntitySource.getRowId() );
			// Bind attributes and sub-entities to root entity
			bindAttributes( rootEntityBinding, rootEntitySource );
			if ( inheritanceTypes.peek() != InheritanceType.NO_INHERITANCE ) {
				bindSubEntities( rootEntityBinding, rootEntitySource );
			}
		} finally {
			bindingContexts.pop();
			inheritanceTypes.pop();
			entityModes.pop();
		}
		return rootEntityBinding;
	}

	public void bindEntities( final Iterable< EntityHierarchy > entityHierarchies ) {
		entitySourcesByName.clear();
		attributeSourcesByName.clear();
		inheritanceTypes.clear();
		entityModes.clear();
		bindingContexts.clear();
		// Index sources by name so we can find and resolve entities on the fly as references to them
		// are encountered (e.g., within associations)
		for ( final EntityHierarchy entityHierarchy : entityHierarchies ) {
			entityHierarchiesByRootEntitySource.put( entityHierarchy.getRootEntitySource(), entityHierarchy );
			mapSourcesByName( entityHierarchy.getRootEntitySource() );
		}
		// Bind each entity hierarchy
		for ( final EntityHierarchy entityHierarchy : entityHierarchies ) {
			bindEntities( entityHierarchy );
		}
	}

	private EntityBinding bindEntity( final EntitySource entitySource, final EntityBinding superEntityBinding ) {
		// Return existing binding if available
		EntityBinding entityBinding = metadata.getEntityBinding( entitySource.getEntityName() );
		if ( entityBinding != null ) {
			return entityBinding;
		}
		final LocalBindingContext bindingContext = entitySource.getLocalBindingContext();
		bindingContexts.push( bindingContext );
		try {
			// Create new entity binding
			entityBinding = createEntityBinding( entitySource, superEntityBinding );
			bindAttributes( entityBinding, entitySource );
			bindSubEntities( entityBinding, entitySource );
			return entityBinding;
		} finally {
			bindingContexts.pop();
		}
	}

	private void bindHibernateResolvedType( final HibernateTypeDescriptor hibernateTypeDescriptor, final Type resolvedType ) {
		// Configure relational value JDBC type from Hibernate type descriptor now that its configured
		if ( resolvedType != null ) {
			hibernateTypeDescriptor.setResolvedTypeMapping( resolvedType );
			if ( hibernateTypeDescriptor.getJavaTypeName() == null ) {
				hibernateTypeDescriptor.setJavaTypeName( resolvedType.getReturnedClass().getName() );
			}
			hibernateTypeDescriptor.setToOne( resolvedType.isEntityType() );
		}
	}

	private void bindHibernateTypeDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final ExplicitHibernateTypeSource explicitTypeSource,
			final org.hibernate.internal.util.Value< Class< ? >> defaultJavaType ) {
		// if there is an explicit type name specified, then there's no reason to
		// initialize the default Java type name; simply pass a null default instead.
		bindHibernateTypeDescriptor(
				hibernateTypeDescriptor, explicitTypeSource, explicitTypeSource.getName() == null
				? defaultJavaType.getValue().getName()
				: null
		);
	}

	private void bindHibernateTypeDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final ExplicitHibernateTypeSource explicitTypeSource,
			final String defaultJavaTypeName ) {
		if ( explicitTypeSource == null ) {
			bindHibernateTypeDescriptor(
					hibernateTypeDescriptor, null, null, defaultJavaTypeName
			);
		}
		else {
			bindHibernateTypeDescriptor(
					hibernateTypeDescriptor,
					explicitTypeSource.getName(),
					explicitTypeSource.getParameters(),
					defaultJavaTypeName
			);
		}
	}

	private void bindHibernateTypeDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final String explicitTypeName,
			final Map<String,String> explictTypeParameters,
			final String defaultJavaTypeName ) {
		if ( explicitTypeName == null ) {
			if ( hibernateTypeDescriptor.getJavaTypeName() != null ) {
				throw new MappingException( String.format(
						"Attempt to re-initialize (non-explicit) Java type name; current=%s new=%s",
						hibernateTypeDescriptor.getJavaTypeName(),
						defaultJavaTypeName ), bindingContexts.peek().getOrigin() );
			}
			hibernateTypeDescriptor.setJavaTypeName( defaultJavaTypeName );
		} else {
			// Check if user-specified name is of a User-Defined Type (UDT)
			final TypeDefinition typeDef = metadata.getTypeDefinition( explicitTypeName );
			if ( hibernateTypeDescriptor.getExplicitTypeName() != null ) {
				throw new MappingException( String.format(
						"Attempt to re-initialize explicity-mapped Java type name; current=%s new=%s",
						hibernateTypeDescriptor.getExplicitTypeName(),
						explicitTypeName ), bindingContexts.peek().getOrigin() );
			}
			if ( typeDef == null ) {
				hibernateTypeDescriptor.setExplicitTypeName( explicitTypeName );
			} else {
				hibernateTypeDescriptor.setExplicitTypeName( typeDef.getTypeImplementorClass().getName() );
				hibernateTypeDescriptor.setTypeParameters( typeDef.getParameters() );
			}
			if ( explictTypeParameters != null ) {
				hibernateTypeDescriptor.getTypeParameters().putAll( explictTypeParameters );
			}
		}
	}

	private void bindIdentifier( final EntityBinding rootEntityBinding, final IdentifierSource identifierSource ) {
		final EntityIdentifierNature nature = identifierSource.getNature();
		switch ( nature ) {
			case SIMPLE: {
				bindSimpleIdentifier( rootEntityBinding, ( SimpleIdentifierSource ) identifierSource );
				break;
			}
			case AGGREGATED_COMPOSITE: {
				bindAggregatedCompositeIdentifier( rootEntityBinding, ( AggregatedCompositeIdentifierSource ) identifierSource );
				break;
			}
			case COMPOSITE: {
				bindNonAggregatedCompositeIdentifier(
						rootEntityBinding,
						( NonAggregatedCompositeIdentifierSource ) identifierSource );
				break;
			}
			default: {
				throw bindingContext().makeMappingException( "Unknown identifier nature : " + nature.name() );
			}
		}
	}

	private void bindSimpleIdentifier( final EntityBinding rootEntityBinding, final SimpleIdentifierSource identifierSource ) {
		// locate the attribute binding
		final BasicAttributeBinding idAttributeBinding =
				( BasicAttributeBinding ) bindAttribute( rootEntityBinding, identifierSource.getIdentifierAttributeSource() );

		// Configure ID generator
		IdGenerator generator = identifierSource.getIdentifierGeneratorDescriptor();
		if ( generator == null ) {
			final Map< String, String > params = new HashMap< String, String >();
			params.put( IdentifierGenerator.ENTITY_NAME, rootEntityBinding.getEntity().getName() );
			generator = new IdGenerator( "default_assign_identity_generator", "assigned", params );
		}

		// determine the unsaved value mapping
		final String unsavedValue = interpretIdentifierUnsavedValue( identifierSource, generator );

		rootEntityBinding.getHierarchyDetails().getEntityIdentifier().prepareAsSimpleIdentifier(
				idAttributeBinding,
				generator,
				unsavedValue );
	}

	private void bindAggregatedCompositeIdentifier(
			EntityBinding rootEntityBinding,
			AggregatedCompositeIdentifierSource identifierSource ) {
		// locate the attribute binding
		final CompositeAttributeBinding idAttributeBinding =
				( CompositeAttributeBinding ) bindAttribute( rootEntityBinding, identifierSource.getIdentifierAttributeSource() );

		// Configure ID generator
		IdGenerator generator = identifierSource.getIdentifierGeneratorDescriptor();
		if ( generator == null ) {
			final Map< String, String > params = new HashMap< String, String >();
			params.put( IdentifierGenerator.ENTITY_NAME, rootEntityBinding.getEntity().getName() );
			generator = new IdGenerator( "default_assign_identity_generator", "assigned", params );
		}

		// determine the unsaved value mapping
		final String unsavedValue = interpretIdentifierUnsavedValue( identifierSource, generator );

		rootEntityBinding.getHierarchyDetails().getEntityIdentifier().prepareAsAggregatedCompositeIdentifier(
				idAttributeBinding,
				generator,
				unsavedValue );
	}

	private void bindNonAggregatedCompositeIdentifier(
			EntityBinding rootEntityBinding,
			NonAggregatedCompositeIdentifierSource identifierSource ) {
		// locate the attribute bindings for the real attributes
		List< SingularAttributeBinding > idAttributeBindings = new ArrayList< SingularAttributeBinding >();
		for ( SingularAttributeSource attributeSource : identifierSource.getAttributeSourcesMakingUpIdentifier() ) {
			idAttributeBindings.add( ( SingularAttributeBinding ) bindAttribute( rootEntityBinding, attributeSource ) );
		}

		// Create the synthetic attribute
		SingularAttribute syntheticAttribute =
				rootEntityBinding.getEntity().createSyntheticCompositeAttribute(
						SYNTHETIC_COMPOSITE_ID_ATTRIBUTE_NAME,
						rootEntityBinding.getEntity() );

		// Create the synthetic attribute binding.
		final CompositeAttributeBinding syntheticAttributeBinding =
				rootEntityBinding.makeVirtualComponentAttributeBinding(
						syntheticAttribute,
						idAttributeBindings,
						createMetaAttributeContext( rootEntityBinding, identifierSource.getMetaAttributeSources() ) );

		rootEntityBinding.getHierarchyDetails().getEntityIdentifier().prepareAsNonAggregatedCompositeIdentifier(
				syntheticAttributeBinding,
				identifierSource.getLookupIdClass() );
	}

	private void bindIndexedTablePrimaryKey( IndexedPluralAttributeBinding attributeBinding ) {
		final PrimaryKey primaryKey = attributeBinding.getPluralAttributeKeyBinding().getCollectionTable().getPrimaryKey();
		final ForeignKey foreignKey = attributeBinding.getPluralAttributeKeyBinding().getForeignKey();
		final PluralAttributeIndexBinding indexBinding = attributeBinding.getPluralAttributeIndexBinding();
		for ( final Column foreignKeyColumn : foreignKey.getSourceColumns() ) {
			primaryKey.addColumn( foreignKeyColumn );
		}
		final Value value = indexBinding.getIndexRelationalValue();
		if ( value instanceof Column ) {
			primaryKey.addColumn( ( Column ) value );
		}
	}

	LocalBindingContext bindingContext() {
		return bindingContexts.peek();
	}

	private void bindJdbcDataType( final Type resolvedType, final AbstractValue value ) {
		if ( resolvedType != null && value != null ) {
			final Type resolvedRelationalType =
					resolvedType.isEntityType()
							? EntityType.class.cast( resolvedType ).getIdentifierOrUniqueKeyType( metadata )
							: resolvedType;
			value.setJdbcDataType( new JdbcDataType(
					resolvedRelationalType.sqlTypes( metadata )[ 0 ],
					resolvedRelationalType.getName(),
					resolvedRelationalType.getReturnedClass() ) );
		}
	}

	// Foreign Keys ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private void bindForeignKeyColumns(
			ForeignKey foreignKey,
			List< RelationalValueBinding > sourceValueBindings,
			ForeignKeyContributingSource.JoinColumnResolutionDelegate resolutionDelegate,
			ForeignKeyContributingSource.JoinColumnResolutionContext resolutionContext ) {
		final List< ? extends Value > targetValues =
				resolutionDelegate == null
						? foreignKey.getTargetTable().getPrimaryKey().getColumns()
						: resolutionDelegate.getJoinColumns( resolutionContext );

		if ( sourceValueBindings.size() != targetValues.size() ) {
			throw bindingContext().makeMappingException( "Cannot create foreign key because number of columns did not match." );
		}

		for ( int i = 0; i < sourceValueBindings.size(); i++ ) {
			final Value sourceValue = sourceValueBindings.get( i ).getValue();
			final Value targetValue = targetValues.get( i );
			// todo : currently formulas are not supported here... :(
			if ( !Column.class.isInstance( sourceValue ) || !Column.class.isInstance( targetValue ) ) {
				throw new NotYetImplementedException(
						"Derived values are not supported when creating a foreign key that targets attribute columns." );
			}
			foreignKey.addColumnMapping( Column.class.cast( sourceValue ), Column.class.cast( targetValue ) );
		}
	}

	private AttributeBinding determineReferencedAttributeBinding(
			ForeignKeyContributingSource.JoinColumnResolutionDelegate resolutionDelegate,
			ForeignKeyContributingSource.JoinColumnResolutionContext resolutionContext,
			EntityBinding referencedEntityBinding) {
		if ( resolutionDelegate == null ) {
			return referencedEntityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding();
		}

		String explicitName = resolutionDelegate.getReferencedAttributeName();
		if(explicitName != null) {
			return referencedEntityBinding.locateAttributeBinding( explicitName );
		} else {
			return referencedEntityBinding.locateAttributeBinding(resolutionDelegate.getJoinColumns( resolutionContext ) );
		}
	}

	private void bindPrimaryTable( final EntityBinding entityBinding, final EntitySource entitySource ) {
		entityBinding.setPrimaryTable( createTable( entitySource.getPrimaryTable(), new DefaultNamingStrategy() {

			@Override
			public String defaultName() {
				return bindingContexts.peek().getNamingStrategy().classToTableName( entityBinding.getEntity().getClassName() );
			}
		} ) );
	}

	private void bindSecondaryTables( final EntityBinding entityBinding, final EntitySource entitySource ) {
		final TableSpecification primaryTable = entityBinding.getPrimaryTable();
		int position = 0;
		for ( final SecondaryTableSource secondaryTableSource : entitySource.getSecondaryTables() ) {
			final TableSpecification table = createTable( secondaryTableSource.getTableSource(), null );
			final ForeignKey foreignKey =
					createOrLocateForeignKey( secondaryTableSource.getExplicitForeignKeyName(), table, primaryTable );

			final List< Column > fkTargetColumns = determineForeignKeyTargetColumns( entityBinding, secondaryTableSource );
			final List< ColumnSource > pkColumnSources = secondaryTableSource.getPrimaryKeyColumnSources();

			if ( fkTargetColumns.size() != pkColumnSources.size() ) {
				throw bindingContext().makeMappingException(
						String.format(
								"Non-matching number columns in secondary table primary key [%s : %s] and primary table [%s : %s]",
								table.getLogicalName().getName(),
								pkColumnSources.size(),
								primaryTable.getLogicalName().getName(),
								fkTargetColumns.size() ) );
			}

			for ( final ColumnSource joinColumnSource : pkColumnSources ) {
				// todo : apply naming strategy to infer missing column name
				Column column = table.locateColumn( joinColumnSource.getName() );
				if ( column == null ) {
					column = table.createColumn( joinColumnSource.getName() );
					if ( joinColumnSource.getSqlType() != null ) {
						column.setSqlType( joinColumnSource.getSqlType() );
					}
				}

				foreignKey.addColumnMapping( column, fkTargetColumns.get( position++ ) );
			}
			entityBinding.addSecondaryTable( new SecondaryTable( table, foreignKey ) );
		}
	}

	private List< Column > determineForeignKeyTargetColumns(
			final EntityBinding entityBinding,
			ForeignKeyContributingSource foreignKeyContributingSource ) {
		final ForeignKeyContributingSource.JoinColumnResolutionDelegate fkColumnResolutionDelegate =
				foreignKeyContributingSource.getForeignKeyTargetColumnResolutionDelegate();

		if ( fkColumnResolutionDelegate == null ) {
			return entityBinding.getPrimaryTable().getPrimaryKey().getColumns();
		} else {
			final List< Column > columns = new ArrayList< Column >();
			final ForeignKeyContributingSource.JoinColumnResolutionContext resolutionContext =
					new ForeignKeyContributingSource.JoinColumnResolutionContext() {
						@Override
						public List< Value > resolveRelationalValuesForAttribute( String attributeName ) {
							final AttributeBinding referencedAttributeBinding =
									entityBinding.locateAttributeBinding( attributeName );
							if ( referencedAttributeBinding == null ) {
								throw bindingContext().makeMappingException(
										String.format(
												"Could not resolve named property-ref [%s] against entity [%s]",
												attributeName,
												entityBinding.getEntity().getName() ) );
							}
							return null; //To change body of implemented methods use File | Settings | File Templates.
						}

						@Override
						public Column resolveColumn(
								String logicalColumnName,
								String logicalTableName,
								String logicalSchemaName,
								String logicalCatalogName ) {
							// ignore table, schema, catalog name
							Column column = entityBinding.getPrimaryTable().locateColumn( logicalColumnName );
							if ( column == null ) {
								entityBinding.getPrimaryTable().createColumn( logicalColumnName );
							}
							return column;
						}
					};
			for ( Value relationalValue : fkColumnResolutionDelegate.getJoinColumns( resolutionContext ) ) {
				if ( !Column.class.isInstance( relationalValue ) ) {
					throw bindingContext().makeMappingException( "Foreign keys can currently only name columns, not formulas" );
				}
				columns.add( ( Column ) relationalValue );
			}
			return columns;
		}
	}

	private void bindSortingAndOrdering(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeSource attributeSource ) {
		if ( Sortable.class.isInstance( attributeSource ) ) {
			final Sortable sortable = ( Sortable ) attributeSource;
			if ( sortable.isSorted() ) {
				Class< Comparator< ? > > comparatorClass =
						bindingContext().< Comparator< ? > >locateClassByName( sortable.getComparatorName() );
				try {
					attributeBinding.setComparator( comparatorClass.newInstance() );
				} catch ( Exception error ) {
					bindingContext().makeMappingException(
							String.format(
									"Unable to create comparator [%s] for attribute [%s]",
									sortable.getComparatorName(),
									attributeSource.getName() ),
							error );
				}
			}
			return;
		}
		if ( Orderable.class.isInstance( attributeSource ) ) {
			final Orderable orderable = ( Orderable ) attributeSource;
			if ( orderable.isOrdered() ) {
				attributeBinding.setOrderBy( orderable.getOrder() );
			}
		}
	}

	private void bindSubEntities( final EntityBinding entityBinding, final EntitySource entitySource ) {
		for ( final SubclassEntitySource subEntitySource : entitySource.subclassEntitySources() ) {
			bindEntity( subEntitySource, entityBinding );
		}
	}

	private void bindUniqueConstraints( final EntityBinding entityBinding, final EntitySource entitySource ) {
		for ( final ConstraintSource constraintSource : entitySource.getConstraints() ) {
			if ( constraintSource instanceof UniqueConstraintSource ) {
				final TableSpecification table = entityBinding.locateTable( constraintSource.getTableName() );
				final String constraintName = constraintSource.name();
				if ( constraintName == null ) {
					throw new NotYetImplementedException( "create default constraint name" );
				}
				final UniqueKey uniqueKey = table.getOrCreateUniqueKey( constraintName );
				for ( final String columnName : constraintSource.columnNames() ) {
					uniqueKey.addColumn( table.locateOrCreateColumn( quotedIdentifier( columnName ) ) );
				}
			}
		}
	}

	private List< RelationalValueBinding > bindValues(
			final AttributeBindingContainer attributeBindingContainer,
			final RelationalValueSourceContainer valueSourceContainer,
			final Attribute attribute,
			final TableSpecification defaultTable ) {
		final List< RelationalValueBinding > valueBindings = new ArrayList< RelationalValueBinding >();
		if ( valueSourceContainer.relationalValueSources().isEmpty() ) {
			final String columnName =
					quotedIdentifier( bindingContexts.peek().getNamingStrategy().propertyToColumnName( attribute.getName() ) );
			final Column column = defaultTable.locateOrCreateColumn( columnName );
			column.setNullable( valueSourceContainer.areValuesNullableByDefault() );
			valueBindings.add( new RelationalValueBinding( column ) );
		} else {
			final String name = attribute.getName();
			for ( final RelationalValueSource valueSource : valueSourceContainer.relationalValueSources() ) {
				final TableSpecification table =
						valueSource.getContainingTableName() == null
								? defaultTable
								: attributeBindingContainer.seekEntityBinding().locateTable( valueSource.getContainingTableName() );
				if ( valueSource instanceof ColumnSource ) {
					final ColumnSource columnSource = ( ColumnSource ) valueSource;
					final boolean isIncludedInInsert =
							toBoolean(
									columnSource.isIncludedInInsert(),
									valueSourceContainer.areValuesIncludedInInsertByDefault() );
					final boolean isIncludedInUpdate =
							toBoolean(
									columnSource.isIncludedInUpdate(),
									valueSourceContainer.areValuesIncludedInUpdateByDefault() );

					valueBindings.add( new RelationalValueBinding( createColumn(
							table,
							columnSource,
							name,
							valueSourceContainer.areValuesNullableByDefault(),
							true ), isIncludedInInsert, isIncludedInUpdate ) );
				} else {
					final DerivedValue derivedValue =
							table.locateOrCreateDerivedValue( ( ( DerivedValueSource ) valueSource ).getExpression() );
					valueBindings.add( new RelationalValueBinding( derivedValue ) );
				}
			}
		}
		return valueBindings;
	}

	private void bindVersion( final EntityBinding rootEntityBinding, final VersionAttributeSource versionAttributeSource ) {
		if ( versionAttributeSource == null ) {
			return;
		}
		final EntityVersion version = rootEntityBinding.getHierarchyDetails().getEntityVersion();
		version.setVersioningAttributeBinding( ( BasicAttributeBinding ) bindAttribute( rootEntityBinding, versionAttributeSource ) );
		version.setUnsavedValue( versionAttributeSource.getUnsavedValue() == null
				? "undefined"
				: versionAttributeSource.getUnsavedValue() );
	}

	private TableSpecification createCollectionTable(
			final AbstractPluralAttributeBinding pluralAttributeBinding,
			final PluralAttributeSource attributeSource ) {
		final DefaultNamingStrategy defaultNamingStategy = new DefaultNamingStrategy() {

			@Override
			public String defaultName() {
				final EntityBinding owner = pluralAttributeBinding.getContainer().seekEntityBinding();
				final String ownerTableLogicalName =
						Table.class.isInstance( owner.getPrimaryTable() )
								? ( ( Table ) owner.getPrimaryTable() ).getTableName().getName()
								: null;
				return bindingContexts.peek().getNamingStrategy().collectionTableName(
						owner.getEntity().getName(),
						ownerTableLogicalName,
						null, // todo: here
						null, // todo: and here
						createAttributePath( pluralAttributeBinding )
				);
			}
		};
		return createTable( attributeSource.getCollectionTableSpecificationSource(), defaultNamingStategy );
	}

	private String createAttributePath(final AttributeBinding attributeBinding) {
		return new StringBuffer( attributeBinding.getContainer().getPathBase() )
				.append( '.' )
				.append( attributeBinding.getAttribute().getName() )
				.toString();
	}
	
	private Column createColumn(
			final TableSpecification table,
			final ColumnSource columnSource,
			final String defaultName,
			final boolean isNullableByDefault,
			final boolean isDefaultAttributeName ) {
		if ( columnSource.getName() == null && defaultName == null ) {
			throw new MappingException(
					"Cannot resolve name for column because no name was specified and default name is null.",
					bindingContexts.peek().getOrigin() );
		}
		String name;
		if ( columnSource.getName() != null ) {
			name = bindingContexts.peek().getNamingStrategy().columnName( columnSource.getName() );
		} else if ( isDefaultAttributeName ) {
			name = bindingContexts.peek().getNamingStrategy().propertyToColumnName( defaultName );
		} else {
			name = bindingContexts.peek().getNamingStrategy().columnName( defaultName );
		}
		final String resolvedColumnName = quotedIdentifier( name );
		final Column column = table.locateOrCreateColumn( resolvedColumnName );
		column.setNullable( toBoolean( columnSource.isNullable(), isNullableByDefault ) );
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

	private EntityBinding createEntityBinding( final EntitySource entitySource, final EntityBinding superEntityBinding ) {
		// Create binding
		final InheritanceType inheritanceType = inheritanceTypes.peek();
		final EntityMode entityMode = entityModes.peek();
		final EntityBinding entityBinding =
				entitySource instanceof RootEntitySource ? new EntityBinding( inheritanceType, entityMode ) : new EntityBinding(
						superEntityBinding );
		// Create domain entity
		final String entityClassName = entityMode == EntityMode.POJO ? entitySource.getClassName() : null;
		LocalBindingContext bindingContext = bindingContexts.peek();
		entityBinding.setEntity( new Entity(
				entitySource.getEntityName(),
				entityClassName,
				bindingContext.makeClassReference( entityClassName ),
				superEntityBinding == null ? null : superEntityBinding.getEntity() ) );
		// Create relational table
		if ( superEntityBinding != null && inheritanceType == InheritanceType.SINGLE_TABLE ) {
			entityBinding.setPrimaryTable( superEntityBinding.getPrimaryTable() );
			entityBinding.setPrimaryTableName( superEntityBinding.getPrimaryTableName() );
			// Configure discriminator if present
			final String discriminatorValue = entitySource.getDiscriminatorMatchValue();
			if ( discriminatorValue != null ) {
				entityBinding.setDiscriminatorMatchValue( discriminatorValue );
			}
		} else {
			bindPrimaryTable( entityBinding, entitySource );
		}
		// todo: deal with joined and unioned subclass bindings
		// todo: bind fetch profiles
		// Configure rest of binding
		final String customTuplizerClassName = entitySource.getCustomTuplizerClassName();
		if ( customTuplizerClassName != null ) {
			entityBinding.setCustomEntityTuplizerClass( bindingContext.< EntityTuplizer >locateClassByName( customTuplizerClassName ) );
		}
		final String customPersisterClassName = entitySource.getCustomPersisterClassName();
		if ( customPersisterClassName != null ) {
			entityBinding.setCustomEntityPersisterClass( bindingContext.< EntityPersister >locateClassByName( customPersisterClassName ) );
		}
		entityBinding.setMetaAttributeContext( createMetaAttributeContext(
				entitySource.metaAttributes(),
				true,
				metadata.getGlobalMetaAttributeContext() ) );
		entityBinding.setJpaEntityName( entitySource.getJpaEntityName() );
		entityBinding.setDynamicUpdate( entitySource.isDynamicUpdate() );
		entityBinding.setDynamicInsert( entitySource.isDynamicInsert() );
		entityBinding.setBatchSize( entitySource.getBatchSize() );
		entityBinding.setSelectBeforeUpdate( entitySource.isSelectBeforeUpdate() );
		entityBinding.setAbstract( entitySource.isAbstract() );

		entityBinding.setCustomLoaderName( entitySource.getCustomLoaderName() );
		entityBinding.setCustomInsert( entitySource.getCustomSqlInsert() );
		entityBinding.setCustomUpdate( entitySource.getCustomSqlUpdate() );
		entityBinding.setCustomDelete( entitySource.getCustomSqlDelete() );
		entityBinding.setJpaCallbackClasses( entitySource.getJpaCallbackClasses() );
		if ( entitySource.getSynchronizedTableNames() != null ) {
			entityBinding.addSynchronizedTableNames( entitySource.getSynchronizedTableNames() );
		}
		if ( entityMode == EntityMode.POJO ) {
			final String proxy = entitySource.getProxy();
			if ( proxy == null ) {
				if ( entitySource.isLazy() ) {
					entityBinding.setProxyInterfaceType( entityBinding.getEntity().getClassReferenceUnresolved() );
					entityBinding.setLazy( true );
				}
			} else {
				entityBinding.setProxyInterfaceType( bindingContext.makeClassReference( bindingContext.qualifyClassName( proxy ) ) );
				entityBinding.setLazy( true );
			}
		} else {
			entityBinding.setProxyInterfaceType( null );
			entityBinding.setLazy( entitySource.isLazy() );
		}
		bindSecondaryTables( entityBinding, entitySource );
		bindUniqueConstraints( entityBinding, entitySource );
		// Register binding with metadata
		metadata.addEntity( entityBinding );
		return entityBinding;
	}

	private Identifier createIdentifier( String name, final String defaultName ) {
		if ( StringHelper.isEmpty( name ) ) {
			name = defaultName;
		}
		name = quotedIdentifier( name );
		return Identifier.toIdentifier( name );
	}

	private void createIdentifierGenerator( final EntityBinding rootEntityBinding ) {
		final Properties properties = new Properties();
		properties.putAll( metadata.getServiceRegistry().getService( ConfigurationService.class ).getSettings() );
		if ( !properties.contains( AvailableSettings.PREFER_POOLED_VALUES_LO ) ) {
			properties.put( AvailableSettings.PREFER_POOLED_VALUES_LO, "false" );
		}
		if ( !properties.contains( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER ) ) {
			properties.put( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER, nameNormalizer );
		}
		rootEntityBinding.getHierarchyDetails().getEntityIdentifier().createIdentifierGenerator(
				identifierGeneratorFactory,
				properties );
	}

	private MetaAttributeContext createMetaAttributeContext(
			final AttributeBindingContainer attributeBindingContainer,
			final AttributeSource attributeSource ) {
		return createMetaAttributeContext( attributeBindingContainer, attributeSource.metaAttributes() );
	}

	private MetaAttributeContext createMetaAttributeContext(
			final AttributeBindingContainer attributeBindingContainer,
			final Iterable< MetaAttributeSource > metaAttributeSources ) {
		return createMetaAttributeContext( metaAttributeSources, false, attributeBindingContainer.getMetaAttributeContext() );
	}

	private MetaAttributeContext createMetaAttributeContext(
			final Iterable< MetaAttributeSource > metaAttributeSources,
			final boolean onlyInheritable,
			final MetaAttributeContext parentContext ) {
		final MetaAttributeContext subContext = new MetaAttributeContext( parentContext );
		for ( final MetaAttributeSource metaAttributeSource : metaAttributeSources ) {
			if ( onlyInheritable && !metaAttributeSource.isInheritable() ) {
				continue;
			}
			final String name = metaAttributeSource.getName();
			MetaAttribute metaAttribute = subContext.getLocalMetaAttribute( name );
			if ( metaAttribute == null || metaAttribute == parentContext.getMetaAttribute( name ) ) {
				metaAttribute = new MetaAttribute( name );
				subContext.add( metaAttribute );
			}
			metaAttribute.addValue( metaAttributeSource.getValue() );
		}
		return subContext;
	}

	//	private ForeignKey createOrLocateForeignKey(
	//			String foreignKeyName,
	//			SingularAttributeBinding sourceAttributeBinding,
	//			SingularAttributeBinding targetAttributeBinding ) {
	//		if ( sourceAttributeBinding.getRelationalValueBindings().isEmpty() ) {
	//			throw new MappingException( String.format(
	//					"Cannot create foreign key for attribute (%s) because it has no columns/derived values configured.",
	//					sourceAttributeBinding.getAttribute().getName() ), bindingContexts.peek().getOrigin() );
	//		}
	//		if ( targetAttributeBinding.getRelationalValueBindings().isEmpty() ) {
	//			throw new MappingException(
	//					String.format(
	//							"Cannot create foreign key for attribute (%s) because the target attribute (%s) has no columns/derived values configured.",
	//							sourceAttributeBinding.getAttribute().getName(),
	//							targetAttributeBinding.getAttribute().getName() ),
	//					bindingContexts.peek().getOrigin() );
	//		}
	//		// TODO: deal with secondary tables
	//		// for now just use the the EntityBinding primary table.
	//		return createOrLocateForeignKey(
	//				foreignKeyName,
	//				sourceAttributeBinding.getContainer().seekEntityBinding().getPrimaryTable(),
	//				targetAttributeBinding.getContainer().seekEntityBinding().getPrimaryTable()
	//		);
	//	}

	private ForeignKey createOrLocateForeignKey(
			String foreignKeyName,
			TableSpecification sourceTable,
			TableSpecification targetTable ) {
		ForeignKey foreignKey;
		if ( foreignKeyName == null ) {
			// todo: for now lets assume we have to create it, but eventually we should look through the
			// candidate foreign keys referencing targetTable also...
			foreignKey = sourceTable.createForeignKey( targetTable, null );
		} else {
			foreignKey = sourceTable.locateForeignKey( foreignKeyName );
			if ( foreignKey == null ) {
				foreignKey = sourceTable.createForeignKey( targetTable, foreignKeyName );
			}
		}
		return foreignKey;
	}

	private SingularAttribute createSingularAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeSource attributeSource ) {
		return attributeSource.isVirtualAttribute()
				? attributeBindingContainer.getAttributeContainer().createSyntheticSingularAttribute( attributeSource.getName() )
				: attributeBindingContainer.getAttributeContainer().createSingularAttribute( attributeSource.getName() );
	}

	private TableSpecification createTable(
			final TableSpecificationSource tableSpecSource,
			final DefaultNamingStrategy defaultNamingStrategy ) {
		final LocalBindingContext bindingContext = bindingContexts.peek();
		final MappingDefaults mappingDefaults = bindingContext.getMappingDefaults();
		final Schema.Name schemaName =
				new Schema.Name(
						createIdentifier( tableSpecSource.getExplicitSchemaName(), mappingDefaults.getSchemaName() ),
						createIdentifier( tableSpecSource.getExplicitCatalogName(), mappingDefaults.getCatalogName() ) );
		final Schema schema = metadata.getDatabase().locateSchema( schemaName );
		if ( tableSpecSource instanceof TableSource ) {
			final TableSource tableSource = ( TableSource ) tableSpecSource;
			String tableName = tableSource.getExplicitTableName();
			if ( tableName == null ) {
				if ( defaultNamingStrategy == null ) {
					throw new MappingException( "An explicit name must be specified for the table", bindingContext.getOrigin() );
				}
				tableName = defaultNamingStrategy.defaultName();
			}
			tableName = quotedIdentifier( tableName );
			final Identifier logicalTableId = Identifier.toIdentifier( tableName );
			tableName = quotedIdentifier( bindingContext.getNamingStrategy().tableName( tableName ) );
			final Identifier physicalTableId = Identifier.toIdentifier( tableName );
			final Table table = schema.locateTable( logicalTableId );
			return ( table == null ? schema.createTable( logicalTableId, physicalTableId ) : table );
		}
		final InLineViewSource inLineViewSource = ( InLineViewSource ) tableSpecSource;
		return schema.createInLineView(
				Identifier.toIdentifier( inLineViewSource.getLogicalName() ),
				inLineViewSource.getSelectStatement()
		);
	}

	private String defaultCollectionElementJavaTypeName(
			HibernateTypeHelper.ReflectedCollectionJavaTypes reflectedCollectionJavaTypes ) {
		return reflectedCollectionJavaTypes != null && reflectedCollectionJavaTypes.getCollectionElementType() != null
				? reflectedCollectionJavaTypes.getCollectionElementType().getName()
				: null;
	}

	private String defaultCollectionIndexJavaTypeName(
			HibernateTypeHelper.ReflectedCollectionJavaTypes reflectedCollectionJavaTypes ) {
		return reflectedCollectionJavaTypes != null && reflectedCollectionJavaTypes.getCollectionIndexType() != null
				? reflectedCollectionJavaTypes.getCollectionIndexType().getName()
				: null;
	}

	private String defaultCollectionJavaTypeName(
			HibernateTypeHelper.ReflectedCollectionJavaTypes reflectedCollectionJavaTypes,
			PluralAttributeSource attributeSource ) {
		return reflectedCollectionJavaTypes != null && reflectedCollectionJavaTypes.getCollectionType() != null
				? reflectedCollectionJavaTypes.getCollectionType().getName()
				: attributeSource.getPluralAttributeNature().reportedJavaType().getName();
	}

	private EntityBinding entityBinding( final String entityName ) {
		// Check if binding has already been created
		EntityBinding entityBinding = metadata.getEntityBinding( entityName );
		if ( entityBinding == null ) {
			// Find appropriate source to create binding
			final EntitySource entitySource = entitySourcesByName.get( entityName );
			// Get super entity binding (creating it if necessary using recursive call to this method)
			final EntityBinding superEntityBinding =
					entitySource instanceof SubclassEntitySource
							? entityBinding( ( ( SubclassEntitySource ) entitySource ).superclassEntitySource().getEntityName() )
							: null;
			// Create entity binding
			entityBinding =
					superEntityBinding == null
							? bindEntities( entityHierarchiesByRootEntitySource.get( entitySource ) )
							: bindEntity( entitySource, superEntityBinding );
		}
		return entityBinding;
	}

	private Type heuristicType( HibernateTypeDescriptor hibernateTypeDescriptor ) {
		final String typeName =
				hibernateTypeDescriptor.getExplicitTypeName() != null
						? hibernateTypeDescriptor.getExplicitTypeName()
						: hibernateTypeDescriptor.getJavaTypeName();
		final Properties properties = new Properties();
		properties.putAll( hibernateTypeDescriptor.getTypeParameters() );
		return metadata.getTypeResolver().heuristicType( typeName, properties );
	}

	private void mapSourcesByName( final EntitySource entitySource ) {
		String entityName = entitySource.getEntityName();
		entitySourcesByName.put( entityName, entitySource );
		log.debugf( "Mapped entity source \"%s\"", entityName );
		for ( final AttributeSource attributeSource : entitySource.attributeSources() ) {
			String key = attributeSourcesByNameKey( entityName, attributeSource.getName() );
			attributeSourcesByName.put( key, attributeSource );
			log.debugf( "Mapped attribute source \"%s\" for entity source \"%s\"", key, entitySource.getEntityName() );
		}
		for ( final SubclassEntitySource subclassEntitySource : entitySource.subclassEntitySources() ) {
			mapSourcesByName( subclassEntitySource );
		}
	}

	private PluralAttributeElementNature pluralAttributeElementNature( final PluralAttributeSource attributeSource ) {
		return PluralAttributeElementNature.valueOf( attributeSource.getElementSource().getNature().name() );
	}

	private SingularAttributeBinding pluralAttributeKeyBinding(
			final AttributeBindingContainer attributeBindingContainer,
			final PluralAttributeSource attributeSource ) {
		final EntityBinding entityBinding = attributeBindingContainer.seekEntityBinding();
		final ForeignKeyContributingSource.JoinColumnResolutionDelegate resolutionDelegate =
				attributeSource.getKeySource().getForeignKeyTargetColumnResolutionDelegate();

		if ( resolutionDelegate == null ) {
			return entityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding();
		}

		final String explicitName = resolutionDelegate.getReferencedAttributeName();
		final String referencedAttributeName;
		if ( explicitName == null ) {
			throw new NotYetImplementedException( "Annotation-style property-ref resolution not yet implemented" );
		} else {
			referencedAttributeName = explicitName;
		}

		AttributeBinding referencedAttributeBinding = attributeBindingContainer.locateAttributeBinding( referencedAttributeName );

		if ( referencedAttributeBinding == null ) {
			referencedAttributeBinding = attributeBinding( entityBinding.getEntity().getName(), referencedAttributeName );
		}
		if ( referencedAttributeBinding == null ) {
			throw new MappingException( "Plural atttribute key references an attribute binding that does not exist: "
					+ referencedAttributeBinding, bindingContexts.peek().getOrigin() );
		}
		if ( !referencedAttributeBinding.getAttribute().isSingular() ) {
			throw new MappingException( "Plural atttribute key references a plural attribute; it must be plural: "
					+ referencedAttributeName, bindingContexts.peek().getOrigin() );
		}
		return ( SingularAttributeBinding ) referencedAttributeBinding;
	}

	private String propertyAccessorName( final AttributeSource attributeSource ) {
		return attributeSource.getPropertyAccessorName() == null
				? bindingContexts.peek().getMappingDefaults().getPropertyAccessorName()
				: attributeSource.getPropertyAccessorName();
	}

	private String quotedIdentifier( final String name ) {
		return bindingContexts.peek().isGloballyQuotedIdentifiers() ? StringHelper.quote( name ) : name;
	}

	private Type resolveBagType( BagBinding bagBinding ) {
		if ( bagBinding.getHibernateTypeDescriptor().getExplicitTypeName() != null ) {
			return resolveCustomCollectionType( bagBinding );
		} else {
			return metadata.getTypeResolver().getTypeFactory().bag(
					bagBinding.getAttribute().getRole(),
					bagBinding.getReferencedPropertyName(),
					bagBinding.getPluralAttributeElementBinding().getPluralAttributeElementNature() == PluralAttributeElementNature.COMPOSITE );
		}
	}

	private Type resolveCustomCollectionType( PluralAttributeBinding pluralAttributeBinding ) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = pluralAttributeBinding.getHibernateTypeDescriptor();
		Properties typeParameters = new Properties();
		typeParameters.putAll( hibernateTypeDescriptor.getTypeParameters() );
		return metadata.getTypeResolver().getTypeFactory().customCollection(
				hibernateTypeDescriptor.getExplicitTypeName(),
				typeParameters,
				pluralAttributeBinding.getAttribute().getName(),
				pluralAttributeBinding.getReferencedPropertyName(),
				pluralAttributeBinding.getPluralAttributeElementBinding().getPluralAttributeElementNature() == PluralAttributeElementNature.COMPOSITE );
	}

	private Type resolveListType( ListBinding listBinding ) {
		if ( listBinding.getHibernateTypeDescriptor().getExplicitTypeName() != null ) {
			return resolveCustomCollectionType( listBinding );
		} else {
			return metadata.getTypeResolver().getTypeFactory().list(
					listBinding.getAttribute().getRole(),
					listBinding.getReferencedPropertyName(),
					listBinding.getPluralAttributeElementBinding()
							.getPluralAttributeElementNature() == PluralAttributeElementNature.COMPOSITE
			);
		}
	}

	private Type resolveMapType( MapBinding mapBinding ) {
		if ( mapBinding.getHibernateTypeDescriptor().getExplicitTypeName() != null ) {
			return resolveCustomCollectionType( mapBinding );
		} else {
			return metadata.getTypeResolver().getTypeFactory().map(
					mapBinding.getAttribute().getRole(),
					mapBinding.getReferencedPropertyName(),
					mapBinding.getPluralAttributeElementBinding()
							.getPluralAttributeElementNature() == PluralAttributeElementNature.COMPOSITE
			);
		}
	}

	private Type resolveSetType( SetBinding setBinding ) {
		if ( setBinding.getHibernateTypeDescriptor().getExplicitTypeName() != null ) {
			return resolveCustomCollectionType( setBinding );
		} else {
			return metadata.getTypeResolver().getTypeFactory().set(
					setBinding.getAttribute().getRole(),
					setBinding.getReferencedPropertyName(),
					setBinding.getPluralAttributeElementBinding()
							.getPluralAttributeElementNature() == PluralAttributeElementNature.COMPOSITE
			);
		}
	}

	private static org.hibernate.internal.util.Value< Class< ? >> createSingularAttributeJavaType(
			final SingularAttribute attribute ) {
		org.hibernate.internal.util.Value.DeferredInitializer< Class< ? >> deferredInitializer =
				new org.hibernate.internal.util.Value.DeferredInitializer< Class< ? >>() {
					public Class< ? > initialize() {
						return ReflectHelper.reflectedPropertyClass(
								attribute.getAttributeContainer().getClassReference(),
								attribute.getName() );
					}
				};
		return new org.hibernate.internal.util.Value< Class< ? >>( deferredInitializer );
	}

	private static String interpretIdentifierUnsavedValue( IdentifierSource identifierSource, IdGenerator generator ) {
		if ( identifierSource == null ) {
			throw new IllegalArgumentException( "identifierSource must be non-null." );
		}
		if ( generator == null || StringHelper.isEmpty( generator.getStrategy() ) ) {
			throw new IllegalArgumentException( "generator must be non-null and its strategy must be non-empty." );
		}
		String unsavedValue = null;
		if ( identifierSource.getUnsavedValue() != null ) {
			unsavedValue = identifierSource.getUnsavedValue();
		} else if ( "assigned".equals( generator.getStrategy() ) ) {
			unsavedValue = "undefined";
		} else {
			switch ( identifierSource.getNature() ) {
				case SIMPLE: {
					// unsavedValue = null;
					break;
				}
				case COMPOSITE: {
					// The generator strategy should be "assigned" and processed above.
					throw new IllegalStateException( String.format(
							"Expected generator strategy for composite ID: 'assigned'; instead it is: %s",
							generator.getStrategy() ) );
				}
				case AGGREGATED_COMPOSITE: {
					// TODO: if the component only contains 1 attribute (when flattened)
					// and it is not an association then null should be returned;
					// otherwise "undefined" should be returned.
					throw new NotYetImplementedException( String.format(
							"Unsaved value for (%s) identifier not implemented yet.",
							identifierSource.getNature() ) );
				}
				default: {
					throw new AssertionFailure( String.format( "Unexpected identifier nature: %s", identifierSource.getNature() ) );
				}
			}
		}
		return unsavedValue;
	}

	private static boolean toBoolean( final TruthValue truthValue, final boolean truthValueDefault ) {
		if ( truthValue == TruthValue.TRUE ) {
			return true;
		}
		if ( truthValue == TruthValue.FALSE ) {
			return false;
		}
		return truthValueDefault;
	}

	private interface DefaultNamingStrategy {

		String defaultName();
	}


	public class JoinColumnResolutionContext implements ForeignKeyContributingSource.JoinColumnResolutionContext {
		private final EntityBinding referencedEntityBinding;
		private final ToOneAttributeSource attributeSource;


		public JoinColumnResolutionContext(EntityBinding referencedEntityBinding, ToOneAttributeSource attributeSource) {
			this.referencedEntityBinding = referencedEntityBinding;
			this.attributeSource = attributeSource;
		}

		@Override
		public List< Value > resolveRelationalValuesForAttribute( String attributeName ) {
			final AttributeBinding referencedAttributeBinding =
					referencedEntityBinding.locateAttributeBinding( attributeName );
			if ( !referencedAttributeBinding.getAttribute().isSingular() ) {
				throw bindingContext().makeMappingException(
						String.format(
								"Many-to-one attribute [%s] named plural attribute as property-ref [%s]",
								attributeSource.getName(),
								attributeName ) );
			}
			List< Value > values = new ArrayList< Value >();
			SingularAttributeBinding referencedAttributeBindingAsSingular =
					( SingularAttributeBinding ) referencedAttributeBinding;
			for ( RelationalValueBinding valueBinding : referencedAttributeBindingAsSingular.getRelationalValueBindings() ) {
				values.add( valueBinding.getValue() );
			}
			return values;
		}

		@Override
		public Column resolveColumn(
				String logicalColumnName,
				String logicalTableName,
				String logicalSchemaName,
				String logicalCatalogName ) {
			Identifier tableIdentifier = Identifier.toIdentifier(logicalTableName);
			if(tableIdentifier == null) {
				tableIdentifier = referencedEntityBinding.getPrimaryTable().getLogicalName();
			}

			Schema schema = metadata.getDatabase().getSchema( logicalSchemaName, logicalCatalogName );
			Table table = schema.locateTable(tableIdentifier );

			if(bindingContexts.peek().isGloballyQuotedIdentifiers() && !StringHelper.isQuoted(logicalColumnName)) {
				logicalColumnName = StringHelper.quote( logicalColumnName );
			}

			return table.locateColumn( logicalColumnName );
		}
	}
}
