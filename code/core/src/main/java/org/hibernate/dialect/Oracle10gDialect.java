//$Id: Oracle9Dialect.java 11286 2007-03-15 10:33:16Z steve.ebersole@jboss.com $
package org.hibernate.dialect;

import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.ANSIJoinFragment;


/**
 * A dialect specifically for use with Oracle 10g.
 * <p/>
 * The main difference between this dialect and {@link Oracle9iDialect}
 * is the use of "ANSI join syntax" here...
 *
 * @author Steve Ebersole
 */
public class Oracle10gDialect extends Oracle9iDialect {

	public Oracle10gDialect() {
		super();
	}

	public JoinFragment createOuterJoinFragment() {
		return new ANSIJoinFragment();
	}

}
