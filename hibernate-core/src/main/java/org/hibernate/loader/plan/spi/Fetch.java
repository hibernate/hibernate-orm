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
package org.hibernate.loader.plan.spi;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.spi.ResultSetProcessingContext;

/**
 * Contract for associations that are being fetched.
 * <p/>
 * NOTE : can represent components/embeddables
 *
 * @author Steve Ebersole
 */
public interface Fetch {
	/**
	 * Obtain the owner of this fetch.
	 *
	 * @return The fetch owner.
	 */
	public FetchOwner getOwner();

	/**
	 * Obtain the name of the property, relative to the owner, being fetched.
	 *
	 * @return The fetched property name.
	 */
	public String getOwnerPropertyName();

	public FetchStrategy getFetchStrategy();

	/**
	 * Get the property path to this fetch
	 *
	 * @return The property path
	 */
	public PropertyPath getPropertyPath();

	public void hydrate(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException;

	public Object resolve(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException;
}
