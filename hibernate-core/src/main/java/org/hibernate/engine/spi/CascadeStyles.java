/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import java.util.HashMap;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.internal.util.collections.ArrayHelper;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class CascadeStyles {
	private static final Logger log = Logger.getLogger( CascadeStyles.class );

	/**
	 * Disallow instantiation
	 */
	private CascadeStyles() {
	}

	/**
	 * save / delete / update / evict / lock / replicate / merge / persist + delete orphans
	 */
	public static final CascadeStyle ALL_DELETE_ORPHAN = new BaseCascadeStyle() {
		@Override
		public boolean doCascade(CascadingAction action) {
			return true;
		}

		@Override
		public boolean hasOrphanDelete() {
			return true;
		}

		@Override
		public String toString() {
			return "STYLE_ALL_DELETE_ORPHAN";
		}
	};

	/**
	 * save / delete / update / evict / lock / replicate / merge / persist
	 */
	public static final CascadeStyle ALL = new BaseCascadeStyle() {
		@Override
		public boolean doCascade(CascadingAction action) {
			return true;
		}

		@Override
		public String toString() {
			return "STYLE_ALL";
		}
	};

	/**
	 * save / update
	 */
	public static final CascadeStyle UPDATE = new BaseCascadeStyle() {
		@Override
		public boolean doCascade(CascadingAction action) {
			return action == CascadingActions.SAVE_UPDATE;
		}

		@Override
		public String toString() {
			return "STYLE_SAVE_UPDATE";
		}
	};

	/**
	 * lock
	 */
	public static final CascadeStyle LOCK = new BaseCascadeStyle() {
		@Override
		public boolean doCascade(CascadingAction action) {
			return action == CascadingActions.LOCK;
		}

		@Override
		public String toString() {
			return "STYLE_LOCK";
		}
	};

	/**
	 * refresh
	 */
	public static final CascadeStyle REFRESH = new BaseCascadeStyle() {
		@Override
		public boolean doCascade(CascadingAction action) {
			return action == CascadingActions.REFRESH;
		}

		@Override
		public String toString() {
			return "STYLE_REFRESH";
		}
	};

	/**
	 * evict
	 */
	public static final CascadeStyle EVICT = new BaseCascadeStyle() {
		@Override
		public boolean doCascade(CascadingAction action) {
			return action == CascadingActions.EVICT;
		}

		@Override
		public String toString() {
			return "STYLE_EVICT";
		}
	};

	/**
	 * replicate
	 */
	public static final CascadeStyle REPLICATE = new BaseCascadeStyle() {
		@Override
		public boolean doCascade(CascadingAction action) {
			return action == CascadingActions.REPLICATE;
		}

		@Override
		public String toString() {
			return "STYLE_REPLICATE";
		}
	};

	/**
	 * merge
	 */
	public static final CascadeStyle MERGE = new BaseCascadeStyle() {
		@Override
		public boolean doCascade(CascadingAction action) {
			return action == CascadingActions.MERGE;
		}

		@Override
		public String toString() {
			return "STYLE_MERGE";
		}
	};

	/**
	 * create
	 */
	public static final CascadeStyle PERSIST = new BaseCascadeStyle() {
		@Override
		public boolean doCascade(CascadingAction action) {
			return action == CascadingActions.PERSIST
					|| action == CascadingActions.PERSIST_ON_FLUSH;
		}

		@Override
		public String toString() {
			return "STYLE_PERSIST";
		}
	};

	/**
	 * delete
	 */
	public static final CascadeStyle DELETE = new BaseCascadeStyle() {
		@Override
		public boolean doCascade(CascadingAction action) {
			return action == CascadingActions.DELETE;
		}

		@Override
		public String toString() {
			return "STYLE_DELETE";
		}
	};

	/**
	 * delete + delete orphans
	 */
	public static final CascadeStyle DELETE_ORPHAN = new BaseCascadeStyle() {
		@Override
		public boolean doCascade(CascadingAction action) {
			return action == CascadingActions.DELETE || action == CascadingActions.SAVE_UPDATE;
		}

		@Override
		public boolean reallyDoCascade(CascadingAction action) {
			return action == CascadingActions.DELETE;
		}

		@Override
		public boolean hasOrphanDelete() {
			return true;
		}

		@Override
		public String toString() {
			return "STYLE_DELETE_ORPHAN";
		}
	};

	/**
	 * no cascades
	 */
	public static final CascadeStyle NONE = new BaseCascadeStyle() {
		@Override
		public boolean doCascade(CascadingAction action) {
			return false;
		}

		@Override
		public String toString() {
			return "STYLE_NONE";
		}
	};

	private static final Map<String, CascadeStyle> STYLES = buildBaseCascadeStyleMap();

	private static Map<String, CascadeStyle> buildBaseCascadeStyleMap() {
		final HashMap<String, CascadeStyle> base = new HashMap<String, CascadeStyle>();

		base.put( "all", ALL );
		base.put( "all-delete-orphan", ALL_DELETE_ORPHAN );
		base.put( "save-update", UPDATE );
		base.put( "persist", PERSIST );
		base.put( "merge", MERGE );
		base.put( "lock", LOCK );
		base.put( "refresh", REFRESH );
		base.put( "replicate", REPLICATE );
		base.put( "evict", EVICT );
		base.put( "delete", DELETE );
		base.put( "remove", DELETE ); // adds remove as a sort-of alias for delete...
		base.put( "delete-orphan", DELETE_ORPHAN );
		base.put( "none", NONE );

		return base;
	}

	/**
	 * Factory method for obtaining named cascade styles
	 *
	 * @param cascade The named cascade style name.
	 *
	 * @return The appropriate CascadeStyle
	 */
	public static CascadeStyle getCascadeStyle(String cascade) {
		CascadeStyle style = STYLES.get( cascade );
		if ( style == null ) {
			throw new MappingException( "Unsupported cascade style: " + cascade );
		}
		else {
			return style;
		}
	}

	public static void registerCascadeStyle(String name, BaseCascadeStyle cascadeStyle) {
		log.tracef( "Registering external cascade style [%s : %s]", name, cascadeStyle );
		final CascadeStyle old = STYLES.put( name, cascadeStyle );
		if ( old != null ) {
			log.debugf(
					"External cascade style regsitration [%s : %s] overrode base registration [%s]",
					name,
					cascadeStyle,
					old
			);
		}
	}

	public static abstract class BaseCascadeStyle implements CascadeStyle {
		@Override
		public boolean reallyDoCascade(CascadingAction action) {
			return doCascade( action );
		}

		@Override
		public boolean hasOrphanDelete() {
			return false;
		}
	}

	public static final class MultipleCascadeStyle extends BaseCascadeStyle {
		private final CascadeStyle[] styles;

		public MultipleCascadeStyle(CascadeStyle[] styles) {
			this.styles = styles;
		}

		@Override
		public boolean doCascade(CascadingAction action) {
			for ( CascadeStyle style : styles ) {
				if ( style.doCascade( action ) ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean reallyDoCascade(CascadingAction action) {
			for ( CascadeStyle style : styles ) {
				if ( style.reallyDoCascade( action ) ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean hasOrphanDelete() {
			for ( CascadeStyle style : styles ) {
				if ( style.hasOrphanDelete() ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return ArrayHelper.toString( styles );
		}
	}
}
