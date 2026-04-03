/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.exporter.lint;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

/**
 * Analyzes entities for common mapping issues and generates a
 * text report. Replaces the old {@code HbmLintExporter} by working
 * with {@link ClassDetails} instead of {@code Metadata}.
 * <p>
 * Uses the same {@code lint/text-report.ftl} template as the old
 * exporter.
 *
 * @author Koen Aers
 */
public class HbmLintExporter {

	private static final String FTL_TEXT_REPORT = "lint/text-report.ftl";

	private final List<ClassDetails> entities;
	private final LintDetector[] detectors;
	private final Configuration freemarkerConfig;
	private final BeansWrapper beansWrapper;

	private HbmLintExporter(List<ClassDetails> entities,
							LintDetector[] detectors,
							String[] templatePath) {
		this.entities = entities;
		this.detectors = detectors;
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
		return new HbmLintExporter(entities, defaultDetectors(),
				new String[0]);
	}

	public static HbmLintExporter create(List<ClassDetails> entities,
										  String[] templatePath) {
		return new HbmLintExporter(entities, defaultDetectors(),
				templatePath);
	}

	public static HbmLintExporter create(List<ClassDetails> entities,
										  LintDetector[] detectors) {
		return new HbmLintExporter(entities, detectors, new String[0]);
	}

	private static LintDetector[] defaultDetectors() {
		return new LintDetector[] {
				new BadCachingDetector(),
				new ShadowedIdentifierDetector()
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
