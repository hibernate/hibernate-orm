/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.collections.JoinedList;

import static org.hibernate.persister.entity.DiscriminatorHelper.getDiscriminatorSQLValue;


/**
 * A mapping model object that represents a subclass in a
 * {@linkplain jakarta.persistence.InheritanceType#SINGLE_TABLE single table}
 * inheritance hierarchy.
 *
 * @author Gavin King
 */
public final class SingleTableSubclass extends Subclass {

	public SingleTableSubclass(PersistentClass superclass, MetadataBuildingContext buildingContext) {
		super( superclass, buildingContext );
	}

	protected List<Property> getNonDuplicatedProperties() {
		return new JoinedList<>( getSuperclass().getUnjoinedProperties(), getUnjoinedProperties() );
	}

	public Object accept(PersistentClassVisitor mv) {
		return mv.accept( this );
	}

	public void validate(Metadata mapping) throws MappingException {
		if ( getDiscriminator() == null ) {
			throw new MappingException( "No discriminator defined by '" + getSuperclass().getEntityName()
					+ "' which is a root class in a 'SINGLE_TABLE' inheritance hierarchy"
			);
		}
		super.validate( mapping );
	}

	@Override
	public void createConstraints(MetadataBuildingContext context) {
		if ( !isAbstract() ) {
			final var dialect = context.getMetadataCollector().getDatabase().getDialect();
			if ( dialect.supportsTableCheck() ) {
				final var discriminator = getDiscriminator();
				final var selectables = discriminator.getSelectables();
				if ( selectables.size() == 1 ) {
					final var check = new StringBuilder();
					check.append( selectables.get( 0 ).getText( dialect ) );
					if ( isDiscriminatorValueNull() ) {
						check.append( " is " );
					}
					else if ( isDiscriminatorValueNotNull() ) {
						check.append( " is " );
						// can't enforce this for now, because 'not null'
						// really means "not null and not any of the other
						// explicitly listed values"
						return;  //ABORT
					}
					else {
						check.append( " <> " );
					}
					check.append( getDiscriminatorSQLValue( this, dialect ) )
							.append( " or (" );
					boolean first = true;
					for ( var property : getNonDuplicatedProperties() ) {
						if ( !property.isComposite() && !property.isOptional() ) {
							for ( var selectable : property.getSelectables() ) {
								if ( selectable instanceof Column column && column.isNullable() ) {
									if ( first ) {
										first = false;
									}
									else {
										check.append( " and " );
									}
									check.append( column.getQuotedName( dialect ) )
											.append( " is not null" );
								}
							}
						}
					}
					check.append( ")" );
					if ( !first ) {
						getTable().addCheck( new CheckConstraint( check.toString() ) );
					}
				}
			}
		}
	}
}
