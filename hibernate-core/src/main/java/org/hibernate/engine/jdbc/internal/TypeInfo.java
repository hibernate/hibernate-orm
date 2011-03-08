/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.engine.jdbc.internal;


/**
 * Models type info extracted from {@link java.sql.DatabaseMetaData#getTypeInfo()}
 *
 * @author Steve Ebersole
 */
public class TypeInfo {
	private final String typeName;
	private final int jdbcTypeCode;
	private final String[] createParams;
	private final boolean unsigned;
	private final int precision;
	private final short minimumScale;
	private final short maximumScale;
	private final boolean fixedPrecisionScale;
	private final String literalPrefix;
	private final String literalSuffix;
	private final boolean caseSensitive;
	private final TypeSearchability searchability;
	private final TypeNullability nullability;

	public TypeInfo(
			String typeName,
			int jdbcTypeCode,
			String[] createParams,
			boolean unsigned,
			int precision,
			short minimumScale,
			short maximumScale,
			boolean fixedPrecisionScale,
			String literalPrefix,
			String literalSuffix,
			boolean caseSensitive,
			TypeSearchability searchability,
			TypeNullability nullability) {
		this.typeName = typeName;
		this.jdbcTypeCode = jdbcTypeCode;
		this.createParams = createParams;
		this.unsigned = unsigned;
		this.precision = precision;
		this.minimumScale = minimumScale;
		this.maximumScale = maximumScale;
		this.fixedPrecisionScale = fixedPrecisionScale;
		this.literalPrefix = literalPrefix;
		this.literalSuffix = literalSuffix;
		this.caseSensitive = caseSensitive;
		this.searchability = searchability;
		this.nullability = nullability;
	}

	public String getTypeName() {
		return typeName;
	}

	public int getJdbcTypeCode() {
		return jdbcTypeCode;
	}

	public String[] getCreateParams() {
		return createParams;
	}

	public boolean isUnsigned() {
		return unsigned;
	}

	public int getPrecision() {
		return precision;
	}

	public short getMinimumScale() {
		return minimumScale;
	}

	public short getMaximumScale() {
		return maximumScale;
	}

	public boolean isFixedPrecisionScale() {
		return fixedPrecisionScale;
	}

	public String getLiteralPrefix() {
		return literalPrefix;
	}

	public String getLiteralSuffix() {
		return literalSuffix;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public TypeSearchability getSearchability() {
		return searchability;
	}

	public TypeNullability getNullability() {
		return nullability;
	}
}
