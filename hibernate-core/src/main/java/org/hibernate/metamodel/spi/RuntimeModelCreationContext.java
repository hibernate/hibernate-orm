/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.spi;

import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.Map;

/**
 * @author Steve Ebersole
 */
public interface RuntimeModelCreationContext {
	SessionFactoryImplementor getSessionFactory();

	BootstrapContext getBootstrapContext();

	MetadataImplementor getBootModel();

	MappingMetamodelImplementor getDomainModel();

	default TypeConfiguration getTypeConfiguration() {
		return getBootstrapContext().getTypeConfiguration();
	}

	default JavaTypeRegistry getJavaTypeRegistry() {
		return getTypeConfiguration().getJavaTypeRegistry();
	}

	default MetadataImplementor getMetadata() {
		return getBootModel();
	}

	SqmFunctionRegistry getFunctionRegistry();

	Map<String, Object> getSettings();

	Dialect getDialect();

	CacheImplementor getCache();

	SessionFactoryOptions getSessionFactoryOptions();

	JdbcServices getJdbcServices();

	SqlStringGenerationContext getSqlStringGenerationContext();

	ServiceRegistry getServiceRegistry();
}
