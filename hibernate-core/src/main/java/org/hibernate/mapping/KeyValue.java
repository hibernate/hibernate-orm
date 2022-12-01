/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.dialect.Dialect;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.tuple.Generator;
import org.hibernate.tuple.InMemoryGenerator;

/**
 * A mapping model {@link Value} which may be treated as an identifying key of a
 * relational database table. A {@code KeyValue} might represent the primary key
 * of an entity or the foreign key of a collection, join table, secondary table,
 * or joined subclass table.
 *
 * @author Gavin King
 */
public interface KeyValue extends Value {

	ForeignKey createForeignKeyOfEntity(String entityName);
	
	boolean isCascadeDeleteEnabled();
	
	String getNullValue();
	
	boolean isUpdateable();

	Generator createGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			RootClass rootClass);

	/**
	 * @deprecated Use {@link #createGenerator(IdentifierGeneratorFactory, Dialect, RootClass)} instead.
	 */
	@Deprecated(since="6.2")
	default IdentifierGenerator createIdentifierGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			String defaultCatalog,
			String defaultSchema,
			RootClass rootClass) {
		return (IdentifierGenerator) createGenerator( identifierGeneratorFactory, dialect, rootClass );
	}

	/**
	 * @deprecated Use {@link #createGenerator(IdentifierGeneratorFactory, Dialect, RootClass)} instead.
	 */
	@Deprecated(since="6.2")
	default IdentifierGenerator createIdentifierGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			RootClass rootClass) {
		return (IdentifierGenerator) createGenerator( identifierGeneratorFactory, dialect, rootClass );
	}

	/**
	 * @deprecated We need to add {@code Column.isIdentity()}
	 */
	@Deprecated(since="6.2")
	boolean isIdentityColumn(IdentifierGeneratorFactory identifierGeneratorFactory, Dialect dialect);

}
