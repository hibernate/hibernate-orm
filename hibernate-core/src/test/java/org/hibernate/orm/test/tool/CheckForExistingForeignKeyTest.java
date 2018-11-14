package org.hibernate.orm.test.tool;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.boot.model.TruthValue;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.metamodel.model.relational.internal.ColumnMappingsImpl;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.Namespace;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.metamodel.model.relational.spi.PhysicalTable;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.naming.Identifier;
import org.hibernate.naming.NamespaceName;
import org.hibernate.naming.QualifiedTableName;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.internal.ColumnInformationImpl;
import org.hibernate.tool.schema.extract.internal.ForeignKeyInformationImpl;
import org.hibernate.tool.schema.extract.internal.TableInformationImpl;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation.ColumnReferenceMapping;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.internal.AbstractSchemaMigrator;
import org.hibernate.tool.schema.internal.HibernateSchemaManagementTool;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.spi.ExecutionOptions;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Milo van der Zee
 */
public class CheckForExistingForeignKeyTest {

	private static Method checkForExistingForeignKey;

	@BeforeAll
	public static void setUp() throws Exception {
		// Get the private method
		checkForExistingForeignKey = AbstractSchemaMigrator.class.getDeclaredMethod(
				"checkForExistingForeignKey",
				ForeignKey.class,
				TableInformation.class
		);
		checkForExistingForeignKey.setAccessible( true );
	}

	/**
	 * If the key has no name it should never be found. Result is that those keys are always recreated. But keys always
	 * have a name so this is no problem.
	 */
	@Test
	public void testForeignKeyWithoutName() {
		// foreignKey name with same name should match
		ForeignKey foreignKey = new ForeignKey( null, true, "", false, false, null, null, null );
		TableInformation tableInformation = new TableInformationImpl( null, null, null, false, null );

		boolean found = checkForExistingForeignKey( foreignKey, tableInformation );
		assertFalse( found, "Key should not be found" );
	}

	/**
	 * Test key not found if tableinformation is missing.
	 */
	@Test
	public void testMissingTableInformation() {
		// foreignKey name with same name should match
		ForeignKey foreignKey = new ForeignKey( "objectId2id", true, "", false, false, null, null, null );

		boolean found = checkForExistingForeignKey( foreignKey, null );
		assertFalse( found, "Key should not be found" );
	}

	/**
	 * Check detection of existing foreign keys with the same name exists.
	 */
	@Test
	public void testKeyWithSameNameExists() {

		String referringColumnName = "referring_id";
		String fkName = "objectId2id";
		String targetTableName = "target_table";
		String targetColumnName = "id";
		String referringTableName = "referring_table";

		Table referringTable = createTable( referringTableName );
		Table targetTable = createTable( targetTableName );

		List<Column> referringColumns = new ArrayList<>();
		referringColumns.add( createColumn( referringColumnName, referringTable ) );

		List<Column> targetColumns = new ArrayList<>();
		targetColumns.add( createColumn( targetColumnName, targetTable ) );

		ForeignKey foreignKey = createForeignKey(
				fkName,
				referringTable,
				targetTable,
				referringColumns,
				targetColumns
		);

		List<ForeignKeyInformation> fks = new ArrayList<>();
		fks.add( new ForeignKeyInformationImpl( new Identifier( fkName, false ), new ArrayList<>() ) );
		TableInformation tableInformation = createTableInfo( fks );

		// foreignKey name with same name should match
		boolean found = checkForExistingForeignKey( foreignKey, tableInformation );
		assertTrue( found, "Key should be found" );
	}

	/**
	 * Check detection of existing foreign keys with the same name exists.
	 */
	@Test
	public void testKeyWithSameNameNotExists() {

		String referringColumnName = "referring_id";
		String fkName = "objectId2id";
		String targetTableName = "target_table";
		String targetColumnName = "id";
		String referringTableName = "referring_table";

		Table referringTable = createTable( referringTableName );
		Table targetTable = createTable( targetTableName );

		List<Column> referringColumns = new ArrayList<>();
		referringColumns.add( createColumn( referringColumnName, referringTable ) );

		List<Column> targetColumns = new ArrayList<>();
		targetColumns.add( createColumn( targetColumnName, targetTable ) );

		ForeignKey foreignKey = createForeignKey(
				fkName,
				referringTable,
				targetTable,
				referringColumns,
				targetColumns
		);

		TableInformation tableInformation = createTableInfo( new ArrayList<>() );


		// foreignKey name with same name should match
		boolean found = checkForExistingForeignKey( foreignKey, tableInformation );
		assertFalse( found, "Key should not be found" );
	}

	/**
	 * Check detection of existing foreign key with the same mappings for a simple mapping (table1.objectId =>
	 * table2.id).
	 */
	@Test
	public void testCheckForExistingForeignKeyOne2One() {

		String referringColumnName = "referring_id";
		String fkName = "objectId2id_1";
		String targetTableName = "target_table";
		String targetColumnName = "id";
		String referringTableName = "referring_table";

		Table referringTable = createTable( referringTableName );
		Table targetTable = createTable( targetTableName );

		List<Column> referringColumns = new ArrayList<>();
		referringColumns.add( createColumn( referringColumnName, referringTable ) );

		List<Column> targetColumns = new ArrayList<>();
		targetColumns.add( createColumn( targetColumnName, targetTable ) );

		ForeignKey foreignKey = createForeignKey(
				fkName,
				referringTable,
				targetTable,
				referringColumns,
				targetColumns
		);

		List<ForeignKeyInformation> fks = new ArrayList<>();
		fks.add( getForeignKeyInformation(
				referringTableName,
				referringColumnName,
				targetTableName,
				targetColumnName,
				"object2Id_2"
		) );
		TableInformation tableInformation = createTableInfo( fks );

		// Check single-column-key to single-column-key, existing (table1.objectId => table2.id)
		boolean found = checkForExistingForeignKey( foreignKey, tableInformation );
		assertTrue( found, "Key should be found" );
	}

	/**
	 * Check detection of not existing foreign key with the same mappings for a simple mapping (table1.objectId =>
	 * table2.id).
	 */
	@Test
	public void testCheckForNotExistingForeignKeyOne2One() {

		String referringColumnName = "referring_id";
		String fkName = "objectId2id_1";
		String targetTableName = "target_table";
		String targetColumnName = "id";
		String referringTableName = "referring_table";

		Table referringTable = createTable( referringTableName );
		Table targetTable = createTable( targetTableName );

		List<Column> referringColumns = new ArrayList<>();
		referringColumns.add( createColumn( referringColumnName, referringTable ) );

		List<Column> targetColumns = new ArrayList<>();
		targetColumns.add( createColumn( targetColumnName, targetTable ) );

		ForeignKey foreignKey = createForeignKey(
				fkName,
				referringTable,
				targetTable,
				referringColumns,
				targetColumns
		);

		List<ForeignKeyInformation> fks = new ArrayList<>();
//		fks.add( getForeignKeyInformation(
//				"different_referring_table_name",
//				referringColumnName,
//				targetTableName,
//				targetColumnName,
//				"blahKey_001"
//		) );
		fks.add( getForeignKeyInformation(
				referringTableName,
				"different_referring_column_name",
				targetTableName,
				targetColumnName,
				"blahKey_002"
		) );
		fks.add( getForeignKeyInformation(
				referringTableName,
				referringColumnName,
				"different_target_table_name",
				targetColumnName,
				"blahKey_003"
		) );
//		fks.add( getForeignKeyInformation(
//				referringTableName,
//				referringColumnName,
//				targetTableName,
//				"different_target_column_name",
//				"blahKey_004"
//		) );
		TableInformation tableInformation = createTableInfo( fks );

		// Check single-column-key to single-column-key, existing (table1.objectId => table2.id)
		boolean found = checkForExistingForeignKey( foreignKey, tableInformation );
		assertFalse( found, "Key should not be found" );
	}

	private TableInformation createTableInfo(List<ForeignKeyInformation> fks) {
		NamespaceName schemaName = new NamespaceName( new Identifier( "-", false ), new Identifier( "-", false ) );
		InformationExtractor informationExtractor = Mockito.mock( InformationExtractor.class );
		IdentifierHelper identifierHelper = new IdentifierHelperImpl();
		Mockito.when( informationExtractor.getForeignKeys( Mockito.any() ) ).thenReturn( fks );

		return new TableInformationImpl(
				informationExtractor,
				identifierHelper,
				new QualifiedTableName( schemaName, new Identifier( "-", false ) ),
				false,
				null
		);
	}

	private boolean checkForExistingForeignKey(ForeignKey foreignKey, TableInformation tableInformation) {
		try {
			return (boolean) checkForExistingForeignKey.invoke( new SchemaMigrator(), foreignKey, tableInformation );
		}
		catch (Exception e) {
			e.printStackTrace();
			fail( e );
		}
		return false;
	}

	private ForeignKeyInformation getForeignKeyInformation(
			String referringTableName,
			String refererringColumnName,
			String targetTableName,
			String targetColumnName,
			String keyName) {
		List<ColumnReferenceMapping> columnMappingList = new ArrayList<>();
		ColumnReferenceMapping columnReferenceMapping = new ColumnReferenceMappingImpl(
				getColumnInformation( referringTableName, refererringColumnName ),
				getColumnInformation( targetTableName, targetColumnName )
		);
		columnMappingList.add( columnReferenceMapping );
		return new ForeignKeyInformationImpl(
				new Identifier( keyName, false ),
				columnMappingList
		);
	}

	private ColumnInformation getColumnInformation(String tableName, String columnName) {
		NamespaceName schemaName = new NamespaceName( new Identifier( "-", false ), new Identifier( "-", false ) );
		TableInformation containingTableInformation = new TableInformationImpl(
				null,
				null,
				new QualifiedTableName( schemaName, new Identifier( tableName, false ) ),
				false,
				null
		);
		Identifier columnIdentifier = new Identifier( columnName, false );
		int typeCode = 0;
		String typeName = null;
		int columnSize = 0;
		int decimalDigits = 0;
		TruthValue nullable = null;
		ColumnInformationImpl columnInformation = new ColumnInformationImpl(
				containingTableInformation,
				columnIdentifier,
				typeCode,
				typeName,
				columnSize,
				decimalDigits,
				nullable
		);
		return columnInformation;
	}

	private class FakeHibernateSchemaManagementTool extends HibernateSchemaManagementTool {
		private class FakeServiceRegistry implements ServiceRegistry {

			@Override
			public ServiceRegistry getParentServiceRegistry() {
				return null;
			}

			@Override
			public <R extends Service> R getService(Class<R> serviceRole) {
				return null;
			}

			@Override
			public void close() {
			}
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return new FakeServiceRegistry();
		}
	}

	private class SchemaMigrator extends AbstractSchemaMigrator {
		/**
		 * Needed constructor.
		 */
		public SchemaMigrator() {
			super( new FakeHibernateSchemaManagementTool(), null, null );
		}

		/**
		 * Needed implementation. Not used in test.
		 */
		@Override
		protected NameSpaceTablesInformation performTablesMigration(
				DatabaseInformation existingDatabase,
				ExecutionOptions options,
				Dialect dialect,
				Formatter formatter,
				Set<String> exportIdentifiers,
				boolean tryToCreateCatalogs,
				boolean tryToCreateSchemas,
				Set<Identifier> exportedCatalogs,
				Namespace namespace,
				GenerationTarget[] targets) {
			return null;
		}
	}

	private class ColumnReferenceMappingImpl implements ColumnReferenceMapping {

		private ColumnInformation referencingColumnMetadata;
		private ColumnInformation referencedColumnMetadata;

		public ColumnReferenceMappingImpl(
				ColumnInformation referencingColumnMetadata,
				ColumnInformation referencedColumnMetadata) {
			this.referencingColumnMetadata = referencingColumnMetadata;
			this.referencedColumnMetadata = referencedColumnMetadata;
		}

		@Override
		public ColumnInformation getReferencingColumnMetadata() {
			return referencingColumnMetadata;
		}

		@Override
		public ColumnInformation getReferencedColumnMetadata() {
			return referencedColumnMetadata;
		}
	}

	private class IdentifierHelperImpl implements IdentifierHelper {

		@Override
		public Identifier normalizeQuoting(Identifier identifier) {
			return null;
		}

		@Override
		public Identifier toIdentifier(String text) {
			return null;
		}

		@Override
		public Identifier toIdentifier(String text, boolean quoted) {
			return null;
		}

		@Override
		public Identifier applyGlobalQuoting(String text) {
			return null;
		}

		@Override
		public boolean isReservedWord(String word) {
			return false;
		}

		@Override
		public String toMetaDataCatalogName(Identifier catalogIdentifier) {
			return null;
		}

		@Override
		public String toMetaDataSchemaName(Identifier schemaIdentifier) {
			return null;
		}

		@Override
		public String toMetaDataObjectName(Identifier identifier) {
			return identifier.getText();
		}
	}

	private ForeignKey createForeignKey(
			String fkName,
			Table referringTable,
			Table targetTable,
			List<Column> referringColumns, List<Column> targetColumns) {
		return new ForeignKey(
				fkName,
				true,
				"",
				false,
				false,
				referringTable,
				targetTable,
				new ColumnMappingsImpl( referringTable, targetTable, referringColumns, targetColumns )
		);
	}

	private PhysicalColumn createColumn(String referringColumnName, Table referringTable) {
		return new PhysicalColumn(
				referringTable,
				new Identifier( referringColumnName, false ),
				null,
				null,
				"",
				"",
				false,
				false,
				null
		);
	}

	private Table createTable(String tableName) {
		QualifiedTableName qualifiedTableName = new QualifiedTableName(
				null,
				null,
				new Identifier( tableName, false )
		);
		return new PhysicalTable( null, qualifiedTableName, false, "" );
	}

}
