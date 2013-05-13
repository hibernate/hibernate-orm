/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.persister.walking.internal;

import java.util.Iterator;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadeStyles;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.spi.HydratedCompoundValueHandler;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AssociationKey;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeSource;
import org.hibernate.persister.walking.spi.CollectionDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.persister.walking.spi.EncapsulatedEntityIdentifierDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.persister.walking.spi.EntityIdentifierDefinition;
import org.hibernate.persister.walking.spi.NonEncapsulatedEntityIdentifierDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.AssociationType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

import static org.hibernate.engine.internal.JoinHelper.getLHSColumnNames;
import static org.hibernate.engine.internal.JoinHelper.getLHSTableName;

/**
 * @author Gail Badner
 */
public class EntityIdentifierDefinitionHelper {

	public static EntityIdentifierDefinition buildSimpleEncapsulatedIdentifierDefinition(final AbstractEntityPersister entityPersister) {
		return new EncapsulatedEntityIdentifierDefinition() {
			@Override
			public AttributeDefinition getAttributeDefinition() {
				return new AttributeDefinitionAdapter( entityPersister);
			}

			@Override
			public boolean isEncapsulated() {
				return true;
			}

			@Override
			public EntityDefinition getEntityDefinition() {
				return entityPersister;
			}
		};
	}

	public static EntityIdentifierDefinition buildEncapsulatedCompositeIdentifierDefinition(final AbstractEntityPersister entityPersister) {

		return new EncapsulatedEntityIdentifierDefinition() {
			@Override
			public AttributeDefinition getAttributeDefinition() {
				return new CompositeAttributeDefinitionAdapter( entityPersister );
			}

			@Override
			public boolean isEncapsulated() {
				return true;
			}

			@Override
			public EntityDefinition getEntityDefinition() {
				return entityPersister;
			}
		};
	}

	public static EntityIdentifierDefinition buildNonEncapsulatedCompositeIdentifierDefinition(final AbstractEntityPersister entityPersister) {
		return new NonEncapsulatedEntityIdentifierDefinition() {
			@Override
			public Iterable<AttributeDefinition> getAttributes() {
				return new CompositeAttributeDefinitionAdapter( entityPersister ).getAttributes();
			}

			@Override
			public Class getSeparateIdentifierMappingClass() {
				return entityPersister.getEntityMetamodel().getIdentifierProperty().getType().getReturnedClass();
			}

			@Override
			public boolean isEncapsulated() {
				return false;
			}

			@Override
			public EntityDefinition getEntityDefinition() {
				return entityPersister;
			}
		};
	}

	private static class AttributeDefinitionAdapter implements AttributeDefinition {
		private final AbstractEntityPersister entityPersister;

		AttributeDefinitionAdapter(AbstractEntityPersister entityPersister) {
			this.entityPersister = entityPersister;
		}

		@Override
		public String getName() {
			return entityPersister.getEntityMetamodel().getIdentifierProperty().getName();
		}

		@Override
		public Type getType() {
			return entityPersister.getEntityMetamodel().getIdentifierProperty().getType();
		}

		@Override
		public AttributeSource getSource() {
			return entityPersister;
		}

		@Override
		public String toString() {
			return "<identifier-property:" + getName() + ">";
		}

		protected AbstractEntityPersister getEntityPersister() {
			return entityPersister;
		}
	}

	private static class CompositeAttributeDefinitionAdapter extends AttributeDefinitionAdapter implements CompositionDefinition {

		CompositeAttributeDefinitionAdapter(AbstractEntityPersister entityPersister) {
			super( entityPersister );
		}

		@Override
		public Iterable<AttributeDefinition> getAttributes() {
			return new Iterable<AttributeDefinition>() {
				@Override
				public Iterator<AttributeDefinition> iterator() {
					final ComponentType componentType = (ComponentType) getType();
					return new Iterator<AttributeDefinition>() {
						private final int numberOfAttributes = componentType.getSubtypes().length;
						private int currentSubAttributeNumber = 0;
						private int currentColumnPosition = 0;

						@Override
						public boolean hasNext() {
							return currentSubAttributeNumber < numberOfAttributes;
						}

						@Override
						public AttributeDefinition next() {
							final int subAttributeNumber = currentSubAttributeNumber;
							currentSubAttributeNumber++;

							final AttributeSource source = getSource();
							final String name = componentType.getPropertyNames()[subAttributeNumber];
							final Type type = componentType.getSubtypes()[subAttributeNumber];

							final int columnPosition = currentColumnPosition;
							currentColumnPosition += type.getColumnSpan( getEntityPersister().getFactory() );

							if ( type.isAssociationType() ) {
								final AssociationType aType = (AssociationType) type;
								final Joinable joinable = aType.getAssociatedJoinable( getEntityPersister().getFactory() );
								return new AssociationAttributeDefinition() {
									@Override
									public AssociationKey getAssociationKey() {
										/* TODO: is this always correct? */
										//return new AssociationKey(
										//		joinable.getTableName(),
										//		JoinHelper.getRHSColumnNames( aType, getEntityPersister().getFactory() )
										//);
										return new AssociationKey(
												getEntityPersister().getTableName(),
												getLHSColumnNames(
														aType,
														-1,
														columnPosition,
														getEntityPersister(),
														getEntityPersister().getFactory()
												)
										);

									}

									@Override
									public boolean isCollection() {
										return false;
									}

									@Override
									public EntityDefinition toEntityDefinition() {
										return (EntityPersister) joinable;
									}

									@Override
									public CollectionDefinition toCollectionDefinition() {
										throw new WalkingException( "A collection cannot be mapped to a composite ID sub-attribute." );
									}

									@Override
									public FetchStrategy determineFetchPlan(LoadQueryInfluencers loadQueryInfluencers, PropertyPath propertyPath) {
										return new FetchStrategy( FetchTiming.IMMEDIATE, FetchStyle.JOIN );
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
									public Type getType() {
										return type;
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
									public Type getType() {
										return type;
									}

									@Override
									public AttributeSource getSource() {
										return source;
									}

									@Override
									public Iterable<AttributeDefinition> getAttributes() {
										return null;  //To change body of implemented methods use File | Settings | File Templates.
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
}