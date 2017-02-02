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
import java.util.HashMap;
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
public class Database {
	private final Dialect dialect;
	private final MetadataBuildingOptions buildingOptions;
	private final JdbcEnvironment jdbcEnvironment;

	private Namespace implicitNamespace;

	private final Map<Namespace.Name,Namespace> namespaceMap = new TreeMap<Namespace.Name, Namespace>();

	private Map<String,AuxiliaryDatabaseObject> auxiliaryDatabaseObjects;
	private List<InitCommand> initCommands;

	public Database(MetadataBuildingOptions buildingOptions) {
		this( buildingOptions, buildingOptions.getServiceRegistry().getService( JdbcEnvironment.class ) );
	}

	public Database(MetadataBuildingOptions buildingOptions, JdbcEnvironment jdbcEnvironment) {
		this.buildingOptions = buildingOptions;

		this.jdbcEnvironment = jdbcEnvironment;

		this.dialect = determineDialect( buildingOptions );

		this.implicitNamespace = makeNamespace(
				new Namespace.Name(
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

	private Namespace makeNamespace(Namespace.Name name) {
		Namespace namespace;
		namespace = new Namespace( this, name );
		namespaceMap.put( name, namespace );
		return namespace;
	}

	public MetadataBuildingOptions getBuildingOptions() {
		return buildingOptions;
	}

	public Dialect getDialect() {
		return dialect;
	}

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
	public Identifier toIdentifier(String text) {
		return text == null
				? null
				: jdbcEnvironment.getIdentifierHelper().toIdentifier( text );
	}

	public PhysicalNamingStrategy getPhysicalNamingStrategy() {
		return getBuildingOptions().getPhysicalNamingStrategy();
	}

	public Iterable<Namespace> getNamespaces() {
		return namespaceMap.values();
	}

	public Namespace getDefaultNamespace() {
		return implicitNamespace;
	}

	public Namespace locateNamespace(Identifier catalogName, Identifier schemaName) {
		if ( catalogName == null && schemaName == null ) {
			return getDefaultNamespace();
		}

		final Namespace.Name name = new Namespace.Name( catalogName, schemaName );
		Namespace namespace = namespaceMap.get( name );
		if ( namespace == null ) {
			namespace = makeNamespace( name );
		}
		return namespace;
	}

	public Namespace adjustDefaultNamespace(Identifier catalogName, Identifier schemaName) {
		final Namespace.Name name = new Namespace.Name( catalogName, schemaName );
		if ( implicitNamespace.getName().equals( name ) ) {
			return implicitNamespace;
		}

		Namespace namespace = namespaceMap.get( name );
		if ( namespace == null ) {
			namespace = makeNamespace( name );
		}
		implicitNamespace = namespace;
		return implicitNamespace;
	}

	public Namespace adjustDefaultNamespace(String implicitCatalogName, String implicitSchemaName) {
		return adjustDefaultNamespace( toIdentifier( implicitCatalogName ), toIdentifier( implicitSchemaName ) );
	}

	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		if ( auxiliaryDatabaseObjects == null ) {
			auxiliaryDatabaseObjects = new HashMap<String,AuxiliaryDatabaseObject>();
		}
		auxiliaryDatabaseObjects.put( auxiliaryDatabaseObject.getExportIdentifier(), auxiliaryDatabaseObject );
	}

	public Collection<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjects() {
		return auxiliaryDatabaseObjects == null
				? Collections.<AuxiliaryDatabaseObject>emptyList()
				: auxiliaryDatabaseObjects.values();
	}

	public Collection<InitCommand> getInitCommands() {
		return initCommands == null
				? Collections.<InitCommand>emptyList()
				: initCommands;
	}

	public void addInitCommand(InitCommand initCommand) {
		if ( initCommands == null ) {
			initCommands = new ArrayList<InitCommand>();
		}
		initCommands.add( initCommand );
	}
}
