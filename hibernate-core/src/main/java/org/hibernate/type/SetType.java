/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.HashSet;

import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

public class SetType extends CollectionType {

	/**
	 * @deprecated Use {@link #SetType(org.hibernate.type.TypeFactory.TypeScope, String, String)} instead.
	 * See Jira issue: <a href="https://hibernate.onjira.com/browse/HHH-7771">HHH-7771</a>
	 */
	@Deprecated
	public SetType(TypeFactory.TypeScope typeScope, String role, String propertyRef, boolean isEmbeddedInXML) {
		super( typeScope, role, propertyRef, isEmbeddedInXML );
	}

	public SetType(TypeFactory.TypeScope typeScope, String role, String propertyRef) {
		super( typeScope, role, propertyRef );
	}

	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister, Serializable key) {
		return new PersistentSet(session);
	}

	public Class getReturnedClass() {
		return java.util.Set.class;
	}

	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		return new PersistentSet( session, (java.util.Set) collection );
	}

	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0
				? new HashSet()
				: new HashSet( anticipatedSize + (int)( anticipatedSize * .75f ), .75f );
	}
	
}
