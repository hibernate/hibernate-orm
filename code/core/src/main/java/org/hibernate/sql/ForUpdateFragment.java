//$Id: ForUpdateFragment.java 11320 2007-03-20 11:50:53Z steve.ebersole@jboss.com $
package org.hibernate.sql;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.QueryException;
import org.hibernate.dialect.Dialect;
import org.hibernate.util.StringHelper;

/**
 * @author Gavin King
 */
public class ForUpdateFragment {
	private final StringBuffer aliases = new StringBuffer();
	private boolean isNowaitEnabled;
	private final Dialect dialect;

	public ForUpdateFragment(Dialect dialect) {
		this.dialect = dialect;
	}

	public ForUpdateFragment(Dialect dialect, Map lockModes, Map keyColumnNames) throws QueryException {
		this( dialect );
		LockMode upgradeType = null;
		Iterator iter = lockModes.entrySet().iterator();
		while ( iter.hasNext() ) {
			final Map.Entry me = ( Map.Entry ) iter.next();
			final LockMode lockMode = ( LockMode ) me.getValue();
			if ( LockMode.READ.lessThan( lockMode ) ) {
				final String tableAlias = ( String ) me.getKey();
				if ( dialect.forUpdateOfColumns() ) {
					String[] keyColumns = ( String[] ) keyColumnNames.get( tableAlias ); //use the id column alias
					if ( keyColumns == null ) {
						throw new IllegalArgumentException( "alias not found: " + tableAlias );
					}
					keyColumns = StringHelper.qualify( tableAlias, keyColumns );
					for ( int i = 0; i < keyColumns.length; i++ ) {
						addTableAlias( keyColumns[i] );
					}
				}
				else {
					addTableAlias( tableAlias );
				}
				if ( upgradeType != null && lockMode != upgradeType ) {
					throw new QueryException( "mixed LockModes" );
				}
				upgradeType = lockMode;
			}
		}

		if ( upgradeType == LockMode.UPGRADE_NOWAIT ) {
			setNowaitEnabled( true );
		}
	}

	public ForUpdateFragment addTableAlias(String alias) {
		if ( aliases.length() > 0 ) {
			aliases.append( ", " );
		}
		aliases.append( alias );
		return this;
	}

	public String toFragmentString() {
		if ( aliases.length() == 0 ) {
			return "";
		}
		return isNowaitEnabled ?
				dialect.getForUpdateNowaitString( aliases.toString() ) :
				dialect.getForUpdateString( aliases.toString() );
	}

	public ForUpdateFragment setNowaitEnabled(boolean nowait) {
		isNowaitEnabled = nowait;
		return this;
	}
}
