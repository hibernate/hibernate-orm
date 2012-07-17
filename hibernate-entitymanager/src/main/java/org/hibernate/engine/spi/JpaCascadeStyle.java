/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.spi;

/**
 * Because CascadeStyle is not opened and package protected,
 * I need to subclass and override the persist alias
 *
 * Note that This class has to be triggered by JpaPersistEventListener at class loading time
 *
 * TODO get rid of it for 3.3
 *
 * @author Emmanuel Bernard
 */
public abstract class JpaCascadeStyle extends CascadeStyle {

	/**
	 * cascade using JpaCascadingAction
	 */
	public static final CascadeStyle PERSIST_JPA = new CascadeStyle() {
		public boolean doCascade(CascadingAction action) {
			return action== JpaCascadingAction.PERSIST_SKIPLAZY
					|| action==CascadingAction.PERSIST_ON_FLUSH;
		}
		public String toString() {
			return "STYLE_PERSIST_SKIPLAZY";
		}
	};

	static {
		STYLES.put( "persist", PERSIST_JPA );
	}
}
