/*
 * jDocBook, processing of DocBook sources
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

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.walking.spi.AssociationKey;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.AttributeSource;
import org.hibernate.persister.walking.spi.CompositeDefinition;
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
 * @author Steve Ebersole
 */
public abstract class AbstractCompositeDefinition extends AbstractNonIdentifierAttribute implements CompositeDefinition {
	protected AbstractCompositeDefinition(
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
					private int currentAttributeNumber = 0;
					private int currentColumnPosition = 0;

					@Override
					public boolean hasNext() {
						return currentAttributeNumber < numberOfAttributes;
					}

					@Override
					public AttributeDefinition next() {
						final int attributeNumber = currentAttributeNumber;
						currentAttributeNumber++;

						final String name = getType().getPropertyNames()[attributeNumber];
						final Type type = getType().getSubtypes()[attributeNumber];

						int columnPosition = currentColumnPosition;
						currentColumnPosition += type.getColumnSpan( sessionFactory() );

						if ( type.isAssociationType() ) {
							// we build the association-key here because of the "goofiness" with 'currentColumnPosition'
							final AssociationKey associationKey;
							final AssociationType aType = (AssociationType) type;
							final Joinable joinable = aType.getAssociatedJoinable( sessionFactory() );
							if ( aType.getForeignKeyDirection() == ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT ) {
								associationKey = new AssociationKey(
										getLHSTableName(
												aType,
												attributeNumber(),
												(OuterJoinLoadable) joinable
										),
										getLHSColumnNames(
												aType,
												attributeNumber(),
												columnPosition,
												(OuterJoinLoadable) joinable,
												sessionFactory()
										)
								);
							}
							else {
								associationKey = new AssociationKey(
										joinable.getTableName(),
										getRHSColumnNames( aType, sessionFactory() )
								);
							}

							return new CompositeBasedAssociationAttribute(
									AbstractCompositeDefinition.this,
									sessionFactory(),
									currentAttributeNumber,
									name,
									(AssociationType) type,
									new BaselineAttributeInformation.Builder()
											.setInsertable( AbstractCompositeDefinition.this.isInsertable() )
											.setUpdateable( AbstractCompositeDefinition.this.isUpdateable() )
											.setInsertGenerated( AbstractCompositeDefinition.this.isInsertGenerated() )
											.setUpdateGenerated( AbstractCompositeDefinition.this.isUpdateGenerated() )
											.setNullable( getType().getPropertyNullability()[currentAttributeNumber] )
											.setDirtyCheckable( true )
											.setVersionable( AbstractCompositeDefinition.this.isVersionable() )
											.setCascadeStyle( getType().getCascadeStyle( currentAttributeNumber ) )
											.setFetchMode( getType().getFetchMode( currentAttributeNumber ) )
											.createInformation(),
									AbstractCompositeDefinition.this.attributeNumber(),
									associationKey
							);
						}
						else if ( type.isComponentType() ) {
							return new CompositeBasedCompositeAttribute(
									AbstractCompositeDefinition.this,
									sessionFactory(),
									currentAttributeNumber,
									name,
									(CompositeType) type,
									new BaselineAttributeInformation.Builder()
											.setInsertable( AbstractCompositeDefinition.this.isInsertable() )
											.setUpdateable( AbstractCompositeDefinition.this.isUpdateable() )
											.setInsertGenerated( AbstractCompositeDefinition.this.isInsertGenerated() )
											.setUpdateGenerated( AbstractCompositeDefinition.this.isUpdateGenerated() )
											.setNullable( getType().getPropertyNullability()[currentAttributeNumber] )
											.setDirtyCheckable( true )
											.setVersionable( AbstractCompositeDefinition.this.isVersionable() )
											.setCascadeStyle( getType().getCascadeStyle( currentAttributeNumber ) )
											.setFetchMode( getType().getFetchMode( currentAttributeNumber ) )
											.createInformation()
							);
						}
						else {
							return new CompositeBasedBasicAttribute(
									AbstractCompositeDefinition.this,
									sessionFactory(),
									currentAttributeNumber,
									name,
									type,
									new BaselineAttributeInformation.Builder()
											.setInsertable( AbstractCompositeDefinition.this.isInsertable() )
											.setUpdateable( AbstractCompositeDefinition.this.isUpdateable() )
											.setInsertGenerated( AbstractCompositeDefinition.this.isInsertGenerated() )
											.setUpdateGenerated( AbstractCompositeDefinition.this.isUpdateGenerated() )
											.setNullable( getType().getPropertyNullability()[currentAttributeNumber] )
											.setDirtyCheckable( true )
											.setVersionable( AbstractCompositeDefinition.this.isVersionable() )
											.setCascadeStyle( getType().getCascadeStyle( currentAttributeNumber ) )
											.setFetchMode( getType().getFetchMode( currentAttributeNumber ) )
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
			return ( (AbstractCompositeDefinition) getSource() ).locateOwningPersister();
		}
	}
}

