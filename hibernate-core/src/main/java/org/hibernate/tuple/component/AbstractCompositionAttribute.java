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
package org.hibernate.tuple.component;

import java.util.Iterator;

import org.hibernate.engine.internal.JoinHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.walking.spi.AssociationKey;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeSource;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.tuple.AbstractNonIdentifierAttribute;
import org.hibernate.tuple.BaselineAttributeInformation;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;

import static org.hibernate.engine.internal.JoinHelper.getLHSColumnNames;
import static org.hibernate.engine.internal.JoinHelper.getLHSTableName;
import static org.hibernate.engine.internal.JoinHelper.getRHSColumnNames;

/**
 * A base class for a composite, non-identifier attribute.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractCompositionAttribute extends AbstractNonIdentifierAttribute implements
																						   CompositionDefinition {
	protected AbstractCompositionAttribute(
			AttributeSource source,
			SessionFactoryImplementor sessionFactory,
			int attributeNumber,
			String attributeName,
			CompositeType attributeType,
			BaselineAttributeInformation baselineInfo) {
		super( source, sessionFactory, attributeNumber, attributeName, attributeType, baselineInfo );
	}

	@Override
	public CompositeType getType() {
		return (CompositeType) super.getType();
	}

	@Override
	public Iterable<AttributeDefinition> getAttributes() {
		return new Iterable<AttributeDefinition>() {
			@Override
			public Iterator<AttributeDefinition> iterator() {
				return new Iterator<AttributeDefinition>() {
					private final int numberOfAttributes = getType().getSubtypes().length;
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

						final String name = getType().getPropertyNames()[subAttributeNumber];
						final Type type = getType().getSubtypes()[subAttributeNumber];

						int columnPosition = currentColumnPosition;
						currentColumnPosition += type.getColumnSpan( sessionFactory() );

						if ( type.isAssociationType() ) {
							// we build the association-key here because of the "goofiness" with 'currentColumnPosition'
							final AssociationKey associationKey;
							final AssociationType aType = (AssociationType) type;
							final Joinable joinable = aType.getAssociatedJoinable( sessionFactory() );

							if ( aType.isAnyType() ) {
								associationKey = new AssociationKey(
										JoinHelper.getLHSTableName(
												aType,
												attributeNumber(),
												(OuterJoinLoadable) getSource()
										),
										JoinHelper.getLHSColumnNames(
												aType,
												attributeNumber(),
												0,
												(OuterJoinLoadable) getSource(),
												sessionFactory()
										)
								);
							}
							else if ( aType.getForeignKeyDirection() == ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT ) {
								final String lhsTableName;
								final String[] lhsColumnNames;

								if ( joinable.isCollection() ) {
									final QueryableCollection collectionPersister = (QueryableCollection) joinable;
									lhsTableName = collectionPersister.getTableName();
									lhsColumnNames = collectionPersister.getElementColumnNames();
								}
								else {
									final OuterJoinLoadable entityPersister = (OuterJoinLoadable) locateOwningPersister();
									lhsTableName = getLHSTableName( aType, attributeNumber(), entityPersister );
									lhsColumnNames = getLHSColumnNames( aType, attributeNumber(), entityPersister, sessionFactory() );
								}
								associationKey = new AssociationKey( lhsTableName, lhsColumnNames );

//								associationKey = new AssociationKey(
//										getLHSTableName(
//												aType,
//												attributeNumber(),
//												(OuterJoinLoadable) locateOwningPersister()
//										),
//										getLHSColumnNames(
//												aType,
//												attributeNumber(),
//												columnPosition,
//												(OuterJoinLoadable) locateOwningPersister(),
//												sessionFactory()
//										)
//								);
							}
							else {
								associationKey = new AssociationKey(
										joinable.getTableName(),
										getRHSColumnNames( aType, sessionFactory() )
								);
							}

							final CompositeType cType = getType();
							final boolean nullable = cType.getPropertyNullability() == null || cType.getPropertyNullability()[subAttributeNumber];

							return new CompositeBasedAssociationAttribute(
									AbstractCompositionAttribute.this,
									sessionFactory(),
									subAttributeNumber,
									name,
									(AssociationType) type,
									new BaselineAttributeInformation.Builder()
											.setInsertable( AbstractCompositionAttribute.this.isInsertable() )
											.setUpdateable( AbstractCompositionAttribute.this.isUpdateable() )
											.setInsertGenerated( AbstractCompositionAttribute.this.isInsertGenerated() )
											.setUpdateGenerated( AbstractCompositionAttribute.this.isUpdateGenerated() )
											.setNullable( nullable )
											.setDirtyCheckable( true )
											.setVersionable( AbstractCompositionAttribute.this.isVersionable() )
											.setCascadeStyle( getType().getCascadeStyle( subAttributeNumber ) )
											.setFetchMode( getType().getFetchMode( subAttributeNumber ) )
											.createInformation(),
									AbstractCompositionAttribute.this.attributeNumber(),
									associationKey
							);
						}
						else if ( type.isComponentType() ) {
							return new CompositionBasedCompositionAttribute(
									AbstractCompositionAttribute.this,
									sessionFactory(),
									subAttributeNumber,
									name,
									(CompositeType) type,
									new BaselineAttributeInformation.Builder()
											.setInsertable( AbstractCompositionAttribute.this.isInsertable() )
											.setUpdateable( AbstractCompositionAttribute.this.isUpdateable() )
											.setInsertGenerated( AbstractCompositionAttribute.this.isInsertGenerated() )
											.setUpdateGenerated( AbstractCompositionAttribute.this.isUpdateGenerated() )
											.setNullable( getType().getPropertyNullability()[subAttributeNumber] )
											.setDirtyCheckable( true )
											.setVersionable( AbstractCompositionAttribute.this.isVersionable() )
											.setCascadeStyle( getType().getCascadeStyle( subAttributeNumber ) )
											.setFetchMode( getType().getFetchMode( subAttributeNumber ) )
											.createInformation()
							);
						}
						else {
							final CompositeType cType = getType();
							final boolean nullable = cType.getPropertyNullability() == null || cType.getPropertyNullability()[subAttributeNumber];

							return new CompositeBasedBasicAttribute(
									AbstractCompositionAttribute.this,
									sessionFactory(),
									subAttributeNumber,
									name,
									type,
									new BaselineAttributeInformation.Builder()
											.setInsertable( AbstractCompositionAttribute.this.isInsertable() )
											.setUpdateable( AbstractCompositionAttribute.this.isUpdateable() )
											.setInsertGenerated( AbstractCompositionAttribute.this.isInsertGenerated() )
											.setUpdateGenerated( AbstractCompositionAttribute.this.isUpdateGenerated() )
											.setNullable( nullable )
											.setDirtyCheckable( true )
											.setVersionable( AbstractCompositionAttribute.this.isVersionable() )
											.setCascadeStyle( getType().getCascadeStyle( subAttributeNumber ) )
											.setFetchMode( getType().getFetchMode( subAttributeNumber ) )
											.createInformation()
							);
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

	public EntityPersister locateOwningPersister() {
		if ( EntityDefinition.class.isInstance( getSource() ) ) {
			return ( (EntityDefinition) getSource() ).getEntityPersister();
		}
		else {
			return ( (AbstractCompositionAttribute) getSource() ).locateOwningPersister();
		}
	}

	@Override
	protected String loggableMetadata() {
		return super.loggableMetadata() + ",composition";
	}
}

