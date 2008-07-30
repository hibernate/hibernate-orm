/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.util.JoinedIterator;

/**
 * @author Gavin King
 */
public class SingleTableSubclass extends Subclass {
	
	public SingleTableSubclass(PersistentClass superclass) {
		super(superclass);
	}
	
	protected Iterator getNonDuplicatedPropertyIterator() {
		return new JoinedIterator( 
				getSuperclass().getUnjoinedPropertyIterator(),
				getUnjoinedPropertyIterator()
		);
	}
	
	protected Iterator getDiscriminatorColumnIterator() {
		if ( isDiscriminatorInsertable() && !getDiscriminator().hasFormula() ) {
			return getDiscriminator().getColumnIterator();
		}
		else {
			return super.getDiscriminatorColumnIterator();
		}
	}

	public Object accept(PersistentClassVisitor mv) {
		return mv.accept(this);
	}
    
    public void validate(Mapping mapping) throws MappingException {
        if(getDiscriminator()==null) {
            throw new MappingException("No discriminator found for " + getEntityName() + ". Discriminator is needed when 'single-table-per-hierarchy' is used and a class has subclasses");
        }
        super.validate(mapping);
    }
}
