package org.hibernate.tool.internal.reveng;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.dialect.MetaDataDialect;
import org.hibernate.tool.api.reveng.DatabaseCollector;
import org.hibernate.tool.api.reveng.ProgressListener;
import org.hibernate.tool.api.reveng.ReverseEngineeringRuntimeInfo;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.SchemaSelection;

public class JDBCReader {

	private final ReverseEngineeringStrategy revengStrategy;
	
	private MetaDataDialect metadataDialect;

	private final ConnectionProvider provider;

	private final SQLExceptionConverter sec;

	private final String defaultSchema;
	private final String defaultCatalog;
	
	public JDBCReader(MetaDataDialect dialect, ConnectionProvider provider, SQLExceptionConverter sec, String defaultCatalog, String defaultSchema, ReverseEngineeringStrategy reveng) {
		this.metadataDialect = dialect;
		this.provider = provider;
		this.sec = sec;
		this.revengStrategy = reveng;
		this.defaultCatalog = defaultCatalog;
		this.defaultSchema = defaultSchema;
		if(revengStrategy==null) {
			throw new IllegalStateException("Strategy cannot be null");
		}
	}
		
	public List<Table> readDatabaseSchema(DatabaseCollector dbs, String catalog, String schema, ProgressListener progress) {
		try {
			ReverseEngineeringRuntimeInfo info = 
					ReverseEngineeringRuntimeInfo.createInstance(provider, sec, dbs);
			getMetaDataDialect().configure(info);
			revengStrategy.configure(info);
			
			Set<Table> hasIndices = new HashSet<Table>();
			
			List<SchemaSelection> schemaSelectors = revengStrategy.getSchemaSelections();
			List<Table> foundTables = new ArrayList<Table>();
			if(schemaSelectors==null) {
				foundTables.addAll(TableProcessor.processTables(getMetaDataDialect(), revengStrategy, defaultSchema, defaultCatalog, dbs, new SchemaSelection(catalog, schema), hasIndices, progress));
			} else {
				for (Iterator<SchemaSelection> iter = schemaSelectors.iterator(); iter.hasNext();) {
					SchemaSelection selection = iter.next();
					foundTables.addAll(TableProcessor.processTables(getMetaDataDialect(), revengStrategy, defaultSchema, defaultCatalog, dbs, selection, hasIndices, progress));
				}
			}
			
			Iterator<Table> tables = foundTables.iterator(); // not dbs.iterateTables() to avoid "double-read" of columns etc.
			while ( tables.hasNext() ) {
				Table table = tables.next();
				BasicColumnProcessor.processBasicColumns(getMetaDataDialect(), revengStrategy, defaultSchema, defaultCatalog, table, progress);
				PrimaryKeyProcessor.processPrimaryKey(getMetaDataDialect(), revengStrategy, defaultSchema, defaultCatalog, dbs, table);
				if(hasIndices.contains(table)) {
					IndexProcessor.processIndices(getMetaDataDialect(), defaultSchema, defaultCatalog, table);
				}
			}
			
			tables = foundTables.iterator(); //dbs.iterateTables();
			Map<String, List<ForeignKey>> oneToManyCandidates = resolveForeignKeys( dbs, tables, progress );
			
			dbs.setOneToManyCandidates(oneToManyCandidates);
			
			return foundTables;
		} finally {
			getMetaDataDialect().close();
			revengStrategy.close();
		}
	}

	/**
	 * Iterates the tables and find all the foreignkeys that refers to something that is available inside the DatabaseCollector.
	 * @param dbs
	 * @param progress
	 * @param tables
	 * @return
	 */
	private Map<String, List<ForeignKey>> resolveForeignKeys(DatabaseCollector dbs, Iterator<Table> tables, ProgressListener progress) {
		List<ForeignKeysInfo> fks = new ArrayList<ForeignKeysInfo>();
		while ( tables.hasNext() ) {
			Table table = (Table) tables.next();
			// Done here after the basic process of collections as we might not have touched 
			// all referenced tables (this ensure the columns are the same instances througout the basic JDBC derived model.
			// after this stage it should be "ok" to divert from keeping columns in sync as it can be required if the same 
			//column is used with different aliases in the ORM mapping.
			ForeignKeysInfo foreignKeys = ForeignKeyProcessor.processForeignKeys(getMetaDataDialect(), revengStrategy, defaultSchema, defaultCatalog, dbs, table, progress);
			fks.add( foreignKeys );				  	   
		}
		
		Map<String, List<ForeignKey>> oneToManyCandidates = new HashMap<String, List<ForeignKey>>();			
		for (Iterator<ForeignKeysInfo> iter = fks.iterator(); iter.hasNext();) {
			ForeignKeysInfo element = iter.next();
			Map<String, List<ForeignKey>> map = element.process( revengStrategy ); // the actual foreignkey is created here.
			mergeMultiMap( oneToManyCandidates, map );
		}
		return oneToManyCandidates;
	}
	
	public MetaDataDialect getMetaDataDialect() {
		return metadataDialect;
	}
	
	    private void mergeMultiMap(Map<String, List<ForeignKey>> dest, Map<String, List<ForeignKey>> src) {
	    	Iterator<Entry<String, List<ForeignKey>>> items = src.entrySet().iterator();
	    	
	    	while ( items.hasNext() ) {
	    		Entry<String, List<ForeignKey>> element = items.next();
	    		
	    		List<ForeignKey> existing = dest.get( element.getKey() );
	    		if(existing == null) {
	    			dest.put( element.getKey(), element.getValue() );
	    		} 
	    		else {
	    			existing.addAll(element.getValue());
	    		}			
	    	}
	    	
	    }

		static class NoopProgressListener implements ProgressListener {
			public void startSubTask(String name) {	// noop };
			}
		}
		
		public List<Table> readDatabaseSchema(DatabaseCollector dbs, String catalog, String schema) {
			return readDatabaseSchema(dbs, catalog, schema, new NoopProgressListener());
		}
				
		public Set<String> readSequences(String sql) {
			Set<String> sequences = new HashSet<String>();
			if (sql!=null) {
				Connection connection = null;
				try {
				
					connection = provider.getConnection();
					Statement statement = null;
					ResultSet rs = null;
					try {
						statement = connection.createStatement();
						rs = statement.executeQuery(sql);
						while ( rs.next() ) {
							sequences.add( rs.getString("SEQUENCE_NAME").toLowerCase().trim() );
						}
					}
					finally {
						if (rs!=null) rs.close();
						if (statement!=null) statement.close();
					}

				} catch (SQLException e) {
					sec.convert(e, "Problem while closing connection", null);
				}
				finally {
					if(connection!=null)
						try {
							provider.closeConnection( connection );
						}
						catch (SQLException e) {
							sec.convert(e, "Problem while closing connection", null);
						}
				} 
			}
			return sequences;
		}
}		

