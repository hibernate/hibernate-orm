/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.RootClass;

/**
 * @author Andrea Boriero
 */
public interface KeyValueMapping extends ValueMapping {
	boolean isIdentityColumn(IdentifierGeneratorFactory identifierGeneratorFactory);

	ForeignKey createForeignKeyOfEntity(String entityName);

	boolean isCascadeDeleteEnabled();

	String getNullValue();

	boolean isUpdateable();

	IdentifierGenerator createIdentifierGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			String defaultCatalog,
			String defaultSchema,
			RootClass rootClass) throws MappingException;
}
