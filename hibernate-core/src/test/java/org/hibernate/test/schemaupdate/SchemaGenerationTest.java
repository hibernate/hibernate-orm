package org.hibernate.test.schemaupdate;

import java.sql.Types;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class SchemaGenerationTest extends BaseUnitTestCase {
	private ServiceRegistry serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@After
	public void tearDown() {
		if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7612")
	public void testSqlCreatePrimaryAndForeignKeyOrder() {
		final Dialect dialect = createDialect( 32 );
		final Table table = new Table(
				new Schema( null, null ),
				Identifier.toIdentifier( "col_order_table" ),
				Identifier.toIdentifier( "col_order_table" )
		);
		addForeignKeyColumn( table, "fk1", "cot_fk1", dialect.getTypeName( Types.INTEGER ) );
		addForeignKeyColumn( table, "fk1", "cot_fk2", dialect.getTypeName( Types.VARCHAR, 32, 0, 0 ) );
		table.createColumn( "desc1" ).setSqlType( dialect.getTypeName( Types.VARCHAR, 100, 0, 0 ) );
		addPrimaryKeyColumn( table, "pk1", "id1", dialect.getTypeName( Types.INTEGER ) );
		addPrimaryKeyColumn( table, "pk1", "id2", dialect.getTypeName( Types.VARCHAR, 32, 0, 0 ) );
		table.createColumn( "desc2" ).setSqlType( dialect.getTypeName( Types.BLOB )  );

		final String[] sqlCreate = dialect.getTableExporter().getSqlCreateStrings( table, serviceRegistry.getService( JdbcEnvironment.class ) );

		Assert.assertEquals(
				"PK and FK columns should appear first in CREATE TABLE statement.",
				"create table col_order_table (id1 integer, id2 varchar(32), cot_fk1 integer, cot_fk2 varchar(32), desc1 varchar(100), desc2 blob, primary key (id1, id2))",
				sqlCreate[0]
		);
	}

	private Dialect createDialect(final int maxAliasLength) {
		return new Dialect() {
			public int getMaxAliasLength() {
				return maxAliasLength;
			}
		};
	}

	private void addPrimaryKeyColumn(Table table, String name, String colName, String sqlType) {
		final Column column = table.createColumn( colName );
		column.setSqlType( sqlType );
		if ( table.getPrimaryKey().getName() == null ) {
			table.getPrimaryKey().setName( name );
		}
		table.getPrimaryKey().addColumn( column );
	}

	private void addForeignKeyColumn(Table table, String name, String colName, String sqlType) {
		final Column column = table.createColumn( colName );
		column.setSqlType( sqlType );
		ForeignKey foreignKey = table.locateForeignKey( name );
		if ( foreignKey == null ) {
			foreignKey = table.createForeignKey( table, name );
		}
		foreignKey.addColumn( column );
	}
}
