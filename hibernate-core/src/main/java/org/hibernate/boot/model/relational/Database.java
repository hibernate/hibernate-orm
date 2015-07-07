package org.hibernate.boot.model.relational;

import java.util.Collection;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public interface Database {

	MetadataBuildingOptions getBuildingOptions();

	Dialect getDialect();

	JdbcEnvironment getJdbcEnvironment();

	/**
	 * Wrap the raw name of a database object in its Identifier form accounting for quoting from any of:
	 * <ul>
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
	Identifier toIdentifier(String text);

	PhysicalNamingStrategy getPhysicalNamingStrategy();

	Iterable<Schema> getSchemas();

	Schema getDefaultSchema();

	Schema locateSchema(Identifier catalogName, Identifier schemaName);

	Schema adjustDefaultSchema(Identifier catalogName, Identifier schemaName);

	Schema adjustDefaultSchema(String implicitCatalogName, String implicitSchemaName);

	void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject);

	Collection<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjects();

	Collection<InitCommand> getInitCommands();

	void addInitCommand(InitCommand initCommand);

}
