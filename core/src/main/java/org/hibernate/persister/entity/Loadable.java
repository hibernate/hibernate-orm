//$Id: Loadable.java 7458 2005-07-12 20:12:57Z oneovthafew $
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
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

}
