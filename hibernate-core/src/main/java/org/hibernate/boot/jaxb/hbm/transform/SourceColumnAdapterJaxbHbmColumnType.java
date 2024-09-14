/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.hbm.transform;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;

/**
 * @author Steve Ebersole
 */
public class SourceColumnAdapterJaxbHbmColumnType implements SourceColumnAdapter {
	private final JaxbHbmColumnType hbmColumn;

	public SourceColumnAdapterJaxbHbmColumnType(JaxbHbmColumnType hbmColumn) {
		this.hbmColumn = hbmColumn;
	}

	@Override
	public String getName() {
		return hbmColumn.getName();
	}

	@Override
	public Boolean isNotNull() {
		return hbmColumn.isNotNull();
	}

	@Override
	public Boolean isUnique() {
		return hbmColumn.isUnique();
	}

	@Override
	public Integer getLength() {
		return hbmColumn.getLength();
	}

	@Override
	public Integer getPrecision() {
		return hbmColumn.getPrecision();
	}

	@Override
	public Integer getScale() {
		return hbmColumn.getScale();
	}

	@Override
	public String getSqlType() {
		return hbmColumn.getSqlType();
	}

	@Override
	public String getComment() {
		return hbmColumn.getComment();
	}

	@Override
	public String getCheck() {
		return hbmColumn.getCheck();
	}

	@Override
	public String getDefault() {
		return hbmColumn.getDefault();
	}

	@Override
	public String getIndex() {
		return hbmColumn.getIndex();
	}

	@Override
	public String getUniqueKey() {
		return hbmColumn.getUniqueKey();
	}

	@Override
	public String getRead() {
		return hbmColumn.getRead();
	}

	@Override
	public String getWrite() {
		return hbmColumn.getWrite();
	}
}
