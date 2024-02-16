/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal.script;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Environment;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.spi.SqlScriptCommandExtractor;

/**
 * @author Steve Ebersole
 */
public class SqlScriptExtractorInitiator implements StandardServiceInitiator<SqlScriptCommandExtractor> {
	public static final SqlScriptExtractorInitiator INSTANCE = new SqlScriptExtractorInitiator();

	@Override
	public Class<SqlScriptCommandExtractor> getServiceInitiated() {
		return SqlScriptCommandExtractor.class;
	}

	@Override
	public SqlScriptCommandExtractor initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		final Object explicitSettingValue = configurationValues.get( Environment.HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR );

		if ( explicitSettingValue == null ) {
			return SingleLineSqlScriptExtractor.INSTANCE;
		}

		if ( explicitSettingValue instanceof SqlScriptCommandExtractor ) {
			return (SqlScriptCommandExtractor) explicitSettingValue;
		}

		final String explicitSettingName = explicitSettingValue.toString().trim();

		if ( explicitSettingName.isEmpty() || SingleLineSqlScriptExtractor.SHORT_NAME.equals( explicitSettingName ) ) {
			return SingleLineSqlScriptExtractor.INSTANCE;
		}
		else if ( MultiLineSqlScriptExtractor.SHORT_NAME.equals( explicitSettingName ) ) {
			return MultiLineSqlScriptExtractor.INSTANCE;
		}

		final ClassLoaderService classLoaderService = registry.requireService( ClassLoaderService.class );
		return instantiateExplicitCommandExtractor( explicitSettingName, classLoaderService );
	}

	private SqlScriptCommandExtractor instantiateExplicitCommandExtractor(
			String extractorClassName,
			ClassLoaderService classLoaderService) {
		try {
			return (SqlScriptCommandExtractor) classLoaderService.classForName( extractorClassName ).newInstance();
		}
		catch (Exception e) {
			throw new HibernateException(
					"Could not instantiate import sql command extractor [" + extractorClassName + "]", e
			);
		}
	}
}
