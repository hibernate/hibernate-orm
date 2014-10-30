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
package org.hibernate.persister.internal;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.persister.spi.UnknownPersisterException;

/**
 * @author Steve Ebersole
 */
public class StandardPersisterClassResolver implements PersisterClassResolver {

	@Override
	public Class<? extends EntityPersister> getEntityPersisterClass(PersistentClass metadata) {
		// todo : make sure this is based on an attribute kept on the metamodel in the new code, not the concrete PersistentClass impl found!
		if ( RootClass.class.isInstance( metadata ) ) {
            if ( metadata.hasSubclasses() ) {
                //If the class has children, we need to find of which kind
                metadata = (PersistentClass) metadata.getDirectSubclasses().next();
            }
            else {
			    return singleTableEntityPersister();
            }
		}
		if ( JoinedSubclass.class.isInstance( metadata ) ) {
			return joinedSubclassEntityPersister();
		}
		else if ( UnionSubclass.class.isInstance( metadata ) ) {
			return unionSubclassEntityPersister();
		}
        else if ( SingleTableSubclass.class.isInstance( metadata ) ) {
			return singleTableEntityPersister();
		}
		else {
			throw new UnknownPersisterException(
					"Could not determine persister implementation for entity [" + metadata.getEntityName() + "]"
			);
		}
	}

    public Class<? extends EntityPersister> singleTableEntityPersister() {
		return SingleTableEntityPersister.class;
	}

	public Class<? extends EntityPersister> joinedSubclassEntityPersister() {
		return JoinedSubclassEntityPersister.class;
	}

	public Class<? extends EntityPersister> unionSubclassEntityPersister() {
		return UnionSubclassEntityPersister.class;
	}

	@Override
	public Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata) {
		return metadata.isOneToMany() ? oneToManyPersister() : basicCollectionPersister();
	}

	private Class<OneToManyPersister> oneToManyPersister() {
		return OneToManyPersister.class;
	}

	private Class<BasicCollectionPersister> basicCollectionPersister() {
		return BasicCollectionPersister.class;
	}
}
