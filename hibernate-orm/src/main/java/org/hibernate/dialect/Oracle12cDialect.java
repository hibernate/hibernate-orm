/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.Oracle12cIdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.SQL2008StandardLimitHandler;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.MaterializedBlobType;
import org.hibernate.type.WrappedMaterializedBlobType;

/**
 * An SQL dialect for Oracle 12c.
 *
 * @author zhouyanming (zhouyanming@gmail.com)
 */
public class Oracle12cDialect extends Oracle10gDialect {
	public static final String PREFER_LONG_RAW = "hibernate.dialect.oracle.prefer_long_raw";

	public Oracle12cDialect() {
		super();
		getDefaultProperties().setProperty( Environment.BATCH_VERSIONED_DATA, "true" );
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		// account for Oracle's deprecated support for LONGVARBINARY...
		// 		prefer BLOB, unless the user opts out of it
		boolean preferLong = serviceRegistry.getService( ConfigurationService.class ).getSetting(
				PREFER_LONG_RAW,
				StandardConverters.BOOLEAN,
				false
		);

		if ( !preferLong ) {
			typeContributions.contributeType( MaterializedBlobType.INSTANCE, "byte[]", byte[].class.getName() );
			typeContributions.contributeType( WrappedMaterializedBlobType.INSTANCE, "Byte[]", Byte[].class.getName() );
		}
	}

	@Override
	protected void registerDefaultProperties() {
		super.registerDefaultProperties();
		getDefaultProperties().setProperty( Environment.USE_GET_GENERATED_KEYS, "true" );
	}

	@Override
	public LimitHandler getLimitHandler() {
		return SQL2008StandardLimitHandler.INSTANCE;
	}

	@Override
	public String getNativeIdentifierGeneratorStrategy() {
		return "sequence";
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new Oracle12cIdentityColumnSupport();
	}
}
