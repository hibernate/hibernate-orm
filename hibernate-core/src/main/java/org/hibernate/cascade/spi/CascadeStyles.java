/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.spi;

import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.internal.util.collections.ArrayHelper;

/**
 * @author Steve Ebersole
 */
@Internal
public final class CascadeStyles {

	/**
	 * Disallow instantiation
	 */
	private CascadeStyles() {
	}

	/**
	 * save / delete / update / evict / lock / merge / persist + delete orphans
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
	 * save / delete / update / evict / merge / persist
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

	private static final Map<String, CascadeStyle> STYLES = Map.of(
			"all", ALL,
			"all-delete-orphan", ALL_DELETE_ORPHAN,
			"persist", PERSIST,
			"merge", MERGE,
			"refresh", REFRESH,
			"evict", EVICT,
			"delete", DELETE,
			"remove", DELETE,
			"delete-orphan", DELETE_ORPHAN,
			"none", NONE
	);

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

	abstract static class BaseCascadeStyle implements CascadeStyle {
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
