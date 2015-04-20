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
package org.hibernate.resource.jdbc.spi;

/**
 * Contract to allow inspection (and swapping) of SQL to be prepared
 *
 * @author Steve Ebersole
 */
public interface StatementInspector {
	/**
	 * Inspect the given SQL, possibly returning a different SQL to be used instead.  Note that returning {@code null}
	 * is interpreted as returning the same SQL as was passed.
	 *
	 * @param sql The SQL to inspect
	 *
	 * @return The SQL to use; may be {@code null}
	 */
	public String inspect(String sql);
}
