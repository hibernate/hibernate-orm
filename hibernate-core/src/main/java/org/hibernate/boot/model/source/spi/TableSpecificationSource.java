/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Contract describing source of "table specification" information.
 *
 * @author Steve Ebersole
 */
public interface TableSpecificationSource {
	/**
	 * Obtain the supplied schema name
	 *
	 * @return The schema name. If {@code null}, the binder will apply the default.
	 */
	public String getExplicitSchemaName();

	/**
	 * Obtain the supplied catalog name
	 *
	 * @return The catalog name. If {@code null}, the binder will apply the default.
	 */
	public String getExplicitCatalogName();
}
