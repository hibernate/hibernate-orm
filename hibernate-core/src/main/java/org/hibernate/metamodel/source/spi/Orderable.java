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

/**
 * Contact to define if a plural attribute source is orderable or not.
 *
 * @author Steve Ebersole
 */
public interface Orderable {
	/**
	 * If the source of plural attribute is supposed to be applied the <b>order by</b> when loading.
	 *
	 * @return <code>true</code> for applying the <b>order by</b> or <code>false</code> means not.
	 */
	boolean isOrdered();

	/**
	 * The order by clause used during loading this plural attribute.
	 *
	 * <p/>
	 * If the ordering element is not specified, ordering by
	 * the primary key of the associated entity is assumed
	 *
	 * {@see javax.persistence.OrderBy#value()}
	 *
	 * @return  The <b>order by</b> clause used during loading this plural attribute from DB.
	 */
	String getOrder();
}
