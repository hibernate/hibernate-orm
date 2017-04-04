/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.PrimaryKeyInformation;

/**
 * @author Steve Ebersole
 */
public class PrimaryKeyInformationImpl implements PrimaryKeyInformation {
	private final Identifier identifier;
	private final Iterable<ColumnInformation> columns;

	public PrimaryKeyInformationImpl(Identifier identifier, Iterable<ColumnInformation> columns) {
		this.identifier = identifier;
		this.columns = columns;
	}

	@Override
	public Identifier getPrimaryKeyIdentifier() {
		return identifier;
	}

	@Override
	public Iterable<ColumnInformation> getColumns() {
		return columns;
	}
}
