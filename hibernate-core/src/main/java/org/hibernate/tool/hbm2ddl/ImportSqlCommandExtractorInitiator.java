package org.hibernate.tool.hbm2ddl;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.BasicServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Instantiates and configures an appropriate {@link ImportSqlCommandExtractor}. By default
 * {@link SingleLineSqlCommandExtractor} is used.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class ImportSqlCommandExtractorInitiator implements BasicServiceInitiator<ImportSqlCommandExtractor> {
	private static final String DEFAULT_EXTRACTOR = SingleLineSqlCommandExtractor.class.getName();
	public static final ImportSqlCommandExtractorInitiator INSTANCE = new ImportSqlCommandExtractorInitiator();

	@Override
	public ImportSqlCommandExtractor initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		String extractorClassName = (String) configurationValues.get( Environment.HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR );
		if ( StringHelper.isEmpty( extractorClassName ) ) {
			extractorClassName = DEFAULT_EXTRACTOR;
		}
		final ClassLoaderService classLoaderService = registry.getService( ClassLoaderService.class );
		return instantiateExplicitCommandExtractor( extractorClassName, classLoaderService );
	}

	private ImportSqlCommandExtractor instantiateExplicitCommandExtractor(String extractorClassName,
																		  ClassLoaderService classLoaderService) {
		try {
			return (ImportSqlCommandExtractor) classLoaderService.classForName( extractorClassName ).newInstance();
		}
		catch ( Exception e ) {
			throw new HibernateException(
					"Could not instantiate import sql command extractor [" + extractorClassName + "]", e
			);
		}
	}

	@Override
	public Class<ImportSqlCommandExtractor> getServiceInitiated() {
		return ImportSqlCommandExtractor.class;
	}
}
