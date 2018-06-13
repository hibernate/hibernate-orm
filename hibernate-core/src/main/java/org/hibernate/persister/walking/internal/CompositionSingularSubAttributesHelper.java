/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.walking.internal;

import java.util.Iterator;

import org.hibernate.FetchMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.spi.HydratedCompoundValueHandler;
import org.hibernate.persister.walking.spi.AnyMappingDefinition;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AssociationKey;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeSource;
import org.hibernate.persister.walking.spi.CollectionDefinition;
import org.hibernate.persister.walking.spi.CompositeCollectionElementDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.AnyType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * A helper for getting attributes from a composition that is known
 * to have only singular attributes; for example, sub-attributes of a
 * composite ID or a composite collection element.
 *
 * TODO: This should be refactored into a delegate and renamed.
 *
 * @author Gail Badner
 */
public final class CompositionSingularSubAttributesHelper {
	private CompositionSingularSubAttributesHelper() {
	}

	/**
	 * Get composite ID sub-attribute definitions.
	 *
	 * @param entityPersister - the entity persister.
	 *
	 * @return composite ID sub-attribute definitions.
	 */
	public static Iterable<AttributeDefinition> getIdentifierSubAttributes(AbstractEntityPersister entityPersister) {
		return getSingularSubAttributes(
				entityPersister,
				entityPersister,
				(CompositeType) entityPersister.getIdentifierType(),
				entityPersister.getTableName(),
				entityPersister.getRootTableIdentifierColumnNames()
		);
	}

	/**
	 * Get sub-attribute definitions for a composite collection element.
	 * @param compositionElementDefinition - composite collection element definition.
	 * @return sub-attribute definitions for a composite collection element.
	 */
	public static Iterable<AttributeDefinition> getCompositeCollectionElementSubAttributes(
			CompositeCollectionElementDefinition compositionElementDefinition) {
		final QueryableCollection collectionPersister =
				(QueryableCollection) compositionElementDefinition.getCollectionDefinition().getCollectionPersister();
		return getSingularSubAttributes(
				compositionElementDefinition.getSource(),
				(OuterJoinLoadable) collectionPersister.getOwnerEntityPersister(),
				(CompositeType) collectionPersister.getElementType(),
				collectionPersister.getTableName(),
				collectionPersister.getElementColumnNames()
		);
	}

	public static Iterable<AttributeDefinition> getCompositeCollectionIndexSubAttributes(CompositeCollectionElementDefinition compositionElementDefinition){
		final QueryableCollection collectionPersister =
				(QueryableCollection) compositionElementDefinition.getCollectionDefinition().getCollectionPersister();
		return getSingularSubAttributes(
				compositionElementDefinition.getSource(),
				(OuterJoinLoadable) collectionPersister.getOwnerEntityPersister(),
				(CompositeType) collectionPersister.getIndexType(),
				collectionPersister.getTableName(),
				collectionPersister.toColumns( "index" )
		);
	}

	private static Iterable<AttributeDefinition> getSingularSubAttributes(
			final AttributeSource source,
			final OuterJoinLoadable ownerEntityPersister,
			final CompositeType compositeType,
			final String lhsTableName,
			final String[] lhsColumns) {
		return new Iterable<AttributeDefinition>() {
			@Override
			public Iterator<AttributeDefinition> iterator() {
				return new Iterator<AttributeDefinition>() {
					private final int numberOfAttributes = compositeType.getSubtypes().length;
					private int currentSubAttributeNumber;
					private int currentColumnPosition;

					@Override
					public boolean hasNext() {
						return currentSubAttributeNumber < numberOfAttributes;
					}

					@Override
					public AttributeDefinition next() {
						final int subAttributeNumber = currentSubAttributeNumber;
						currentSubAttributeNumber++;

						final String name = compositeType.getPropertyNames()[subAttributeNumber];
						final Type type = compositeType.getSubtypes()[subAttributeNumber];
						final FetchMode fetchMode = compositeType.getFetchMode( subAttributeNumber );

						final int columnPosition = currentColumnPosition;
						final int columnSpan = type.getColumnSpan( ownerEntityPersister.getFactory() );
						final String[] subAttributeLhsColumns = ArrayHelper.slice( lhsColumns, columnPosition, columnSpan );


						final boolean[] propertyNullability = compositeType.getPropertyNullability();
						final boolean nullable = propertyNullability == null || propertyNullability[subAttributeNumber];

						currentColumnPosition += columnSpan;

						if ( type.isAssociationType() ) {
							final AssociationType aType = (AssociationType) type;
							return new AssociationAttributeDefinition() {
								@Override
								public AssociationKey getAssociationKey() {
									return new AssociationKey( lhsTableName, subAttributeLhsColumns );
								}


								@Override
								public AssociationNature getAssociationNature() {
									if ( type.isAnyType() ) {
										return AssociationNature.ANY;
									}
									else {
										// cannot be a collection
										return AssociationNature.ENTITY;
									}
								}

								@Override
								public EntityDefinition toEntityDefinition() {
									if ( getAssociationNature() != AssociationNature.ENTITY ) {
										throw new WalkingException(
												"Cannot build EntityDefinition from non-entity-typed attribute"
										);
									}
									return (EntityPersister) aType.getAssociatedJoinable( ownerEntityPersister.getFactory() );
								}

								@Override
								public AnyMappingDefinition toAnyDefinition() {
									if ( getAssociationNature() != AssociationNature.ANY ) {
										throw new WalkingException(
												"Cannot build AnyMappingDefinition from non-any-typed attribute"
										);
									}
									// todo : not sure how lazy is propogated into the component for a subattribute of type any
									return new StandardAnyTypeDefinition( (AnyType) aType, false );
								}

								@Override
								public CollectionDefinition toCollectionDefinition() {
									throw new WalkingException( "A collection cannot be mapped to a composite ID sub-attribute." );
								}

								@Override
								public FetchStrategy determineFetchPlan(LoadQueryInfluencers loadQueryInfluencers, PropertyPath propertyPath) {
									final FetchStyle style = FetchStrategyHelper.determineFetchStyleByMetadata(
											fetchMode,
											(AssociationType) type,
											ownerEntityPersister.getFactory()
									);
									return new FetchStrategy(
											FetchStrategyHelper.determineFetchTiming(
													style,
													getType(),
													ownerEntityPersister.getFactory()
											),
											style
									);
								}

								@Override
								public CascadeStyle determineCascadeStyle() {
									return CascadeStyles.NONE;
								}

								@Override
								public HydratedCompoundValueHandler getHydratedCompoundValueExtractor() {
									return null;
								}

								@Override
								public String getName() {
									return name;
								}

								@Override
								public AssociationType getType() {
									return aType;
								}

								@Override
								public boolean isNullable() {
									return nullable;
								}

								@Override
								public AttributeSource getSource() {
									return source;
								}
							};
						}
						else if ( type.isComponentType() ) {
							return new CompositionDefinition() {
								@Override
								public String getName() {
									return name;
								}

								@Override
								public CompositeType getType() {
									return (CompositeType) type;
								}

								@Override
								public boolean isNullable() {
									return nullable;
								}

								@Override
								public AttributeSource getSource() {
									return source;
								}

								@Override
								public Iterable<AttributeDefinition> getAttributes() {
									return CompositionSingularSubAttributesHelper.getSingularSubAttributes(
											this,
											ownerEntityPersister,
											(CompositeType) type,
											lhsTableName,
											subAttributeLhsColumns
									);
								}
							};
						}
						else {
							return new AttributeDefinition() {
								@Override
								public String getName() {
									return name;
								}

								@Override
								public Type getType() {
									return type;
								}

								@Override
								public boolean isNullable() {
									return nullable;
								}

								@Override
								public AttributeSource getSource() {
									return source;
								}
							};
						}
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException( "Remove operation not supported here" );
					}
				};
			}
		};
	}
}
