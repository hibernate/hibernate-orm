/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

/**
 * Contract for things that need to be aware of JPA {@code orm.xml}-defined persistence-unit-defaults.  Only
 * MetadataBuildingOptions are supported to implement this contract.
 * <p/>
 * NOTE: it is expected that this contract will go away as we migrate to Jandex for annotation processing
 * and move to the annotation binding constructs done on the metamodel branch.
 *
 * @since 5.0
 *
 * @author Steve Ebersole
 */
public interface JpaOrmXmlPersistenceUnitDefaultAware {
	/**
	 * Represents the {@code persistence-unit-defaults} to be applied
	 */
	interface JpaOrmXmlPersistenceUnitDefaults {
		String getDefaultSchemaName();
		String getDefaultCatalogName();
		boolean shouldImplicitlyQuoteIdentifiers();
	}

	/**
	 * Apply the {@code orm.xml}-defined {@code persistence-unit-defaults} values.
	 *
	 * @param jpaOrmXmlPersistenceUnitDefaults The {@code persistence-unit-defaults} values
	 */
	void apply(JpaOrmXmlPersistenceUnitDefaults jpaOrmXmlPersistenceUnitDefaults);
}
