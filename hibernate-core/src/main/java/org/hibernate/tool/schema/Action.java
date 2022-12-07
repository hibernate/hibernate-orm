/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema;

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
 * @author Steve Ebersole
 */
public enum Action {
	/**
	 * No action.
	 * <p>
	 * Valid in JPA; compatible with Hibernate's HBM2DDL action of the same name.
	 */
	NONE( "none" ),
	/**
	 * Create the schema.
	 * <p>
	 * This is an action introduced by JPA; Hibernate's legacy HBM2DDL had no such
	 * action. Its "create" action was actually equivalent to {@link #CREATE}.
	 *
	 * @see org.hibernate.tool.schema.spi.SchemaCreator
	 */
	CREATE_ONLY( "create", "create-only" ),
	/**
	 * Drop the schema.
	 * <p>
	 * Valid in JPA; compatible with Hibernate's HBM2DDL action of the same name.
	 *
	 * @see org.hibernate.tool.schema.spi.SchemaDropper
	 */
	DROP( "drop" ),
	/**
	 * Drop and then recreate the schema.
	 *
	 * @see org.hibernate.tool.schema.spi.SchemaDropper
	 * @see org.hibernate.tool.schema.spi.SchemaCreator
	 */
	CREATE( "drop-and-create", "create" ),
	/**
	 * Drop the schema and then recreate it on {@code SessionFactory} startup.
	 * Additionally, drop the schema on {@code SessionFactory} shutdown.
	 * <p>
	 * This action is not defined by JPA.
	 * <p>
	 * While this is a valid option for auto schema tooling, it's not a
	 * valid action for the {@code SchemaManagementTool}; instead the
	 * caller of {@code SchemaManagementTool} must split this into two
	 * separate requests to:
	 * <ol>
	 *     <li>{@linkplain #CREATE drop and create} the schema, and then</li>
	 *     <li>later, {@linkplain #DROP drop} the schema again.</li>
	 * </ol>
	 *
	 * @see org.hibernate.tool.schema.spi.SchemaDropper
	 * @see org.hibernate.tool.schema.spi.SchemaCreator
	 */
	CREATE_DROP( null, "create-drop" ),
	/**
	 * Validate the database schema.
	 * <p>
	 * This action is not defined by JPA.
	 *
	 * @see org.hibernate.tool.schema.spi.SchemaValidator
	 */
	VALIDATE( null, "validate" ),
	/**
	 * Update (alter) the database schema.
	 * <p>
	 * This action is not defined by JPA.
	 *
	 * @see org.hibernate.tool.schema.spi.SchemaMigrator
	 */
	UPDATE( null, "update" ),
	/**
	 * Truncate the tables in the schema.
	 * <p>
	 * This action is not defined by JPA.
	 *
	 * @see org.hibernate.tool.schema.spi.SchemaTruncator
	 *
	 * @since 6.2
	 */
	TRUNCATE( null, null);

	private final String externalJpaName;
	private final String externalHbm2ddlName;

	Action(String externalJpaName) {
		this( externalJpaName, externalJpaName );
	}

	Action(String externalJpaName, String externalHbm2ddlName) {
		this.externalJpaName = externalJpaName;
		this.externalHbm2ddlName = externalHbm2ddlName;
	}

	public String getExternalJpaName() {
		return externalJpaName;
	}

	public String getExternalHbm2ddlName() {
		return externalHbm2ddlName;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(externalJpaName=" + externalJpaName + ", externalHbm2ddlName=" + externalHbm2ddlName + ")";
	}

	/**
	 * Used when processing JPA configuration to interpret the user config values.  Generally
	 * this will be a value specified by {@value org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_DATABASE_ACTION}
	 * or {@value org.hibernate.cfg.AvailableSettings#JAKARTA_HBM2DDL_SCRIPTS_ACTION}
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

		if ( value instanceof Action ) {
			return (Action) value;
		}

		final String name = value.toString().trim();
		if ( name.isEmpty() || NONE.externalJpaName.equals( name ) ) {
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

		// lastly, look at the enum name
		for ( Action action : values() ) {
			if ( action.name().equals( name ) ) {
				return action;
			}
		}

		throw new IllegalArgumentException( "Unrecognized JPA schema generation action value : " + value );
	}

	/**
	 * Used to interpret the value of {@value org.hibernate.cfg.AvailableSettings#HBM2DDL_AUTO}
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

		if ( value instanceof Action ) {
			return hbm2ddlSetting( (Action) value );
		}

		final String name = value.toString().trim();
		if ( name.isEmpty() || NONE.externalJpaName.equals( name ) ) {
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

		throw new IllegalArgumentException( "Unrecognized legacy `hibernate.hbm2ddl.auto` value : `" + value + "`");
	}

	private static Action hbm2ddlSetting(Action action) {
		return action;
	}

}
