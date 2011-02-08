/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.dialect;
import org.hibernate.sql.ANSIJoinFragment;
import org.hibernate.sql.JoinFragment;


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
}
