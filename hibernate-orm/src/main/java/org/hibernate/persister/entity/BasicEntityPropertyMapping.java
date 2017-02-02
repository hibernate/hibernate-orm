/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;
import org.hibernate.QueryException;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
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

	public Type getType() {
		return persister.getType();
	}

	public String[] toColumns(final String alias, final String propertyName) throws QueryException {
		return super.toColumns( 
				AbstractEntityPersister.generateTableAlias(
						alias,
						persister.getSubclassPropertyTableNumber( propertyName )
				),
				propertyName 
			);
	}
	
	
}
