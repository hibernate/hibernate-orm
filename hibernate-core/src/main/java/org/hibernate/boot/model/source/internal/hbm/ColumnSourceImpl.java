/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;


import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.model.TruthValue;
import org.hibernate.boot.model.source.spi.ColumnSource;
import org.hibernate.boot.model.source.spi.JdbcDataType;
import org.hibernate.boot.model.source.spi.SizeSource;

/**
 * @author Steve Ebersole
 */
class ColumnSourceImpl
		extends AbstractHbmSourceNode
		implements ColumnSource {
	private final String tableName;
	private final JaxbHbmColumnType columnElement;
	private final TruthValue nullable;

	ColumnSourceImpl(
			MappingDocument mappingDocument,
			String tableName,
			JaxbHbmColumnType columnElement) {
		this(
				mappingDocument,
				tableName,
				columnElement,
				interpretNotNullToNullability( columnElement.isNotNull() )
		);
	}

	private static TruthValue interpretNotNullToNullability(Boolean notNull) {
		if ( notNull == null ) {
			return TruthValue.UNKNOWN;
		}
		else {
			// not-null == nullable, so the booleans are reversed
			return notNull ? TruthValue.FALSE : TruthValue.TRUE;
		}
	}

	ColumnSourceImpl(
			MappingDocument mappingDocument,
			String tableName,
			JaxbHbmColumnType columnElement,
			TruthValue nullable) {
		super( mappingDocument );
		this.tableName = tableName;
		this.columnElement = columnElement;
		this.nullable = nullable;
	}

	@Override
	public Nature getNature() {
		return Nature.COLUMN;
	}

	@Override
	public String getName() {
		return columnElement.getName();
	}

	@Override
	public TruthValue isNullable() {
		return nullable;
	}

	@Override
	public String getDefaultValue() {
		return columnElement.getDefault();
	}

	@Override
	public String getSqlType() {
		return columnElement.getSqlType();
	}

	@Override
	public JdbcDataType getDatatype() {
		return null;
	}

	@Override
	public SizeSource getSizeSource() {
		return Helper.interpretSizeSource(
				columnElement.getLength(),
				columnElement.getScale(),
				columnElement.getPrecision()
		);
	}

	@Override
	public String getReadFragment() {
		return columnElement.getRead();
	}

	@Override
	public String getWriteFragment() {
		return columnElement.getWrite();
	}

	@Override
	public boolean isUnique() {
		// TODO: should TruthValue be returned instead of boolean?
		return columnElement.isUnique() != null && columnElement.isUnique().booleanValue();
	}

	@Override
	public String getCheckCondition() {
		return columnElement.getCheck();
	}

	@Override
	public String getComment() {
		return columnElement.getComment();
	}

	@Override
	public String getContainingTableName() {
		return tableName;
	}
}
