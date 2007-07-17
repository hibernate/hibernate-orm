package org.hibernate.loader;

import java.util.Map;

import org.hibernate.persister.entity.Loadable;

/**
 * EntityAliases that chooses the column names over the alias names.
 * 
 * @author max
 *
 */
public class ColumnEntityAliases extends DefaultEntityAliases {

	public ColumnEntityAliases(Map returnProperties, Loadable persister, String suffix) {
		super(returnProperties, persister, suffix);
	}
	
	protected String[] getIdentifierAliases(Loadable persister, String suffix) {
		return persister.getIdentifierColumnNames();
	}
	
	protected String getDiscriminatorAlias(Loadable persister, String suffix) {
		return persister.getDiscriminatorColumnName();
	}
	
	protected String[] getPropertyAliases(Loadable persister, int j) {
		return persister.getPropertyColumnNames(j);
	}
	 

}
