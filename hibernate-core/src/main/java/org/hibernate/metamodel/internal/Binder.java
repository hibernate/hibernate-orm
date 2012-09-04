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
import org.hibernate.FetchMode;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.TruthValue;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.metamodel.internal.HibernateTypeHelper.ReflectedCollectionJavaTypes;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicPluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.BasicPluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityDiscriminator;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.metamodel.spi.binding.EntityVersion;
import org.hibernate.metamodel.spi.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.spi.binding.IdGenerator;
import org.hibernate.metamodel.spi.binding.IndexedPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.metamodel.spi.binding.ManyToOneAttributeBinding;
import org.hibernate.metamodel.spi.binding.MetaAttribute;
import org.hibernate.metamodel.spi.binding.MutableAttributeBindingContainer;
import org.hibernate.metamodel.spi.binding.NonAggregatedCompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.OneToManyPluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeElementBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeIndexBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeKeyBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SecondaryTable;
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
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

import static org.hibernate.engine.spi.SyntheticAttributeHelper.SYNTHETIC_COMPOSITE_ID_ATTRIBUTE_NAME;

/**
 * The common binder shared between annotations and {@code hbm.xml} processing.
 * <p/>
 * The API consists of {@link #Binder(org.hibernate.metamodel.spi.MetadataImplementor, IdentifierGeneratorFactory)} and {@link #bindEntities(Iterable)}
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Gail Badner
 */
public class Binder {
	private static final CoreMessageLogger log = Logger.getMessageLogger(
			CoreMessageLogger.class,
			Binder.class.getName()
	);

	private static org.hibernate.internal.util.ValueHolder< Class< ? >> createSingularAttributeJavaType(
			final Class< ? > attributeContainerClassReference,
			final String attributeName ) {
		org.hibernate.internal.util.ValueHolder.DeferredInitializer< Class< ? >> deferredInitializer =
				new org.hibernate.internal.util.ValueHolder.DeferredInitializer< Class< ? >>() {
					public Class< ? > initialize() {
						return ReflectHelper.reflectedPropertyClass(
								attributeContainerClassReference,
								attributeName );
					}
				};
		return new org.hibernate.internal.util.ValueHolder< Class< ? >>( deferredInitializer );
	}

	private static org.hibernate.internal.util.ValueHolder< Class< ? >> createSingularAttributeJavaType(
			final SingularAttribute attribute ) {
		return createSingularAttributeJavaType(
				attribute.getAttributeContainer().getClassReference(),
				attribute.getName()
		);
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

	private static boolean toBoolean( final TruthValue truthValue, final boolean truthValueDefault ) {
		if ( truthValue == TruthValue.TRUE ) {
			return true;
		}
		if ( truthValue == TruthValue.FALSE ) {
			return false;
		}
		return truthValueDefault;
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

	private void addUniqueConstraintForNaturalIdColumn(final TableSpecification table, final Column column) {
		final UniqueKey uniqueKey = table.getOrCreateUniqueKey( "natural_id_unique_key_" );
		uniqueKey.addColumn( column );
	}

	private AttributeBinding attributeBinding( final String entityName, final String attributeName ) {
		// Check if binding has already been created
		final EntityBinding entityBinding = entityBinding( entityName );
		final AttributeSource attributeSource = attributeSourcesByName.get( attributeSourcesByNameKey( entityName, attributeName ) );
		bindAttribute( entityBinding, attributeSource );
		return entityBinding.locateAttributeBinding( attributeName );
	}

	private String attributeSourcesByNameKey( final String entityName, final String attributeName ) {
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
			final MutableAttributeBindingContainer attributeBindingContainer,
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
			final MutableAttributeBindingContainer attributeBindingContainer,
			final AttributeSourceContainer attributeSourceContainer ) {
		for ( final AttributeSource attributeSource : attributeSourceContainer.attributeSources() ) {
			bindAttribute( attributeBindingContainer, attributeSource );
		}
	}

	private AbstractPluralAttributeBinding bindBagAttribute(
			final MutableAttributeBindingContainer attributeBindingContainer,
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
			final MutableAttributeBindingContainer attributeBindingContainer,
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
						attributeSource.getNaturalIdMutability(),
						createMetaAttributeContext( attributeBindingContainer, attributeSource ),
						attributeSource.getGeneration() );
		final HibernateTypeDescriptor hibernateTypeDescriptor = attributeBinding.getHibernateTypeDescriptor();
		bindHibernateTypeDescriptor(
				attributeBinding.getHibernateTypeDescriptor(),
				attributeSource.getTypeInformation(),
				createSingularAttributeJavaType( attributeBinding.getAttribute() ) );
		Type resolvedType = heuristicType( hibernateTypeDescriptor );
		bindHibernateResolvedType( attributeBinding.getHibernateTypeDescriptor(), resolvedType );
		typeHelper.bindJdbcDataType( resolvedType, relationalValueBindings );
		attributeBinding.getAttribute().resolveType( bindingContext().makeJavaType( hibernateTypeDescriptor.getJavaTypeName() ) );
		return attributeBinding;
	}

	private void bindBasicCollectionElement(
			final BasicPluralAttributeElementBinding elementBinding,
			final BasicPluralAttributeElementSource elementSource,
			final String defaultElementJavaTypeName ) {
		bindBasicPluralElementRelationalValues( elementSource, elementBinding );
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

	private void bindBasicCollectionKey(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeSource attributeSource) {
		if ( attributeSource.getElementSource().getNature() != PluralAttributeElementSource.Nature.BASIC ) {
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

	private void bindBasicPluralElementRelationalValues(
			final RelationalValueSourceContainer relationalValueSourceContainer,
			final BasicPluralAttributeElementBinding elementBinding ) {
		elementBinding.setRelationalValueBindings( bindValues(
				elementBinding.getPluralAttributeBinding().getContainer(),
				relationalValueSourceContainer,
				elementBinding.getPluralAttributeBinding().getAttribute(),
				elementBinding.getPluralAttributeBinding().getPluralAttributeKeyBinding().getCollectionTable() ) );
	}

	private void bindBasicSetElementTablePrimaryKey(final PluralAttributeBinding attributeBinding) {
		final BasicPluralAttributeElementBinding elementBinding =
				( BasicPluralAttributeElementBinding ) attributeBinding.getPluralAttributeElementBinding();
		if ( elementBinding.getNature() != PluralAttributeElementBinding.Nature.BASIC ) {
			bindingContext().makeMappingException( String.format(
					"Expected a SetBinding with an element of nature Nature.BASIC; instead was %s",
					elementBinding.getNature() ) );
		}
		if ( hasAnyNonNullableColumns( elementBinding.getRelationalValueBindings() ) ) {
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
		else {
			// for backward compatibility, allow a set with no not-null
			// element columns, using all columns in the row locater SQL
			// todo: create an implicit not null constraint on all cols?
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
				indexedAttributeBinding.getPluralAttributeKeyBinding().getCollectionTable() ).get( 0 ).getValue() );
		if ( attributeBinding.getPluralAttributeElementBinding().getNature() == PluralAttributeElementBinding.Nature.ONE_TO_MANY ) {
			if ( !Column.class.isInstance( indexBinding.getIndexRelationalValue() ) ) {
				throw new NotYetImplementedException( "derived value as collection index is not supported yet." );
			}
			// TODO: fix this when column nullability is refactored
			( (Column) indexBinding.getIndexRelationalValue() ).setNullable( true );
		}

		bindHibernateTypeDescriptor(
				indexBinding.getHibernateTypeDescriptor(),
				attributeSource.explicitHibernateTypeSource(),
				defaultIndexJavaTypeName );
		Type resolvedElementType = heuristicType( indexBinding.getHibernateTypeDescriptor() );
		bindHibernateResolvedType( indexBinding.getHibernateTypeDescriptor(), resolvedElementType );
		typeHelper.bindJdbcDataType( resolvedElementType, indexBinding.getIndexRelationalValue() );
	}

	void bindCollectionTableForeignKey(
			final AbstractPluralAttributeBinding attributeBinding,
			final PluralAttributeKeySource keySource,
			TableSpecification collectionTable) {

		final AttributeBindingContainer attributeBindingContainer = attributeBinding.getContainer();
		final PluralAttributeKeyBinding keyBinding = attributeBinding.getPluralAttributeKeyBinding();

		List<RelationalValueBinding> sourceColumnBindings =
				bindValues( attributeBindingContainer, keySource, attributeBinding.getAttribute(), collectionTable );
		// Determine if the foreign key (source) column is updatable and also extract the columns out
		// of the RelationalValueBindings.
		boolean isUpdatable = false;
		List<Column> sourceColumns = new ArrayList<Column>( sourceColumnBindings.size() );
		for ( RelationalValueBinding relationalValueBinding : sourceColumnBindings ) {
			final Value value = relationalValueBinding.getValue();
			// todo : currently formulas are not supported here... :(
			if ( !Column.class.isInstance( value ) ) {
				throw new NotYetImplementedException(
						"Derived values are not supported when creating a foreign key that targets columns." );
			}
			isUpdatable = isUpdatable || relationalValueBinding.isIncludeInUpdate();
			sourceColumns.add( (Column) value );
		}
		keyBinding.setIncludedInUpdate( isUpdatable );

		List<Column> targetColumns =
				determineForeignKeyTargetColumns(
						attributeBindingContainer.seekEntityBinding(),
						keySource
				);

		final String foreignKeyName =
				StringHelper.isEmpty( keySource.getExplicitForeignKeyName() )
						? null
						: quotedIdentifier( keySource.getExplicitForeignKeyName() );
		ForeignKey foreignKey = bindForeignKey(
				foreignKeyName,
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
			final PluralAttributeSource attributeSource,
			final HibernateTypeHelper.ReflectedCollectionJavaTypes reflectedCollectionJavaTypes ) {
		PluralAttributeSource.Nature pluralAttributeNature = attributeSource.getNature();
		if ( attributeSource.getElementSource().getNature() == PluralAttributeElementSource.Nature.ONE_TO_MANY
				|| pluralAttributeNature == PluralAttributeSource.Nature.BAG ) {
			return;
		}
		if ( attributeBinding.getPluralAttributeElementBinding().getNature() == PluralAttributeElementBinding.Nature.BASIC ) {
			if ( pluralAttributeNature == PluralAttributeSource.Nature.SET ) {
				bindBasicSetElementTablePrimaryKey( attributeBinding );
			} else if ( pluralAttributeNature == PluralAttributeSource.Nature.LIST || pluralAttributeNature == PluralAttributeSource.Nature.MAP ) {
				bindIndexedTablePrimaryKey( ( IndexedPluralAttributeBinding ) attributeBinding );
			} else {
				throw new NotYetImplementedException( "Only Sets with basic elements are supported so far." );
			}
		}
	}

	private CompositeAttributeBinding bindAggregatedCompositeAttribute(
			final MutableAttributeBindingContainer attributeBindingContainer,
			final ComponentAttributeSource attributeSource,
			SingularAttribute attribute,
			boolean isAttributeIdentifier) {
		final Composite composite;
		ValueHolder<Class<?>> defaultJavaClassReference = null;
		if ( attribute == null ) {
			if ( attributeSource.getClassName() != null ) {
				composite = new Composite(
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
				composite = new Composite(
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
			composite = (Composite) attribute.getSingularAttributeType();
		}

		final SingularAttribute referencingAttribute =
				StringHelper.isEmpty( attributeSource.getParentReferenceAttributeName() ) ?
						null :
						composite.createSingularAttribute( attributeSource.getParentReferenceAttributeName() );
		final SingularAttributeBinding.NaturalIdMutability naturalIdMutability = attributeSource.getNaturalIdMutability();
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
		// TODO: binding the HibernateTypeDescriptor should be simplified since we know the class name already
		bindHibernateTypeDescriptor(
				attributeBinding.getHibernateTypeDescriptor(),
				composite.getClassName(),
				null,
				defaultJavaClassReference == null ? null : defaultJavaClassReference.getValue().getName()
		);
		if (  referencingAttribute == null ) {
			bindAttributes( attributeBinding, attributeSource );
		}
		else {
			for ( final AttributeSource subAttributeSource : attributeSource.attributeSources() ) {
				// TODO: don't create a "parent" attribute binding???
				if ( ! subAttributeSource.getName().equals( referencingAttribute.getName() ) ) {
					bindAttribute( attributeBinding, subAttributeSource );
				}
			}
		}
		Type resolvedType = metadata.getTypeResolver().getTypeFactory().component(
				new ComponentMetamodel( attributeBinding, isAttributeIdentifier, false )
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
							bindingContexts.peek().getMappingDefaults().getDiscriminatorColumnName(),
							false,
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
		typeHelper.bindJdbcDataType( resolvedType, value );
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
			entityBinding.setMutable( entityBinding.getHierarchyDetails().getRootEntityBinding().isMutable() );
			bindSecondaryTables( entityBinding, entitySource );
			bindUniqueConstraints( entityBinding, entitySource );
			bindAttributes( entityBinding, entitySource );
			bindSubEntities( entityBinding, entitySource );
			return entityBinding;
		} finally {
			bindingContexts.pop();
		}
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
			final org.hibernate.internal.util.ValueHolder< Class< ? >> defaultJavaType ) {
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
				bindingContext().makeMappingException( String.format(
						"Attempt to re-initialize (non-explicit) Java type name; current=%s new=%s",
						hibernateTypeDescriptor.getJavaTypeName(),
						defaultJavaTypeName ) );
			}
			hibernateTypeDescriptor.setJavaTypeName( defaultJavaTypeName );
		} else {
			// Check if user-specified name is of a User-Defined Type (UDT)
			final TypeDefinition typeDef = metadata.getTypeDefinition( explicitTypeName );
			if ( hibernateTypeDescriptor.getExplicitTypeName() != null ) {
				bindingContext().makeMappingException( String.format(
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
			final MutableAttributeBindingContainer attributeBindingContainer,
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
				bindingContext().makeMappingException( String.format(
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


	private AbstractPluralAttributeBinding bindListAttribute(
			final MutableAttributeBindingContainer attributeBindingContainer,
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

	private ManyToOneAttributeBinding bindManyToOneAttribute(
			final MutableAttributeBindingContainer attributeBindingContainer,
			final ToOneAttributeSource attributeSource,
			SingularAttribute attribute ) {
		if( attribute == null ){
			attribute = createSingularAttribute( attributeBindingContainer, attributeSource );
		}
		// TODO: figure out which table is used (could be secondary table...)
		final TableSpecification table = attributeBindingContainer.seekEntityBinding().getPrimaryTable();


		//find the referenced entitybinding
		org.hibernate.internal.util.ValueHolder< Class< ? >> referencedJavaTypeValue = createSingularAttributeJavaType( attribute );
		final String referencedEntityName =
				bindingContext().qualifyClassName( attributeSource.getReferencedEntityName() != null
						? attributeSource.getReferencedEntityName()
						: referencedJavaTypeValue.getValue().getName() );
		final EntityBinding referencedEntityBinding = entityBinding( referencedEntityName );
		//now find the referenced attribute binding, either the referenced entity's id attribute or the referenced attribute
		//todo referenced entityBinding null check?
		// Foreign key...
		final ForeignKeyContributingSource.JoinColumnResolutionDelegate resolutionDelegate =
				attributeSource.getForeignKeyTargetColumnResolutionDelegate();
		final ForeignKeyContributingSource.JoinColumnResolutionContext resolutionContext;
		if ( resolutionDelegate != null ) {
			resolutionContext = new JoinColumnResolutionContext(referencedEntityBinding, attributeSource);
		} else {
			resolutionContext = null;
		}

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
		/**
		 * this is not correct, here, if no @JoinColumn defined, we simply create the FK column only with column calucated
		 * but what we should do is get all the column info from the referenced column(s), including nullable, size etc.
		 */
		final List<RelationalValueBinding> relationalValueBindings =
				bindValues(
						attributeBindingContainer,
						attributeSource,
						attribute,
						table,
						attributeSource.getDefaultNamingStrategies(
								attributeBindingContainer.seekEntityBinding().getEntity().getName(),
								table.getLogicalName().getText(),
								referencedAttributeBinding
						));


		// todo : currently a chicken-egg problem here between creating the attribute binding and binding its FK values...
		// now we have everything to create a ManyToOneAttributeBinding
		final ManyToOneAttributeBinding attributeBinding =
				attributeBindingContainer.makeManyToOneAttributeBinding(
						attribute,
						propertyAccessorName( attributeSource ),
						attributeSource.isIncludedInOptimisticLocking(),
						attributeSource.isLazy(),
						attributeSource.getNaturalIdMutability(),
						createMetaAttributeContext( attributeBindingContainer, attributeSource ),
						referencedEntityBinding,
						(SingularAttributeBinding) referencedAttributeBinding,
						relationalValueBindings
				);

		bindForeignKey(
				attributeSource.getExplicitForeignKeyName(),
				extractColumnsFromRelationalValueBindings( attributeBinding.getRelationalValueBindings() ),
				determineForeignKeyTargetColumns( referencedEntityBinding, attributeSource )
		);

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
		typeHelper.bindJdbcDataType( resolvedType, relationalValueBindings );

		attributeBinding.setCascadeStyles( attributeSource.getCascadeStyles() );
		attributeBinding.setFetchTiming( attributeSource.getFetchTiming() );
		attributeBinding.setFetchStyle( attributeSource.getFetchStyle() );
//		attributeBinding.setFetchMode( attributeSource.getFetchMode() );


		return attributeBinding;
	}

	private AbstractPluralAttributeBinding bindMapAttribute(
			final MutableAttributeBindingContainer attributeBindingContainer,
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
		final NonAggregatedCompositeAttributeBinding syntheticAttributeBinding =
				rootEntityBinding.makeVirtualCompositeAttributeBinding(
						syntheticAttribute,
						idAttributeBindings,
						createMetaAttributeContext( rootEntityBinding, identifierSource.getMetaAttributeSources() )
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
		TableSpecification collectionTable = referencedEntityBinding.getPrimaryTable();
		bindCollectionTableForeignKey( attributeBinding, attributeSource.getKeySource(), collectionTable );
		attributeBinding.getPluralAttributeKeyBinding().setInverse( attributeSource.isInverse() );
	}

	private AbstractPluralAttributeBinding bindPluralAttribute(
			final MutableAttributeBindingContainer attributeBindingContainer,
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
			default:
				throw new NotYetImplementedException( nature.toString() );
		}

		// Must do first -- sorting/ordering can determine the resolved type
		// (ex: Set vs. SortedSet).
		bindSortingAndOrdering( attributeBinding, attributeSource );
		
		final Type resolvedType = resolvePluralType( attributeBinding, nature );
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

		if ( attributeSource.getElementSource().getNature() == PluralAttributeElementSource.Nature.BASIC ) {
			bindBasicCollectionKey( attributeBinding, attributeSource );
			bindBasicCollectionElement(
					( BasicPluralAttributeElementBinding ) attributeBinding.getPluralAttributeElementBinding(),
					( BasicPluralAttributeElementSource ) attributeSource.getElementSource(),
					defaultCollectionElementJavaTypeName( reflectedCollectionJavaTypes ) );
		}
		else if ( attributeSource.getElementSource().getNature() == PluralAttributeElementSource.Nature.ONE_TO_MANY ) {
			final OneToManyPluralAttributeElementSource elementSource =
					(OneToManyPluralAttributeElementSource) attributeSource.getElementSource();
			final String defaultElementJavaTypeName = defaultCollectionElementJavaTypeName( reflectedCollectionJavaTypes );
			String referencedEntityName =
					elementSource.getReferencedEntityName() != null ?
							elementSource.getReferencedEntityName() :
							defaultElementJavaTypeName;
			if  ( referencedEntityName == null ) {
				bindingContext().makeMappingException( String.format( "The mapping for the entity associated with one-to-many attribute (%s) is undefined.",
						createAttributePath( attributeBinding )
				) );
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

	private void bindPrimaryTable( final EntityBinding entityBinding, final EntitySource entitySource ) {
		entityBinding.setPrimaryTable( createTable( entitySource.getPrimaryTable(), new DefaultNamingStrategy() {

			@Override
			public String defaultName() {
				return bindingContexts.peek().getNamingStrategy().classToTableName( entityBinding.getEntity().getClassName() );
			}
		} ) );
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
						}
					}
					joinColumns.add( column );
				}
			}

			// TODO: make the foreign key column the primary key???
			final ForeignKey foreignKey = bindForeignKey(
					secondaryTableSource.getExplicitForeignKeyName(),
					joinColumns,
					determineForeignKeyTargetColumns( entityBinding, secondaryTableSource )
			);

			entityBinding.addSecondaryTable( new SecondaryTable( table, foreignKey ) );
		}
	}

	private AbstractPluralAttributeBinding bindSetAttribute(
			final MutableAttributeBindingContainer attributeBindingContainer,
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
				unsavedValue );
	}

	private SingularAttributeBinding bindSingularAttribute(
			final MutableAttributeBindingContainer attributeBindingContainer,
			final SingularAttributeSource attributeSource,
			boolean isIdentifierAttribute) {
		final SingularAttributeSource.Nature nature = attributeSource.getNature();
		final SingularAttribute attribute =
				attributeBindingContainer.getAttributeContainer().locateSingularAttribute( attributeSource.getName() );
		switch ( nature ) {
			case BASIC:
				return bindBasicAttribute( attributeBindingContainer, attributeSource, attribute );
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
			if ( UniqueConstraintSource.class.isInstance( constraintSource ) ) {
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

	private List<RelationalValueBinding> bindValues(
			final AttributeBindingContainer attributeBindingContainer,
			final RelationalValueSourceContainer valueSourceContainer,
			final Attribute attribute,
			final TableSpecification defaultTable) {
		final List<DefaultNamingStrategy> list = new ArrayList<DefaultNamingStrategy>( 1 );
		list.add(
				new DefaultNamingStrategy() {
					@Override
					public String defaultName() {
						return bindingContext().getNamingStrategy().propertyToColumnName( attribute.getName() );
					}
				}
		);
		return bindValues( attributeBindingContainer, valueSourceContainer, attribute, defaultTable, list );
	}

	private List< RelationalValueBinding > bindValues(
			final AttributeBindingContainer attributeBindingContainer,
			final RelationalValueSourceContainer valueSourceContainer,
			final Attribute attribute,
			final TableSpecification defaultTable,
			final List<DefaultNamingStrategy> defaultNamingStrategyList) {
		final List<RelationalValueBinding> valueBindings = new ArrayList<RelationalValueBinding>();
		final SingularAttributeBinding.NaturalIdMutability naturalIdMutability = SingularAttributeSource.class.isInstance(
				valueSourceContainer
		) ? SingularAttributeSource.class.cast( valueSourceContainer ).getNaturalIdMutability()
				: SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID;
		final boolean isNaturalId = naturalIdMutability != SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID;
		final boolean isImmutableNaturalId = isNaturalId && (naturalIdMutability == SingularAttributeBinding.NaturalIdMutability.IMMUTABLE);

		if ( valueSourceContainer.relationalValueSources().isEmpty() ) {
			for(DefaultNamingStrategy defaultNamingStrategy : defaultNamingStrategyList){
				final String columnName =
						quotedIdentifier( defaultNamingStrategy.defaultName() );
				final Column column = defaultTable.locateOrCreateColumn( columnName );
				column.setNullable( !isNaturalId && valueSourceContainer.areValuesNullableByDefault() );
				if(isNaturalId){
					addUniqueConstraintForNaturalIdColumn( defaultTable, column );
				}
				valueBindings.add( new RelationalValueBinding(
						column,
						valueSourceContainer.areValuesIncludedInInsertByDefault(),
						valueSourceContainer.areValuesIncludedInUpdateByDefault() && !isImmutableNaturalId ) );
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
							toBoolean(
									columnSource.isIncludedInInsert(),
									valueSourceContainer.areValuesIncludedInInsertByDefault() );
					final boolean isIncludedInUpdate =
							toBoolean(
									columnSource.isIncludedInUpdate(),
									valueSourceContainer.areValuesIncludedInUpdateByDefault() );
					Column column = createColumn( table, columnSource, name, isNaturalId, valueSourceContainer.areValuesNullableByDefault(), true );
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
					bindingContexts.peek().getMappingDefaults().getDiscriminatorColumnName(),
					false,
					false,
					false
			);
		}
		else {
			return table.locateOrCreateDerivedValue( ( ( DerivedValueSource ) valueSource ).getExpression() );
		}
	}

	private String createAttributePath(final AttributeBinding attributeBinding) {
		return new StringBuffer( attributeBinding.getContainer().getPathBase() )
				.append( '.' )
				.append( attributeBinding.getAttribute().getName() )
				.toString();
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
								? ( ( Table ) owner.getPrimaryTable() ).getPhysicalName().getText()
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

	private Column createColumn(
			final TableSpecification table,
			final ColumnSource columnSource,
			final String defaultName,
			final boolean forceNotNull,
			final boolean isNullableByDefault,
			final boolean isDefaultAttributeName ) {
		if ( columnSource.getName() == null && defaultName == null ) {
			bindingContext().makeMappingException( "Cannot resolve name for column because no name was specified and default name is null." );
		}
		final String name;
		if ( StringHelper.isNotEmpty( columnSource.getName() ) ) {
			name = bindingContexts.peek().getNamingStrategy().columnName( columnSource.getName() );
		} else if ( isDefaultAttributeName ) {
			name = bindingContexts.peek().getNamingStrategy().propertyToColumnName( defaultName );
		} else {
			name = bindingContexts.peek().getNamingStrategy().columnName( defaultName );
		}
		final String resolvedColumnName = quotedIdentifier( name );
		final Column column = table.locateOrCreateColumn( resolvedColumnName );
		if ( forceNotNull ) {
			column.setNullable( false );
			if(columnSource.isNullable() == TruthValue.TRUE){
				log.warn( String.format( "Natural Id column[%s] has explicit set to allow nullable, we have to make it force not null ", columnSource.getName() ) );
			}
		}
		else {
			// if the column is already non-nullable, leave it alone
			if ( column.isNullable() ) {
				column.setNullable( toBoolean( columnSource.isNullable(), isNullableByDefault ) );
			}
		}
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
		}
		else {
			bindPrimaryTable( entityBinding, entitySource );
		}

		if ( inheritanceType == InheritanceType.JOINED && superEntityBinding != null ) {
			ForeignKey fk = entityBinding.getPrimaryTable().createForeignKey(
					superEntityBinding.getPrimaryTable(),
					( (SubclassEntitySource) entitySource ).getJoinedForeignKeyName()
			);
			// explicitly maps to target table pk
			for ( Column column : entityBinding.getPrimaryTable().getPrimaryKey().getColumns() ) {
				fk.addColumn( column );
			}
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
				entitySource.getMetaAttributeSources(),
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

	private MetaAttributeContext createMetaAttributeContext(
			final AttributeBindingContainer attributeBindingContainer,
			final AttributeSource attributeSource ) {
		return createMetaAttributeContext( attributeBindingContainer, attributeSource.getMetaAttributeSources() );
	}

	private MetaAttributeContext createMetaAttributeContext(
			final AttributeBindingContainer attributeBindingContainer,
			final Iterable<? extends MetaAttributeSource > metaAttributeSources ) {
		return createMetaAttributeContext(
				metaAttributeSources,
				false,
				attributeBindingContainer.getMetaAttributeContext()
		);
	}

	private MetaAttributeContext createMetaAttributeContext(
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

	private SingularAttribute createSingularAttribute(
			final AttributeBindingContainer attributeBindingContainer,
			final SingularAttributeSource attributeSource ) {
		return attributeSource.isVirtualAttribute()
				? attributeBindingContainer.getAttributeContainer().createSyntheticSingularAttribute( attributeSource.getName() )
				: attributeBindingContainer.getAttributeContainer().createSingularAttribute( attributeSource.getName() );
	}

	private TableSpecification createTable(
			final TableSpecificationSource tableSpecSource,
			final DefaultNamingStrategy defaultNamingStrategy) {

		final LocalBindingContext bindingContext = bindingContexts.peek();
		final MappingDefaults mappingDefaults = bindingContext.getMappingDefaults();
		final String explicitCatalogName = tableSpecSource == null ? null : tableSpecSource.getExplicitCatalogName();
		final String explicitSchemaName = tableSpecSource == null ? null : tableSpecSource.getExplicitSchemaName();
		final Schema.Name schemaName =
				new Schema.Name(
						createIdentifier( explicitCatalogName, mappingDefaults.getCatalogName() ),
						createIdentifier( explicitSchemaName, mappingDefaults.getSchemaName() )
				);
		final Schema schema = metadata.getDatabase().locateSchema( schemaName );

		TableSpecification tableSpec = null;
		if ( tableSpecSource == null ) {
			if ( defaultNamingStrategy == null ) {
				bindingContext().makeMappingException( "An explicit name must be specified for the table" );
			}
			String tableName = defaultNamingStrategy.defaultName();
			tableSpec = createTableSpecification( bindingContext, schema, tableName );
		}
		else if ( tableSpecSource instanceof TableSource ) {
			final TableSource tableSource = ( TableSource ) tableSpecSource;
			String tableName = tableSource.getExplicitTableName();
			if ( tableName == null ) {
				if ( defaultNamingStrategy == null ) {
					bindingContext().makeMappingException( "An explicit name must be specified for the table" );
				}
				tableName = defaultNamingStrategy.defaultName();
			}
			tableSpec = createTableSpecification( bindingContext, schema, tableName );
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

	private TableSpecification createTableSpecification(LocalBindingContext bindingContext, Schema schema, String tableName) {
		TableSpecification tableSpec;
		tableName = quotedIdentifier( tableName );
		final Identifier logicalTableId = Identifier.toIdentifier( tableName );
		tableName = quotedIdentifier( bindingContext.getNamingStrategy().tableName( tableName ) );
		final Identifier physicalTableId = Identifier.toIdentifier( tableName );
		final Table table = schema.locateTable( logicalTableId );
		tableSpec = ( table == null ? schema.createTable( logicalTableId, physicalTableId ) : table );
		return tableSpec;
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
				: attributeSource.getNature().reportedJavaType().getName();
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
									entityBinding.locateAttributeBinding( attributeName );
							if ( referencedAttributeBinding == null ) {
								throw bindingContext().makeMappingException(
										String.format(
												"Could not resolve named property-ref [%s] against entity [%s]",
												attributeName,
												entityBinding.getEntity().getName() ) );
							}
							if ( ! referencedAttributeBinding.getAttribute().isSingular() ) {
								throw bindingContext().makeMappingException(
										String.format(
												"Property-ref [%s] against entity [%s] is a plural attribute; it must be a singular attribute.",
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
		final ForeignKeyContributingSource.JoinColumnResolutionDelegate resolutionDelegate =
				attributeSource.getKeySource().getForeignKeyTargetColumnResolutionDelegate();

		if ( resolutionDelegate == null ) {
			return entityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding();
		}

		AttributeBinding referencedAttributeBinding;
		final String referencedAttributeName = resolutionDelegate.getReferencedAttributeName();
		if ( referencedAttributeName == null ) {
			referencedAttributeBinding = attributeBindingContainer.locateAttributeBinding(
					resolutionDelegate.getJoinColumns( new ForeignKeyContributingSource.JoinColumnResolutionContext() {
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
			bindingContext().makeMappingException( "Plural attribute key references an attribute binding that does not exist: "
					+ referencedAttributeBinding );
		}
		if ( !referencedAttributeBinding.getAttribute().isSingular() ) {
			bindingContext().makeMappingException( "Plural attribute key references a plural attribute; it must not be plural: "
					+ referencedAttributeName );
		}
		return ( SingularAttributeBinding ) referencedAttributeBinding;
	}

	private AttributeBinding determineReferencedAttributeBinding(
			ForeignKeyContributingSource.JoinColumnResolutionDelegate resolutionDelegate,
			ForeignKeyContributingSource.JoinColumnResolutionContext resolutionContext,
			EntityBinding referencedEntityBinding) {
		if ( resolutionDelegate == null ) {
			return referencedEntityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding();
		}

		final String explicitName = resolutionDelegate.getReferencedAttributeName();
		return explicitName != null
				? referencedEntityBinding.locateAttributeBinding( explicitName )
				:referencedEntityBinding.locateAttributeBinding(resolutionDelegate.getJoinColumns( resolutionContext ) );
	}

	private EntityBinding entityBinding( final String entityName ) {
		// Check if binding has already been created
		EntityBinding entityBinding = metadata.getEntityBinding( entityName );
		if ( entityBinding == null ) {
			// Find appropriate source to create binding
			final EntitySource entitySource = entitySourcesByName.get( entityName );
			if(entitySource == null) {
				String msg = log.missingEntitySource( entityName );
				bindingContext().makeMappingException( msg );
			}

			// Get super entity binding (creating it if necessary using recursive call to this method)
			final EntityBinding superEntityBinding =
					SubclassEntitySource.class.isInstance( entitySource )
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

	// TODO: try to get rid of this...
	private List<Column> extractColumnsFromRelationalValueBindings(
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

	private boolean hasAnyNonNullableColumns(List<RelationalValueBinding> relationalValueBindings) {
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

	private ForeignKey locateForeignKeyByColumnMapping(
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

	private PluralAttributeElementBinding.Nature pluralAttributeElementNature(PluralAttributeSource attributeSource) {
		return PluralAttributeElementBinding.Nature.valueOf( attributeSource.getElementSource().getNature().name() );
	}

	private PluralAttributeIndexBinding.Nature pluralAttributeIndexNature(PluralAttributeSource attributeSource) {
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
				? bindingContexts.peek().getMappingDefaults().getPropertyAccessorName()
				: propertyAccessorName;
	}

	private String quotedIdentifier( final String name ) {
		return bindingContexts.peek().isGloballyQuotedIdentifiers() ? StringHelper.quote( name ) : name;
	}


	private String getReferencedPropertyNameIfNotId(PluralAttributeBinding pluralAttributeBinding) {
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
						.getNature() == PluralAttributeElementBinding.Nature.COMPOSITE
		);
	}

	private Type resolvePluralType( PluralAttributeBinding pluralAttributeBinding, PluralAttributeSource.Nature nature){
		if ( pluralAttributeBinding.getHibernateTypeDescriptor().getExplicitTypeName() != null ) {
			return resolveCustomCollectionType( pluralAttributeBinding );
		} else {
			final TypeFactory typeFactory = metadata.getTypeResolver().getTypeFactory();
			final String role = pluralAttributeBinding.getAttribute().getRole();
			final String propertyRef = getReferencedPropertyNameIfNotId( pluralAttributeBinding );
			final boolean embedded = pluralAttributeBinding.getPluralAttributeElementBinding().getNature() == PluralAttributeElementBinding.Nature.COMPOSITE;
			switch ( nature ){
				case BAG:
					return typeFactory.bag( role, propertyRef, embedded );
				case LIST:
					return typeFactory.list( role, propertyRef, embedded );
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


	public class JoinColumnResolutionContext implements ForeignKeyContributingSource.JoinColumnResolutionContext {
		private final EntityBinding referencedEntityBinding;
		private final ToOneAttributeSource attributeSource;


		public JoinColumnResolutionContext(EntityBinding referencedEntityBinding, ToOneAttributeSource attributeSource) {
			this.referencedEntityBinding = referencedEntityBinding;
			this.attributeSource = attributeSource;
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

			if(bindingContexts.peek().isGloballyQuotedIdentifiers() && !StringHelper.isQuoted(logicalColumnName)) {
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
	}
}
