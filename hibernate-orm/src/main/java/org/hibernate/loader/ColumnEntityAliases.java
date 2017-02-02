/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader;
import java.util.Map;

import org.hibernate.persister.entity.Loadable;

/**
 * EntityAliases that chooses the column names over the alias names.  This strategy is used
 * when the result-set mapping did not give specific aliases to use in extracting from the
 * result set.  We use the column names from the underlying persister.
 * 
 * @author max
 * @author Steve Ebersole
 */
public class ColumnEntityAliases extends DefaultEntityAliases {

	public ColumnEntityAliases(
			Map returnProperties,
			Loadable persister, 
			String suffix) {
		super( returnProperties, persister, suffix );
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
