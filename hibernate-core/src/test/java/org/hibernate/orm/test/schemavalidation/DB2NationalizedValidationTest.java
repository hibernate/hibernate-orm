package org.hibernate.orm.test.schemavalidation;

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
import org.hibernate.type.SqlTypes;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

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

	private static void createThenValidate(Class<?> entityClass) {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistry();
		try {
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
