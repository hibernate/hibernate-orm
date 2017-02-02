/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;


import java.util.Set;

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
	private final Set<String> indexConstraintNames;
	private final Set<String> ukConstraintNames;

	ColumnSourceImpl(
			MappingDocument mappingDocument,
			String tableName,
			JaxbHbmColumnType columnElement,
			Set<String> indexConstraintNames,
			Set<String> ukConstraintNames) {
		this(
				mappingDocument,
				tableName,
				columnElement,
				interpretNotNullToNullability( columnElement.isNotNull() ),
				indexConstraintNames,
				ukConstraintNames
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
			TruthValue nullable,
			Set<String> indexConstraintNames,
			Set<String> ukConstraintNames) {
		super( mappingDocument );
		this.tableName = tableName;
		this.columnElement = columnElement;
		this.nullable = nullable;

		this.indexConstraintNames = CommaSeparatedStringHelper.splitAndCombine(
				indexConstraintNames,
				columnElement.getIndex()
		);
		this.ukConstraintNames = CommaSeparatedStringHelper.splitAndCombine(
				ukConstraintNames,
				columnElement.getUniqueKey()
		);
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

	@Override
	public Set<String> getIndexConstraintNames() {
		return indexConstraintNames;
	}

	@Override
	public Set<String> getUniqueKeyConstraintNames() {
		return ukConstraintNames;
	}
}
