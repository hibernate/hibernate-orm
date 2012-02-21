/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations.entity;

import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.spi.source.PrimaryKeyJoinColumnSource;
import org.hibernate.metamodel.spi.source.SecondaryTableSource;
import org.hibernate.metamodel.spi.source.TableSpecificationSource;

/**
 * @author Steve Ebersole
 */
public class SecondaryTableSourceImpl implements SecondaryTableSource {
	private final TableSpecificationSource joinTable;
	private final List<PrimaryKeyJoinColumnSource> joinColumns;

	public SecondaryTableSourceImpl(
			TableSpecificationSource joinTable,
			List<PrimaryKeyJoinColumnSource> joinColumns) {
		this.joinTable = joinTable;
		this.joinColumns = Collections.unmodifiableList( joinColumns );
	}

	@Override
	public List<PrimaryKeyJoinColumnSource> getJoinColumns() {
		return joinColumns;
	}

	@Override
	public TableSpecificationSource getTableSource() {
		return joinTable;
	}

	@Override
	public String getForeignKeyName() {
		// not supported from annotations, unless docs for @ForeignKey are wrong...
		return null;
	}
}
