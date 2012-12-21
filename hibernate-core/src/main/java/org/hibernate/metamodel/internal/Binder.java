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

import static org.hibernate.engine.spi.SyntheticAttributeHelper.SYNTHETIC_COMPOSITE_ID_ATTRIBUTE_NAME;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.TruthValue;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.internal.HibernateTypeHelper.ReflectedCollectionJavaTypes;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicPluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.BasicPluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.Cascadeable;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.CompositePluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityDiscriminator;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.metamodel.spi.binding.EntityVersion;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.IdGenerator;
import org.hibernate.metamodel.spi.binding.IndexedPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.metamodel.spi.binding.ManyToManyPluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.MetaAttribute;
import org.hibernate.metamodel.spi.binding.OneToManyPluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.OneToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeKeyBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SecondaryTable;
import org.hibernate.metamodel.spi.binding.SetBinding;
import org.hibernate.metamodel.spi.binding.SingularAssociationAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding.NaturalIdMutability;
import org.hibernate.metamodel.spi.binding.TypeDefinition;
import org.hibernate.metamodel.spi.domain.Aggregate;
import org.hibernate.metamodel.spi.domain.Attribute;
import org.hibernate.metamodel.spi.domain.Entity;
import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.AbstractValue;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.DerivedValue;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Identifier;
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
import org.hibernate.metamodel.spi.source.CompositePluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.ConstraintSource;
import org.hibernate.metamodel.spi.source.DerivedValueSource;
import org.hibernate.metamodel.spi.source.DiscriminatorSource;
import org.hibernate.metamodel.spi.source.EntityHierarchy;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.ForeignKeyContributingSource;
import org.hibernate.metamodel.spi.source.ForeignKeyContributingSource.JoinColumnResolutionContext;
import org.hibernate.metamodel.spi.source.ForeignKeyContributingSource.JoinColumnResolutionDelegate;
import org.hibernate.metamodel.spi.source.IdentifierSource;
import org.hibernate.metamodel.spi.source.InLineViewSource;
import org.hibernate.metamodel.spi.source.IndexedPluralAttributeSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.ManyToManyPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.MappingDefaults;
import org.hibernate.metamodel.spi.source.MetaAttributeContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.MultiTenancySource;
import org.hibernate.metamodel.spi.source.NonAggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.spi.source.OneToManyPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.Orderable;
import org.hibernate.metamodel.spi.source.PluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.PluralAttributeIndexSource;
import org.hibernate.metamodel.spi.source.PluralAttributeKeySource;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.metamodel.spi.source.PrimaryKeyJoinColumnSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.RelationalValueSourceContainer;
import org.hibernate.metamodel.spi.source.RootEntitySource;
import org.hibernate.metamodel.spi.source.SecondaryTableSource;
import org.hibernate.metamodel.spi.source.SimpleIdentifierSource;
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
import org.hibernate.tuple.component.ComponentMetamodel;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.type.ComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.jboss.logging.Logger;

/**
 * The common binder shared between annotations and {@code hbm.xml} processing.
 * <p/>
 * The API consists of {@link #Binder(org.hibernate.metamodel.spi.MetadataImplementor, IdentifierGeneratorFactory)} and {@link #bindEntityHierarchies}
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Gail Badner
 * @author Brett Meyer
 */
public class Binder {
	private static final CoreMessageLogger log = Logger.getMessageLogger(
			CoreMessageLogger.class,
			Binder.class.getName()
	);

	private static ValueHolder< Class< ? >> createSingularAttributeJavaType(
			final Class< ? > attributeContainerClassReference,
			final String attributeName ) {
		ValueHolder.DeferredInitializer< Class< ? >> deferredInitializer =
				new ValueHolder.DeferredInitializer< Class< ? >>() {
					public Class< ? > initialize() {
						return ReflectHelper.reflectedPropertyClass(
								attributeContainerClassReference,
								attributeName );
					}
				};
		return new ValueHolder< Class< ? >>( deferredInitializer );
	}

	private static ValueHolder< Class< ? >> createSingularAttributeJavaType(
			final SingularAttribute attribute ) {
		return createSingularAttributeJavaType(
				attribute.getAttributeContainer().getClassReference(),
				attribute.getName()
		);
	}

	private static String interpretIdentifierUnsavedValue( final IdentifierSource identifierSource,final IdGenerator generator ) {
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
				case NON_AGGREGATED_COMPOSITE: {
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

	private final MetadataImplementor metadata;
	private final IdentifierGeneratorFactory identifierGeneratorFactory;
	private final ObjectNameNormalizer nameNormalizer;
	private final HashMap<String, EntitySource> entitySourcesByName = new HashMap<String, EntitySource>();
	private final HashMap<RootEntitySource, EntityHierarchy> entityHierarchiesByRootEntitySource =
			new HashMap<RootEntitySource, EntityHierarchy>();
	private final HashMap<String, AttributeSource> attributeSourcesByName = new HashMap<String, AttributeSource>();
	// todo : apply org.hibernate.metamodel.MetadataSources.getExternalCacheRegionDefinitions()
	private final LinkedList<LocalBindingContext> bindingContexts = new LinkedList<LocalBindingContext>();
	private final LinkedList<InheritanceType> inheritanceTypes = new LinkedList<InheritanceType>();
	private final LinkedList<EntityMode> entityModes = new LinkedList<EntityMode>();
	private final HibernateTypeHelper typeHelper; // todo: refactor helper and remove redundant methods in this class

	public Binder( final MetadataImplementor metadata, final IdentifierGeneratorFactory identifierGeneratorFactory ) {
		this.metadata = metadata;
		this.identifierGeneratorFactory = identifierGeneratorFactory;
		this.typeHelper = new HibernateTypeHelper( this, metadata );
		this.nameNormalizer = metadata.getObjectNameNormalizer();
	}

	private static void addUniqueConstraintForNaturalIdColumn(final TableSpecification table, final Column column) {
		final UniqueKey uniqueKey = table.getOrCreateUniqueKey( "natural_id_unique_key_" );
		uniqueKey.addColumn( column );
	}

	private AttributeBinding attributeBinding( final String entityName, final String attributeName ) {
		// Check if binding has already been created
		final EntityBinding entityBinding = findOrBindEntityBinding( entityName );
		final AttributeSource attributeSource = attributeSourcesByName.get( attributeSourcesByNameKey( entityName, attributeName ) );
		bindAttribute( entityBinding, attributeSource );
		return entityBinding.locateAttributeBinding( attributeName );
	}

	private static String attributeSourcesByNameKey( final String entityName, final String attributeName ) {
		return entityName + "." + attributeName;
	}

	private void bindAggregatedCompositeIdentifier(
			EntityBinding rootEntityBinding,
			AggregatedCompositeIdentifierSource identifierSource ) {
		// locate the attribute binding
		final CompositeAttributeBinding idAttributeBinding =
				(CompositeAttributeBinding) bindIdentifierAttribute(
						rootEntityBinding, identifierSource.getIdentifierAttributeSource()
				);

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

	private AttributeBinding bindAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final AttributeSource attributeSource) {
		// Return existing binding if available
		final String attributeName = attributeSource.getName();
		final AttributeBinding attributeBinding = attributeBindingContainer.locateAttributeBinding( attributeName );
		if ( attributeBinding != null ) {
			return attributeBinding;
		}
		return attributeSource.isSingular() ?
				bindSingularAttribute(
						attributeBindingContainer,
						SingularAttributeSource.class.cast( attributeSource ),
						false
				) :
				bindPluralAttribute( attributeBindingContainer,PluralAttributeSource.class.cast( attributeSource ) );
	}

	private void bindAttributes(
			final CompositeAttributeBindingContainer compositeAttributeBindingContainer,
			final AttributeSourceContainer attributeSourceContainer
	) {
		if (  compositeAttributeBindingContainer.getParentReference() == null ) {
			bindAttributes(
					(AttributeBindingContainer) compositeAttributeBindingContainer,
					attributeSourceContainer
			);
		}
		else {
			for ( final AttributeSource subAttributeSource : attributeSourceContainer.attributeSources() ) {
				if ( ! subAttributeSource.getName().equals( compositeAttributeBindingContainer.getParentReference().getName() ) ) {
					bindAttribute(
							compositeAttributeBindingContainer,
							subAttributeSource
					);
				}
			}
		}

	}

	private void bindAttributes(
			final AttributeBindingContainer attributeBindingContainer,
			final AttributeSourceContainer attributeSourceContainer ) {
		for ( final AttributeSource attributeSource : attributeSourceContainer.attributeSources() ) {
			bindAttribute( attributeBindingContainer, attributeSource );
		}
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
				determinePluralAttributeKeyReferencedBinding( attributeBindingContainer, attributeSource ),
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				createMetaAttributeContext( attributeBindingContainer, attributeSource )
		);
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
						attributeBindingContainer.getPrimaryTable(),
						false);
		final BasicAttributeBinding attributeBinding =
				attributeBindingContainer.makeBasicAttributeBinding(
						attribute,
						relationalValueBindings,
						propertyAccessorName( attributeSource ),
						attributeSource.isIncludedInOptimisticLocking(),
						attributeSource.isLazy(),
						attributeSource.getNaturalIdMutability(),
						createMetaAttributeContext( attributeBindingContainer, attributeSource ),
						attributeSource.getGeneration() );
		typeHelper.bindSingularAttributeTypeInformation( attributeSource,
				attributeBinding );
		return attributeBinding;
	}

	private void bindBasicCollectionElement(
			final BasicPluralAttributeElementBinding elementBinding,
			final BasicPluralAttributeElementSource elementSource,
			final String defaultElementJavaTypeName) {
		bindBasicPluralElementRelationalValues( elementSource, elementBinding );
		bindBasicCollectionElementType( elementBinding, elementSource, defaultElementJavaTypeName );
	}

	private void bindBasicCollectionElementType(
			final BasicPluralAttributeElementBinding elementBinding,
			final BasicPluralAttributeElementSource elementSource,
			final String defaultElementJavaTypeName) {
		bindHibernateTypeDescriptor(
				elementBinding.getHibernateTypeDescriptor(),
				elementSource.getExplicitHibernateTypeSource(),
				defaultElementJavaTypeName
		);
		Type resolvedElementType = heuristicType( elementBinding.getHibernateTypeDescriptor() );
		bindHibernateResolvedType( elementBinding.getHibernateTypeDescriptor(), resolvedElementType );
		typeHelper.bindJdbcDataType(
				resolvedElementType,
				elementBinding.getRelationalValueBindings()
		);
	}

	private void bindNonAssociationCollectionKey(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeSource attributeSource) {
		if ( attributeSource.getElementSource().getNature() != PluralAttributeElementSource.Nature.BASIC &&
				attributeSource.getElementSource().getNature() != PluralAttributeElementSource.Nature.AGGREGATE) {
			throw new AssertionFailure(
					String.format(
							"Expected basic or aggregate attribute binding; instead got {%s}",
							attributeSource.getElementSource().getNature()
					)
			);
		}
		attributeBinding.getPluralAttributeKeyBinding().setInverse( false );
		TableSpecification collectionTable = createBasicCollectionTable(
				attributeBinding, attributeSource.getCollectionTableSpecificationSource()
		);
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

	private void bindBasicPluralElementRelationalValues(
			final RelationalValueSourceContainer relationalValueSourceContainer,
			final BasicPluralAttributeElementBinding elementBinding ) {
		elementBinding.setRelationalValueBindings( bindValues(
				elementBinding.getPluralAttributeBinding().getContainer(),
				relationalValueSourceContainer,
				elementBinding.getPluralAttributeBinding().getAttribute(),
				elementBinding.getPluralAttributeBinding().getPluralAttributeKeyBinding().getCollectionTable(),
				false) );
	}

	private void bindBasicSetCollectionTablePrimaryKey(final SetBinding attributeBinding) {
		final BasicPluralAttributeElementBinding elementBinding =
				( BasicPluralAttributeElementBinding ) attributeBinding.getPluralAttributeElementBinding();
		if ( elementBinding.getNature() != PluralAttributeElementBinding.Nature.BASIC ) {
			throw bindingContext().makeMappingException( String.format(
					"Expected a SetBinding with an element of nature Nature.BASIC; instead was %s",
					elementBinding.getNature() ) );
		}
		if ( hasAnyNonNullableColumns( elementBinding.getRelationalValueBindings() ) ) {
			bindSetCollectionTablePrimaryKey( attributeBinding );
		}
		else {
			// for backward compatibility, allow a set with no not-null
			// element columns, using all columns in the row locater SQL
			// todo: create an implicit not null constraint on all cols?
		}
	}

	private void bindSetCollectionTablePrimaryKey(final SetBinding attributeBinding) {
		final PluralAttributeElementBinding elementBinding = attributeBinding.getPluralAttributeElementBinding();
		final PrimaryKey primaryKey = attributeBinding.getPluralAttributeKeyBinding().getCollectionTable().getPrimaryKey();
		final ForeignKey foreignKey = attributeBinding.getPluralAttributeKeyBinding().getForeignKey();
		for ( final Column foreignKeyColumn : foreignKey.getSourceColumns() ) {
			primaryKey.addColumn( foreignKeyColumn );
		}
		for ( final RelationalValueBinding elementValueBinding : elementBinding.getRelationalValueBindings() ) {
			if ( elementValueBinding.getValue() instanceof Column && !elementValueBinding.isNullable() ) {
				primaryKey.addColumn( ( Column ) elementValueBinding.getValue() );
			}
		}
	}

	private void bindCompositeCollectionElement(
			final CompositePluralAttributeElementBinding elementBinding,
			final CompositePluralAttributeElementSource elementSource,
			final String defaultElementJavaTypeName ) {
		final PluralAttributeBinding pluralAttributeBinding = elementBinding.getPluralAttributeBinding();
		ValueHolder<Class<?>> defaultElementJavaClassReference = null;
		// Create the aggregate type
		// TODO: aggregateName should be set to elementSource.getPath() (which is currently not implemented)
		//       or Binder should define AttributeBindingContainer paths instead.
		String aggregateName = pluralAttributeBinding.getAttribute().getRole() + ".element";
		final Aggregate aggregate;
		if ( elementSource.getClassName() != null ) {
			aggregate = new Aggregate(
					aggregateName,
					elementSource.getClassName(),
					elementSource.getClassReference() != null ?
							elementSource.getClassReference() :
							bindingContext().makeClassReference( elementSource.getClassName() ),
					null
			);
		}
		else {
			defaultElementJavaClassReference = bindingContext().makeClassReference( defaultElementJavaTypeName );
			aggregate = new Aggregate(
					aggregateName,
					defaultElementJavaClassReference.getValue().getName(),
					defaultElementJavaClassReference,
					null
			);
		}
		final SingularAttribute parentAttribute =
				StringHelper.isEmpty( elementSource.getParentReferenceAttributeName() ) ?
						null :
						aggregate.createSingularAttribute( elementSource.getParentReferenceAttributeName() );
		final CompositeAttributeBindingContainer compositeAttributeBindingContainer =
				elementBinding.createCompositeAttributeBindingContainer(
						aggregate,
						createMetaAttributeContext(
								pluralAttributeBinding.getContainer(),
								elementSource.getMetaAttributeSources()
						),
						parentAttribute
				);

		bindAttributes( compositeAttributeBindingContainer, elementSource );
		Type resolvedType = metadata.getTypeResolver().getTypeFactory().component(
				new ComponentMetamodel( compositeAttributeBindingContainer, false, false )
		);
		// TODO: binding the HibernateTypeDescriptor should be simplified since we know the class name already
		bindHibernateTypeDescriptor(
				elementBinding.getHibernateTypeDescriptor(),
				aggregate.getClassName(),
				null,
				defaultElementJavaClassReference == null ? null : defaultElementJavaClassReference.getValue().getName()
		);
		bindHibernateResolvedType( elementBinding.getHibernateTypeDescriptor(), resolvedType );
		/**
		 * TODO
		 * don't know why, but see org.hibernate.mapping.Property#getCompositeCascadeStyle
		 *
		 * and not sure if this is the right place to apply this logic, apparently source level is not okay, so here it is, for now.
		 */
		for ( AttributeBinding ab : compositeAttributeBindingContainer.attributeBindings() ) {
			if ( ab.isCascadeable() ) {
				CascadeStyle cascadeStyle = Cascadeable.class.cast( ab ).getCascadeStyle();
				if ( cascadeStyle != CascadeStyles.NONE ) {
					elementBinding.setCascadeStyle( CascadeStyles.ALL );
				}
			}
		}
		if ( elementBinding.getCascadeStyle() == null || elementBinding.getCascadeStyle() == CascadeStyles.NONE ) {
			elementBinding.setCascadeStyle( determineCascadeStyle( elementSource.getCascadeStyles() ) );
		}
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
				indexedAttributeBinding.getPluralAttributeKeyBinding().getCollectionTable(),
				attributeBinding.getPluralAttributeElementBinding().getNature() != PluralAttributeElementBinding.Nature.ONE_TO_MANY )
				.get( 0 ).getValue() );
		if ( attributeBinding.getPluralAttributeElementBinding().getNature() == PluralAttributeElementBinding.Nature.ONE_TO_MANY ) {
			if ( Column.class.isInstance( indexBinding.getIndexRelationalValue() ) ) {
				// TODO: fix this when column nullability is refactored
				( (Column) indexBinding.getIndexRelationalValue() ).setNullable( true );
			}
		}
		// TODO: create a foreign key if non-inverse and the index is an association

		bindHibernateTypeDescriptor(
				indexBinding.getHibernateTypeDescriptor(),
				attributeSource.explicitHibernateTypeSource(),
				defaultIndexJavaTypeName );
		Type resolvedElementType = heuristicType( indexBinding.getHibernateTypeDescriptor() );
		bindHibernateResolvedType( indexBinding.getHibernateTypeDescriptor(), resolvedElementType );
		typeHelper.bindJdbcDataType( resolvedElementType, indexBinding.getIndexRelationalValue() );
	}

	private void bindCollectionTableForeignKey(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeKeySource keySource,
			TableSpecification collectionTable) {

		final AttributeBindingContainer attributeBindingContainer = attributeBinding.getContainer();
		final PluralAttributeKeyBinding keyBinding = attributeBinding.getPluralAttributeKeyBinding();

		List<RelationalValueBinding> sourceColumnBindings =
				bindValues(
						attributeBindingContainer,
						keySource,
						attributeBinding.getAttribute(),
						collectionTable,
						attributeBinding.getPluralAttributeElementBinding().getNature() != PluralAttributeElementBinding.Nature.ONE_TO_MANY
				);
		// Determine if the foreign key (source) column is updatable and also extract the columns out
		// of the RelationalValueBindings.
		boolean isInsertable = false;
		boolean isUpdatable = false;
		List<Column> sourceColumns = new ArrayList<Column>( sourceColumnBindings.size() );
		for ( RelationalValueBinding relationalValueBinding : sourceColumnBindings ) {
			final Value value = relationalValueBinding.getValue();
			// todo : currently formulas are not supported here... :(
			if ( !Column.class.isInstance( value ) ) {
				throw new NotYetImplementedException(
						"Derived values are not supported when creating a foreign key that targets columns." );
			}
			isInsertable = isInsertable || relationalValueBinding.isIncludeInInsert();
			isUpdatable = isUpdatable || relationalValueBinding.isIncludeInUpdate();
			sourceColumns.add( (Column) value );
		}
		keyBinding.setInsertable( isInsertable );
		keyBinding.setUpdatable( isUpdatable );

		List<Column> targetColumns =
				determineForeignKeyTargetColumns(
						attributeBindingContainer.seekEntityBinding(),
						keySource
				);

		ForeignKey foreignKey = bindForeignKey(
				quoteIdentifierIfNonEmpty( keySource.getExplicitForeignKeyName() ),
				sourceColumns,
				targetColumns
		);
		foreignKey.setDeleteRule( keySource.getOnDeleteAction() );
		keyBinding.setForeignKey( foreignKey );
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
				typeHelper.bindJdbcDataType( subType, fkColumnIterator.next() );
			}
		} else {
			typeHelper.bindJdbcDataType( resolvedKeyType, fkColumnIterator.next() );
		}
	}

	private void bindCollectionTablePrimaryKey(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeSource attributeSource) {
		PluralAttributeSource.Nature pluralAttributeNature = attributeSource.getNature();
		if ( attributeSource.getElementSource().getNature() == PluralAttributeElementSource.Nature.ONE_TO_MANY
				|| pluralAttributeNature == PluralAttributeSource.Nature.BAG ) {
			return;
		}
		if ( attributeBinding.getPluralAttributeElementBinding().getNature() == PluralAttributeElementBinding.Nature.BASIC ) {
			if ( pluralAttributeNature == PluralAttributeSource.Nature.SET ) {
				bindBasicSetCollectionTablePrimaryKey( (SetBinding) attributeBinding );
			} else if (
					pluralAttributeNature == PluralAttributeSource.Nature.LIST
					|| pluralAttributeNature == PluralAttributeSource.Nature.MAP
					|| pluralAttributeNature == PluralAttributeSource.Nature.ARRAY ) {
				bindIndexedCollectionTablePrimaryKey( (IndexedPluralAttributeBinding) attributeBinding );
			} else {
				throw new NotYetImplementedException(
						String.format( "%s of basic elements is not supported yet.", pluralAttributeNature )
				);
			}
		}
		else if ( attributeBinding.getPluralAttributeElementBinding().getNature() == PluralAttributeElementBinding.Nature.MANY_TO_MANY ) {
			if ( ! attributeBinding.getPluralAttributeKeyBinding().isInverse() ) {
				if ( pluralAttributeNature == PluralAttributeSource.Nature.SET ) {
					bindSetCollectionTablePrimaryKey( (SetBinding) attributeBinding );
				}
				else if (
					pluralAttributeNature == PluralAttributeSource.Nature.LIST
							|| pluralAttributeNature == PluralAttributeSource.Nature.MAP
							|| pluralAttributeNature == PluralAttributeSource.Nature.ARRAY) {
					bindIndexedCollectionTablePrimaryKey( (IndexedPluralAttributeBinding) attributeBinding );
				}
				else {
					throw new NotYetImplementedException(
							String.format( "Many-to-many %s is not supported yet.", pluralAttributeNature )
					);
				}
			}
		}
	}

	private CompositeAttributeBinding bindAggregatedCompositeAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final ComponentAttributeSource attributeSource,
			SingularAttribute attribute,
			boolean isAttributeIdentifier) {
		final Aggregate composite;
		ValueHolder<Class<?>> defaultJavaClassReference = null;
		if ( attribute == null ) {
			if ( attributeSource.getClassName() != null ) {
				composite = new Aggregate(
						attributeSource.getPath(),
						attributeSource.getClassName(),
						attributeSource.getClassReference() != null ?
								attributeSource.getClassReference() :
								bindingContext().makeClassReference( attributeSource.getClassName() ),
						null
				);
				// no need for a default because there's an explicit class name provided
			}
			else {
				defaultJavaClassReference = createSingularAttributeJavaType(
						attributeBindingContainer.getClassReference(), attributeSource.getName()
				);
				composite = new Aggregate(
						attributeSource.getPath(),
						defaultJavaClassReference.getValue().getName(),
						defaultJavaClassReference,
						null
				);
			}
			attribute = attributeBindingContainer.getAttributeContainer().createCompositeAttribute(
					attributeSource.getName(),
					composite
			);
		}
		else {
			composite = (Aggregate) attribute.getSingularAttributeType();
		}

		final SingularAttribute referencingAttribute =
				StringHelper.isEmpty( attributeSource.getParentReferenceAttributeName() ) ?
						null :
						composite.createSingularAttribute( attributeSource.getParentReferenceAttributeName() );
		final NaturalIdMutability naturalIdMutability = attributeSource.getNaturalIdMutability();
		final CompositeAttributeBinding attributeBinding =
				attributeBindingContainer.makeAggregatedCompositeAttributeBinding(
						attribute,
						referencingAttribute,
						propertyAccessorName( attributeSource ),
						attributeSource.isIncludedInOptimisticLocking(),
						attributeSource.isLazy(),
						naturalIdMutability,
						createMetaAttributeContext( attributeBindingContainer, attributeSource )
				);
		bindAttributes( attributeBinding, attributeSource );
		Type resolvedType = metadata.getTypeResolver().getTypeFactory().component(
				new ComponentMetamodel( attributeBinding, isAttributeIdentifier, false )
		);
		// TODO: binding the HibernateTypeDescriptor should be simplified since we know the class name already
		bindHibernateTypeDescriptor(
				attributeBinding.getHibernateTypeDescriptor(),
				composite.getClassName(),
				null,
				defaultJavaClassReference == null ? null : defaultJavaClassReference.getValue().getName()
		);
		bindHibernateResolvedType( attributeBinding.getHibernateTypeDescriptor(), resolvedType );
		return attributeBinding;
	}

	private void bindDiscriminator( final EntityBinding rootEntityBinding, final RootEntitySource rootEntitySource ) {
		final DiscriminatorSource discriminatorSource = rootEntitySource.getDiscriminatorSource();
		if ( discriminatorSource == null ) {
			return;
		}
		final RelationalValueSource valueSource = discriminatorSource.getDiscriminatorRelationalValueSource();
		final TableSpecification table = rootEntityBinding.locateTable( valueSource.getContainingTableName() );
		AbstractValue value;
		if ( valueSource.getNature() == RelationalValueSource.Nature.COLUMN ) {
			value =
					createColumn(
							table,
							( ColumnSource ) valueSource,
							bindingContext().getMappingDefaults().getDiscriminatorColumnName(),
							false,
							false,
							false );
		} else {
			value = table.locateOrCreateDerivedValue( ( ( DerivedValueSource ) valueSource ).getExpression() );
		}
		final EntityDiscriminator discriminator =
				new EntityDiscriminator( value, discriminatorSource.isInserted(), discriminatorSource.isForced() );
		rootEntityBinding.getHierarchyDetails().setEntityDiscriminator( discriminator );
		final String discriminatorValue = rootEntitySource.getDiscriminatorMatchValue();
		if ( discriminatorValue != null ) {
			rootEntityBinding.setDiscriminatorMatchValue( discriminatorValue );
		}
		else if ( !Modifier.isAbstract( bindingContext().locateClassByName( rootEntitySource.getEntityName() ).getModifiers() ) ) {
			// Use the class name as a default if no dscriminator value.
			// However, skip abstract classes -- obviously no discriminators there.
			rootEntityBinding.setDiscriminatorMatchValue( rootEntitySource.getEntityName() );
		}
		// Configure discriminator hibernate type
		final String typeName =
				discriminatorSource.getExplicitHibernateTypeName() != null
						? discriminatorSource.getExplicitHibernateTypeName()
						: "string";
		final HibernateTypeDescriptor hibernateTypeDescriptor = discriminator.getExplicitHibernateTypeDescriptor();
		hibernateTypeDescriptor.setExplicitTypeName( typeName );
		Type resolvedType = heuristicType( hibernateTypeDescriptor );
		bindHibernateResolvedType( hibernateTypeDescriptor, resolvedType );
		typeHelper.bindJdbcDataType( resolvedType, value );
	}

	private EntityBinding bindSubEntity(final EntitySource entitySource, final EntityBinding superEntityBinding) {
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
			entityBinding.setMutable( entityBinding.getHierarchyDetails().getRootEntityBinding().isMutable() );
			markSuperEntityTableAbstractIfNecessary(superEntityBinding);
			bindPrimaryTable( entityBinding, entitySource );
			bindSubEntityPrimaryKey( entityBinding, entitySource );
			bindSecondaryTables( entityBinding, entitySource );
			bindUniqueConstraints( entityBinding, entitySource );
			bindAttributes( entityBinding, entitySource );
			bindSubEntities( entityBinding, entitySource );
			return entityBinding;
		} finally {
			bindingContexts.pop();
		}
	}

	/**
	 * applied on sub entity
	 * @param superEntityBinding
	 */
	private static void markSuperEntityTableAbstractIfNecessary(EntityBinding superEntityBinding) {
		if ( superEntityBinding == null ) {
			return;
		}
		if ( superEntityBinding.getHierarchyDetails().getInheritanceType() != InheritanceType.TABLE_PER_CLASS ) {
			return;
		}
		if ( superEntityBinding.isAbstract() != Boolean.TRUE ) {
			return;
		}
		if ( !Table.class.isInstance( superEntityBinding.getPrimaryTable() ) ) {
			return;
		}
		Table.class.cast( superEntityBinding.getPrimaryTable() ).setPhysicalTable( false );
	}

	private void bindSubEntityPrimaryKey(final EntityBinding entityBinding, final EntitySource entitySource) {
		final InheritanceType inheritanceType = entityBinding.getHierarchyDetails().getInheritanceType();
		final EntityBinding superEntityBinding = entityBinding.getSuperEntityBinding();
		if ( superEntityBinding == null ) {
			throw new AssertionFailure( "super entitybinding is null " );
		}
		if ( inheritanceType == InheritanceType.TABLE_PER_CLASS ) {

		}
		if ( inheritanceType == InheritanceType.JOINED ) {
			SubclassEntitySource subclassEntitySource = (SubclassEntitySource) entitySource;
			ForeignKey fk = entityBinding.getPrimaryTable().createForeignKey(
					superEntityBinding.getPrimaryTable(),
					subclassEntitySource.getJoinedForeignKeyName()
			);
			final List<PrimaryKeyJoinColumnSource> primaryKeyJoinColumnSources = subclassEntitySource.getPrimaryKeyJoinColumnSources();
			final boolean hasPrimaryKeyJoinColumns = CollectionHelper.isNotEmpty( primaryKeyJoinColumnSources );
			final List<Column> superEntityBindingPrimaryKeyColumns = superEntityBinding.getPrimaryTable().getPrimaryKey().getColumns();

			for ( int i = 0, size = superEntityBindingPrimaryKeyColumns.size(); i < size; i++ ) {
				Column superEntityBindingPrimaryKeyColumn = superEntityBindingPrimaryKeyColumns.get( i );
				PrimaryKeyJoinColumnSource primaryKeyJoinColumnSource = hasPrimaryKeyJoinColumns && i < primaryKeyJoinColumnSources
						.size() ? primaryKeyJoinColumnSources.get( i ) : null;
				final String columnName;
				if ( primaryKeyJoinColumnSource != null && StringHelper.isNotEmpty( primaryKeyJoinColumnSource.getColumnName() ) ) {
					columnName = bindingContext().getNamingStrategy()
							.columnName( primaryKeyJoinColumnSource.getColumnName() );
				}
				else {
					columnName = superEntityBindingPrimaryKeyColumn.getColumnName().getText();
				}
				Column column = entityBinding.getPrimaryTable().locateOrCreateColumn( columnName );
				column.setCheckCondition( superEntityBindingPrimaryKeyColumn.getCheckCondition() );
				column.setComment( superEntityBindingPrimaryKeyColumn.getComment() );
				column.setDefaultValue( superEntityBindingPrimaryKeyColumn.getDefaultValue() );
				column.setIdentity( superEntityBindingPrimaryKeyColumn.isIdentity() );
				column.setNullable( superEntityBindingPrimaryKeyColumn.isNullable() );
				column.setReadFragment( superEntityBindingPrimaryKeyColumn.getReadFragment() );
				column.setWriteFragment( superEntityBindingPrimaryKeyColumn.getWriteFragment() );
				column.setUnique( superEntityBindingPrimaryKeyColumn.isUnique() );
				final String sqlType = getSqlTypeFromPrimaryKeyJoinColumnSourceIfExist(
						superEntityBindingPrimaryKeyColumn,
						primaryKeyJoinColumnSource
				);
				column.setSqlType( sqlType );
				column.setSize( superEntityBindingPrimaryKeyColumn.getSize() );
				column.setJdbcDataType( superEntityBindingPrimaryKeyColumn.getJdbcDataType() );
				entityBinding.getPrimaryTable().getPrimaryKey().addColumn( column );
				//todo still need to figure out how to handle the referencedColumnName property
				fk.addColumnMapping( column, superEntityBindingPrimaryKeyColumn );
			}
		}
	}

	private static String getSqlTypeFromPrimaryKeyJoinColumnSourceIfExist(Column superEntityBindingPrimaryKeyColumn, PrimaryKeyJoinColumnSource primaryKeyJoinColumnSource) {
		final boolean isColumnDefOverrided = primaryKeyJoinColumnSource != null && StringHelper.isNotEmpty(
				primaryKeyJoinColumnSource.getColumnDefinition()
		);
		return isColumnDefOverrided ? primaryKeyJoinColumnSource.getColumnDefinition() : superEntityBindingPrimaryKeyColumn
				.getSqlType();
	}

	/**
	 * Binding a single entity hierarchy.
	 *
	 * @param entityHierarchy The entity hierarchy to be binded.
	 *
	 * @return The root {@link EntityBinding} of the entity hierarchy mapping.
	 */
	private EntityBinding bindEntityHierarchy(final EntityHierarchy entityHierarchy) {
		final RootEntitySource rootEntitySource = entityHierarchy.getRootEntitySource();
		// Return existing binding if available
		EntityBinding rootEntityBinding = metadata.getEntityBinding( rootEntitySource.getEntityName() );
		if ( rootEntityBinding != null ) {
			return rootEntityBinding;
		}
		setupBindingContext( entityHierarchy, rootEntitySource );
		try {
			// Create root entity binding
			rootEntityBinding = createEntityBinding( rootEntitySource, null );
			bindPrimaryTable( rootEntityBinding, rootEntitySource );
			// Create/Bind root-specific information
			bindIdentifier( rootEntityBinding, rootEntitySource.getIdentifierSource() );
			bindSecondaryTables( rootEntityBinding, rootEntitySource );
			bindUniqueConstraints( rootEntityBinding, rootEntitySource );
			bindVersion( rootEntityBinding, rootEntitySource.getVersioningAttributeSource() );
			bindDiscriminator( rootEntityBinding, rootEntitySource );
			bindIdentifierGenerator( rootEntityBinding );
			bindMultiTenancy( rootEntityBinding, rootEntitySource );
			rootEntityBinding.getHierarchyDetails().setCaching( rootEntitySource.getCaching() );
			rootEntityBinding.getHierarchyDetails().setNaturalIdCaching( rootEntitySource.getNaturalIdCaching() );
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
			cleanupBindingContext();
		}
		return rootEntityBinding;
	}

	private void cleanupBindingContext() {
		bindingContexts.pop();
		inheritanceTypes.pop();
		entityModes.pop();
	}

	private void setupBindingContext(EntityHierarchy entityHierarchy, RootEntitySource rootEntitySource) {
		// Save inheritance type and entity mode that will apply to entire hierarchy
		inheritanceTypes.push( entityHierarchy.getHierarchyInheritanceType() );
		entityModes.push( rootEntitySource.getEntityMode() );
		bindingContexts.push( rootEntitySource.getLocalBindingContext() );
	}

	/**
	 * The entry point of {@linkplain Binder} class, binds all the entity hierarchy one by one.
	 *
	 * @param entityHierarchies The entity hierarchies resolved from mappings
	 */
	public void bindEntityHierarchies(final Iterable<EntityHierarchy> entityHierarchies) {
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
			bindEntityHierarchy( entityHierarchy );
		}
	}

	private SingularAttributeBinding determineReferencedAttributeBinding(
			ForeignKeyContributingSource foreignKeyContributingSource,
			EntityBinding referencedEntityBinding) {
		final JoinColumnResolutionDelegate resolutionDelegate =
				foreignKeyContributingSource.getForeignKeyTargetColumnResolutionDelegate();
		final JoinColumnResolutionContext resolutionContext = resolutionDelegate == null ? null : new JoinColumnResolutionContextImpl(
				referencedEntityBinding
		);
		return determineReferencedAttributeBinding(
				resolutionDelegate,
				resolutionContext,
				referencedEntityBinding
		);
	}

	private ForeignKey bindForeignKey(
			String foreignKeyName,
			List<Column> sourceColumns,
			List<Column> targetColumns) {
		ForeignKey foreignKey = null;
		if ( foreignKeyName != null ) {
			foreignKey = locateAndBindForeignKeyByName( foreignKeyName, sourceColumns, targetColumns );
		}
		if ( foreignKey == null ) {
			foreignKey = locateForeignKeyByColumnMapping( sourceColumns, targetColumns );
			if ( foreignKey != null && foreignKeyName != null ) {
				if ( foreignKey.getName() == null ) {
					// the foreign key name has not be initialized; set it to foreignKeyName
					foreignKey.setName( foreignKeyName );
				}
				else {
					// the foreign key name has already been initialized so cannot rename it
					// TODO: should this just be INFO?
					log.warn(
							String.format(
									"A foreign key mapped as %s will not be created because foreign key %s already exists with the same column mapping.",
									foreignKeyName,
									foreignKey.getName())
					);
				}
			}
		}
		if ( foreignKey == null ) {
			// no foreign key found; create one
			final TableSpecification sourceTable = sourceColumns.get( 0 ).getTable();
			final TableSpecification targetTable = targetColumns.get( 0 ).getTable();
			foreignKey = sourceTable.createForeignKey( targetTable, foreignKeyName );
			bindForeignKeyColumns( foreignKey, sourceColumns, targetColumns );
		}
		return foreignKey;
	}

	private void bindForeignKeyColumns(
			ForeignKey foreignKey,
			List<Column> sourceColumns,
			List<Column> targetColumns) {
		if ( sourceColumns.size() != targetColumns.size() ) {
			throw bindingContext().makeMappingException(
					String.format(
							"Non-matching number columns in foreign key source columns [%s : %s] and target columns [%s : %s]",
							sourceColumns.get( 0 ).getTable().getLogicalName().getText(),
							sourceColumns.size(),
							targetColumns.get( 0 ).getTable().getLogicalName().getText(),
							targetColumns.size()
					)
			);
		}
		for ( int i = 0; i < sourceColumns.size(); i++ ) {
			foreignKey.addColumnMapping( sourceColumns.get( i ), targetColumns.get( i ) );
		}
	}

	private static void bindHibernateResolvedType( final HibernateTypeDescriptor hibernateTypeDescriptor, final Type resolvedType ) {
		// Configure relational value JDBC type from Hibernate type descriptor now that its configured
		if ( resolvedType != null ) {
			hibernateTypeDescriptor.setResolvedTypeMapping( resolvedType );
			if ( hibernateTypeDescriptor.getJavaTypeName() == null ) {
				hibernateTypeDescriptor.setJavaTypeName( resolvedType.getReturnedClass().getName() );
			}
			hibernateTypeDescriptor.setToOne( resolvedType.isEntityType() );
		}
	}

	// TODO: The following 3 methods should eventually be replaced w/
	// typeHelper use.
	
	private void bindHibernateTypeDescriptor(
			final HibernateTypeDescriptor hibernateTypeDescriptor,
			final ExplicitHibernateTypeSource explicitTypeSource,
			final ValueHolder< Class< ? >> defaultJavaType ) {
		// if there is an explicit type name specified, then there's no reason to
		// initialize the default Java type name; simply pass a null default instead.
		bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				explicitTypeSource,
				explicitTypeSource == null || explicitTypeSource.getName() == null
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
				throw bindingContext().makeMappingException( String.format(
						"Attempt to re-initialize (non-explicit) Java type name; current=%s new=%s",
						hibernateTypeDescriptor.getJavaTypeName(),
						defaultJavaTypeName ) );
			}
			hibernateTypeDescriptor.setJavaTypeName( defaultJavaTypeName );
		} else {
			// Check if user-specified name is of a User-Defined Type (UDT)
			final TypeDefinition typeDef = metadata.getTypeDefinition( explicitTypeName );
			if ( hibernateTypeDescriptor.getExplicitTypeName() != null ) {
				throw bindingContext().makeMappingException( String.format(
						"Attempt to re-initialize explicity-mapped Java type name; current=%s new=%s",
						hibernateTypeDescriptor.getExplicitTypeName(),
						explicitTypeName ) );
			}
			if ( typeDef == null ) {
				hibernateTypeDescriptor.setExplicitTypeName( explicitTypeName );
			} else {
				hibernateTypeDescriptor.setExplicitTypeName( typeDef.getTypeImplementorClass().getName() );
				// Don't use set() -- typeDef#parameters is unmodifiable
				hibernateTypeDescriptor.getTypeParameters().putAll( typeDef.getParameters() );
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
			case NON_AGGREGATED_COMPOSITE: {
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

	private SingularAttributeBinding bindIdentifierAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeSource attributeSource) {
		return bindSingularAttribute( attributeBindingContainer, attributeSource, true );
	}

	private void bindIdentifierGenerator(final EntityBinding rootEntityBinding) {
		final Properties properties = new Properties();
		properties.putAll( metadata.getServiceRegistry().getService( ConfigurationService.class ).getSettings() );
		if ( !properties.contains( AvailableSettings.PREFER_POOLED_VALUES_LO ) ) {
			properties.put( AvailableSettings.PREFER_POOLED_VALUES_LO, "false" );
		}
		if ( !properties.contains( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER ) ) {
			properties.put( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER, nameNormalizer );
		}
		final EntityIdentifier entityIdentifier = rootEntityBinding.getHierarchyDetails().getEntityIdentifier();
		entityIdentifier.createIdentifierGenerator( identifierGeneratorFactory, properties );
		if ( IdentityGenerator.class.isInstance( entityIdentifier.getIdentifierGenerator() ) ) {
			if ( rootEntityBinding.getPrimaryTable().getPrimaryKey().getColumnSpan() != 1 ) {
				throw bindingContext().makeMappingException( String.format(
						"ID for %s is mapped as an identity with %d columns. IDs mapped as an identity can only have 1 column.",
						rootEntityBinding.getEntity().getName(),
						rootEntityBinding.getPrimaryTable().getPrimaryKey().getColumnSpan()
				) );
			}
			rootEntityBinding.getPrimaryTable().getPrimaryKey().getColumns().get( 0 ).setIdentity( true );
		}
		if ( PersistentIdentifierGenerator.class.isInstance( entityIdentifier.getIdentifierGenerator() ) ) {
			( (PersistentIdentifierGenerator) entityIdentifier.getIdentifierGenerator() ).registerExportables( metadata.getDatabase() );
		}
	}

	private static void bindIndexedCollectionTablePrimaryKey(IndexedPluralAttributeBinding attributeBinding) {
		final PrimaryKey primaryKey = attributeBinding.getPluralAttributeKeyBinding().getCollectionTable().getPrimaryKey();
		final ForeignKey foreignKey = attributeBinding.getPluralAttributeKeyBinding().getForeignKey();
		final PluralAttributeIndexBinding indexBinding = attributeBinding.getPluralAttributeIndexBinding();
		for ( final Column foreignKeyColumn : foreignKey.getSourceColumns() ) {
			primaryKey.addColumn( foreignKeyColumn );
		}
		final Value value = indexBinding.getIndexRelationalValue();
		if ( value instanceof Column ) {
			primaryKey.addColumn( (Column) value );
		}
	}

	public LocalBindingContext bindingContext() {
		return bindingContexts.peek();
	}


	private AbstractPluralAttributeBinding bindListAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final PluralAttributeSource attributeSource,
			PluralAttribute attribute ) {
		if ( attribute == null ) {
			attribute = attributeBindingContainer.getAttributeContainer().createList( attributeSource.getName() );
		}
		final int base = IndexedPluralAttributeSource.class.isInstance( attributeSource ) ? IndexedPluralAttributeSource.class.cast( attributeSource ).getIndexSource().base() : 0;
		return attributeBindingContainer.makeListAttributeBinding(
				attribute,
				pluralAttributeElementNature( attributeSource ),
				determinePluralAttributeKeyReferencedBinding( attributeBindingContainer, attributeSource ),
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				createMetaAttributeContext( attributeBindingContainer, attributeSource ),
				base
		);
	}
	
	private AbstractPluralAttributeBinding bindArrayAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final PluralAttributeSource attributeSource,
			PluralAttribute attribute ) {
		if ( attribute == null ) {
			attribute = attributeBindingContainer.getAttributeContainer().createArray( attributeSource.getName() );
		}
		final int base = IndexedPluralAttributeSource.class.isInstance( attributeSource ) ? IndexedPluralAttributeSource.class.cast( attributeSource ).getIndexSource().base() : 0;
		return attributeBindingContainer.makeArrayAttributeBinding(
				attribute,
				pluralAttributeElementNature( attributeSource ),
				determinePluralAttributeKeyReferencedBinding( attributeBindingContainer, attributeSource ),
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				createMetaAttributeContext( attributeBindingContainer, attributeSource ),
				base
		);
	}
	private static interface ToOneAttributeBindingContext {
		SingularAssociationAttributeBinding createToOneAttributeBinding(
				EntityBinding referencedEntityBinding,
				SingularAttributeBinding referencedAttributeBinding
		);
		EntityType resolveEntityType(
				EntityBinding referencedEntityBinding,
				SingularAttributeBinding referencedAttributeBinding
		);
	}

	private ManyToOneAttributeBinding bindManyToOneAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final ToOneAttributeSource attributeSource,
			SingularAttribute attribute ) {
		final SingularAttribute actualAttribute =
				attribute != null ?
						attribute :
						createSingularAttribute( attributeBindingContainer, attributeSource );

		ToOneAttributeBindingContext toOneAttributeBindingContext = new ToOneAttributeBindingContext() {
			@Override
			public SingularAssociationAttributeBinding createToOneAttributeBinding(
					EntityBinding referencedEntityBinding,
					SingularAttributeBinding referencedAttributeBinding
			) {
				/**
				 * this is not correct, here, if no @JoinColumn defined, we simply create the FK column only with column calucated
				 * but what we should do is get all the column info from the referenced column(s), including nullable, size etc.
				 */
				final TableSpecification table = locateTableSpecificationForAttribute( attributeBindingContainer, attributeSource );
				final List<RelationalValueBinding> relationalValueBindings =
						bindValues(
								attributeBindingContainer,
								attributeSource,
								actualAttribute,
								table,
								attributeSource.getDefaultNamingStrategies(
										attributeBindingContainer.seekEntityBinding().getEntity().getName(),
										table.getLogicalName().getText(),
										referencedAttributeBinding
								),
								false
						);

				// todo : currently a chicken-egg problem here between creating the attribute binding and binding its FK values...
				// now we have everything to create a ManyToOneAttributeBinding
				return attributeBindingContainer.makeManyToOneAttributeBinding(
								actualAttribute,
								propertyAccessorName( attributeSource ),
								attributeSource.isIncludedInOptimisticLocking(),
								attributeSource.isLazy(),
								attributeSource.getNaturalIdMutability(),
								createMetaAttributeContext( attributeBindingContainer, attributeSource ),
								referencedEntityBinding,
								referencedAttributeBinding,
								relationalValueBindings
						);
			}

			@Override
			public EntityType resolveEntityType(
					EntityBinding referencedEntityBinding,
					SingularAttributeBinding referencedAttributeBinding) {
				final boolean isRefToPk =
						referencedEntityBinding
								.getHierarchyDetails().getEntityIdentifier().isIdentifierAttributeBinding(
								referencedAttributeBinding );
				final String uniqueKeyAttributeName =
						isRefToPk ? null : getRelativePathFromEntityName( referencedAttributeBinding );
				return metadata.getTypeResolver().getTypeFactory().manyToOne(
						referencedEntityBinding.getEntity().getName(),
						uniqueKeyAttributeName,
						attributeSource.getFetchTiming() != FetchTiming.IMMEDIATE,
						attributeSource.isUnWrapProxy(),
						true, //TODO: is isEmbedded() obsolete?
						false, //TODO: should be attributeBinding.isIgnoreNotFound(),
						false //TODO: determine if isLogicalOneToOne
				);
			}
		};

		final ManyToOneAttributeBinding attributeBinding = (ManyToOneAttributeBinding) bindToOneAttribute(
				actualAttribute,
				attributeSource,
				toOneAttributeBindingContext
		);
		typeHelper.bindJdbcDataType(
				attributeBinding.getHibernateTypeDescriptor().getResolvedTypeMapping(),
				attributeBinding.getRelationalValueBindings()
		);
		if ( !hasDerivedValue( attributeBinding.getRelationalValueBindings() ) ) {
			bindForeignKey(
					quoteIdentifierIfNonEmpty( attributeSource.getExplicitForeignKeyName() ),
					extractColumnsFromRelationalValueBindings( attributeBinding.getRelationalValueBindings() ),
					determineForeignKeyTargetColumns(
							attributeBinding.getReferencedEntityBinding(),
							attributeSource
					)
			);
		}
		return attributeBinding;
	}

	private OneToOneAttributeBinding bindOneToOneAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final ToOneAttributeSource attributeSource,
			SingularAttribute attribute ) {
		final SingularAttribute actualAttribute =
				attribute != null ?
						attribute :
						createSingularAttribute( attributeBindingContainer, attributeSource );

		ToOneAttributeBindingContext toOneAttributeBindingContext = new ToOneAttributeBindingContext() {
			@Override
			public SingularAssociationAttributeBinding createToOneAttributeBinding(
					EntityBinding referencedEntityBinding,
					SingularAttributeBinding referencedAttributeBinding
			) {
				// todo : currently a chicken-egg problem here between creating the attribute binding and binding its FK values...
				return attributeBindingContainer.makeOneToOneAttributeBinding(
						actualAttribute,
						propertyAccessorName( attributeSource ),
						attributeSource.isIncludedInOptimisticLocking(),
						attributeSource.isLazy(),
						attributeSource.getNaturalIdMutability(),
						createMetaAttributeContext( attributeBindingContainer, attributeSource ),
						referencedEntityBinding,
						referencedAttributeBinding,
						attributeSource.getForeignKeyDirection() == ForeignKeyDirection.FROM_PARENT
				);
			}

			@Override
			public EntityType resolveEntityType(
					EntityBinding referencedEntityBinding,
					SingularAttributeBinding referencedAttributeBinding) {
				final boolean isRefToPk =
						referencedEntityBinding
								.getHierarchyDetails().getEntityIdentifier().isIdentifierAttributeBinding(
								referencedAttributeBinding );
				final String uniqueKeyAttributeName =
						isRefToPk ? null : getRelativePathFromEntityName( referencedAttributeBinding );
				return metadata.getTypeResolver().getTypeFactory().oneToOne(
						referencedEntityBinding.getEntity().getName(),
						attributeSource.getForeignKeyDirection(),
						uniqueKeyAttributeName,
						attributeSource.getFetchTiming() != FetchTiming.IMMEDIATE,
						attributeSource.isUnWrapProxy(),
						true, //TODO: is isEmbedded() obsolete?
						attributeBindingContainer.seekEntityBinding().getEntityName(),
						actualAttribute.getName()
				);
			}
		};

		OneToOneAttributeBinding attributeBinding = (OneToOneAttributeBinding) bindToOneAttribute(
				actualAttribute,
				attributeSource,
				toOneAttributeBindingContext
		);
		if ( attributeSource.getForeignKeyDirection() == ForeignKeyDirection.FROM_PARENT ) {
			List<Column> foreignKeyColumns = extractColumnsFromRelationalValueBindings(
					attributeBinding
							.getContainer()
							.seekEntityBinding()
							.getHierarchyDetails()
							.getEntityIdentifier()
							.getAttributeBinding()
							.getRelationalValueBindings()
			);

			bindForeignKey(
					quoteIdentifierIfNonEmpty( attributeSource.getExplicitForeignKeyName() ),
					foreignKeyColumns,
					determineForeignKeyTargetColumns(
							attributeBinding.getReferencedEntityBinding(),
							attributeSource
					)
			);
		}
		return attributeBinding;
	}

	private SingularAssociationAttributeBinding bindToOneAttribute(
			SingularAttribute attribute,
			ToOneAttributeSource attributeSource,
			final ToOneAttributeBindingContext attributeBindingContext) {

		final ValueHolder< Class< ? >> referencedEntityJavaTypeValue = createSingularAttributeJavaType( attribute );
		final EntityBinding referencedEntityBinding = findOrBindEntityBinding(
				referencedEntityJavaTypeValue,
				attributeSource.getReferencedEntityName()
		);

		// Type resolution...
		if ( !attribute.isTypeResolved() ) {
			attribute.resolveType( referencedEntityBinding.getEntity() );
		}

		//now find the referenced attribute binding, either the referenced entity's id attribute or the referenced attribute
		//todo referenced entityBinding null check?
		final SingularAttributeBinding referencedAttributeBinding = determineReferencedAttributeBinding(
				attributeSource,
				referencedEntityBinding
		);
		// todo : currently a chicken-egg problem here between creating the attribute binding and binding its FK values...
		// now we have everything to create the attribute binding
		final SingularAssociationAttributeBinding attributeBinding =
				attributeBindingContext.createToOneAttributeBinding(
						referencedEntityBinding,
						referencedAttributeBinding
				);

		if ( referencedAttributeBinding != referencedEntityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding() ) {
			referencedAttributeBinding.setAlternateUniqueKey( true );
		}

		attributeBinding.setCascadeStyle( determineCascadeStyle( attributeSource.getCascadeStyles() ) );
		attributeBinding.setFetchTiming( attributeSource.getFetchTiming() );
		attributeBinding.setFetchStyle( attributeSource.getFetchStyle() );

		final Type resolvedType =
				attributeBindingContext.resolveEntityType( referencedEntityBinding, referencedAttributeBinding );
		bindHibernateTypeDescriptor(
				attributeBinding.getHibernateTypeDescriptor(),
				attributeSource.getTypeInformation(),
				referencedEntityJavaTypeValue );
		bindHibernateResolvedType( attributeBinding.getHibernateTypeDescriptor(), resolvedType );

		return attributeBinding;
	}

	private static String getRelativePathFromEntityName(AttributeBinding attributeBinding) {
		final String fullPath = attributeBinding.getContainer().getPathBase() + "." + attributeBinding.getAttribute().getName();
		return fullPath.substring( attributeBinding.getContainer().seekEntityBinding().getEntityName().length() + 1 );
	}

	private TableSpecification locateTableSpecificationForAttribute(
			AttributeBindingContainer attributeBindingContainer,
			SingularAttributeSource attributeSource) {
		String tableName = null;
		if ( ! attributeSource.relationalValueSources().isEmpty() ) {
			 tableName = attributeSource.relationalValueSources().get( 0 ).getContainingTableName();
		}
		return tableName == null ?
				attributeBindingContainer.getPrimaryTable() :
				attributeBindingContainer.seekEntityBinding().locateTable( tableName );
	}

	private AbstractPluralAttributeBinding bindMapAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final PluralAttributeSource attributeSource,
			PluralAttribute attribute ) {
		if ( attribute == null ) {
			attribute = attributeBindingContainer.getAttributeContainer().createMap( attributeSource.getName() );
		}
		return attributeBindingContainer.makeMapAttributeBinding(
				attribute,
				pluralAttributeElementNature( attributeSource ),
				pluralAttributeIndexNature( attributeSource ),
				determinePluralAttributeKeyReferencedBinding( attributeBindingContainer, attributeSource ),
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),

				createMetaAttributeContext( attributeBindingContainer, attributeSource )
		);
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

	private void bindNonAggregatedCompositeIdentifier(
			EntityBinding rootEntityBinding,
			NonAggregatedCompositeIdentifierSource identifierSource ) {
		// locate the attribute bindings for the real attributes
		List<SingularAttributeBinding> idAttributeBindings =
				new ArrayList< SingularAttributeBinding >();
		for ( SingularAttributeSource attributeSource : identifierSource.getAttributeSourcesMakingUpIdentifier() ) {
			SingularAttributeBinding singularAttributeBinding =
					bindIdentifierAttribute( rootEntityBinding, attributeSource );
			if ( singularAttributeBinding.isAssociation() ) {
				throw new NotYetImplementedException( "composite IDs containing an association attribute is not implemented yet." );
			}
			idAttributeBindings.add( singularAttributeBinding );
		}

		final Class<?> idClassClass = identifierSource.getLookupIdClass();
		final String idClassPropertyAccessorName =
				idClassClass == null ?
						null :
						propertyAccessorName( identifierSource.getIdClassPropertyAccessorName() );

		// Configure ID generator
		IdGenerator generator = identifierSource.getIdentifierGeneratorDescriptor();
		if ( generator == null ) {
			final Map< String, String > params = new HashMap< String, String >();
			params.put( IdentifierGenerator.ENTITY_NAME, rootEntityBinding.getEntity().getName() );
			generator = new IdGenerator( "default_assign_identity_generator", "assigned", params );
		}
		// Create the synthetic attribute
		final SingularAttribute syntheticAttribute =
				rootEntityBinding.getEntity().createSyntheticCompositeAttribute(
						SYNTHETIC_COMPOSITE_ID_ATTRIBUTE_NAME,
						rootEntityBinding.getEntity()
				);

		final CompositeAttributeBinding syntheticAttributeBinding =
				rootEntityBinding.makeVirtualCompositeAttributeBinding(
						syntheticAttribute,
						createMetaAttributeContext( rootEntityBinding, identifierSource.getMetaAttributeSources() ),
						idAttributeBindings
				);
		bindHibernateTypeDescriptor(
				syntheticAttributeBinding.getHibernateTypeDescriptor(),
				syntheticAttribute.getSingularAttributeType().getClassName(),
				null,
				null
		);

		// Create the synthetic attribute binding.
		rootEntityBinding.getHierarchyDetails().getEntityIdentifier().prepareAsNonAggregatedCompositeIdentifier(
				syntheticAttributeBinding,
				generator,
				interpretIdentifierUnsavedValue( identifierSource, generator ),
				idClassClass,
				idClassPropertyAccessorName
		);

		final Type resolvedType = metadata.getTypeResolver().getTypeFactory().embeddedComponent(
				new ComponentMetamodel( syntheticAttributeBinding, true, false )
		);
		bindHibernateResolvedType( syntheticAttributeBinding.getHibernateTypeDescriptor(), resolvedType );
	}

	private void bindOneToManyCollectionElement(
			final OneToManyPluralAttributeElementBinding elementBinding,
			final OneToManyPluralAttributeElementSource elementSource,
			final EntityBinding referencedEntityBinding,
			final String defaultElementJavaTypeName) {

		elementBinding.setElementEntityIdentifier(
			referencedEntityBinding.getHierarchyDetails().getEntityIdentifier()
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
		elementBinding.setCascadeStyle( determineCascadeStyle( elementSource.getCascadeStyles() ) );
	}

	private void bindManyToManyCollectionElement(
			final ManyToManyPluralAttributeElementBinding elementBinding,
			final ManyToManyPluralAttributeElementSource elementSource,
			final EntityBinding referencedEntityBinding,
			final String defaultElementJavaTypeName) {

		elementBinding.setRelationalValueBindings(
				bindValues(
						elementBinding.getPluralAttributeBinding().getContainer(),
						elementSource,
						elementBinding.getPluralAttributeBinding().getAttribute(),
						elementBinding.getPluralAttributeBinding().getPluralAttributeKeyBinding().getCollectionTable(),
						true
				)
		);

		if ( !elementBinding.getPluralAttributeBinding().getPluralAttributeKeyBinding().isInverse() &&
				! hasDerivedValue( elementBinding.getRelationalValueBindings() ) ) {
			bindForeignKey(
					quoteIdentifierIfNonEmpty( elementSource.getExplicitForeignKeyName() ),
					extractColumnsFromRelationalValueBindings( elementBinding.getRelationalValueBindings() ),
					determineForeignKeyTargetColumns( referencedEntityBinding, elementSource )
			);
		}

		final HibernateTypeDescriptor hibernateTypeDescriptor = elementBinding.getHibernateTypeDescriptor();
		bindHibernateTypeDescriptor(
				hibernateTypeDescriptor,
				referencedEntityBinding.getEntity().getName(),
				null,
				defaultElementJavaTypeName
		);

		final Type resolvedElementType = metadata.getTypeResolver().getTypeFactory().manyToOne(
				referencedEntityBinding.getEntity().getName(),
				elementSource.getReferencedEntityAttributeName(),
				false,
				false,
				true,
				false, //TODO: should be attributeBinding.isIgnoreNotFound(),
				false
		);
		bindHibernateResolvedType( elementBinding.getHibernateTypeDescriptor(), resolvedElementType );
		typeHelper.bindJdbcDataType(
				resolvedElementType,
				elementBinding.getRelationalValueBindings()
		);
		elementBinding.setCascadeStyle( determineCascadeStyle( elementSource.getCascadeStyles() ) );
		elementBinding.setManyToManyWhere( elementSource.getWhere() );
		elementBinding.setManyToManyOrderBy( elementSource.getOrderBy() );
		//TODO: initialize filters from elementSource
	}

	private static boolean hasDerivedValue(List<RelationalValueBinding> relationalValueBindings) {
		for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
			if ( DerivedValue.class.isInstance( relationalValueBinding.getValue() ) ) {
				return true;
			}
		}
		return false;
	}

	// TODO: should this be moved to CascadeStyles as a static method?
	// TODO: sources already factor in default cascade; should that be done here instead?
	private static CascadeStyle determineCascadeStyle(Iterable<CascadeStyle> cascadeStyles) {
		CascadeStyle cascadeStyleResult;
		List<CascadeStyle> cascadeStyleList = new ArrayList<CascadeStyle>();
		for ( CascadeStyle style : cascadeStyles ) {
			if ( style != CascadeStyles.NONE ) {
				cascadeStyleList.add( style );
			}
		}
		if ( cascadeStyleList.isEmpty() ) {
			cascadeStyleResult = CascadeStyles.NONE;
		}
		else if ( cascadeStyleList.size() == 1 ) {
			cascadeStyleResult = cascadeStyleList.get( 0 );
		}
		else {
			cascadeStyleResult = new CascadeStyles.MultipleCascadeStyle(
					cascadeStyleList.toArray( new CascadeStyle[ cascadeStyleList.size() ] )
			);
		}
		return cascadeStyleResult;
	}

	private void bindOneToManyCollectionKey(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeSource attributeSource,
			final EntityBinding referencedEntityBinding) {
		if ( attributeSource.getElementSource().getNature() != PluralAttributeElementSource.Nature.ONE_TO_MANY ) {
			throw new AssertionFailure(
					String.format(
							"Expected one-to-many attribute binding; instead got {%s}",
							attributeSource.getElementSource().getNature()
					)
			);
		}
		// TODO - verify whether table spec source should/can be null or whether there will always be one (HF)
		if ( attributeSource.getCollectionTableSpecificationSource() != null ) {
			TableSpecificationSource tableSpecificationSource = attributeSource.getCollectionTableSpecificationSource();
			if(tableSpecificationSource.getExplicitCatalogName() != null || tableSpecificationSource.getExplicitSchemaName() != null) {
				// TODO: Need to look up the table to be able to create the foreign key
				throw new NotYetImplementedException( "one-to-many using a join table is not supported yet." );
			}
		}
		final TableSpecification collectionTable = referencedEntityBinding.getPrimaryTable();
		final boolean isInverse = attributeSource.isInverse();
		final PluralAttributeKeyBinding keyBinding = attributeBinding.getPluralAttributeKeyBinding();
		keyBinding.setInverse( isInverse );
		if ( isInverse && StringHelper.isNotEmpty( attributeSource.getMappedBy() ) ) {
			final String mappedBy = attributeSource.getMappedBy();
			SingularAssociationAttributeBinding referencedAttributeBinding = (SingularAssociationAttributeBinding) referencedEntityBinding
					.locateAttributeBinding( mappedBy );
			if ( referencedAttributeBinding == null ) {
				referencedAttributeBinding = (SingularAssociationAttributeBinding) attributeBinding(
						referencedEntityBinding.getEntity()
								.getName(), mappedBy
				);
			}
			keyBinding.setHibernateTypeDescriptor(
					referencedAttributeBinding.getReferencedAttributeBinding()
							.getHibernateTypeDescriptor()
			);
			boolean isUpdatable = false;
			List<RelationalValueBinding> sourceColumnBindings = referencedAttributeBinding.getRelationalValueBindings();
			List<Column> sourceColumns = new ArrayList<Column>();
			for ( RelationalValueBinding relationalValueBinding : sourceColumnBindings ) {
				Value v = relationalValueBinding.getValue();
				if ( Column.class.isInstance( v ) ) {
					sourceColumns.add( Column.class.cast( v ) );
				}
				isUpdatable = isUpdatable || relationalValueBinding.isIncludeInUpdate();
			}
			for ( ForeignKey fk : referencedEntityBinding.getPrimaryTable().getForeignKeys() ) {
				if ( fk.getSourceColumns().equals( sourceColumns ) ) {
					keyBinding.setForeignKey( fk );
				}
			}
			keyBinding.setUpdatable( isUpdatable );
		}
		else {
			bindCollectionTableForeignKey( attributeBinding, attributeSource.getKeySource(), collectionTable );
		}

	}

	private void bindManyToManyCollectionKey(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeSource attributeSource,
			final EntityBinding referencedEntityBinding) {
		if ( attributeSource.getElementSource().getNature() != PluralAttributeElementSource.Nature.MANY_TO_MANY ) {
			throw new AssertionFailure(
					String.format(
							"Expected many-to-many attribute binding; instead got {%s}",
							attributeSource.getElementSource().getNature()
					)
			);
		}
		final boolean isInverse = attributeSource.isInverse();
		final TableSpecification collectionTable = createManyToManyCollectionTable(
				attributeBinding,
				isInverse,
				attributeSource.getCollectionTableSpecificationSource(),
				referencedEntityBinding
		);
		final PluralAttributeKeyBinding keyBinding = attributeBinding.getPluralAttributeKeyBinding();
		keyBinding.setInverse( isInverse );
		bindCollectionTableForeignKey( attributeBinding, attributeSource.getKeySource(), collectionTable );
	}

	private AbstractPluralAttributeBinding bindPluralAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final PluralAttributeSource attributeSource ) {
		final PluralAttributeSource.Nature nature = attributeSource.getNature();
		final PluralAttribute attribute =
				attributeBindingContainer.getAttributeContainer().locatePluralAttribute( attributeSource.getName() );
		final AbstractPluralAttributeBinding attributeBinding;
		switch ( nature ) {
			case BAG:
				attributeBinding = bindBagAttribute( attributeBindingContainer, attributeSource, attribute );
				break;
			case SET:
				attributeBinding = bindSetAttribute( attributeBindingContainer, attributeSource, attribute );
				break;
			case LIST:
				attributeBinding = bindListAttribute(
						attributeBindingContainer,
						attributeSource,
						attribute
				);
				break;
			case MAP:
				attributeBinding = bindMapAttribute(
						attributeBindingContainer,
						attributeSource,
						attribute
				);
				break;
			case ARRAY:
				attributeBinding = bindArrayAttribute(
						attributeBindingContainer,
						attributeSource,
						attribute
				);
				break;
			default:
				throw new NotYetImplementedException( nature.toString() );
		}

		// Must do first -- sorting/ordering can determine the resolved type
		// (ex: Set vs. SortedSet).
		bindSortingAndOrdering( attributeBinding, attributeSource );
		
		final Type resolvedType = resolvePluralType( attributeBinding, attributeSource, nature );
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
		if ( attributeSource.getFetchStyle() == FetchStyle.SUBSELECT ) {
			attributeBindingContainer.seekEntityBinding().setSubselectLoadableCollections( true );
		}
		attributeBinding.setCaching( attributeSource.getCaching() );
		if ( StringHelper.isNotEmpty( attributeSource.getCustomPersisterClassName() ) ) {
			attributeBinding.setExplicitPersisterClass( bindingContext().< CollectionPersister >locateClassByName(
					attributeSource.getCustomPersisterClassName() ) );
		}
		attributeBinding.setCustomLoaderName( attributeSource.getCustomLoaderName() );
		attributeBinding.setCustomSqlInsert( attributeSource.getCustomSqlInsert() );
		attributeBinding.setCustomSqlUpdate( attributeSource.getCustomSqlUpdate() );
		attributeBinding.setCustomSqlDelete( attributeSource.getCustomSqlDelete() );
		attributeBinding.setCustomSqlDeleteAll( attributeSource.getCustomSqlDeleteAll() );
		attributeBinding.setWhere( attributeSource.getWhere() );

		switch ( attributeSource.getElementSource().getNature() ){
			case BASIC:
				bindBasicPluralAttribute( attributeSource, attributeBinding, reflectedCollectionJavaTypes );
				break;
			case ONE_TO_MANY:
				bindOneToManyAttribute( attributeSource, attributeBinding, reflectedCollectionJavaTypes );
				break;
			case MANY_TO_MANY:
				bindManyToManyAttribute( attributeSource, attributeBinding, reflectedCollectionJavaTypes );
				break;
			case AGGREGATE:
				bindPluralAggregateAttribute( attributeSource, attributeBinding, reflectedCollectionJavaTypes );
				break;
			case MANY_TO_ANY:
				//todo??
			default:
				throw bindingContext().makeMappingException(
						String.format(
								"Unknown type of collection element: %s",
								attributeSource.getElementSource().getNature()
						)
				);
		}
		bindIndexedPluralAttributeIfPossible( attributeSource, attributeBinding, reflectedCollectionJavaTypes );
		bindCollectionTablePrimaryKey( attributeBinding, attributeSource );
		metadata.addCollection( attributeBinding );
		return attributeBinding;
	}

	private void bindIndexedPluralAttributeIfPossible(PluralAttributeSource attributeSource, AbstractPluralAttributeBinding attributeBinding, ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		if ( attributeBinding.getAttribute().getNature().isIndexable() && attributeSource instanceof IndexedPluralAttributeSource ) {
			attributeBinding.setIndex( true );
			bindCollectionIndex(
					(IndexedPluralAttributeBinding) attributeBinding,
					( ( IndexedPluralAttributeSource ) attributeSource ).getIndexSource(),
					defaultCollectionIndexJavaTypeName( reflectedCollectionJavaTypes ) );
		} else {
			attributeBinding.setIndex( false );
		}
	}

	private void bindPluralAggregateAttribute(PluralAttributeSource attributeSource, AbstractPluralAttributeBinding attributeBinding, ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		bindNonAssociationCollectionKey( attributeBinding, attributeSource );
		bindCompositeCollectionElement(
				(CompositePluralAttributeElementBinding) attributeBinding.getPluralAttributeElementBinding(),
				(CompositePluralAttributeElementSource) attributeSource.getElementSource(),
				defaultCollectionElementJavaTypeName( reflectedCollectionJavaTypes )
		);
	}

	private void bindManyToManyAttribute(PluralAttributeSource attributeSource, AbstractPluralAttributeBinding attributeBinding, ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		final ManyToManyPluralAttributeElementSource elementSource =
				(ManyToManyPluralAttributeElementSource) attributeSource.getElementSource();
		final String defaultElementJavaTypeName = defaultCollectionElementJavaTypeName( reflectedCollectionJavaTypes );
		String referencedEntityName =
				elementSource.getReferencedEntityName() != null ?
						elementSource.getReferencedEntityName() :
						defaultElementJavaTypeName;
		if  ( referencedEntityName == null ) {
			throw bindingContext().makeMappingException( String.format( "The mapping for the entity associated with one-to-many attribute (%s) is undefined.",
					createAttributePath( attributeBinding )
			) );
		}
		EntityBinding referencedEntityBinding = findOrBindEntityBinding( referencedEntityName );
		bindManyToManyCollectionKey( attributeBinding, attributeSource, referencedEntityBinding );
		bindManyToManyCollectionElement(
				(ManyToManyPluralAttributeElementBinding) attributeBinding.getPluralAttributeElementBinding(),
				(ManyToManyPluralAttributeElementSource) attributeSource.getElementSource(),
				referencedEntityBinding,
				defaultElementJavaTypeName
		);
	}

	private void bindOneToManyAttribute(PluralAttributeSource attributeSource, AbstractPluralAttributeBinding attributeBinding, ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		final OneToManyPluralAttributeElementSource elementSource =
				(OneToManyPluralAttributeElementSource) attributeSource.getElementSource();
		final String defaultElementJavaTypeName = defaultCollectionElementJavaTypeName( reflectedCollectionJavaTypes );
		String referencedEntityName =
				elementSource.getReferencedEntityName() != null ?
						elementSource.getReferencedEntityName() :
						defaultElementJavaTypeName;
		if  ( referencedEntityName == null ) {
			throw bindingContext().makeMappingException( String.format( "The mapping for the entity associated with one-to-many attribute (%s) is undefined.",
					createAttributePath( attributeBinding )
			) );
		}
		EntityBinding referencedEntityBinding = findOrBindEntityBinding( referencedEntityName );
		bindOneToManyCollectionKey( attributeBinding, attributeSource, referencedEntityBinding );
		bindOneToManyCollectionElement(
				(OneToManyPluralAttributeElementBinding) attributeBinding.getPluralAttributeElementBinding(),
				(OneToManyPluralAttributeElementSource) attributeSource.getElementSource(),
				referencedEntityBinding,
				defaultElementJavaTypeName
		);
	}

	private void bindBasicPluralAttribute(PluralAttributeSource attributeSource, AbstractPluralAttributeBinding attributeBinding, ReflectedCollectionJavaTypes reflectedCollectionJavaTypes) {
		bindNonAssociationCollectionKey( attributeBinding, attributeSource );
		bindBasicCollectionElement(
				(BasicPluralAttributeElementBinding) attributeBinding.getPluralAttributeElementBinding(),
				(BasicPluralAttributeElementSource) attributeSource.getElementSource(),
				defaultCollectionElementJavaTypeName( reflectedCollectionJavaTypes ) );
	}

	private void bindPrimaryTable(final EntityBinding entityBinding, final EntitySource entitySource) {
		final EntityBinding superEntityBinding = entityBinding.getSuperEntityBinding();
		final InheritanceType inheritanceType = entityBinding.getHierarchyDetails().getInheritanceType();
		final TableSpecification table;
		final String tableName;
		// single table and sub entity
		if ( superEntityBinding != null && inheritanceType == InheritanceType.SINGLE_TABLE ) {
			table = superEntityBinding.getPrimaryTable();
			tableName = superEntityBinding.getPrimaryTableName();
			// Configure discriminator if present
			final String discriminatorValue = entitySource.getDiscriminatorMatchValue() != null ?
					entitySource.getDiscriminatorMatchValue()
					: entitySource.getEntityName();
			entityBinding.setDiscriminatorMatchValue( discriminatorValue );
		}

		// single table and root entity
		// joined
		// table per class and non-abstract  entity
		else {
			Table includedTable = null;
			if ( superEntityBinding != null
					&& inheritanceType == InheritanceType.TABLE_PER_CLASS
					&& Table.class.isInstance( superEntityBinding.getPrimaryTable() ) ) {
				includedTable = Table.class.cast( superEntityBinding.getPrimaryTable() );
			}
			table = createTable(
					entitySource.getPrimaryTable(), new DefaultNamingStrategy() {

				@Override
				public String defaultName() {
					String name = StringHelper.isNotEmpty( entityBinding.getJpaEntityName() ) ? entityBinding.getJpaEntityName() : entityBinding
							.getEntity().getName();
					return bindingContext().getNamingStrategy().classToTableName( name );
				}
			},
					includedTable
			);
			tableName = table.getLogicalName().getText();
		}
		entityBinding.setPrimaryTable( table );
		entityBinding.setPrimaryTableName( tableName );
	}

	private void bindSecondaryTables( final EntityBinding entityBinding, final EntitySource entitySource ) {
		for ( final SecondaryTableSource secondaryTableSource : entitySource.getSecondaryTables() ) {
			final TableSpecification table = createTable( secondaryTableSource.getTableSource(), null );

			List<Column> joinColumns;
			// TODO: deal with property-refs???
			if ( secondaryTableSource.getPrimaryKeyColumnSources().isEmpty() ) {
				final List<Column> joinedColumns = entityBinding.getPrimaryTable().getPrimaryKey().getColumns();
				joinColumns = new ArrayList<Column>( joinedColumns.size() );
				for ( Column joinedColumn : joinedColumns ) {
					Column joinColumn = table.locateOrCreateColumn(
							bindingContext().getNamingStrategy().joinKeyColumnName(
									joinedColumn.getColumnName().getText(),
									entityBinding.getPrimaryTable().getLogicalName().getText()
							)
					);
					joinColumns.add( joinColumn );
				}
			}
			else {
				joinColumns = new ArrayList<Column>( secondaryTableSource.getPrimaryKeyColumnSources().size() );
				for ( final ColumnSource joinColumnSource : secondaryTableSource.getPrimaryKeyColumnSources() ) {
					// todo : apply naming strategy to infer missing column name
					Column column = table.locateColumn( joinColumnSource.getName() );
					if ( column == null ) {
						column = table.createColumn( joinColumnSource.getName() );
						if ( joinColumnSource.getSqlType() != null ) {
							column.setSqlType( joinColumnSource.getSqlType() );
						} else {
							throw new NotYetImplementedException( "join key column sql type is not binded if not explicitly set" );
						}
					}
					joinColumns.add( column );
				}
			}

			// TODO: make the foreign key column the primary key???
			final ForeignKey foreignKey = bindForeignKey(
					quoteIdentifierIfNonEmpty( secondaryTableSource.getExplicitForeignKeyName() ),
					joinColumns,
					determineForeignKeyTargetColumns( entityBinding, secondaryTableSource )
			);

			entityBinding.addSecondaryTable( new SecondaryTable( table, foreignKey ) );
		}
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
				determinePluralAttributeKeyReferencedBinding( attributeBindingContainer, attributeSource ),
				propertyAccessorName( attributeSource ),
				attributeSource.isIncludedInOptimisticLocking(),
				createMetaAttributeContext( attributeBindingContainer, attributeSource )
		);
	}

	private void bindSimpleIdentifier( final EntityBinding rootEntityBinding, final SimpleIdentifierSource identifierSource ) {
		// locate the attribute binding
		final BasicAttributeBinding idAttributeBinding = ( BasicAttributeBinding ) bindIdentifierAttribute(
				rootEntityBinding, identifierSource.getIdentifierAttributeSource()
		);

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
				unsavedValue
		);
	}

	private SingularAttributeBinding bindSingularAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeSource attributeSource,
			boolean isIdentifierAttribute) {
		final SingularAttributeSource.Nature nature = attributeSource.getNature();
		final SingularAttribute attribute =
				attributeBindingContainer.getAttributeContainer().locateSingularAttribute( attributeSource.getName() );
		switch ( nature ) {
			case BASIC:
				return bindBasicAttribute( attributeBindingContainer, attributeSource, attribute );
			case ONE_TO_ONE:
				return bindOneToOneAttribute(
						attributeBindingContainer,
						ToOneAttributeSource.class.cast( attributeSource ),
						attribute
				);
			case MANY_TO_ONE:
				return bindManyToOneAttribute(
						attributeBindingContainer,
						ToOneAttributeSource.class.cast( attributeSource ),
						attribute
				);
			case COMPOSITE:
				return bindAggregatedCompositeAttribute(
						attributeBindingContainer,
						ComponentAttributeSource.class.cast( attributeSource ),
						attribute,
						isIdentifierAttribute
				);
			default:
				throw new NotYetImplementedException( nature.toString() );
		}
	}

	private void bindSortingAndOrdering(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeSource attributeSource ) {
		if ( Sortable.class.isInstance( attributeSource ) ) {
			final Sortable sortable = ( Sortable ) attributeSource;
			attributeBinding.setSorted( sortable.isSorted() );
			if ( sortable.isSorted()
					&& !sortable.getComparatorName().equalsIgnoreCase( "natural" ) ) {
				Class< Comparator< ? > > comparatorClass =
						bindingContext().locateClassByName( sortable.getComparatorName() );
				try {
					attributeBinding.setComparator( comparatorClass.newInstance() );
				} catch ( Exception error ) {
					throw bindingContext().makeMappingException(
							String.format(
									"Unable to create comparator [%s] for attribute [%s]",
									sortable.getComparatorName(),
									attributeSource.getName() ),
							error );
				}
			}
		}
		if ( Orderable.class.isInstance( attributeSource ) ) {
			final Orderable orderable = (Orderable) attributeSource;
			if ( orderable.isOrdered() ) {
				String orderBy = orderable.getOrder();
				if ( orderBy.equals( "" ) ) {
					PrimaryKey pk = attributeBinding.getPluralAttributeKeyBinding()
							.getReferencedAttributeBinding()
							.getContainer()
							.seekEntityBinding()
							.getPrimaryTable()
							.getPrimaryKey();
					List<Column> pkColumns = pk.getColumns();
					StringBuffer sb = new StringBuffer();
					for ( final Column column : pkColumns ) {
						sb.append( column.getColumnName().getText() );
						sb.append( " asc ," );
					}
					orderBy = sb.substring( 0, sb.length()-2 );
				}
				attributeBinding.setOrderBy( orderBy );

			}
		}
	}

	private void bindSubEntities( final EntityBinding entityBinding, final EntitySource entitySource ) {
		for ( final SubclassEntitySource subEntitySource : entitySource.subclassEntitySources() ) {
			bindSubEntity( subEntitySource, entityBinding );
		}
	}

	private void bindUniqueConstraints( final EntityBinding entityBinding, final EntitySource entitySource ) {
		int uniqueIndexPerTable = 0;
		for ( final ConstraintSource constraintSource : entitySource.getConstraints() ) {
			if ( UniqueConstraintSource.class.isInstance( constraintSource ) ) {
				final TableSpecification table = entityBinding.locateTable( constraintSource.getTableName() );
				uniqueIndexPerTable++;
				final String constraintName = StringHelper.isEmpty( constraintSource.name() )
						? "key" + uniqueIndexPerTable
						: constraintSource.name();
				final UniqueKey uniqueKey = table.getOrCreateUniqueKey( constraintName );
				for ( final String columnName : constraintSource.columnNames() ) {
					uniqueKey.addColumn( table.locateOrCreateColumn( quotedIdentifier( columnName ) ) );
				}
			}
		}
	}

	private List<RelationalValueBinding> bindValues(
			final AttributeBindingContainer attributeBindingContainer,
			final RelationalValueSourceContainer valueSourceContainer,
			final Attribute attribute,
			final TableSpecification defaultTable,
			final boolean forceNonNullable) {
		final List<DefaultNamingStrategy> list = new ArrayList<DefaultNamingStrategy>( 1 );
		list.add(
				new DefaultNamingStrategy() {
					@Override
					public String defaultName() {
						return bindingContext().getNamingStrategy().propertyToColumnName( attribute.getName() );
					}
				}
		);
		return bindValues( attributeBindingContainer, valueSourceContainer, attribute, defaultTable, list, forceNonNullable );
	}

	private List< RelationalValueBinding > bindValues(
			final AttributeBindingContainer attributeBindingContainer,
			final RelationalValueSourceContainer valueSourceContainer,
			final Attribute attribute,
			final TableSpecification defaultTable,
			final List<DefaultNamingStrategy> defaultNamingStrategyList,
			final boolean forceNonNullable) {
		final List<RelationalValueBinding> valueBindings = new ArrayList<RelationalValueBinding>();
		final NaturalIdMutability naturalIdMutability = SingularAttributeSource.class.isInstance(
				valueSourceContainer
		) ? SingularAttributeSource.class.cast( valueSourceContainer ).getNaturalIdMutability()
				: NaturalIdMutability.NOT_NATURAL_ID;
		final boolean isNaturalId = naturalIdMutability != NaturalIdMutability.NOT_NATURAL_ID;
		final boolean isImmutableNaturalId = isNaturalId && (naturalIdMutability == NaturalIdMutability.IMMUTABLE);
		final boolean reallyForceNonNullable = forceNonNullable || isNaturalId;

		if ( valueSourceContainer.relationalValueSources().isEmpty() ) {
			for ( DefaultNamingStrategy defaultNamingStrategy : defaultNamingStrategyList ) {
				final String columnName =
						quotedIdentifier( defaultNamingStrategy.defaultName() );
				final Column column = defaultTable.locateOrCreateColumn( columnName );
				column.setNullable( !reallyForceNonNullable && valueSourceContainer.areValuesNullableByDefault() );
				if ( isNaturalId ) {
					addUniqueConstraintForNaturalIdColumn( defaultTable, column );
				}
				valueBindings.add(
						new RelationalValueBinding(
								column,
								valueSourceContainer.areValuesIncludedInInsertByDefault(),
								valueSourceContainer.areValuesIncludedInUpdateByDefault() && !isImmutableNaturalId
						)
				);
			}

		} else {
			final String name = attribute.getName();
			for ( final RelationalValueSource valueSource : valueSourceContainer.relationalValueSources() ) {
				final TableSpecification table =
						valueSource.getContainingTableName() == null
								? defaultTable
								: attributeBindingContainer.seekEntityBinding().locateTable( valueSource.getContainingTableName() );
				if ( valueSource.getNature() == RelationalValueSource.Nature.COLUMN ) {
					final ColumnSource columnSource = ( ColumnSource ) valueSource;
					final boolean isIncludedInInsert =
							TruthValue.toBoolean(
									columnSource.isIncludedInInsert(),
									valueSourceContainer.areValuesIncludedInInsertByDefault() );
					final boolean isIncludedInUpdate =
							TruthValue.toBoolean(
									columnSource.isIncludedInUpdate(),
									valueSourceContainer.areValuesIncludedInUpdateByDefault() );
					Column column = createColumn(
							table,
							columnSource,
							name,
							reallyForceNonNullable,
							valueSourceContainer.areValuesNullableByDefault(),
							true
					);
					if(isNaturalId){
						addUniqueConstraintForNaturalIdColumn( table, column );
					}
					valueBindings.add( new RelationalValueBinding( column, isIncludedInInsert, !isImmutableNaturalId && isIncludedInUpdate ) );
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
		// ensure version is non-nullable
		for ( RelationalValueBinding valueBinding : version.getVersioningAttributeBinding().getRelationalValueBindings() ) {
			if ( valueBinding.getValue() instanceof Column ) {
				( (Column) valueBinding.getValue() ).setNullable( false );
			}
		}
		version.setUnsavedValue(
				versionAttributeSource.getUnsavedValue() == null
						? "undefined"
						: versionAttributeSource.getUnsavedValue()
		);
	}

	private Value buildRelationValue(RelationalValueSource valueSource, TableSpecification table) {
		if ( valueSource.getNature() == RelationalValueSource.Nature.COLUMN ) {
			return createColumn(
					table,
					( ColumnSource ) valueSource,
					bindingContext().getMappingDefaults().getDiscriminatorColumnName(),
					false,
					false,
					false
			);
		}
		else {
			return table.locateOrCreateDerivedValue( ( ( DerivedValueSource ) valueSource ).getExpression() );
		}
	}

	private static String createAttributePath(final AttributeBinding attributeBinding) {
		return new StringBuffer( attributeBinding.getContainer().getPathBase() )
				.append( '.' )
				.append( attributeBinding.getAttribute().getName() )
				.toString();
	}

	private TableSpecification createBasicCollectionTable(
			final AbstractPluralAttributeBinding pluralAttributeBinding,
			final TableSpecificationSource tableSpecificationSource) {
		final DefaultNamingStrategy defaultNamingStategy = new DefaultNamingStrategy() {

			@Override
			public String defaultName() {
				final EntityBinding owner = pluralAttributeBinding.getContainer().seekEntityBinding();
				final String ownerTableLogicalName =
						Table.class.isInstance( owner.getPrimaryTable() )
								? ( ( Table ) owner.getPrimaryTable() ).getPhysicalName().getText()
								: null;
				return bindingContext().getNamingStrategy().collectionTableName(
						owner.getEntity().getName(),
						ownerTableLogicalName,
						null,
						null,
						createAttributePath( pluralAttributeBinding )
				);
			}
		};
		return createTable( tableSpecificationSource, defaultNamingStategy );
	}

	private TableSpecification createManyToManyCollectionTable(
			final AbstractPluralAttributeBinding pluralAttributeBinding,
			final boolean isInverse,
			final TableSpecificationSource tableSpecificationSource,
			final EntityBinding associatedEntityBinding) {
		final DefaultNamingStrategy defaultNamingStategy = new DefaultNamingStrategy() {

			@Override
			public String defaultName() {
				final EntityBinding ownerEntityBinding;
				final EntityBinding inverseEntityBinding;
				if ( isInverse ) {
					ownerEntityBinding = associatedEntityBinding;
					inverseEntityBinding = pluralAttributeBinding.getContainer().seekEntityBinding();
				}
				else {
					ownerEntityBinding = pluralAttributeBinding.getContainer().seekEntityBinding();
					inverseEntityBinding = associatedEntityBinding;
				}
				final String ownerTableLogicalName =
						Table.class.isInstance( ownerEntityBinding.getPrimaryTable() )
								? ( ( Table ) ownerEntityBinding.getPrimaryTable() ).getPhysicalName().getText()
								: null;
				final String inverseTableLogicalName =
						Table.class.isInstance( inverseEntityBinding.getPrimaryTable() )
								? ( ( Table ) inverseEntityBinding.getPrimaryTable() ).getPhysicalName().getText()
								: null;
				return bindingContext().getNamingStrategy().collectionTableName(
						ownerEntityBinding.getEntity().getName(),
						ownerTableLogicalName,
						inverseEntityBinding.getEntity().getName(),
						inverseTableLogicalName,
						createAttributePath( pluralAttributeBinding )
				);
			}
		};
		return createTable( tableSpecificationSource, defaultNamingStategy );
	}

	private Column createColumn(
			final TableSpecification table,
			final ColumnSource columnSource,
			final String defaultName,
			final boolean forceNotNull,
			final boolean isNullableByDefault,
			final boolean isDefaultAttributeName ) {
		if ( columnSource.getName() == null && defaultName == null ) {
			throw bindingContext().makeMappingException( "Cannot resolve name for column because no name was specified and default name is null." );
		}
		final String name = resolveColumnName( columnSource, defaultName, isDefaultAttributeName );
		final String resolvedColumnName = quotedIdentifier( name );
		final Column column = table.locateOrCreateColumn( resolvedColumnName );
		resolveColumnNullabl( columnSource, forceNotNull, isNullableByDefault, column );
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

	private void resolveColumnNullabl(ColumnSource columnSource, boolean forceNotNull, boolean isNullableByDefault, Column column) {
		if ( forceNotNull ) {
			column.setNullable( false );
			if(columnSource.isNullable() == TruthValue.TRUE){
				log.warn( String.format( "Natural Id column[%s] has explicit set to allow nullable, we have to make it force not null ", columnSource.getName() ) );
			}
		}
		else {
			// if the column is already non-nullable, leave it alone
			if ( column.isNullable() ) {
				column.setNullable( TruthValue.toBoolean( columnSource.isNullable(), isNullableByDefault ) );
			}
		}
	}

	private String resolveColumnName(ColumnSource columnSource, String defaultName, boolean isDefaultAttributeName) {
		final String name;
		if ( StringHelper.isNotEmpty( columnSource.getName() ) ) {
			name = bindingContext().getNamingStrategy().columnName( columnSource.getName() );
		} else if ( isDefaultAttributeName ) {
			name = bindingContext().getNamingStrategy().propertyToColumnName( defaultName );
		} else {
			name = bindingContext().getNamingStrategy().columnName( defaultName );
		}
		return name;
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
		LocalBindingContext bindingContext = bindingContext();
		entityBinding.setEntity( new Entity(
				entitySource.getEntityName(),
				entityClassName,
				bindingContext.makeClassReference( entityClassName ),
				superEntityBinding == null ? null : superEntityBinding.getEntity() ) );

		entityBinding.setEntityName( entitySource.getEntityName() );  
		entityBinding.setJpaEntityName( entitySource.getJpaEntityName() );          //must before creating primary table
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
				entitySource.getMetaAttributeSources(),
				true,
				metadata.getGlobalMetaAttributeContext() ) );

		if ( entitySource.getSynchronizedTableNames() != null ) {
			entityBinding.addSynchronizedTableNames( entitySource.getSynchronizedTableNames() );
		}
		resolveEntityLaziness( entitySource, entityMode, entityBinding, bindingContext );
		// Register binding with metadata
		metadata.addEntity( entityBinding );
		return entityBinding;
	}

	private void resolveEntityLaziness(EntitySource entitySource, EntityMode entityMode, EntityBinding entityBinding, LocalBindingContext bindingContext) {
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
	}

	private Identifier createIdentifier( String name, final String defaultName ) {
		if ( StringHelper.isEmpty( name ) ) {
			name = defaultName;
		}
		name = quotedIdentifier( name );
		return Identifier.toIdentifier( name );
	}

	private static MetaAttributeContext createMetaAttributeContext(
			final AttributeBindingContainer attributeBindingContainer,
			final AttributeSource attributeSource ) {
		return createMetaAttributeContext( attributeBindingContainer, attributeSource.getMetaAttributeSources() );
	}

	private static MetaAttributeContext createMetaAttributeContext(
			final AttributeBindingContainer attributeBindingContainer,
			final Iterable<? extends MetaAttributeSource > metaAttributeSources ) {
		return createMetaAttributeContext(
				metaAttributeSources,
				false,
				attributeBindingContainer.getMetaAttributeContext()
		);
	}

	private static  MetaAttributeContext createMetaAttributeContext(
			final Iterable<? extends MetaAttributeSource > metaAttributeSources,
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

	private static SingularAttribute createSingularAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeSource attributeSource ) {
		return attributeSource.isVirtualAttribute()
				? attributeBindingContainer.getAttributeContainer().createSyntheticSingularAttribute( attributeSource.getName() )
				: attributeBindingContainer.getAttributeContainer().createSingularAttribute( attributeSource.getName() );
	}
	private TableSpecification createTable(final TableSpecificationSource tableSpecSource,
										   final DefaultNamingStrategy defaultNamingStrategy){
		return createTable( tableSpecSource, defaultNamingStrategy, null );

	}
	private TableSpecification createTable(
			final TableSpecificationSource tableSpecSource,
			final DefaultNamingStrategy defaultNamingStrategy,
			final Table includedTable) {

		final LocalBindingContext bindingContext = bindingContext();
		final MappingDefaults mappingDefaults = bindingContext.getMappingDefaults();
		final boolean isTableSourceNull = tableSpecSource == null;
		final String explicitCatalogName =  isTableSourceNull ? null : tableSpecSource.getExplicitCatalogName();
		final String explicitSchemaName = isTableSourceNull ? null : tableSpecSource.getExplicitSchemaName();
		final Schema.Name schemaName =
				new Schema.Name(
						createIdentifier( explicitCatalogName, mappingDefaults.getCatalogName() ),
						createIdentifier( explicitSchemaName, mappingDefaults.getSchemaName() )
				);
		final Schema schema = bindingContext.getMetadataImplementor().getDatabase().locateSchema( schemaName );

		TableSpecification tableSpec = null;
		if ( isTableSourceNull ) {
			if ( defaultNamingStrategy == null ) {
				throw bindingContext().makeMappingException( "An explicit name must be specified for the table" );
			}
			String tableName = defaultNamingStrategy.defaultName();
			tableSpec = createTableSpecification( bindingContext, schema, tableName, includedTable );
		}
		else if ( tableSpecSource instanceof TableSource ) {
			final TableSource tableSource = ( TableSource ) tableSpecSource;
			String tableName = tableSource.getExplicitTableName();
			if ( tableName == null ) {
				if ( defaultNamingStrategy == null ) {
					throw bindingContext().makeMappingException( "An explicit name must be specified for the table" );
				}
				tableName = defaultNamingStrategy.defaultName();
			}
			tableSpec = createTableSpecification( bindingContext, schema, tableName, includedTable );
		}
		else {
			final InLineViewSource inLineViewSource = ( InLineViewSource ) tableSpecSource;
			tableSpec = schema.createInLineView(
					Identifier.toIdentifier( inLineViewSource.getLogicalName() ),
					inLineViewSource.getSelectStatement()
			);
		}

		return tableSpec;
	}

	private TableSpecification createTableSpecification(LocalBindingContext bindingContext, Schema schema, String tableName, Table includedTable) {
		tableName = quotedIdentifier( tableName );
		final Identifier logicalTableId = Identifier.toIdentifier( tableName );
		tableName = quotedIdentifier( bindingContext.getNamingStrategy().tableName( tableName ) );
		final Identifier physicalTableId = Identifier.toIdentifier( tableName );
		final Table table = schema.locateTable( logicalTableId );
		if ( table != null ) {
			return table;
		}
		TableSpecification tableSpec;
		if ( includedTable == null ) {
			tableSpec = schema.createTable( logicalTableId, physicalTableId );
		}
		else {
			tableSpec = schema.createDenormalizedTable( logicalTableId, physicalTableId, includedTable );
		}
		return tableSpec;
	}

	private static String defaultCollectionElementJavaTypeName(
			HibernateTypeHelper.ReflectedCollectionJavaTypes reflectedCollectionJavaTypes ) {
		return reflectedCollectionJavaTypes != null && reflectedCollectionJavaTypes.getCollectionElementType() != null
				? reflectedCollectionJavaTypes.getCollectionElementType().getName()
				: null;
	}

	private static String defaultCollectionIndexJavaTypeName(
			HibernateTypeHelper.ReflectedCollectionJavaTypes reflectedCollectionJavaTypes ) {
		return reflectedCollectionJavaTypes != null && reflectedCollectionJavaTypes.getCollectionIndexType() != null
				? reflectedCollectionJavaTypes.getCollectionIndexType().getName()
				: null;
	}

	private static String defaultCollectionJavaTypeName(
			HibernateTypeHelper.ReflectedCollectionJavaTypes reflectedCollectionJavaTypes,
			PluralAttributeSource attributeSource ) {
		return reflectedCollectionJavaTypes != null && reflectedCollectionJavaTypes.getCollectionType() != null
				? reflectedCollectionJavaTypes.getCollectionType().getName()
				: attributeSource.getNature().reportedJavaType().getName();
	}

	private  List< Column > determineForeignKeyTargetColumns(
			final EntityBinding entityBinding,
			ForeignKeyContributingSource foreignKeyContributingSource ) {
		
		// TODO: This method, JoinColumnResolutionContext,
		// and JoinColumnResolutionDelegate need re-worked.  There is currently
		// no way to bind to a collection's inverse foreign key.
		
		final JoinColumnResolutionDelegate fkColumnResolutionDelegate =
				foreignKeyContributingSource.getForeignKeyTargetColumnResolutionDelegate();

		if ( fkColumnResolutionDelegate == null ) {
			return entityBinding.getPrimaryTable().getPrimaryKey().getColumns();
		} else {
			final List< Column > columns = new ArrayList< Column >();
			final JoinColumnResolutionContext resolutionContext =
					new JoinColumnResolutionContext() {
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

						@Override
						public List< Value > resolveRelationalValuesForAttribute( String attributeName ) {
							if ( attributeName == null ) {
								List< Value > values = new ArrayList< Value >();
								for ( Column column : entityBinding.getPrimaryTable().getPrimaryKey().getColumns() ) {
									values.add( column );
								}
								return values;
							}
							final AttributeBinding referencedAttributeBinding =
									entityBinding.locateAttributeBindingByPath( attributeName, true );
							if ( referencedAttributeBinding == null ) {
								throw bindingContext().makeMappingException(
										String.format(
												"Could not resolve named referenced property [%s] against entity [%s]",
												attributeName,
												entityBinding.getEntity().getName() ) );
							}
							if ( ! referencedAttributeBinding.getAttribute().isSingular() ) {
								throw bindingContext().makeMappingException(
										String.format(
												"Referenced property [%s] against entity [%s] is a plural attribute; it must be a singular attribute.",
												attributeName,
												entityBinding.getEntity().getName() ) );
							}
							List<RelationalValueBinding> valueBindings =
									( (SingularAttributeBinding) referencedAttributeBinding ).getRelationalValueBindings();
							List<Value> values = new ArrayList<Value>( valueBindings.size());
							for ( RelationalValueBinding valueBinding : valueBindings ) {
								values.add( valueBinding.getValue() );
							}
							return values;
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

	private SingularAttributeBinding determinePluralAttributeKeyReferencedBinding(
			final AttributeBindingContainer attributeBindingContainer,
			final PluralAttributeSource attributeSource ) {
		final EntityBinding entityBinding = attributeBindingContainer.seekEntityBinding();
		final JoinColumnResolutionDelegate resolutionDelegate =
				attributeSource.getKeySource().getForeignKeyTargetColumnResolutionDelegate();

		if ( resolutionDelegate == null ) {
			return entityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding();
		}

		AttributeBinding referencedAttributeBinding;
		final String referencedAttributeName = resolutionDelegate.getReferencedAttributeName();
		if ( referencedAttributeName == null ) {
			referencedAttributeBinding = attributeBindingContainer.locateAttributeBinding(
					resolutionDelegate.getJoinColumns( new JoinColumnResolutionContext() {
				@Override
				public List<Value> resolveRelationalValuesForAttribute(String attributeName) {
					return null;
				}

				@Override
				public Column resolveColumn(String logicalColumnName, String logicalTableName, String logicalSchemaName, String logicalCatalogName) {
					for ( AttributeBinding attributeBinding : attributeBindingContainer.attributeBindings() ) {
						if ( SingularAttributeBinding.class.isInstance( attributeBinding ) ) {
							SingularAttributeBinding singularAttributeBinding = SingularAttributeBinding.class.cast(
									attributeBinding
							);
							for ( RelationalValueBinding relationalValueBinding : singularAttributeBinding.getRelationalValueBindings() ) {
								if ( Column.class.isInstance( relationalValueBinding.getValue() ) ) {
									Identifier columnIdentifier = Identifier.toIdentifier(
											quotedIdentifier(
													logicalColumnName
											)
									);
									Column column = Column.class.cast( relationalValueBinding.getValue() );
									if ( column.getColumnName().equals( columnIdentifier ) ) {
										return column;
									}
								}
							}
						}
					}
					return null;
				}
			} ) );
		} else {
			referencedAttributeBinding = attributeBindingContainer.locateAttributeBinding( referencedAttributeName );
		}


		if ( referencedAttributeBinding == null ) {
			referencedAttributeBinding = attributeBinding( entityBinding.getEntity().getName(), referencedAttributeName );
		}
		if ( referencedAttributeBinding == null ) {
			throw bindingContext().makeMappingException( "Plural attribute key references an attribute binding that does not exist: "
					+ referencedAttributeBinding );
		}
		if ( !referencedAttributeBinding.getAttribute().isSingular() ) {
			throw bindingContext().makeMappingException( "Plural attribute key references a plural attribute; it must not be plural: "
					+ referencedAttributeName );
		}
		return ( SingularAttributeBinding ) referencedAttributeBinding;
	}

	private SingularAttributeBinding determineReferencedAttributeBinding(
			JoinColumnResolutionDelegate resolutionDelegate,
			JoinColumnResolutionContext resolutionContext,
			EntityBinding referencedEntityBinding) {
		if ( resolutionDelegate == null ) {
			return referencedEntityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding();
		}

		final String explicitName = resolutionDelegate.getReferencedAttributeName();
		final AttributeBinding referencedAttributeBinding = explicitName != null
				? referencedEntityBinding.locateAttributeBindingByPath( explicitName, true )
				: referencedEntityBinding.locateAttributeBinding( resolutionDelegate.getJoinColumns( resolutionContext ), true );

		if ( referencedAttributeBinding == null ) {
			if ( explicitName != null ) {
				throw bindingContext().makeMappingException(
						String.format(
								"No attribute binding found with name: %s.%s",
								referencedEntityBinding.getEntityName(),
								explicitName
						)
				);
			}
			else {
				throw new NotYetImplementedException(
						"No support yet for referenced join columns unless they correspond with columns bound for an attribute binding."
				);
			}
		}

		if ( !referencedAttributeBinding.getAttribute().isSingular() ) {
			throw bindingContext().makeMappingException(
					String.format(
							"Foreign key references a non-singular attribute [%s]",
							referencedAttributeBinding.getAttribute().getName() ) );
		}
		return (SingularAttributeBinding) referencedAttributeBinding;
	}

	private EntityBinding findOrBindEntityBinding(ValueHolder< Class< ? >> entityJavaTypeValue, String explicitEntityName) {
		final String referencedEntityName =
				explicitEntityName != null
						? explicitEntityName
						: entityJavaTypeValue.getValue().getName();
		return findOrBindEntityBinding( referencedEntityName );
	}

	/**
	 *
	 * @param entityName
	 * @return
	 */
	private EntityBinding findOrBindEntityBinding(final String entityName) {
		// Check if binding has already been created
		EntityBinding entityBinding = metadata.getEntityBinding( entityName );
		if ( entityBinding == null ) {
			// Find appropriate source to create binding
			final EntitySource entitySource = entitySourcesByName.get( entityName );
			if ( entitySource == null ) {
				String msg = log.missingEntitySource( entityName );
				throw bindingContext().makeMappingException( msg );
			}

			// Get super entity binding (creating it if necessary using recursive call to this method)
			if ( SubclassEntitySource.class.isInstance( entitySource ) ) {
				String superEntityName = ( (SubclassEntitySource) entitySource ).superclassEntitySource()
						.getEntityName();
				EntityBinding superEntityBinding = findOrBindEntityBinding( superEntityName );
				entityBinding = bindSubEntity( entitySource, superEntityBinding );
			}
			else {
				EntityHierarchy entityHierarchy = entityHierarchiesByRootEntitySource.get( entitySource );
				entityBinding = bindEntityHierarchy( entityHierarchy );
			}
		}
		return entityBinding;
	}

	// TODO: try to get rid of this...
	private static List<Column> extractColumnsFromRelationalValueBindings(
			List<RelationalValueBinding> valueBindings) {
		List<Column> columns = new ArrayList<Column>( valueBindings.size() );
		for ( RelationalValueBinding relationalValueBinding : valueBindings ) {
			final Value value = relationalValueBinding.getValue();
			// todo : currently formulas are not supported here... :(
			if ( !Column.class.isInstance( value ) ) {
				throw new NotYetImplementedException(
						"Derived values are not supported when creating a foreign key that targets columns." );
			}
			columns.add( (Column) value );
		}
		return columns;
	}

	private static boolean hasAnyNonNullableColumns(List<RelationalValueBinding> relationalValueBindings) {
		for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
			if ( Column.class.isInstance( relationalValueBinding.getValue() ) && !relationalValueBinding.isNullable() ) {
				return true;
			}
		}
		return false;
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

	private ForeignKey locateAndBindForeignKeyByName (
			String foreignKeyName,
			List<Column> sourceColumns,
			List<Column> targetColumns) {
		if ( foreignKeyName == null ) {
			throw new AssertionFailure( "foreignKeyName must be non-null." );
		}
		final TableSpecification sourceTable = sourceColumns.get( 0 ).getTable();
		final TableSpecification targetTable = targetColumns.get( 0 ).getTable();
		ForeignKey foreignKey = sourceTable.locateForeignKey( foreignKeyName );
		if ( foreignKey != null ) {
			if ( ! targetTable.equals( foreignKey.getTargetTable() ) ) {
				throw bindingContext().makeMappingException(
						String.format(
								"Unexpected target table defined for foreign key \"%s\"; expected \"%s\"; found \"%s\"",
								foreignKeyName, targetTable.getLogicalName(), foreignKey.getTargetTable().getLogicalName()
						)
				);
			}
			// check if source and target columns have been bound already
			if ( foreignKey.getColumnSpan() == 0 ) {
				// foreign key was found, but no columns bound to it yet
				bindForeignKeyColumns( foreignKey, sourceColumns, targetColumns );
			}
			else {
				// The located foreign key already has columns bound;
				// Make sure they are the same columns.
				if ( ! foreignKey.getSourceColumns().equals( sourceColumns ) ||
						foreignKey.getTargetColumns().equals( targetColumns ) ) {
					throw bindingContext().makeMappingException(
							String.format(
									"Attempt to bind exisitng foreign key \"%s\" with different columns.",
									foreignKeyName
							)
					);
				}
			}
		}
		return foreignKey;
	}

	private static ForeignKey locateForeignKeyByColumnMapping(
			List<Column> sourceColumns,
			List<Column> targetColumns) {
		final TableSpecification sourceTable = sourceColumns.get( 0 ).getTable();
		final TableSpecification targetTable = targetColumns.get( 0 ).getTable();
		// check for an existing foreign key with the same source/target columns
		ForeignKey foreignKey = null;
		Iterable<ForeignKey> possibleForeignKeys =  sourceTable.locateForeignKey( targetTable );
		if ( possibleForeignKeys != null ) {
			for ( ForeignKey possibleFK : possibleForeignKeys ) {
				if ( possibleFK.getSourceColumns().equals( sourceColumns ) &&
						possibleFK.getTargetColumns().equals( targetColumns ) ) {
					// this is the foreign key
					foreignKey = possibleFK;
					break;
				}
			}
		}
		return foreignKey;
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

	private static PluralAttributeElementBinding.Nature pluralAttributeElementNature(PluralAttributeSource attributeSource) {
		return PluralAttributeElementBinding.Nature.valueOf( attributeSource.getElementSource().getNature().name() );
	}

	private static PluralAttributeIndexBinding.Nature pluralAttributeIndexNature(PluralAttributeSource attributeSource) {
		if ( ! IndexedPluralAttributeSource.class.isInstance( attributeSource ) ) {
			return null;
		}
		return PluralAttributeIndexBinding.Nature.valueOf(
				( (IndexedPluralAttributeSource) attributeSource ).getIndexSource().getNature().name()
		);
	}

	private String propertyAccessorName( final AttributeSource attributeSource ) {
		return propertyAccessorName( attributeSource.getPropertyAccessorName() );
	}

	private String propertyAccessorName(final String propertyAccessorName) {
		return propertyAccessorName == null
				? bindingContext().getMappingDefaults().getPropertyAccessorName()
				: propertyAccessorName;
	}

	private String quoteIdentifierIfNonEmpty(String name) {
		return StringHelper.isEmpty( name ) ? null : quotedIdentifier( name );
	}

	private String quotedIdentifier( final String name ) {
		return bindingContext().isGloballyQuotedIdentifiers() ? StringHelper.quote( name ) : name;
	}


	private static String getReferencedPropertyNameIfNotId(PluralAttributeBinding pluralAttributeBinding) {
		EntityIdentifier entityIdentifier =
				pluralAttributeBinding.getContainer().seekEntityBinding().getHierarchyDetails().getEntityIdentifier();
		final String idAttributeName =
				entityIdentifier.getAttributeBinding().getAttribute().getName();
		return pluralAttributeBinding.getReferencedPropertyName().equals( idAttributeName ) ?
				null :
				pluralAttributeBinding.getReferencedPropertyName();
	}

	private Type resolveCustomCollectionType( PluralAttributeBinding pluralAttributeBinding ) {
		final HibernateTypeDescriptor hibernateTypeDescriptor = pluralAttributeBinding.getHibernateTypeDescriptor();
		Properties typeParameters = new Properties();
		typeParameters.putAll( hibernateTypeDescriptor.getTypeParameters() );
		return metadata.getTypeResolver().getTypeFactory().customCollection(
				hibernateTypeDescriptor.getExplicitTypeName(),
				typeParameters,
				pluralAttributeBinding.getAttribute().getName(),
				getReferencedPropertyNameIfNotId( pluralAttributeBinding ),
				pluralAttributeBinding.getPluralAttributeElementBinding()
						.getNature() == PluralAttributeElementBinding.Nature.AGGREGATE
		);
	}

	private Type resolvePluralType( 
			PluralAttributeBinding pluralAttributeBinding,
			PluralAttributeSource pluralAttributeSource,
			PluralAttributeSource.Nature nature){
		if ( pluralAttributeBinding.getHibernateTypeDescriptor().getExplicitTypeName() != null ) {
			return resolveCustomCollectionType( pluralAttributeBinding );
		} else {
			final TypeFactory typeFactory = metadata.getTypeResolver().getTypeFactory();
			final String role = pluralAttributeBinding.getAttribute().getRole();
			final String propertyRef = getReferencedPropertyNameIfNotId( pluralAttributeBinding );
			final boolean embedded = pluralAttributeBinding.getPluralAttributeElementBinding().getNature() == PluralAttributeElementBinding.Nature.AGGREGATE;
			switch ( nature ){
				case BAG:
					return typeFactory.bag( role, propertyRef, embedded );
				case LIST:
					return typeFactory.list( role, propertyRef, embedded );
				case ARRAY:
					return typeFactory.array( role, propertyRef, embedded, pluralAttributeSource.getElementClassReference().getValue() );
				case MAP:
					if ( pluralAttributeBinding.isSorted() ) {
						return typeFactory.sortedMap( role, propertyRef, embedded, pluralAttributeBinding.getComparator() );
					}
					// TODO: else if ( pluralAttributeBinding.hasOrder() ) { orderedMap... }
					else {
						return typeFactory.map( role, propertyRef, embedded );
					}
				case SET:
					if ( pluralAttributeBinding.isSorted() ) {
						return typeFactory.sortedSet( role, propertyRef, embedded, pluralAttributeBinding.getComparator() );
					}
					// TODO: else if ( pluralAttributeBinding.hasOrder() ) { orderedSet... }
					else {
						return typeFactory.set( role, propertyRef, embedded );
					}
				default:
					throw new NotYetImplementedException( nature + " is to be implemented" );
			}
		}
	}


	public static interface DefaultNamingStrategy {

		String defaultName();
	}


	public class JoinColumnResolutionContextImpl implements JoinColumnResolutionContext {
		private final EntityBinding referencedEntityBinding;


		public JoinColumnResolutionContextImpl(EntityBinding referencedEntityBinding) {
			this.referencedEntityBinding = referencedEntityBinding;
		}

		@Override
		public Column resolveColumn(
				String logicalColumnName,
				String logicalTableName,
				String logicalSchemaName,
				String logicalCatalogName ) {
			Identifier tableIdentifier = Identifier.toIdentifier(logicalTableName);
			if (tableIdentifier == null) {
				tableIdentifier = referencedEntityBinding.getPrimaryTable().getLogicalName();
			}

			Schema schema = metadata.getDatabase().getSchema( logicalCatalogName, logicalSchemaName );
			Table table = schema.locateTable(tableIdentifier );

			if(bindingContext().isGloballyQuotedIdentifiers() && !StringHelper.isQuoted(logicalColumnName)) {
				logicalColumnName = StringHelper.quote( logicalColumnName );
			}

			return table.locateColumn( logicalColumnName );
		}

		@Override
		public List< Value > resolveRelationalValuesForAttribute( String attributeName ) {
			if ( attributeName == null ) {
				List< Value > values = new ArrayList< Value >();
				for ( Column column : referencedEntityBinding.getPrimaryTable().getPrimaryKey().getColumns() ) {
					values.add( column );
				}
				return values;
			}
			final AttributeBinding referencedAttributeBinding =
					referencedEntityBinding.locateAttributeBinding( attributeName );
			if ( referencedAttributeBinding == null ) {
				throw bindingContext().makeMappingException(
						String.format(
								"Could not resolve named property-ref [%s] against entity [%s]",
								attributeName,
								referencedEntityBinding.getEntity().getName() ) );
			}
			if ( ! referencedAttributeBinding.getAttribute().isSingular() ) {
				throw bindingContext().makeMappingException(
						String.format(
								"Property-ref [%s] against entity [%s] is a plural attribute; it must be a singular attribute.",
								attributeName,
								referencedEntityBinding.getEntity().getName() ) );
			}
			List< Value > values = new ArrayList< Value >();
			SingularAttributeBinding referencedAttributeBindingAsSingular =
					( SingularAttributeBinding ) referencedAttributeBinding;
			for ( RelationalValueBinding valueBinding : referencedAttributeBindingAsSingular.getRelationalValueBindings() ) {
				values.add( valueBinding.getValue() );
			}
			return values;
		}
	}
}
