/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.collections.JoinedList;

/**
 * A mapping model object that represents a subclass in a
 * {@linkplain jakarta.persistence.InheritanceType#SINGLE_TABLE single table}
 * inheritance hierarchy.
 *
 * @author Gavin King
 */
public class SingleTableSubclass extends Subclass {

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
}
