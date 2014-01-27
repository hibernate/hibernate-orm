/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.binding;

import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.TableSpecification;

/**
 * @author Gail Badner
 */
public class JoinRelationalValueBindingContainer extends RelationalValueBindingContainer {
	private final TableSpecification table;
	private final ForeignKey foreignKey;

	public JoinRelationalValueBindingContainer(
			List<RelationalValueBinding> relationalValueBindings,
			ForeignKey foreignKey) {
		super( relationalValueBindings );
		if ( relationalValueBindings.isEmpty() ) {
			table = null;
		}
		else {
			table = relationalValueBindings.get( 0 ).getTable();
			for ( int i = 1; i< relationalValueBindings.size(); i++ ) {
				if ( !table.equals( relationalValueBindings.get( i ).getTable() ) ) {
					throw new AssertionFailure(
							String.format(
									"Multiple tables found in a %s: %s, %s",
									getClass().getName(),
									table.getLogicalName(),
									relationalValueBindings.get( i ).getTable()
							)
					);
				}
			}
		}
		if ( table != null && foreignKey != null && !table.equals( foreignKey.getSourceTable() ) ) {
			throw new IllegalStateException(
					String.format(
							"Unexpected source table for foreign key: %s; expected %s.",
							foreignKey.getSourceTable(),
							table
					)
			);
		}
		this.foreignKey = foreignKey;
	}

	public TableSpecification getTable() {
		return table;
	}

	public ForeignKey getForeignKey() {
		return foreignKey;
	}
}
