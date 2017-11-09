/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.internal;

import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;

/**
 * @author Steve Ebersole
 */
public class ColumnMappingImpl implements ForeignKey.ColumnMappings.ColumnMapping {
	private final Column referringColumn;
	private final Column targetColumn;

	public ColumnMappingImpl(Column referringColumn, Column targetColumn) {
		this.referringColumn = referringColumn;
		this.targetColumn = targetColumn;
	}

	@Override
	public Column getReferringColumn() {
		return referringColumn;
	}

	@Override
	public Column getTargetColumn() {
		return targetColumn;
	}
}
