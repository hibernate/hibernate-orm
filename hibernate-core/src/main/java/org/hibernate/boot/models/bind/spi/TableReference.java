/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.bind.spi;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.models.bind.internal.InLineView;
import org.hibernate.boot.models.bind.internal.PhysicalTable;
import org.hibernate.boot.models.bind.internal.PhysicalView;
import org.hibernate.boot.models.bind.internal.SecondaryTable;
import org.hibernate.mapping.Table;

/**
 * Following the ANSI SQL "table reference" rule, will be one of <ul>
 *     <li>a {@linkplain PhysicalTable physical table}</li>
 *     <li>a {@linkplain SecondaryTable secondary table}</li>
 *     <li>a {@linkplain PhysicalView physical view}</li>
 *     <li>a {@linkplain InLineView in-line view}</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface TableReference {
	/**
	 * The name used across the metamodel sources (in the annotations, XML, etc...).
	 * In the case of physical tables and views, the logical name might not be the same
	 * as the table or view name (through {@linkplain org.hibernate.boot.model.naming.PhysicalNamingStrategy}, e.g.).
	 */
	Identifier logicalName();

	/**
	 * Should this "table" be exposed to schema tooling?
	 */
	boolean exportable();

	Table table();
}
