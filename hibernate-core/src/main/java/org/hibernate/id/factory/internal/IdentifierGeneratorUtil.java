/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.factory.internal;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.LegacyNamingStrategy;
import org.hibernate.id.enhanced.SingleNamingStrategy;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.generator.Generator;

import java.util.Properties;

public class IdentifierGeneratorUtil {

	public static Generator createLegacyIdentifierGenerator(
			SimpleValue simpleValue,
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			String defaultCatalog,
			String defaultSchema,
			RootClass rootClass) {
		final Properties params = new Properties();

		// This is for backwards compatibility only;
		// when this method is called by Hibernate ORM, defaultSchema and defaultCatalog are always
		// null, and defaults are handled later.
		if ( defaultSchema != null ) {
			params.setProperty( PersistentIdentifierGenerator.SCHEMA, defaultSchema );
		}

		if ( defaultCatalog != null ) {
			params.setProperty( PersistentIdentifierGenerator.CATALOG, defaultCatalog );
		}

		// default initial value and allocation size per-JPA defaults
		params.setProperty( OptimizableGenerator.INITIAL_PARAM, String.valueOf( OptimizableGenerator.DEFAULT_INITIAL_VALUE ) );
		final ConfigurationService cs = simpleValue.getMetadata().getMetadataBuildingOptions().getServiceRegistry()
				.getService( ConfigurationService.class );

		final String idNamingStrategy = cs.getSetting(
				AvailableSettings.ID_DB_STRUCTURE_NAMING_STRATEGY,
				StandardConverters.STRING,
				null
		);

		if ( LegacyNamingStrategy.STRATEGY_NAME.equals( idNamingStrategy )
				|| LegacyNamingStrategy.class.getName().equals( idNamingStrategy )
				|| SingleNamingStrategy.STRATEGY_NAME.equals( idNamingStrategy )
				|| SingleNamingStrategy.class.getName().equals( idNamingStrategy ) ) {
			params.setProperty( OptimizableGenerator.INCREMENT_PARAM, "1" );
		}
		else {
			params.setProperty(
					OptimizableGenerator.INCREMENT_PARAM,
					String.valueOf( OptimizableGenerator.DEFAULT_INCREMENT_SIZE )
			);
		}
		//init the table here instead of earlier, so that we can get a quoted table name
		//TODO: would it be better to simply pass the qualified table name, instead of
		//	  splitting it up into schema/catalog/table names
		final String tableName = simpleValue.getTable().getQuotedName(dialect);
		params.setProperty( PersistentIdentifierGenerator.TABLE, tableName );

		//pass the column name (a generated id almost always has a single column)
		final String columnName = ( (Column) simpleValue.getSelectables().get(0) ).getQuotedName(dialect);
		params.setProperty( PersistentIdentifierGenerator.PK, columnName );

		//pass the entity-name, if not a collection-id
		if ( rootClass != null ) {
			params.setProperty( IdentifierGenerator.ENTITY_NAME, rootClass.getEntityName() );
			params.setProperty( IdentifierGenerator.JPA_ENTITY_NAME, rootClass.getJpaEntityName() );
			// The table name is not really a good default for subselect entities, so use the JPA entity name which is short
			if ( simpleValue.getTable().isSubselect() ) {
				params.setProperty( OptimizableGenerator.IMPLICIT_NAME_BASE, rootClass.getJpaEntityName() );
			}
			else {
				params.setProperty( OptimizableGenerator.IMPLICIT_NAME_BASE, simpleValue.getTable().getName() );
			}

			final StringBuilder tables = new StringBuilder();
			for ( Table table : rootClass.getIdentityTables() ) {
				tables.append( table.getQuotedName(dialect) );
				if ( tables.length()>0 ) {
					tables.append( ", " );
				}
			}
			params.setProperty( PersistentIdentifierGenerator.TABLES, tables.toString() );
		}
		else {
			params.setProperty( PersistentIdentifierGenerator.TABLES, tableName );
			params.setProperty( OptimizableGenerator.IMPLICIT_NAME_BASE, tableName );
		}

		if ( simpleValue.getIdentifierGeneratorParameters() != null ) {
			params.putAll( simpleValue.getIdentifierGeneratorParameters() );
		}

		// TODO : we should pass along all settings once "config lifecycle" is hashed out...

		params.put(
				IdentifierGenerator.CONTRIBUTOR_NAME,
				simpleValue.getBuildingContext().getCurrentContributorName()
		);

		if ( cs.getSettings().get( AvailableSettings.PREFERRED_POOLED_OPTIMIZER ) != null ) {
			params.put(
					AvailableSettings.PREFERRED_POOLED_OPTIMIZER,
					cs.getSettings().get( AvailableSettings.PREFERRED_POOLED_OPTIMIZER )
			);
		}

		final Generator generator = identifierGeneratorFactory.createIdentifierGenerator(
				simpleValue.getIdentifierGeneratorStrategy(),
				simpleValue.getType(),
				params
		);

		if ( generator.generatedOnExecution() && generator instanceof BeforeExecutionGenerator ) {
			// support mixed-timing generators
			simpleValue.setNullValue( "undefined" );
		}

		return generator;
	}

}
