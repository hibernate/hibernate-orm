/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
public final class CascadeStyles {

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
		public boolean doCascade(CascadingAction<?> action) {
			return action != CascadingActions.CHECK_ON_FLUSH;
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
		public boolean doCascade(CascadingAction<?> action) {
			return action != CascadingActions.CHECK_ON_FLUSH;
		}

		@Override
		public String toString() {
			return "STYLE_ALL";
		}
	};

	/**
	 * lock
	 */
	public static final CascadeStyle LOCK = new BaseCascadeStyle() {
		@Override
		public boolean doCascade(CascadingAction<?> action) {
			return action == CascadingActions.LOCK
				|| action == CascadingActions.CHECK_ON_FLUSH;
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
		public boolean doCascade(CascadingAction<?> action) {
			return action == CascadingActions.REFRESH
				|| action == CascadingActions.CHECK_ON_FLUSH;
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
		public boolean doCascade(CascadingAction<?> action) {
			return action == CascadingActions.EVICT
				|| action == CascadingActions.CHECK_ON_FLUSH;
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
		public boolean doCascade(CascadingAction<?> action) {
			return action == CascadingActions.REPLICATE
				|| action == CascadingActions.CHECK_ON_FLUSH;
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
		public boolean doCascade(CascadingAction<?> action) {
			return action == CascadingActions.MERGE
				|| action == CascadingActions.CHECK_ON_FLUSH;
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
		public boolean doCascade(CascadingAction<?> action) {
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
		public boolean doCascade(CascadingAction<?> action) {
			return action == CascadingActions.REMOVE
				|| action == CascadingActions.CHECK_ON_FLUSH;
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
		public boolean doCascade(CascadingAction<?> action) {
			return action == CascadingActions.REMOVE
				|| action == CascadingActions.PERSIST_ON_FLUSH
				|| action == CascadingActions.CHECK_ON_FLUSH;
		}

		@Override
		public boolean reallyDoCascade(CascadingAction<?> action) {
			return action == CascadingActions.REMOVE
				|| action == CascadingActions.CHECK_ON_FLUSH;
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
		public boolean doCascade(CascadingAction<?> action) {
			return action == CascadingActions.CHECK_ON_FLUSH;
		}

		@Override
		public String toString() {
			return "STYLE_NONE";
		}
	};

	private static final Map<String, CascadeStyle> STYLES = buildBaseCascadeStyleMap();

	private static Map<String, CascadeStyle> buildBaseCascadeStyleMap() {
		final HashMap<String, CascadeStyle> base = new HashMap<>();

		base.put( "all", ALL );
		base.put( "all-delete-orphan", ALL_DELETE_ORPHAN );
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
					"External cascade style registration [%s : %s] overrode base registration [%s]",
					name,
					cascadeStyle,
					old
			);
		}
	}

	public static abstract class BaseCascadeStyle implements CascadeStyle {
		@Override
		public boolean reallyDoCascade(CascadingAction<?> action) {
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
		public boolean doCascade(CascadingAction<?> action) {
			if ( action == CascadingActions.CHECK_ON_FLUSH ) {
				return !reallyDoCascade( CascadingActions.PERSIST_ON_FLUSH );
			}
			for ( CascadeStyle style : styles ) {
				if ( style.doCascade( action ) ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean reallyDoCascade(CascadingAction<?> action) {
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
