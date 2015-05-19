/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.TruthValue;
import org.hibernate.boot.model.source.spi.ColumnSource;
import org.hibernate.boot.model.source.spi.JdbcDataType;
import org.hibernate.boot.model.source.spi.SizeSource;

/**
 * Implementation of a {@link ColumnSource} when the column is declared as just the name via the column XML
 * attribute.  For example, {@code <property name="socialSecurityNumber" column="ssn"/>}.
 *
 * @author Steve Ebersole
 */
class ColumnAttributeSourceImpl
		extends AbstractHbmSourceNode
		implements ColumnSource {

	private final String tableName;
	private final String columnName;
	private final SizeSource sizeSource;
	private final TruthValue nullable;
	private final TruthValue unique;

	ColumnAttributeSourceImpl(
			MappingDocument mappingDocument,
			String tableName,
			String columnName,
			SizeSource sizeSource,
			TruthValue nullable,
			TruthValue unique) {
		super( mappingDocument );
		this.tableName = tableName;
		this.columnName = columnName;
		this.sizeSource = sizeSource;
		this.nullable = nullable;
		this.unique = unique;
	}

	@Override
	public Nature getNature() {
		return Nature.COLUMN;
	}

	@Override
	public String getContainingTableName() {
		return tableName;
	}

	@Override
	public String getName() {
		return columnName;
	}

	@Override
	public TruthValue isNullable() {
		return nullable;
	}

	@Override
	public String getDefaultValue() {
		return null;
	}

	@Override
	public String getSqlType() {
		return null;
	}

	@Override
	public JdbcDataType getDatatype() {
		return null;
	}

	@Override
	public SizeSource getSizeSource() {
		return sizeSource;
	}

	@Override
	public String getReadFragment() {
		return null;
	}

	@Override
	public String getWriteFragment() {
		return null;
	}

	@Override
	public boolean isUnique() {
		return unique == TruthValue.TRUE;
	}

	@Override
	public String getCheckCondition() {
		return null;
	}

	@Override
	public String getComment() {
		return null;
	}
}
