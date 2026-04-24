/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.doc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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

class DocTemplateRenderer {

	private static final Logger log =
			Logger.getLogger(DocTemplateRenderer.class.getName());

	private final Configuration freemarkerConfig;
	private final BeansWrapper beansWrapper;

	DocTemplateRenderer(String[] templatePath) {
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

	void processTemplate(
			Map<String, Object> params, String templateName,
			File outputFile, EntityDocHelper docHelper,
			EntityDocFileManager docFileManager) {
		SimpleHash model = new SimpleHash(beansWrapper);
		model.put("dochelper", docHelper);
		model.put("docFileManager", docFileManager);
		model.put("jdk5", true);
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			model.put(entry.getKey(), entry.getValue());
		}
		try (Writer writer = new FileWriter(outputFile)) {
			Template template =
					freemarkerConfig.getTemplate(templateName);
			template.process(model, writer);
		}
		catch (IOException | TemplateException e) {
			throw new RuntimeException(
					"Failed to process template " + templateName
					+ " to " + outputFile, e);
		}
	}

	boolean generateDot(
			File outputDirectory, String dotExecutable,
			EntityDocHelper docHelper,
			EntityDocFileManager docFileManager,
			String entityGraphTemplate, String tableGraphTemplate) {
		if (dotExecutable == null || dotExecutable.isEmpty()) {
			log.info("Skipping graph generation since dot executable"
					+ " is not specified.");
			return false;
		}
		File entityDotFile = new File(outputDirectory,
				"entities/entitygraph.dot");
		processTemplate(
				Map.of("entities", docHelper.getClasses()),
				entityGraphTemplate, entityDotFile,
				docHelper, docFileManager);
		File tableDotFile = new File(outputDirectory,
				"tables/tablegraph.dot");
		processTemplate(
				Map.of("tables", docHelper.getTables()),
				tableGraphTemplate, tableDotFile,
				docHelper, docFileManager);
		try {
			convertDotFiles(dotExecutable,
					entityDotFile, outputDirectory + "/entities/entitygraph");
			convertDotFiles(dotExecutable,
					tableDotFile, outputDirectory + "/tables/tablegraph");
			return true;
		}
		catch (IOException e) {
			log.log(Level.WARNING,
					"Skipping graph generation due to error", e);
			return false;
		}
	}

	private void convertDotFiles(
			String dotExecutable, File dotFile,
			String basePath) throws IOException {
		dotToFile(dotExecutable, dotFile.toString(), basePath + ".png");
		dotToFile(dotExecutable, dotFile.toString(), basePath + ".svg");
		dotToFile(dotExecutable, dotFile.toString(), basePath + ".cmapx");
	}

	private void dotToFile(
			String dotExecutable, String dotFileName,
			String outFileName) throws IOException {
		String format = outFileName.substring(
				outFileName.lastIndexOf('.') + 1);
		String exe = escapeFileName(dotExecutable);
		String[] cmd = {
				exe, "-T", format,
				escapeFileName(dotFileName),
				"-o", escapeFileName(outFileName)
		};
		Process p = Runtime.getRuntime().exec(cmd);
		try {
			InputStream errStream = p.getErrorStream();
			StringBuilder sb = new StringBuilder();
			int c;
			while ((c = errStream.read()) != -1) {
				sb.append((char) c);
			}
			errStream.close();
			if (!sb.isEmpty()) {
				log.warning(sb.toString());
			}
			int exitCode = p.waitFor();
			if (exitCode != 0) {
				log.warning("dot exited with code " + exitCode
						+ " for " + outFileName);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while running dot", e);
		}
	}

	static void copyResource(
			ClassLoader loader, String resourceName, File target) {
		try (InputStream is = loader.getResourceAsStream(resourceName)) {
			if (is == null) {
				throw new IllegalArgumentException(
						"Resource not found: " + resourceName);
			}
			try (FileOutputStream out = new FileOutputStream(target)) {
				byte[] buffer = new byte[4096];
				int bytesRead;
				while ((bytesRead = is.read(buffer)) != -1) {
					out.write(buffer, 0, bytesRead);
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException(
					"Failed to copy resource " + resourceName
					+ " to " + target, e);
		}
	}

	static String readFileContent(File file) {
		StringBuilder sb = new StringBuilder();
		try (BufferedReader in =
				new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = in.readLine()) != null) {
				sb.append(line).append(System.lineSeparator());
			}
		}
		catch (IOException ignored) {
		}
		return sb.toString();
	}

	private static String escapeFileName(String fileName) {
		if (System.getProperty("os.name", "").startsWith("Linux")) {
			return fileName;
		}
		return "\"" + fileName + "\"";
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
								"Failed to create template loader for: "
								+ path, e);
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
