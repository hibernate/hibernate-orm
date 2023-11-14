/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.dialect.*;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.descriptor.jdbc.*;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;

public class YugabyteDBDialect extends PostgreSQLDialect {

	protected final static DatabaseVersion MINIMUM_YUGABYTEDB_VERSION = DatabaseVersion.make( 2, 18 );

	public YugabyteDBDialect() {
		super(DatabaseVersion.make(2, 18));
	}

	public YugabyteDBDialect(DialectResolutionInfo info) {
		this( info, PostgreSQLDriverKind.determineKind( info ) );
	}

	public YugabyteDBDialect(DatabaseVersion version, PostgreSQLDriverKind driverKind) {
		super( version, driverKind );
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_YUGABYTEDB_VERSION;
	}

	protected void contributePostgreSQLTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();
		// For discussion of BLOB support in Postgres, as of 8.4, have a peek at
		// <a href="http://jdbc.postgresql.org/documentation/84/binary-data.html">http://jdbc.postgresql.org/documentation/84/binary-data.html</a>.
		// For the effects in regards to Hibernate see <a href="http://in.relation.to/15492.lace">http://in.relation.to/15492.lace</a>

		// Force BLOB binding.  Otherwise, byte[] fields annotated
		// with @Lob will attempt to use
		// BlobTypeDescriptor.PRIMITIVE_ARRAY_BINDING.  Since the
		// dialect uses oid for Blobs, byte arrays cannot be used.
		jdbcTypeRegistry.addDescriptor( Types.BLOB, BlobJdbcType.BLOB_BINDING );
		jdbcTypeRegistry.addDescriptor( Types.CLOB, ClobJdbcType.CLOB_BINDING );
		// Don't use this type due to https://github.com/pgjdbc/pgjdbc/issues/2862
		//jdbcTypeRegistry.addDescriptor( TimestampUtcAsOffsetDateTimeJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( XmlJdbcType.INSTANCE );

		if ( driverKind == PostgreSQLDriverKind.PG_JDBC ) {
			// HHH-9562
			jdbcTypeRegistry.addDescriptorIfAbsent( UUIDJdbcType.INSTANCE );
			if ( isUsable( serviceRegistry ) ) {
				jdbcTypeRegistry.addDescriptorIfAbsent( PgJdbcHelper.getInetJdbcType( serviceRegistry ) );
				jdbcTypeRegistry.addDescriptorIfAbsent( PgJdbcHelper.getIntervalJdbcType( serviceRegistry ) );
				jdbcTypeRegistry.addDescriptorIfAbsent( PgJdbcHelper.getStructJdbcType( serviceRegistry ) );
				jdbcTypeRegistry.addDescriptorIfAbsent( PgJdbcHelper.getJsonbJdbcType( serviceRegistry ) );
			}
			else {
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingInetJdbcType.INSTANCE );
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingIntervalSecondJdbcType.INSTANCE );
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLStructCastingJdbcType.INSTANCE );
				jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingJsonJdbcType.JSONB_INSTANCE );
			}
		}
		else {
			jdbcTypeRegistry.addDescriptorIfAbsent( UUIDJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingInetJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingIntervalSecondJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLStructCastingJdbcType.INSTANCE );
			jdbcTypeRegistry.addDescriptorIfAbsent( PostgreSQLCastingJsonJdbcType.JSONB_INSTANCE );
		}

		// PostgreSQL requires a custom binder for binding untyped nulls as VARBINARY
		typeContributions.contributeJdbcType( ObjectNullAsBinaryTypeJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType(
				new JavaObjectType(
						ObjectNullAsBinaryTypeJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.getDescriptor( Object.class )
				)
		);
	}

	@Override
	public String getQueryHintString(String sql, String hints) {
		return "/*+ " + hints + " */" + sql;
	}

	public static boolean isUsable(ServiceRegistry serviceRegistry) {
		final ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		try {
			classLoaderService.classForName( "com.yugabyte.util.PGobject" );
			return true;
		}
		catch (ClassLoadingException ex) {
			return false;
		}
	}
}
