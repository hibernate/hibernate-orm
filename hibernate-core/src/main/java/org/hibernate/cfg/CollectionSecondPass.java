/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Value;

import org.jboss.logging.Logger;

/**
 * Collection second pass
 *
 * @author Emmanuel Bernard
 */
public abstract class CollectionSecondPass implements SecondPass {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, CollectionSecondPass.class.getName());

	MetadataBuildingContext buildingContext;
	Collection collection;

	public CollectionSecondPass(MetadataBuildingContext buildingContext, Collection collection) {
		this.collection = collection;
		this.buildingContext = buildingContext;
	}

	public void doSecondPass(Map<String, PersistentClass> persistentClasses)
			throws MappingException {
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Second pass for collection: %s", collection.getRole() );
		}

		secondPass( persistentClasses );
		collection.createAllKeys();

		if ( LOG.isDebugEnabled() ) {
			String msg = "Mapped collection key: " + columns( collection.getKey() );
			if ( collection.isIndexed() )
				msg += ", index: " + columns( ( (IndexedCollection) collection ).getIndex() );
			if ( collection.isOneToMany() ) {
				msg += ", one-to-many: "
					+ ( (OneToMany) collection.getElement() ).getReferencedEntityName();
			}
			else {
				msg += ", element: " + columns( collection.getElement() );
			}
			LOG.debug( msg );
		}
	}

	abstract public void secondPass(Map<String, PersistentClass> persistentClasses) throws MappingException;

	private static String columns(Value val) {
		StringBuilder columns = new StringBuilder();
		Iterator<Selectable> iter = val.getColumnIterator();
		while ( iter.hasNext() ) {
			columns.append( iter.next().getText() );
			if ( iter.hasNext() ) columns.append( ", " );
		}
		return columns.toString();
	}
}
