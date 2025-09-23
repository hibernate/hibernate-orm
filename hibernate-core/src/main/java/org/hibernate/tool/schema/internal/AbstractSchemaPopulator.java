/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.tool.schema.internal.exec.ScriptSourceInputFromUrl;
import org.hibernate.tool.schema.internal.exec.ScriptSourceInputNonExistentImpl;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.GenerationTarget;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.SqlScriptCommandExtractor;

import java.net.URL;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_CHARSET_NAME;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_IMPORT_FILES;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_LOAD_SCRIPT_SOURCE;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_SKIP_DEFAULT_IMPORT_FILE;
import static org.hibernate.cfg.SchemaToolingSettings.JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.internal.util.config.ConfigurationHelper.getString;
import static org.hibernate.tool.schema.internal.Helper.applyScript;
import static org.hibernate.tool.schema.internal.Helper.interpretScriptSourceSetting;

/**
 * Handles population from {@value #DEFAULT_IMPORT_FILE} and other scripts.
 */
public abstract class AbstractSchemaPopulator {

	public static final String DEFAULT_IMPORT_FILE = "/import.sql";

	abstract ClassLoaderService getClassLoaderService();

	void applyImportSources(
			ExecutionOptions options,
			SqlScriptCommandExtractor commandExtractor,
			boolean format,
			Dialect dialect,
			GenerationTarget... targets) {
		final var formatter = getImportScriptFormatter(format);
		boolean hasDefaultImportFileScriptBeenExecuted = applyImportScript(
				options,
				commandExtractor,
				dialect,
				formatter,
				targets
		);
		applyImportFiles(
				options,
				commandExtractor,
				dialect,
				formatter,
				hasDefaultImportFileScriptBeenExecuted ? "" : getDefaultImportFile( options ),
				targets
		);
	}

	private String getDefaultImportFile(ExecutionOptions options) {
		return skipDefaultFileImport( options ) ? "" : DEFAULT_IMPORT_FILE;
	}

	private static boolean skipDefaultFileImport(ExecutionOptions options) {
		return getBoolean( HBM2DDL_SKIP_DEFAULT_IMPORT_FILE, options.getConfigurationValues() );
	}

	/**
	 * In principle, we should format the commands in the import script if the
	 * {@code format} parameter is {@code true}, and since it's supposed to be
	 * a list of DML statements, we should use the {@linkplain FormatStyle#BASIC
	 * basic DML formatter} to do that. However, in practice we don't really know
	 * much about what this file contains, and we have never formatted it in the
	 * past, so there's no compelling reason to start now. In fact, if we have
	 * lists of many {@code insert} statements on the same table, which is what
	 * we typically expect, it's probably better to not format.
	 */
	private static Formatter getImportScriptFormatter(boolean format) {
//		return format ? FormatStyle.BASIC.getFormatter() : FormatStyle.NONE.getFormatter();
		return FormatStyle.NONE.getFormatter();
	}

	/**
	 * Handles import scripts specified using
	 * {@link org.hibernate.cfg.SchemaToolingSettings#HBM2DDL_IMPORT_FILES}.
	 *
	 * @return {@code true} if the legacy {@linkplain #DEFAULT_IMPORT_FILE default import file}
	 *         was one of the listed imported files that were executed
	 */
	private boolean applyImportScript(
			ExecutionOptions options,
			SqlScriptCommandExtractor commandExtractor,
			Dialect dialect,
			Formatter formatter,
			GenerationTarget[] targets) {
		final Object importScriptSetting = getImportScriptSetting( options );
		if ( importScriptSetting != null ) {
			final var importScriptInput =
					interpretScriptSourceSetting( importScriptSetting,
							getClassLoaderService(), getCharsetName( options ) );
			applyScript(
					options,
					commandExtractor,
					dialect,
					importScriptInput,
					formatter,
					targets
			);
			return containsDefaultImportFile( importScriptInput, options );
		}
		else {
			return false;
		}
	}

	private boolean containsDefaultImportFile(ScriptSourceInput importScriptInput,ExecutionOptions options ) {
		if ( skipDefaultFileImport( options ) ) {
			return false;
		}
		else {
			final URL defaultImportFileUrl = getClassLoaderService().locateResource( DEFAULT_IMPORT_FILE );
			return defaultImportFileUrl != null && importScriptInput.containsScript( defaultImportFileUrl );
		}
	}

	/**
	 * Handles import scripts specified using
	 * {@link org.hibernate.cfg.SchemaToolingSettings#JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE}.
	 */
	private void applyImportFiles(
			ExecutionOptions options,
			SqlScriptCommandExtractor commandExtractor,
			Dialect dialect,
			Formatter formatter,
			String defaultImportFile,
			GenerationTarget[] targets) {
		final String[] importFiles =
				StringHelper.split( ",",
						getString( HBM2DDL_IMPORT_FILES, options.getConfigurationValues(), defaultImportFile ) );
		final String charsetName = getCharsetName( options );
		final var classLoaderService = getClassLoaderService();
		for ( String currentFile : importFiles ) {
			if ( !currentFile.isBlank() ) { //skip empty resource names
				applyScript(
						options,
						commandExtractor,
						dialect,
						interpretLegacyImportScriptSetting( currentFile.trim(), classLoaderService, charsetName ),
						formatter,
						targets
				);
			}
		}
	}

	private ScriptSourceInput interpretLegacyImportScriptSetting(
			String resourceName,
			ClassLoaderService classLoaderService,
			String charsetName) {
		try {
			final URL resourceUrl = classLoaderService.locateResource( resourceName );
			return resourceUrl == null
					? ScriptSourceInputNonExistentImpl.INSTANCE
					: new ScriptSourceInputFromUrl( resourceUrl, charsetName );
		}
		catch (Exception e) {
			throw new SchemaManagementException( "Error resolving legacy import resource : " + resourceName, e );
		}
	}

	/**
	 * @see org.hibernate.cfg.SchemaToolingSettings#HBM2DDL_CHARSET_NAME
	 */
	private static String getCharsetName(ExecutionOptions options) {
		return (String) options.getConfigurationValues().get( HBM2DDL_CHARSET_NAME );
	}

	/**
	 * @see org.hibernate.cfg.SchemaToolingSettings#JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE
	 *
	 * @return a {@link java.io.Reader} or a string URL
	 */
	private static Object getImportScriptSetting(ExecutionOptions options) {
		final var configuration = options.getConfigurationValues();
		final Object importScriptSetting = configuration.get( HBM2DDL_LOAD_SCRIPT_SOURCE );
		return importScriptSetting == null
				? configuration.get( JAKARTA_HBM2DDL_LOAD_SCRIPT_SOURCE )
				: importScriptSetting;
	}
}
