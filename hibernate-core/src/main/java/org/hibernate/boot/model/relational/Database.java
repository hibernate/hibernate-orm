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

import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.naming.Identifier;
import org.hibernate.naming.NamespaceName;

/**
 * @author Steve Ebersole
 */
public class Database {

	private final Dialect dialect;
	private final JdbcEnvironment jdbcEnvironment;

	private MappedNamespace implicitNamespace;

	private final Map<NamespaceName,MappedNamespace> namespaceMap = new TreeMap<>();

	private Map<String,MappedAuxiliaryDatabaseObject> auxiliaryDatabaseObjects;
	private List<InitCommand> initCommands;

	public Database(MetadataBuildingOptions buildingOptions) {
		this( buildingOptions, buildingOptions.getServiceRegistry().getService( JdbcEnvironment.class ) );
	}

	public Database(MetadataBuildingOptions buildingOptions, JdbcEnvironment jdbcEnvironment) {
		this.jdbcEnvironment = jdbcEnvironment;
		this.dialect = determineDialect( buildingOptions );

		this.implicitNamespace = makeNamespace(
				new NamespaceName(
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

	private MappedNamespace makeNamespace(NamespaceName name) {
		MappedNamespace namespace;
		namespace = new MappedNamespace( name );
		namespaceMap.put( name, namespace );
		return namespace;
	}

	public MappedNamespace locateNamespace(Identifier catalog, Identifier schema) {
		return locateNamespace( new NamespaceName( catalog, schema ) );
	}

	public MappedNamespace locateNamespace(NamespaceName name) {
		if ( name.getCatalog() == null && name.getSchema() == null ) {
			return getDefaultNamespace();
		}

		return namespaceMap.computeIfAbsent(
				name,
				n -> makeNamespace( name )
		);
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

	public Collection<MappedNamespace> getNamespaces() {
		return namespaceMap.values();
	}

	public MappedNamespace getDefaultNamespace() {
		return implicitNamespace;
	}

	public MappedNamespace adjustDefaultNamespace(Identifier catalogName, Identifier schemaName) {
		final NamespaceName name = new NamespaceName( catalogName, schemaName );
		if ( implicitNamespace.getName().equals( name ) ) {
			return implicitNamespace;
		}

		MappedNamespace namespace = namespaceMap.get( name );
		if ( namespace == null ) {
			namespace = makeNamespace( name );
		}
		implicitNamespace = namespace;
		return implicitNamespace;
	}

	public MappedNamespace adjustDefaultNamespace(String implicitCatalogName, String implicitSchemaName) {
		return adjustDefaultNamespace( toIdentifier( implicitCatalogName ), toIdentifier( implicitSchemaName ) );
	}

	public void addAuxiliaryDatabaseObject(MappedAuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		if ( auxiliaryDatabaseObjects == null ) {
			auxiliaryDatabaseObjects = new HashMap<>();
		}
		auxiliaryDatabaseObjects.put( auxiliaryDatabaseObject.getIdentifier(), auxiliaryDatabaseObject );
	}

	public Collection<MappedAuxiliaryDatabaseObject> getAuxiliaryDatabaseObjects() {
		return auxiliaryDatabaseObjects == null
				? Collections.emptyList()
				: auxiliaryDatabaseObjects.values();
	}

	public Collection<InitCommand> getInitCommands() {
		return initCommands == null
				? Collections.emptyList()
				: initCommands;
	}

	public void addInitCommand(InitCommand initCommand) {
		if ( initCommands == null ) {
			initCommands = new ArrayList<>();
		}
		initCommands.add( initCommand );
	}
}
