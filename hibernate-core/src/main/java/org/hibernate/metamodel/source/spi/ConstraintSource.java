/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.spi;

import java.util.List;


/**
 * Contract describing source of table constraints
 *
 * @author Hardy Ferentschik
 */
public interface ConstraintSource {
	/**
	 * @return returns the name of the constraint or {@code null} in case a generated name should be used
	 */
	public String name();

	/**
	 * Obtain the logical name of the table for this constraint.
	 *
	 * @return The logical table name. Can be {@code null} in the case of the "primary table".
	 */
	public String getTableName();
	
	public List<String> columnNames();
	
	public List<String> orderings();
}
