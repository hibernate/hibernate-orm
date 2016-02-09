/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema;

import org.hibernate.internal.util.StringHelper;

/**
 * The allowable actions in terms of schema tooling.  Covers the unified JPA and HBM2DDL
 * cases.
 *
 * @author Steve Ebersole
 */
public enum Action {
	/**
	 * No action will be performed.  Valid in JPA; compatible with Hibernate's
	 * hbm2ddl action of the same name..
	 */
	NONE( "none" ),
	/**
	 * Database creation will be generated.  This is an action introduced by JPA.  Hibernate's
	 * legacy hbm2ddl had no such action - its "create" action is actually equivalent to {@link #CREATE}
	 * <p/>
	 * Corresponds to a call to {@link org.hibernate.tool.schema.spi.SchemaCreator}
	 */
	CREATE_ONLY( "create", "create-only" ),
	/**
	 * Database dropping will be generated.
	 * <p/>
	 * Corresponds to a call to {@link org.hibernate.tool.schema.spi.SchemaDropper}
	 */
	DROP( "drop" ),
	/**
	 * Database dropping will be generated followed by database creation.
	 * <p/>
	 * Corresponds to a call to {@link org.hibernate.tool.schema.spi.SchemaDropper}
	 * followed immediately by a call to {@link org.hibernate.tool.schema.spi.SchemaCreator}
	 */
	CREATE( "drop-and-create", "create" ),
	/**
	 * Drop the schema and recreate it on SessionFactory startup.  Additionally, drop the
	 * schema on SessionFactory shutdown.
	 * <p/>
	 * Has no corresponding call to a SchemaManagementTool delegate.  It is equivalent to a
	 *
	 * <p/>
	 * While this is a valid option for auto schema tooling, it is not a valid action to pass to
	 * SchemaManagementTool; instead it would be expected that the caller to SchemaManagementTool
	 * would split this into 2 separate requests for:<ol>
	 *     <li>{@link #CREATE}</li>
	 *     <li>{@link #DROP}</li>
	 * </ol>
	 */
	CREATE_DROP( null, "create-drop" ),
	/**
	 * "validate" (Hibernate only) - validate the database schema
	 */
	VALIDATE( null, "validate" ),
	/**
	 * "update" (Hibernate only) - update (alter) the database schema
	 */
	UPDATE( null, "update" );

	private final String externalJpaName;
	private final String externalHbm2ddlName;

	Action(String externalJpaName) {
		this( externalJpaName, externalJpaName );
	}

	Action(String externalJpaName, String externalHbm2ddlName) {
		this.externalJpaName = externalJpaName;
		this.externalHbm2ddlName = externalHbm2ddlName;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(externalJpaName=" + externalJpaName + ", externalHbm2ddlName=" + externalHbm2ddlName + ")";
	}

	/**
	 * Used when processing JPA configuration to interpret the user config values.  Generally
	 * this will be a value specified by {@link org.hibernate.cfg.AvailableSettings#HBM2DDL_DATABASE_ACTION}
	 * or {@link org.hibernate.cfg.AvailableSettings#HBM2DDL_SCRIPTS_ACTION}
	 *
	 * @param value The encountered config value
	 *
	 * @return The matching enum value.  An empty value will return {@link #NONE}.
	 *
	 * @throws IllegalArgumentException If the incoming value is unrecognized
	 */
	public static Action interpretJpaSetting(Object value) {
		if ( value == null ) {
			return NONE;
		}

		if ( Action.class.isInstance( value ) ) {
			return (Action) value;
		}

		final String name = value.toString();
		if ( StringHelper.isEmpty( name ) || NONE.externalJpaName.equals( name ) ) {
			// default is NONE
			return NONE;
		}

		// prefer JPA external names
		for ( Action action : values() ) {
			if ( action.externalJpaName == null ) {
				continue;
			}

			if ( action.externalJpaName.equals( name ) ) {
				return action;
			}
		}

		// then check hbm2ddl names
		for ( Action action : values() ) {
			if ( action.externalHbm2ddlName == null ) {
				continue;
			}

			if ( action.externalHbm2ddlName.equals( name ) ) {
				return action;
			}
		}

		throw new IllegalArgumentException( "Unrecognized JPA schema generation action value : " + value );
	}

	/**
	 * Used to interpret the value of {@link org.hibernate.cfg.AvailableSettings#HBM2DDL_AUTO}
	 *
	 * @param value The encountered config value
	 *
	 * @return The matching enum value.  An empty value will return {@link #NONE}.
	 *
	 * @throws IllegalArgumentException If the incoming value is unrecognized
	 */
	public static Action interpretHbm2ddlSetting(Object value) {
		if ( value == null ) {
			return NONE;
		}

		if ( Action.class.isInstance( value ) ) {
			return hbm2ddlSetting( (Action) value );
		}

		final String name = value.toString();
		if ( StringHelper.isEmpty( name ) || NONE.externalJpaName.equals( name ) ) {
			// default is NONE
			return NONE;
		}

		// prefer hbm2ddl names
		for ( Action action : values() ) {
			if ( action.externalHbm2ddlName == null ) {
				continue;
			}

			if ( action.externalHbm2ddlName.equals( name ) ) {
				return hbm2ddlSetting( action );
			}
		}

		// then check JPA external names
		for ( Action action : values() ) {
			if ( action.externalJpaName == null ) {
				continue;
			}

			if ( action.externalJpaName.equals( name ) ) {
				return hbm2ddlSetting( action );
			}
		}

		throw new IllegalArgumentException( "Unrecognized legacy `hibernate.hbm2ddl.auto` value : " + value );
	}

	private static Action hbm2ddlSetting(Action action) {
		return action;
	}

}
