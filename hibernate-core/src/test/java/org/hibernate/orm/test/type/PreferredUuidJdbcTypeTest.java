package org.hibernate.orm.test.type;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgreSQLUUIDJdbcType;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.CharJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel(annotatedClasses = { PreferredUuidJdbcTypeTest.EntityWithUuid.class })
@ServiceRegistry(settings = @Setting(name = AvailableSettings.PREFERRED_UUID_JDBC_TYPE, value = "CHAR"))
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(SQLServerDialect.class)
public class PreferredUuidJdbcTypeTest {

	@Test
	public void verifyMappingsUUid(SessionFactoryScope scope) {
		final MappingMetamodelImplementor mappingMetamodel = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel();
		final EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(
				EntityWithUuid.class );
		final JdbcTypeRegistry jdbcTypeRegistry = mappingMetamodel.getTypeConfiguration().getJdbcTypeRegistry();

		final JdbcType uuidJdbcType = mappingMetamodel.getTypeConfiguration()
				.getBasicTypeForJavaType( UUID.class )
				.getJdbcType();

		// default interval type set by a config property and should be `CHAR`
		assertThat( uuidJdbcType ).isEqualTo( CharJdbcType.INSTANCE );

		final JdbcType uuidType = jdbcTypeRegistry.getDescriptor( SqlTypes.UUID );
		assertThat( uuidType ).isOfAnyClassIn( UUIDJdbcType.class, PostgreSQLUUIDJdbcType.class );

		// a simple duration field with no overrides - so should be using a default JdbcType
		assertThat( entityDescriptor.findAttributeMapping( "uuid" )
							.getSingleJdbcMapping().getJdbcType() )
				.isEqualTo( uuidJdbcType );

		// a field that is using a @JdbcTypeCode annotation to override the JdbcType. Hence, the used JdbcType must match the one
		// set by the annotation.
		assertThat( entityDescriptor.findAttributeMapping( "uuidJdbcTypeCode" )
							.getSingleJdbcMapping().getJdbcType() )
				.isEqualTo( uuidType );
	}

	@Entity(name = "EntityWithUuid")
	@Table(name = "EntityWithUuid")
	public static class EntityWithUuid {
		@Id
		private Integer id;

		private UUID uuid;

		@JdbcTypeCode(SqlTypes.UUID)
		private UUID uuidJdbcTypeCode;

		public EntityWithUuid() {
		}
	}
}
