//$Id$
package org.hibernate.dialect;

import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.ANSIJoinFragment;


/**
 * A dialect specifically for use with Oracle 10g.
 * <p/>
 * The main difference between this dialect and {@link Oracle9iDialect}
 * is the use of "ANSI join syntax".  This dialect also retires the use
 * of the <tt>oracle.jdbc.driver</tt> package in favor of 
 * <tt>oracle.jdbc</tt>.
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

	/*
	 * The package "oracle.jdbc.driver" was retired in 9.0.1 but works fine up
	 * through 10g. So as not to mess with 9i, we're changing it in 10g -- we
	 * may not need an 11g Dialect at all.
	 */
	String getOracleTypesClassName() {
		return "oracle.jdbc.OracleTypes";
	}
}