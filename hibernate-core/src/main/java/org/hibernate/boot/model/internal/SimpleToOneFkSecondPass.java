/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.internal;

import org.hibernate.MappingException;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.ToOne;

/**
 * A simple second pass that just creates the foreign key
 *
 * @author Christian Beikov
 */
public class SimpleToOneFkSecondPass extends FkSecondPass {

	public SimpleToOneFkSecondPass(ToOne value) {
		super( value, null );
	}

	@Override
	public String getReferencedEntityName() {
		return ( (ToOne) value ).getReferencedEntityName();
	}

	@Override
	public boolean isInPrimaryKey() {
		return false;
	}

	@Override
	public void doSecondPass(java.util.Map<String, PersistentClass> persistentClasses) throws MappingException {
		value.createForeignKey();
	}
}
