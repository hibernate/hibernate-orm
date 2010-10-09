/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
 */
package org.hibernate.engine;

/**
 * Becasue CascadeStyle is not opened and package protected,
 * I need to subclass and override the persist alias
 *
 * Note that This class has to be triggered by EJB3PersistEventListener at class loading time
 *
 * TODO get rid of it for 3.3
 *
 * @author Emmanuel Bernard
 */
public abstract class EJB3CascadeStyle extends CascadeStyle {

	/**
	 * cascade using EJB3CascadingAction
	 */
	public static final CascadeStyle PERSIST_EJB3 = new CascadeStyle() {
		public boolean doCascade(CascadingAction action) {
			return action==EJB3CascadingAction.PERSIST_SKIPLAZY
					|| action==CascadingAction.PERSIST_ON_FLUSH;
		}
		public String toString() {
			return "STYLE_PERSIST_SKIPLAZY";
		}
	};

	static {
		STYLES.put( "persist", PERSIST_EJB3 );
	}
}
