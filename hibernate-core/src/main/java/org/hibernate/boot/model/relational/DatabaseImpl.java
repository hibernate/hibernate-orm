/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;

/**
 * @author Steve Ebersole
 */
public class DatabaseImpl implements Database {
	private final Dialect dialect;
	private final MetadataBuildingOptions buildingOptions;
	private final JdbcEnvironment jdbcEnvironment;

	private Schema implicitSchema;

	private final Map<SchemaImpl.Name,Schema> schemaMap = new TreeMap<SchemaImpl.Name, Schema>();

	private List<AuxiliaryDatabaseObject> auxiliaryDatabaseObjects;
	private List<InitCommand> initCommands;

	public DatabaseImpl(MetadataBuildingOptions buildingOptions) {
		this( buildingOptions, buildingOptions.getServiceRegistry().getService( JdbcEnvironment.class ) );
	}

	public DatabaseImpl(MetadataBuildingOptions buildingOptions, JdbcEnvironment jdbcEnvironment) {
		this.buildingOptions = buildingOptions;

		this.jdbcEnvironment = jdbcEnvironment;

		this.dialect = determineDialect( buildingOptions );

		this.implicitSchema = makeSchema(
				new SchemaImpl.Name(
						toIdentifier( buildingOptions.getMappingDefaults().getImplicitCatalogName() ),
						toIdentifier( buildingOptions.getMappingDefaults().getImplicitSchemaName() )
				)
		);
	}

	private static Dialect determineDialect(MetadataBuildingOptions buildingOptions) {
		final Dialect dialect = buildingOptions.getServiceRegistry().getService( JdbcServices.class ).getDialect();
		if ( dialect != null ) {
			return dialect;
		}

		// Use H2 dialect as default
		return new H2Dialect();
	}

	private Schema makeSchema(SchemaImpl.Name name) {
		SchemaImpl schema;
		schema = new SchemaImpl( this, name );
		schemaMap.put( name, schema );
		return schema;
	}

	@Override
	public MetadataBuildingOptions getBuildingOptions() {
		return buildingOptions;
	}

	@Override
	public Dialect getDialect() {
		return dialect;
	}

	@Override
	public JdbcEnvironment getJdbcEnvironment() {
		return jdbcEnvironment;
	}

	/**
	 * Wrap the raw name of a database object in it's Identifier form accounting for quoting from
	 * any of:<ul>
	 *     <li>explicit quoting in the name itself</li>
	 *     <li>global request to quote all identifiers</li>
	 * </ul>
	 * <p/>
	 * NOTE : quoting from database keywords happens only when building physical identifiers
	 *
	 * @param text The raw object name
	 *
	 * @return The wrapped Identifier form
	 */
	@Override
	public Identifier toIdentifier(String text) {
		return text == null
				? null
				: jdbcEnvironment.getIdentifierHelper().toIdentifier( text );
	}

	@Override
	public PhysicalNamingStrategy getPhysicalNamingStrategy() {
		return getBuildingOptions().getPhysicalNamingStrategy();
	}

	@Override
	public Iterable<Schema> getSchemas() {
		return schemaMap.values();
	}

	@Override
	public Schema getDefaultSchema() {
		return implicitSchema;
	}

	@Override
	public Schema locateSchema(Identifier catalogName, Identifier schemaName) {
		if ( catalogName == null && schemaName == null ) {
			return getDefaultSchema();
		}

		final SchemaImpl.Name name = new SchemaImpl.Name( catalogName, schemaName );
		Schema schema = schemaMap.get( name );
		if ( schema == null ) {
			schema = makeSchema( name );
		}
		return schema;
	}

	@Override
	public Schema adjustDefaultSchema(Identifier catalogName, Identifier schemaName) {
		final SchemaImpl.Name name = new SchemaImpl.Name( catalogName, schemaName );
		if ( implicitSchema.getName().equals( name ) ) {
			return implicitSchema;
		}

		Schema schema = schemaMap.get( name );
		if ( schema == null ) {
			schema = makeSchema( name );
		}
		implicitSchema = schema;
		return implicitSchema;
	}

	@Override
	public Schema adjustDefaultSchema(String implicitCatalogName, String implicitSchemaName) {
		return adjustDefaultSchema( toIdentifier( implicitCatalogName ), toIdentifier( implicitSchemaName ) );
	}

	@Override
	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		if ( auxiliaryDatabaseObjects == null ) {
			auxiliaryDatabaseObjects = new ArrayList<AuxiliaryDatabaseObject>();
		}
		auxiliaryDatabaseObjects.add( auxiliaryDatabaseObject );
	}

	@Override
	public Collection<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjects() {
		return auxiliaryDatabaseObjects == null
				? Collections.<AuxiliaryDatabaseObject>emptyList()
				: auxiliaryDatabaseObjects;
	}

	@Override
	public Collection<InitCommand> getInitCommands() {
		return initCommands == null
				? Collections.<InitCommand>emptyList()
				: initCommands;
	}

	@Override
	public void addInitCommand(InitCommand initCommand) {
		if ( initCommands == null ) {
			initCommands = new ArrayList<InitCommand>();
		}
		initCommands.add( initCommand );
	}
}
