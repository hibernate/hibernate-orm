/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations;

import java.sql.Types;

import org.hibernate.TruthValue;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityTypeMetadata;
import org.hibernate.metamodel.source.spi.ColumnSource;
import org.hibernate.metamodel.source.spi.SizeSource;
import org.hibernate.metamodel.spi.relational.JdbcDataType;

/**
 * @author Steve Ebersole
 */
public class ImplicitDiscriminatorColumnSource implements ColumnSource {
	// The implicit column name per JPA spec
	private static final String IMPLICIT_COLUMN_NAME = "DTYPE";

	// The implicit column type per JPA spec
	private final JdbcDataType IMPLICIT_DATA_TYPE = new JdbcDataType(
			Types.VARCHAR,
			"varchar",
			String.class
	);

	private final String comment;

	public ImplicitDiscriminatorColumnSource(EntityTypeMetadata entityTypeMetadata) {
		this.comment = "Discriminator value for " + entityTypeMetadata.getName();
	}

	@Override
	public String getContainingTableName() {
		// null indicates primary table
		return null;
	}

	@Override
	public Nature getNature() {
		return Nature.COLUMN;
	}

	@Override
	public String getName() {
		// The default/implicit column name per JPA spec
		return IMPLICIT_COLUMN_NAME;
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
	public TruthValue isNullable() {
		// discriminators should not be nullable
		return TruthValue.FALSE;
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
		return IMPLICIT_DATA_TYPE;
	}

	@Override
	public SizeSource getSizeSource() {
		return null;
	}

	@Override
	public boolean isUnique() {
		return false;
	}

	@Override
	public String getCheckCondition() {
		return null;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public TruthValue isIncludedInInsert() {
		return null;
	}

	@Override
	public TruthValue isIncludedInUpdate() {
		return null;
	}
}
