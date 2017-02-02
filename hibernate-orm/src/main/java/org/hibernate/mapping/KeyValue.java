/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;

/**
 * Represents an identifying key of a table: the value for primary key
 * of an entity, or a foreign key of a collection or join table or
 * joined subclass table.
 * @author Gavin King
 */
public interface KeyValue extends Value {

	public IdentifierGenerator createIdentifierGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			String defaultCatalog,
			String defaultSchema,
			RootClass rootClass) throws MappingException;

	public boolean isIdentityColumn(IdentifierGeneratorFactory identifierGeneratorFactory, Dialect dialect);
	
	public void createForeignKeyOfEntity(String entityName);
	
	public boolean isCascadeDeleteEnabled();
	
	public String getNullValue();
	
	public boolean isUpdateable();
}
