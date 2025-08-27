/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema;

import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;

/**
 * Enumerates the actions that may be performed by the
 * {@linkplain org.hibernate.tool.schema.spi.SchemaManagementTool schema management tooling}.
 * Covers the actions defined by JPA, those defined by Hibernate's legacy HBM2DDL tool, and
 * several useful actions supported by Hibernate which are not covered by the JPA specification.
 * <ul>
 * <li>An action to be executed against the database may be specified using the configuration
 *     property {@value org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_DATABASE_ACTION}
 *     or using the property {@value org.hibernate.cfg.AvailableSettings#HBM2DDL_AUTO}.
 * <li>An action to be written to a script may be specified using the configuration property
 *     {@value org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_SCRIPTS_ACTION}.
 * </ul>
 *
 * @apiNote There is an ambiguity surrounding the value {@code "create"} here. The old-school
 *          Hibernate configuration interprets this as the action {@link #CREATE}, which drops
 *          the schema before recreating it. The JPA standard interprets it to mean the action
 *          {@link #CREATE_ONLY} which does not first drop the schema.
 *
 * @author Steve Ebersole
 */
public enum Action {
	/**
	 * No action.
	 *
	 * @apiNote Valid in JPA; identical to the HBM2DDL action of the same name.
	 */
	NONE,
	/**
	 * Create the schema.
	 *
	 * @apiNote This is an action introduced by JPA; the legacy HBM2DDL tool had
	 *          no such action. Its action named {@code "create"} was equivalent
	 *          to {@link #CREATE}.
	 *
	 * @see org.hibernate.tool.schema.spi.SchemaCreator
	 */
	CREATE_ONLY,
	/**
	 * Drop the schema.
	 *
	 * @apiNote Valid in JPA; identical to the HBM2DDL action of the same name.
	 *
	 * @see org.hibernate.tool.schema.spi.SchemaDropper
	 */
	DROP,
	/**
	 * Drop and then recreate the schema.
	 *
	 * @apiNote This action is called {@code "drop-and-create"} by JPA, but simply
	 *          {@code "create"} by the legacy HBM2DDL tool.
	 *
	 * @see org.hibernate.tool.schema.spi.SchemaDropper
	 * @see org.hibernate.tool.schema.spi.SchemaCreator
	 */
	CREATE,
	/**
	 * Drop the schema and then recreate it on {@code SessionFactory} startup.
	 * Additionally, drop the schema on {@code SessionFactory} shutdown.
	 * <p>
	 * While this is a valid option for auto schema tooling, it's not a valid
	 * action for the {@code SchemaManagementTool}; instead the caller of
	 * {@code SchemaManagementTool} must split this into two separate requests
	 * to:
	 * <ol>
	 *     <li>{@linkplain #CREATE drop and create} the schema, and then</li>
	 *     <li>later, {@linkplain #DROP drop} the schema again.</li>
	 * </ol>
	 *
	 * @apiNote This action is not defined by JPA.
	 *
	 * @see org.hibernate.tool.schema.spi.SchemaDropper
	 * @see org.hibernate.tool.schema.spi.SchemaCreator
	 */
	CREATE_DROP,
	/**
	 * Validate the database schema.
	 *
	 * @apiNote This action is not defined by JPA.
	 *
	 * @see org.hibernate.tool.schema.spi.SchemaValidator
	 */
	VALIDATE,
	/**
	 * Update (alter) the database schema.
	 *
	 * @apiNote This action is not defined by JPA.
	 *
	 * @see org.hibernate.tool.schema.spi.SchemaMigrator
	 */
	UPDATE,
	/**
	 * Truncate the tables in the schema.
	 *
	 * @apiNote This action is not defined by JPA.
	 *
	 * @see org.hibernate.tool.schema.spi.SchemaTruncator
	 *
	 * @since 6.2
	 */
	TRUNCATE,
	/**
	 * Populate an existing schema by executing {@code /import.sql} and other scripts specified
	 * via {@value org.hibernate.cfg.SchemaToolingSettings#JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE}.
	 *
	 * @apiNote This action is not defined by JPA.
	 *
	 * @see org.hibernate.tool.schema.spi.SchemaPopulator
	 *
	 * @since 7.0
	 */
	POPULATE;

	/**
	 * @see #NONE
	 */
	public static final String ACTION_NONE = "none";
	/**
	 * @see #DROP
	 */
	public static final String ACTION_DROP = "drop";
	/**
	 * @see #CREATE_ONLY
	 */
	public static final String ACTION_CREATE_ONLY = "create-only";
	/**
	 * @see #CREATE
	 */
	public static final String ACTION_CREATE = "create";
	/**
	 * @see #CREATE_DROP
	 */
	public static final String ACTION_CREATE_THEN_DROP = "create-drop";
	/**
	 * @see #VALIDATE
	 */
	public static final String ACTION_VALIDATE = "validate";
	/**
	 * @see #UPDATE
	 */
	public static final String ACTION_UPDATE = "update";
	/**
	 * @see #POPULATE
	 */
	public static final String ACTION_POPULATE = "populate";

	/**
	 * @see #NONE
	 */
	public static final String SPEC_ACTION_NONE = "none";
	/**
	 * @see #DROP
	 */
	public static final String SPEC_ACTION_DROP = "drop";
	/**
	 * @see #CREATE_ONLY
	 */
	public static final String SPEC_ACTION_CREATE = "create";
	/**
	 * @see #CREATE
	 */
	public static final String SPEC_ACTION_DROP_AND_CREATE = "drop-and-create";

	/**
	 * The string configuration value identifying this action in JPA-standard configuration
	 * via {@value org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_DATABASE_ACTION}
	 * or {@value org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_SCRIPTS_ACTION}.
	 */
	public String getExternalJpaName() {
		return switch ( this ) {
			case NONE -> SPEC_ACTION_NONE;
			case CREATE_ONLY -> SPEC_ACTION_CREATE;
			case DROP -> SPEC_ACTION_DROP;
			case CREATE -> SPEC_ACTION_DROP_AND_CREATE;
			default -> null;
		};
	}

	/**
	 * The string configuration value identifying this action in old-school Hibernate
	 * configuration via  {@value org.hibernate.cfg.AvailableSettings#HBM2DDL_AUTO}.
	 */
	public String getExternalHbm2ddlName() {
		return switch ( this ) {
			case NONE -> ACTION_NONE;
			case CREATE_ONLY -> ACTION_CREATE_ONLY;
			case DROP -> ACTION_DROP;
			case CREATE -> ACTION_CREATE;
			case CREATE_DROP -> ACTION_CREATE_THEN_DROP;
			case VALIDATE -> ACTION_VALIDATE;
			case UPDATE -> ACTION_UPDATE;
			case POPULATE -> ACTION_POPULATE;
			default -> null;
		};
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()
				+ "(externalJpaName=" + getExternalJpaName()
				+ ", externalHbm2ddlName=" + getExternalHbm2ddlName() + ")";
	}

	/**
	 * Interpret the value of the JPA-standard configuration property
	 * {@value org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_DATABASE_ACTION}
	 * or {@value org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_SCRIPTS_ACTION}
	 * as an instance of {@link Action}.
	 *
	 * @param value The encountered config value
	 *
	 * @return The matching enum value. An empty value will return {@link #NONE}.
	 *
	 * @throws IllegalArgumentException If the incoming value is unrecognized
	 */
	public static Action interpretJpaSetting(Object value) {
		if ( value == null ) {
			return NONE;
		}

		if ( value instanceof Action action ) {
			return action;
		}

		final String name = value.toString().trim();
		if ( name.isEmpty() ) {
			// default is NONE
			return NONE;
		}

		// prefer JPA external names
		for ( Action action : values() ) {
			final String jpaName = action.getExternalJpaName();
			if ( jpaName != null && jpaName.equals( name ) ) {
				return action;
			}
		}

		// then check hbm2ddl names
		for ( Action action : values() ) {
			final String hbm2ddlName = action.getExternalHbm2ddlName();
			if ( hbm2ddlName != null && hbm2ddlName.equals( name ) ) {
				return action;
			}
		}

		// lastly, look at the enum name
		for ( Action action : values() ) {
			if ( action.name().equals( name ) ) {
				return action;
			}
		}

		throw new IllegalArgumentException( "Unrecognized JPA schema management action setting: '" + value + "'" );
	}

	/**
	 * Interpret the value of the old-school Hibernate configuration property
	 * {@value org.hibernate.cfg.AvailableSettings#HBM2DDL_AUTO} as an instance
	 * of {@link Action}.
	 *
	 * @param value The encountered config value
	 *
	 * @return The matching enum value. An empty value will return {@link #NONE}.
	 *
	 * @throws IllegalArgumentException If the incoming value is unrecognized
	 */
	public static Action interpretHbm2ddlSetting(Object value) {
		if ( value == null ) {
			return NONE;
		}

		if ( value instanceof Action action ) {
			return action;
		}

		final String name = value.toString().trim();
		if ( name.isEmpty() ) {
			// default is NONE
			return NONE;
		}

		// prefer hbm2ddl names
		for ( Action action : values() ) {
			final String hbm2ddlName = action.getExternalHbm2ddlName();
			if ( hbm2ddlName != null && hbm2ddlName.equals( name ) ) {
				return action;
			}
		}

		// then check JPA external names
		for ( Action action : values() ) {
			final String jpaName = action.getExternalJpaName();
			if ( jpaName != null && jpaName.equals( name ) ) {
				return action;
			}
		}

		throw new IllegalArgumentException( "Unrecognized '" + HBM2DDL_AUTO + "' setting: '" + name + "'" );
	}

}
