package org.hibernate.orm.test.schemavalidation;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.type.SqlTypes;
import org.hibernate.testing.jdbc.JdbcUtils;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Hibernate creates the table, then validates the schema it has just created.
 */
@RequiresDialect(DB2Dialect.class)
public class DB2NationalizedValidationTest {

	@Test
	void nvarchar() {
		createThenValidate( NVarcharEntity.class );
	}

	@Test
	void nchar() {
		createThenValidate( NCharEntity.class );
	}

	@Test
	void nclob() {
		createThenValidate( NClobEntity.class );
	}

	@Test
	@RequiresDialect(value = DB2Dialect.class, matchSubTypes = false)
	void rejectsWrongPhysicalType() {
		try ( var registry = ServiceRegistryUtil.serviceRegistry() ) {
			final var metadata = new MetadataSources( registry ).addAnnotatedClass( NClobEntity.class ).buildMetadata();
			JdbcUtils.withConnection( registry, connection -> {
				try ( var statement = connection.createStatement() ) {
					final String wrongType = "CLOB".equalsIgnoreCase( physicalNclobType( statement ) ) ? "dbclob" : "clob";
					statement.execute( "create table t_nclob_db2nchar (id bigint not null primary key, val "
							+ wrongType + ")" );
					if ( !connection.getAutoCommit() ) {
						connection.commit();
					}
				}
			} );
			try {
				assertThatThrownBy( () -> new SchemaValidator().validate( metadata ) )
						.isInstanceOf( SchemaManagementException.class )
						.hasMessageContaining( "wrong column type" );
			}
			finally {
				new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
			}
		}
	}

	private static String physicalNclobType(Statement statement) throws SQLException {
		try ( var rows = statement.executeQuery( "values cast(null as nclob)" ) ) {
			return rows.getMetaData().getColumnTypeName( 1 );
		}
		catch (SQLException e) {
			if ( "560AA".equals( e.getSQLState() ) ) {
				return null; // National types require a Unicode LUW database.
			}
			throw e;
		}
	}

	private static void createThenValidate(Class<?> entityClass) {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistry();
		try {
			if ( DialectContext.getDialectClass() == DB2Dialect.class ) {
				JdbcUtils.withConnection( registry, connection -> {
					try ( var statement = connection.createStatement() ) {
						assumeTrue( physicalNclobType( statement ) != null, "National types require a Unicode LUW database" );
					}
				} );
			}
			final Metadata metadata = new MetadataSources( registry ).addAnnotatedClass( entityClass ).buildMetadata();
			new SchemaExport().create( EnumSet.of( TargetType.DATABASE ), metadata );
			try {
				new SchemaValidator().validate( metadata );
			}
			finally {
				new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Entity(name = "NVarcharEntity")
	@Table(name = "t_nvarchar_db2nchar")
	public static class NVarcharEntity {
		@Id
		Long id;

		@Nationalized
		@Column(length = 100)
		String val;
	}

	@Entity(name = "NCharEntity")
	@Table(name = "t_nchar_db2nchar")
	public static class NCharEntity {
		@Id
		Long id;

		@JdbcTypeCode(SqlTypes.NCHAR)
		@Column(length = 10)
		String val;
	}

	@Entity(name = "NClobEntity")
	@Table(name = "t_nclob_db2nchar")
	public static class NClobEntity {
		@Id
		Long id;

		@Nationalized
		@Lob
		String val;
	}
}
