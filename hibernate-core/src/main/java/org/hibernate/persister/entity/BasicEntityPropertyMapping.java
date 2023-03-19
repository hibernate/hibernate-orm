/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import org.hibernate.Remove;

/**
 * @author Gavin King
 *
 * @deprecated Replaced by {@link org.hibernate.metamodel.mapping.EntityMappingType}
 */
@Deprecated(since = "6", forRemoval = true)
@Remove
public class BasicEntityPropertyMapping extends AbstractPropertyMapping {

	private final AbstractEntityPersister persister;

	public BasicEntityPropertyMapping(AbstractEntityPersister persister) {
		this.persister = persister;
	}
	
	public String[] getIdentifierColumnNames() {
		return persister.getIdentifierColumnNames();
	}
	
	public String[] getIdentifierColumnReaders() {
		return persister.getIdentifierColumnReaders();
	}
	
	public String[] getIdentifierColumnReaderTemplates() {
		return persister.getIdentifierColumnReaderTemplates();
	}

	protected String getEntityName() {
		return persister.getEntityName();
	}


}
