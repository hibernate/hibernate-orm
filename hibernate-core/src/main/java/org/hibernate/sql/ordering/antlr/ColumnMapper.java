/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.sql.ordering.antlr;

import org.hibernate.HibernateException;

/**
 * Contract for mapping a (an assumed) property reference to its columns.
 *
 * @author Steve Ebersole
 */
public interface ColumnMapper {
	/**
	 * Resolve the property reference to its underlying columns.
	 *
	 * @param reference The property reference name.
	 *
	 * @return The underlying columns, or null if the property reference is unknown.
	 *
	 * @throws HibernateException Generally indicates that the property reference is unkown; interpretation is the
	 * same as a null return.
	 */
	public String[] map(String reference) throws HibernateException;
}
