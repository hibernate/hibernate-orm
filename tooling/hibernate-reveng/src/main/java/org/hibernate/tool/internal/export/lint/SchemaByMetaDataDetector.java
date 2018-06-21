package org.hibernate.tool.internal.export.lint;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.api.reveng.DatabaseCollector;
import org.hibernate.tool.api.reveng.SchemaSelection;
import org.hibernate.tool.internal.metadata.DefaultDatabaseCollector;
import org.hibernate.tool.internal.reveng.DefaultReverseEngineeringStrategy;
import org.hibernate.tool.internal.reveng.JDBCReader;
import org.hibernate.tool.internal.reveng.JdbcReaderFactory;
import org.hibernate.tool.internal.reveng.TableSelectorStrategy;
import org.hibernate.tool.internal.util.JdbcToHibernateTypeHelper;
import org.hibernate.tool.internal.util.TableNameQualifier;

public class SchemaByMetaDataDetector extends RelationalModelDetector {

	public String getName() {
		return "schema";
	}
	
	JDBCReader reader;

	private TableSelectorStrategy tableSelector;

	private DatabaseCollector dbc;

	private Dialect dialect;

	private Mapping mapping;
	
	/** current table as read from the database */
	Table currentDbTable = null;

	public void initialize(Metadata metadata) {
		super.initialize( metadata);
		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
		ServiceRegistry serviceRegistry = builder.build();
		
		dialect = serviceRegistry.getService(JdbcServices.class).getDialect();

		tableSelector = new TableSelectorStrategy(
				new DefaultReverseEngineeringStrategy() );
		reader = JdbcReaderFactory.newJDBCReader( 
				Environment.getProperties(),
				tableSelector, 
				serviceRegistry);
		dbc = new DefaultDatabaseCollector(reader.getMetaDataDialect());
	}

	public void visit(IssueCollector collector) {
		super.visit(collector);		
		visitGenerators(collector);				
	}
	
	public void visitGenerators(IssueCollector collector) {
		Iterator<?> iter = iterateGenerators();
		
		Set<?> sequences = Collections.EMPTY_SET;
		if(dialect.supportsSequences()) {
			sequences = reader.readSequences(dialect.getQuerySequencesString());
		}

		// TODO: move this check into something that could check per class or collection instead.
		while ( iter.hasNext() ) {
			PersistentIdentifierGenerator generator = (PersistentIdentifierGenerator) iter.next();
			Object key = generator.generatorKey();
			if ( !isSequence(key, sequences) && !isTable( key ) ) {
				collector.reportIssue( new Issue( "MISSING_ID_GENERATOR", Issue.HIGH_PRIORITY, "Missing sequence or table: " + key));
			}
		}

		
	}

	private boolean isSequence(Object key, Set<?> sequences) {
		if(key instanceof String) {
			if ( sequences.contains( key ) ) {
				return true;
			} else {
				String[] strings = StringHelper.split(".", (String) key);
				if(strings.length==3) {
					return sequences.contains(strings[2]);
				} else if (strings.length==2) {
					return sequences.contains(strings[1]);
				}
			}
		}
		return false;
	}

	private boolean isTable(Object key) throws HibernateException {
		// BIG HACK - should probably utilize the table cache before going to the jdbcreader :(
		if(key instanceof String) {
			String[] strings = StringHelper.split(".", (String) key);
			if(strings.length==1) {
				tableSelector.clearSchemaSelections();
				tableSelector.addSchemaSelection( new SchemaSelection(null,null, strings[0]) );
				List<?> list = reader.readDatabaseSchema( dbc, null, null );
				return !list.isEmpty();
			} else if(strings.length==3) {
				tableSelector.clearSchemaSelections();
				tableSelector.addSchemaSelection( new SchemaSelection(strings[0],strings[1], strings[2]) );
				List<?> list = reader.readDatabaseSchema( dbc, null, null );
				return !list.isEmpty();
			} else if (strings.length==2) {
				tableSelector.clearSchemaSelections();
				tableSelector.addSchemaSelection( new SchemaSelection(null,strings[0], strings[1]) );
				List<?> list = reader.readDatabaseSchema( dbc, null, null );
				return !list.isEmpty();
			}
		}
		return false;
	}
	
	public void visit(Table table, IssueCollector pc) {

		if ( table.isPhysicalTable() ) {
			setSchemaSelection( table );

			List<?> list = reader.readDatabaseSchema( dbc, null, null );

			if ( list.isEmpty() ) {
				pc.reportIssue( new Issue( "SCHEMA_TABLE_MISSING",
						Issue.HIGH_PRIORITY, "Missing table "
								+ TableNameQualifier.qualify( table.getCatalog(), table
										.getSchema(), table.getName() ) ) );
				return;
			}
			else if ( list.size() > 1 ) {
				pc.reportIssue( new Issue( "SCHEMA_TABLE_MISSING",
						Issue.NORMAL_PRIORITY, "Found "
								+ list.size()
								+ " tables for "
								+ TableNameQualifier.qualify( table.getCatalog(), table
										.getSchema(), table.getName() ) ) );
				return;
			}
			else {
				currentDbTable = (Table) list.get( 0 );
				visitColumns(table,pc);				
			}
		}
		else {
			// log?			
		}
	}

	String table(Table t) {
		return TableNameQualifier.qualify( t.getCatalog(), t.getSchema(), t.getName() );
	}
	
	public void visit(
			Table table, 
			Column col,
			IssueCollector pc) {
		if ( currentDbTable == null ) {
			return;
		}

		Column dbColumn = currentDbTable
				.getColumn( new Column( col.getName() ) );

		if ( dbColumn == null ) {
			pc.reportIssue( new Issue( "SCHEMA_COLUMN_MISSING",
					Issue.HIGH_PRIORITY, table(table) + " is missing column: " + col.getName() ) );
		}
		else {
			//TODO: this needs to be able to know if a type is truly compatible or not. Right now it requires an exact match.
			//String sqlType = col.getSqlType( dialect, mapping );
			int dbTypeCode = dbColumn.getSqlTypeCode().intValue();
			int modelTypeCode = col
								.getSqlTypeCode( mapping );
			// TODO: sqltype name string
			if ( !(dbTypeCode == modelTypeCode ) ) {
				pc.reportIssue( new Issue( "SCHEMA_COLUMN_TYPE_MISMATCH",
						Issue.NORMAL_PRIORITY, table(table) + " has a wrong column type for "
								+ col.getName() + ", expected: "
								+ JdbcToHibernateTypeHelper.getJDBCTypeName(modelTypeCode) + " but was " + JdbcToHibernateTypeHelper.getJDBCTypeName(dbTypeCode) + " in db") );
			}
		}
	}

	private void setSchemaSelection(Table table) {
		tableSelector.clearSchemaSelections();
		tableSelector.addSchemaSelection( new SchemaSelection( table
				.getCatalog(), table.getSchema(), table.getName() ) );

	}

	/**
	 * 
	 * @param cfg 
	 * @return iterator over all the IdentifierGenerator's found in the entitymodel and return a list of unique IdentifierGenerators
	 * @throws MappingException
	 */
	@SuppressWarnings("deprecation")
	private Iterator<IdentifierGenerator> iterateGenerators() throws MappingException {

		TreeMap<Object, IdentifierGenerator> generators = 
				new TreeMap<Object, IdentifierGenerator>();
		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
		Properties properties = (Properties)builder.getSettings();
		String defaultCatalog = properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
		String defaultSchema = properties.getProperty(AvailableSettings.DEFAULT_SCHEMA);

		Iterator<PersistentClass> persistentClassIterator = getMetadata().getEntityBindings().iterator();
		while ( persistentClassIterator.hasNext() ) {
			PersistentClass pc = persistentClassIterator.next();

			if ( !pc.isInherited() ) {

				IdentifierGenerator ig = pc.getIdentifier()
						.createIdentifierGenerator(
								getMetadata().getIdentifierGeneratorFactory(),
								dialect,
								defaultCatalog,
								defaultSchema,
								(RootClass) pc
							);

				if ( ig instanceof PersistentIdentifierGenerator ) {
					generators.put( ( (PersistentIdentifierGenerator) ig ).generatorKey(), ig );
				}

			}
		}

		Iterator<?> collectionIterator = getMetadata().getCollectionBindings().iterator();
		while ( collectionIterator.hasNext() ) {
			Collection collection = (Collection) collectionIterator.next();

			if ( collection.isIdentified() ) {

				IdentifierGenerator ig = ( (IdentifierCollection) collection ).getIdentifier()
						.createIdentifierGenerator(
								getMetadata().getIdentifierGeneratorFactory(),
								dialect,
								defaultCatalog,
								defaultSchema,
								null
							);

				if ( ig instanceof PersistentIdentifierGenerator ) {
					generators.put( ( (PersistentIdentifierGenerator) ig ).generatorKey(), ig );
				}

			}
		}

		return generators.values().iterator();
	}

}
