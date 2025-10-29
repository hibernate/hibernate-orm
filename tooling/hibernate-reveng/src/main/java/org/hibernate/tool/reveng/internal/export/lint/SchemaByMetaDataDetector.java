/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.reveng.internal.export.lint;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.generator.Generator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.reveng.api.core.RevengDialect;
import org.hibernate.tool.reveng.api.core.RevengDialectFactory;
import org.hibernate.tool.reveng.api.core.RevengStrategy.SchemaSelection;
import org.hibernate.tool.reveng.internal.core.RevengMetadataCollector;
import org.hibernate.tool.reveng.internal.core.reader.DatabaseReader;
import org.hibernate.tool.reveng.internal.core.strategy.DefaultStrategy;
import org.hibernate.tool.reveng.internal.core.strategy.TableSelectorStrategy;
import org.hibernate.tool.reveng.internal.util.JdbcToHibernateTypeHelper;
import org.hibernate.tool.reveng.internal.util.TableNameQualifier;
import org.hibernate.type.MappingContext;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

@SuppressWarnings("deprecation")
public class SchemaByMetaDataDetector extends RelationalModelDetector {

	public String getName() {
		return "schema";
	}
	
	DatabaseReader reader;
	
	private SequenceCollector sequenceCollector;

	private TableSelectorStrategy tableSelector;

	private RevengDialect metadataDialect;

	private Dialect dialect;

	private MappingContext mapping;
	
	private Properties properties;
	
	/** current table as read from the database */
	Table currentDbTable = null;

	public void initialize(Metadata metadata) {
		super.initialize( metadata);
		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
		ServiceRegistry serviceRegistry = builder.build();
		
		properties = Environment.getProperties();
		dialect = serviceRegistry.getService(JdbcServices.class).getDialect();

		tableSelector = new TableSelectorStrategy(
				new DefaultStrategy() );
		metadataDialect = RevengDialectFactory
				.createMetaDataDialect(
						dialect, 
						properties );
		reader = DatabaseReader.create( 
				properties,
				tableSelector, 
				metadataDialect,
				serviceRegistry);
		ConnectionProvider connectionProvider = serviceRegistry.getService(ConnectionProvider.class);
		sequenceCollector = SequenceCollector.create(connectionProvider);
	}

	public void visit(IssueCollector collector) {
		super.visit(collector);		
		visitGenerators(collector);				
	}
	
	public void visitGenerators(IssueCollector collector) {
		Iterator<?> iter = iterateGenerators();
		
		Set<?> sequences = Collections.EMPTY_SET;
		if(dialect.getSequenceSupport().supportsSequences()) {
			sequences = sequenceCollector.readSequences(dialect.getQuerySequencesString());
		}

		// TODO: move this check into something that could check per class or collection instead.
		while ( iter.hasNext() ) {
			PersistentIdentifierGenerator generator = (PersistentIdentifierGenerator) iter.next();
			Object key = getGeneratorKey(generator);
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
				tableSelector.addSchemaSelection( createSchemaSelection(null,null, strings[0]) );
				Collection<Table> collection = readFromDatabase();
				return !collection.isEmpty();
			} else if(strings.length==3) {
				tableSelector.clearSchemaSelections();
				tableSelector.addSchemaSelection( createSchemaSelection(strings[0],strings[1], strings[2]) );
				Collection<Table> collection = readFromDatabase();
				return !collection.isEmpty();
			} else if (strings.length==2) {
				tableSelector.clearSchemaSelections();
				tableSelector.addSchemaSelection( createSchemaSelection(null,strings[0], strings[1]) );
				Collection<Table> collection = readFromDatabase();
				return !collection.isEmpty();
			}
		}
		return false;
	}
	
	public void visit(Table table, IssueCollector pc) {

		if ( table.isPhysicalTable() ) {
			setSchemaSelection( table );

			Collection<Table> collection = readFromDatabase();

			if ( collection.isEmpty() ) {
				pc.reportIssue( new Issue( "SCHEMA_TABLE_MISSING",
						Issue.HIGH_PRIORITY, "Missing table "
								+ TableNameQualifier.qualify( table.getCatalog(), table
										.getSchema(), table.getName() ) ) );
				return;
			}
			else if ( collection.size() > 1 ) {
				pc.reportIssue( new Issue( "SCHEMA_TABLE_MISSING",
						Issue.NORMAL_PRIORITY, "Found "
								+ collection.size()
								+ " tables for "
								+ TableNameQualifier.qualify( table.getCatalog(), table
										.getSchema(), table.getName() ) ) );
				return;
			}
			else {
				currentDbTable = collection.iterator().next();
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
		tableSelector.addSchemaSelection( createSchemaSelection(
				table.getCatalog(), 
				table.getSchema(), 
				table.getName() ) );
	}

	/**
	 * @return iterator over all the IdentifierGenerator's found in the entitymodel and return a list of unique IdentifierGenerators
	 * @throws MappingException
	 */
	private Iterator<Generator> iterateGenerators() throws MappingException {

		TreeMap<Object, Generator> generators = 
				new TreeMap<Object, Generator>();

		Iterator<PersistentClass> persistentClassIterator = getMetadata().getEntityBindings().iterator();
		while ( persistentClassIterator.hasNext() ) {
			PersistentClass pc = persistentClassIterator.next();

			if ( !pc.isInherited() ) {

				Generator ig = pc.getIdentifier()
						.createGenerator(
								dialect,
								(RootClass) pc
							);

				if ( ig instanceof PersistentIdentifierGenerator ) {
					generators.put( getGeneratorKey( (PersistentIdentifierGenerator) ig ), ig );
				}

			}
		}

		Iterator<?> collectionIterator = getMetadata().getCollectionBindings().iterator();
		while ( collectionIterator.hasNext() ) {
			org.hibernate.mapping.Collection collection = (org.hibernate.mapping.Collection) collectionIterator.next();

			if ( collection.isIdentified() ) {

				Generator ig = ( (IdentifierCollection) collection ).getIdentifier()
						.createGenerator(
								dialect,
								null
							);

				if ( ig instanceof PersistentIdentifierGenerator ) {
					generators.put( ( getGeneratorKey((PersistentIdentifierGenerator) ig )), ig );
				}

			}
		}

		return generators.values().iterator();
	}
	
	private Collection<Table> readFromDatabase() {
		RevengMetadataCollector revengMetadataCollector = new RevengMetadataCollector();
		reader.readDatabaseSchema(revengMetadataCollector);
		return revengMetadataCollector.getTables();
	}
	
	private SchemaSelection createSchemaSelection(String matchCatalog, String matchSchema, String matchTable) {
		return new SchemaSelection() {
			@Override
			public String getMatchCatalog() {
				return matchCatalog;
			}
			@Override
			public String getMatchSchema() {
				return matchSchema;
			}
			@Override
			public String getMatchTable() {
				return matchTable;
			}
			
		};
	}
	
	private String getGeneratorKey(PersistentIdentifierGenerator ig) {
		String result = null;
		if  (ig instanceof SequenceStyleGenerator) {
			result = getKeyForSequenceStyleGenerator((SequenceStyleGenerator)ig);
		} else if (ig instanceof TableGenerator) {
			result = getKeyForTableGenerator((TableGenerator)ig);
		}
		return result;
	}
	
	private String getKeyForSequenceStyleGenerator(SequenceStyleGenerator ig) {
		return ig.getDatabaseStructure().getPhysicalName().render();
	}
	
	private String getKeyForTableGenerator(TableGenerator ig) {
		return ig.getTableName();
	}

}
