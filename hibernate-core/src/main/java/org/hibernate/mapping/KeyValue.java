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
import org.hibernate.generator.Generator;

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

	default Generator createGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			RootClass rootClass,
			Property property) {
		return createGenerator( identifierGeneratorFactory, dialect, rootClass );
	}

	/**
	 * @deprecated Use {@link #createGenerator(IdentifierGeneratorFactory, Dialect, RootClass)} instead.
	 *             No longer used except in legacy tests.
	 *
	 * @return {@code null} if the {@code Generator} returned by {@link #createGenerator} is not an instance
	 *         of {@link IdentifierGenerator}.
	 */
	@Deprecated(since="6.2", forRemoval = true)
	default IdentifierGenerator createIdentifierGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			String defaultCatalog,
			String defaultSchema,
			RootClass rootClass) {
		final Generator generator = createGenerator( identifierGeneratorFactory, dialect, rootClass );
		return generator instanceof IdentifierGenerator ? (IdentifierGenerator) generator : null;
	}

	/**
	 * @deprecated Use {@link #createGenerator(IdentifierGeneratorFactory, Dialect, RootClass)} instead.
	 *             No longer used except in legacy tests.
	 *
	 * @return {@code null} if the {@code Generator} returned by {@link #createGenerator} is not an instance
	 *         of {@link IdentifierGenerator}.
	 */
	@Deprecated(since="6.2", forRemoval = true)
	default IdentifierGenerator createIdentifierGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			RootClass rootClass) {
		final Generator generator = createGenerator( identifierGeneratorFactory, dialect, rootClass );
		return generator instanceof IdentifierGenerator ? (IdentifierGenerator) generator : null;
	}
}
