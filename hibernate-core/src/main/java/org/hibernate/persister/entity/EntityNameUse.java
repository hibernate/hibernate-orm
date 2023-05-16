/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity;

import org.hibernate.Incubating;

/**
 * Describes the kind of entity name use.
 */
@Incubating
public final class EntityNameUse {

	public static final EntityNameUse PROJECTION = new EntityNameUse( UseKind.PROJECTION, true );
	public static final EntityNameUse EXPRESSION = new EntityNameUse( UseKind.EXPRESSION, true );
	public static final EntityNameUse TREAT = new EntityNameUse( UseKind.TREAT, true );
	public static final EntityNameUse OPTIONAL_TREAT = new EntityNameUse( UseKind.TREAT, false );
	public static final EntityNameUse FILTER = new EntityNameUse( UseKind.FILTER, true );

	private final UseKind kind;
	private final boolean requiresRestriction;

	private EntityNameUse(UseKind kind, boolean requiresRestriction) {
		this.kind = kind;
		this.requiresRestriction = requiresRestriction;
	}

	private static EntityNameUse get(UseKind kind) {
		switch ( kind ) {
			case PROJECTION:
				return PROJECTION;
			case EXPRESSION:
				return EXPRESSION;
			case TREAT:
				return TREAT;
			case FILTER:
				return FILTER;
		}
		throw new IllegalArgumentException( "Unknown kind: " + kind );
	}

	public UseKind getKind() {
		return kind;
	}

	public boolean requiresRestriction() {
		return requiresRestriction;
	}

	public EntityNameUse stronger(EntityNameUse other) {
		return other == null || kind.isStrongerThan( other.kind ) ? this : get( other.kind );
	}

	public EntityNameUse weaker(EntityNameUse other) {
		return other == null || kind.isWeakerThan( other.kind ) ? this : get( other.kind );
	}

	public enum UseKind {
		/**
		 * An entity type is used through a path that appears in the {@code SELECT} clause somehow.
		 * This use kind is registered for top level select items or join fetches.
		 */
		PROJECTION,
		/**
		 * An entity type is used through a path expression, but doesn't match the criteria for {@link #PROJECTION}.
		 */
		EXPRESSION,
		/**
		 * An entity type is used through a treat expression.
		 */
		TREAT,
		/**
		 * An entity type is filtered for through a type restriction predicate i.e. {@code type(alias) = Subtype}.
		 */
		FILTER;

		public boolean isStrongerThan(UseKind other) {
			return ordinal() > other.ordinal();
		}

		public UseKind stronger(UseKind other) {
			return other == null || isStrongerThan( other ) ? this : other;
		}

		public boolean isWeakerThan(UseKind other) {
			return ordinal() < other.ordinal();
		}

		public UseKind weaker(UseKind other) {
			return other == null || isWeakerThan( other ) ? this : other;
		}
	}
}
