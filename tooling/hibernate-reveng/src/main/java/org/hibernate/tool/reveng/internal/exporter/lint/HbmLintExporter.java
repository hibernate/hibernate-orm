/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.lint;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.tool.reveng.api.export.Exporter;
import org.hibernate.tool.reveng.api.export.ExporterConstants;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;
import org.hibernate.tool.reveng.internal.util.MetadataHelper;

/**
 * Analyzes entities for common mapping issues and generates a
 * text report. Works with {@link ClassDetails} instead of
 * {@code Metadata}.
 * <p>
 * Uses the same {@code lint/text-report.ftl} template as the old
 * exporter.
 *
 * @author Koen Aers
 */
public class HbmLintExporter implements Exporter {

	private static final String FTL_TEXT_REPORT = "lint/text-report.ftl";

	private List<ClassDetails> entities;
	private Properties properties;
	private LintDetector[] detectors;
	private RelationalModelDetector[] relationalDetectors;
	private Configuration freemarkerConfig;
	private BeansWrapper beansWrapper;
	private Properties exporterProperties = new Properties();

	public HbmLintExporter() {}

	@Override
	public Properties getProperties() {
		return exporterProperties;
	}

	@Override
	public void start() {
		MetadataDescriptor md = (MetadataDescriptor)
				exporterProperties.get(ExporterConstants.METADATA_DESCRIPTOR);
		File destDir = (File)
				exporterProperties.get(ExporterConstants.DESTINATION_FOLDER);
		String[] templatePath = (String[])
				exporterProperties.get(ExporterConstants.TEMPLATE_PATH);
		if (templatePath == null) templatePath = new String[0];
		HbmLintExporter configured = create(md, templatePath);
		configured.export(destDir);
	}

	private HbmLintExporter(List<ClassDetails> entities,
							Properties properties,
							LintDetector[] detectors,
							RelationalModelDetector[] relationalDetectors,
							String[] templatePath) {
		this.entities = entities;
		this.properties = properties;
		this.detectors = detectors;
		this.relationalDetectors = relationalDetectors;
		this.beansWrapper = new BeansWrapperBuilder(
				Configuration.VERSION_2_3_33).build();
		this.freemarkerConfig =
				new Configuration(Configuration.VERSION_2_3_33);
		this.freemarkerConfig.setTemplateLoader(
				createTemplateLoader(templatePath));
		this.freemarkerConfig.setDefaultEncoding("UTF-8");
		this.freemarkerConfig.setTemplateExceptionHandler(
				TemplateExceptionHandler.RETHROW_HANDLER);
		this.freemarkerConfig.setObjectWrapper(beansWrapper);
	}

	public static HbmLintExporter create(List<ClassDetails> entities) {
		return new HbmLintExporter(entities, null, defaultDetectors(),
				new RelationalModelDetector[0], new String[0]);
	}

	public static HbmLintExporter create(List<ClassDetails> entities,
										String[] templatePath) {
		return new HbmLintExporter(entities, null, defaultDetectors(),
				new RelationalModelDetector[0], templatePath);
	}

	public static HbmLintExporter create(List<ClassDetails> entities,
										LintDetector[] detectors) {
		return new HbmLintExporter(entities, null, detectors,
				new RelationalModelDetector[0], new String[0]);
	}

	public static HbmLintExporter create(MetadataDescriptor md,
										String[] templatePath) {
		MetadataHelper helper = MetadataHelper.from(md);
		return new HbmLintExporter(helper.getEntityClassDetails(),
				md.getProperties(), defaultDetectors(),
				defaultRelationalDetectors(), templatePath);
	}

	private static LintDetector[] defaultDetectors() {
		return new LintDetector[] {
				new BadCachingDetector(),
				new ShadowedIdentifierDetector()
		};
	}

	private static RelationalModelDetector[] defaultRelationalDetectors() {
		return new RelationalModelDetector[] {
				new SchemaByMetaDataDetector()
		};
	}

	/**
	 * Runs all detectors and returns the collected issues.
	 */
	public List<Issue> analyze() {
		List<Issue> results = new ArrayList<>();
		IssueCollector collector = results::add;
		for (LintDetector detector : detectors) {
			detector.initialize(entities);
			detector.visit(collector);
		}
		if (relationalDetectors != null) {
			for (RelationalModelDetector detector : relationalDetectors) {
				detector.initialize(entities, properties);
				detector.visit(collector);
			}
		}
		return results;
	}

	/**
	 * Runs all detectors and generates the lint report file.
	 */
	public void export(File outputDirectory) {
		List<Issue> issues = analyze();
		File outputFile = new File(outputDirectory, "hbmlint-result.txt");
		outputFile.getParentFile().mkdirs();

		SimpleHash model = new SimpleHash(beansWrapper);
		model.put("lintissues", issues);

		try (Writer writer = new FileWriter(outputFile)) {
			Template template =
					freemarkerConfig.getTemplate(FTL_TEXT_REPORT);
			template.process(model, writer);
		}
		catch (IOException | TemplateException e) {
			throw new RuntimeException(
					"Failed to generate lint report to "
					+ outputFile, e);
		}
	}

	private TemplateLoader createTemplateLoader(String[] templatePath) {
		List<TemplateLoader> loaders = new ArrayList<>();
		if (templatePath != null) {
			for (String path : templatePath) {
				File dir = new File(path);
				if (dir.isDirectory()) {
					try {
						loaders.add(new FileTemplateLoader(dir));
					}
					catch (IOException e) {
						throw new RuntimeException(
								"Failed to create template loader"
								+ " for: " + path, e);
					}
				}
			}
		}
		loaders.add(new ClassTemplateLoader(
				getClass().getClassLoader(), "/"));
		return new MultiTemplateLoader(
				loaders.toArray(new TemplateLoader[0]));
	}
}
