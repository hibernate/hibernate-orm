/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.factory.internal;

import org.hibernate.InstantiationException;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.id.Assigned;
import org.hibernate.id.Configurable;
import org.hibernate.id.ForeignGenerator;
import org.hibernate.id.GUIDGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.id.OptimizableGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.SelectGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.id.enhanced.LegacyNamingStrategy;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.SingleNamingStrategy;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.generator.Generator;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Properties;

import static org.hibernate.internal.util.ReflectHelper.getDefaultConstructor;

public class IdentifierGeneratorUtil {

	public static Generator createLegacyIdentifierGenerator(
			SimpleValue simpleValue,
			Dialect dialect,
			String defaultCatalog,
			String defaultSchema,
			RootClass rootClass) {
		final Class<? extends Generator> generatorClass = generatorClass( simpleValue );
		final Constructor<? extends Generator> defaultConstructor = getDefaultConstructor( generatorClass );
		if ( defaultConstructor == null ) {
			throw new InstantiationException( "No default constructor for id generator class", generatorClass );
		}
		final Generator identifierGenerator;
		try {
			identifierGenerator = defaultConstructor.newInstance();
		}
		catch (Exception e) {
			throw new InstantiationException( "Could not instantiate id generator", generatorClass, e );
		}
		if ( identifierGenerator instanceof Configurable ) {
			final Properties parameters = collectParameters( simpleValue, dialect, defaultCatalog, defaultSchema, rootClass );
			final Configurable configurable = (Configurable) identifierGenerator;
			configurable.configure( simpleValue.getType(), parameters, simpleValue.getServiceRegistry() );
		}
		return identifierGenerator;
	}

	private static Class<? extends Generator> generatorClass(SimpleValue simpleValue) {
		String strategy = simpleValue.getIdentifierGeneratorStrategy();
		if ( "native".equals(strategy) ) {
			strategy =
					simpleValue.getMetadata().getDatabase().getDialect()
							.getNativeIdentifierGeneratorStrategy();
		}
		switch (strategy) {
			case "assigned":
				return Assigned.class;
			case "enhanced-sequence":
			case "sequence":
				return SequenceStyleGenerator.class;
			case "enhanced-table":
			case "table":
				return TableGenerator.class;
			case "identity":
				return IdentityGenerator.class;
			case "increment":
				return IncrementGenerator.class;
			case "foreign":
				return ForeignGenerator.class;
			case "uuid":
			case "uuid.hex":
				return UUIDHexGenerator.class;
			case "uuid2":
				return UUIDGenerator.class;
			case "select":
				return SelectGenerator.class;
			case "guid":
				return GUIDGenerator.class;
		}
		final Class<? extends Generator> clazz =
				simpleValue.getServiceRegistry().requireService( ClassLoaderService.class )
						.classForName( strategy );
		if ( !Generator.class.isAssignableFrom( clazz ) ) {
			// in principle, this shouldn't happen, since @GenericGenerator
			// constrains the type to subtypes of Generator
			throw new MappingException( clazz.getName() + " does not implement 'Generator'" );
		}
		return clazz;
	}

	public static Properties collectParameters(
			SimpleValue simpleValue,
			Dialect dialect,
			String defaultCatalog,
			String defaultSchema,
			RootClass rootClass) {
		final ConfigurationService configService =
				simpleValue.getMetadata().getMetadataBuildingOptions().getServiceRegistry()
						.requireService( ConfigurationService.class );

		final Properties params = new Properties();

		// This is for backwards compatibility only;
		// when this method is called by Hibernate ORM, defaultSchema and defaultCatalog are always
		// null, and defaults are handled later.
		if ( defaultSchema != null ) {
			params.setProperty( PersistentIdentifierGenerator.SCHEMA, defaultSchema);
		}

		if ( defaultCatalog != null ) {
			params.setProperty( PersistentIdentifierGenerator.CATALOG, defaultCatalog);
		}

		// default initial value and allocation size per-JPA defaults
		params.setProperty( OptimizableGenerator.INITIAL_PARAM,
				String.valueOf( OptimizableGenerator.DEFAULT_INITIAL_VALUE ) );

		params.setProperty( OptimizableGenerator.INCREMENT_PARAM,
				String.valueOf( defaultIncrement( configService ) ) );
		//init the table here instead of earlier, so that we can get a quoted table name
		//TODO: would it be better to simply pass the qualified table name, instead of
		//	  splitting it up into schema/catalog/table names
		final String tableName = simpleValue.getTable().getQuotedName( dialect );
		params.setProperty( PersistentIdentifierGenerator.TABLE, tableName );

		//pass the column name (a generated id almost always has a single column)
		final Column column = (Column) simpleValue.getSelectables().get(0);
		final String columnName = column.getQuotedName( dialect );
		params.setProperty( PersistentIdentifierGenerator.PK, columnName );

		//pass the entity-name, if not a collection-id
		if ( rootClass != null ) {
			params.setProperty( IdentifierGenerator.ENTITY_NAME, rootClass.getEntityName() );
			params.setProperty( IdentifierGenerator.JPA_ENTITY_NAME, rootClass.getJpaEntityName() );
			// The table name is not really a good default for subselect entities,
			// so use the JPA entity name which is short
			params.setProperty( OptimizableGenerator.IMPLICIT_NAME_BASE,
					simpleValue.getTable().isSubselect()
							? rootClass.getJpaEntityName()
							: simpleValue.getTable().getName() );

			params.setProperty( PersistentIdentifierGenerator.TABLES,
					identityTablesString( dialect, rootClass ) );
		}
		else {
			params.setProperty( PersistentIdentifierGenerator.TABLES, tableName );
			params.setProperty( OptimizableGenerator.IMPLICIT_NAME_BASE, tableName );
		}

		if ( simpleValue.getIdentifierGeneratorParameters() != null ) {
			params.putAll( simpleValue.getIdentifierGeneratorParameters() );
		}

		// TODO : we should pass along all settings once "config lifecycle" is hashed out...

		params.put( IdentifierGenerator.CONTRIBUTOR_NAME,
				simpleValue.getBuildingContext().getCurrentContributorName() );

		final Map<String, Object> settings = configService.getSettings();
		if ( settings.containsKey( AvailableSettings.PREFERRED_POOLED_OPTIMIZER ) ) {
			params.put( AvailableSettings.PREFERRED_POOLED_OPTIMIZER,
					settings.get( AvailableSettings.PREFERRED_POOLED_OPTIMIZER ) );
		}

		return params;
	}

	private static String identityTablesString(Dialect dialect, RootClass rootClass) {
		final StringBuilder tables = new StringBuilder();
		for ( Table table : rootClass.getIdentityTables() ) {
			tables.append( table.getQuotedName( dialect ) );
			if ( tables.length()>0 ) {
				tables.append( ", " );
			}
		}
		return tables.toString();
	}

	private static int defaultIncrement(ConfigurationService configService) {
		final String idNamingStrategy =
				configService.getSetting( AvailableSettings.ID_DB_STRUCTURE_NAMING_STRATEGY,
						StandardConverters.STRING, null );
		if ( LegacyNamingStrategy.STRATEGY_NAME.equals( idNamingStrategy )
				|| LegacyNamingStrategy.class.getName().equals( idNamingStrategy )
				|| SingleNamingStrategy.STRATEGY_NAME.equals( idNamingStrategy )
				|| SingleNamingStrategy.class.getName().equals( idNamingStrategy ) ) {
			return 1;
		}
		else {
			return OptimizableGenerator.DEFAULT_INCREMENT_SIZE;
		}
	}

}
