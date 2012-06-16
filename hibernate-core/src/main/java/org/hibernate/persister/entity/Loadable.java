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
package org.hibernate.persister.entity;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.Type;

/**
 * Implemented by a <tt>EntityPersister</tt> that may be loaded
 * using <tt>Loader</tt>.
 *
 * @see org.hibernate.loader.Loader
 * @author Gavin King
 */
public interface Loadable extends EntityPersister {
	
	public static final String ROWID_ALIAS = "rowid_";

	/**
	 * Does this persistent class have subclasses?
	 */
	public boolean hasSubclasses();

	/**
	 * Get the discriminator type
	 */
	public Type getDiscriminatorType();

	/**
	 * Get the discriminator value
	 */
	public Object getDiscriminatorValue();

	/**
	 * Get the concrete subclass corresponding to the given discriminator
	 * value
	 */
	public String getSubclassForDiscriminatorValue(Object value);

	/**
	 * Get the names of columns used to persist the identifier
	 */
	public String[] getIdentifierColumnNames();

	/**
	 * Get the result set aliases used for the identifier columns, given a suffix
	 */
	public String[] getIdentifierAliases(String suffix);
	/**
	 * Get the result set aliases used for the property columns, given a suffix (properties of this class, only).
	 */
	public String[] getPropertyAliases(String suffix, int i);
	
	/**
	 * Get the result set column names mapped for this property (properties of this class, only).
	 */
	public String[] getPropertyColumnNames(int i);
	
	/**
	 * Get the result set aliases used for the identifier columns, given a suffix
	 */
	public String getDiscriminatorAlias(String suffix);
	
	/**
	 * @return the column name for the discriminator as specified in the mapping.
	 */
	public String getDiscriminatorColumnName();
	
	/**
	 * Does the result set contain rowids?
	 */
	public boolean hasRowId();
	
	/**
	 * Retrieve property values from one row of a result set
	 */
	public Object[] hydrate(
			ResultSet rs,
			Serializable id,
			Object object,
			Loadable rootLoadable,
			String[][] suffixedPropertyColumns,
			boolean allProperties, 
			SessionImplementor session)
	throws SQLException, HibernateException;

	public boolean isAbstract();

	/**
	 * Register the name of a fetch profile determined to have an affect on the
	 * underlying loadable in regards to the fact that the underlying load SQL
	 * needs to be adjust when the given fetch profile is enabled.
	 * 
	 * @param fetchProfileName The name of the profile affecting this.
	 */
	public void registerAffectingFetchProfile(String fetchProfileName);

	/**
	 * Given a column name and the root table alias in use for the entity hierarchy, determine the proper table alias
	 * for the table in that hierarchy that contains said column.
	 * <p/>
	 * NOTE : Generally speaking the column is not validated to exist.  Most implementations simply return the
	 * root alias; the exception is {@link JoinedSubclassEntityPersister}
	 *
	 * @param columnName The column name
	 * @param rootAlias The hierarchy root alias
	 *
	 * @return The proper table alias for qualifying the given column.
	 */
	public String getTableAliasForColumn(String columnName, String rootAlias);
}
