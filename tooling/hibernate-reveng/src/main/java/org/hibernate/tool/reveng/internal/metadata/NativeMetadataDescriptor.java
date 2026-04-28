/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.reveng.api.metadata.MetadataDescriptor;

import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class NativeMetadataDescriptor implements MetadataDescriptor {

	private static final Logger log =
			Logger.getLogger(NativeMetadataDescriptor.class);

	private final Properties properties = new Properties();
	private final File cfgXmlFile;
	private final File[] mappingFiles;

	private final BootstrapServiceRegistry bootstrapServiceRegistry;

	// Exposed for legacy tests that add annotated classes via reflection
	@SuppressWarnings("unused")
	private final MetadataSources metadataSources;

	public NativeMetadataDescriptor(
			File cfgXmlFile,
			File[] mappingFiles,
			Properties properties) {
		this.cfgXmlFile = cfgXmlFile;
		this.mappingFiles = mappingFiles;
		if (properties != null) {
			this.properties.putAll(properties);
		}
		bootstrapServiceRegistry =
				new BootstrapServiceRegistryBuilder()
						.disableAutoClose()
						.build();
		metadataSources = new MetadataSources(bootstrapServiceRegistry);
		addMappingFiles(metadataSources);
	}

	public Properties getProperties() {
		Properties result = new Properties();
		result.putAll(properties);
		return result;
	}

	public Metadata createMetadata() {
		StandardServiceRegistryBuilder ssrb =
				new StandardServiceRegistryBuilder(bootstrapServiceRegistry);
		if (cfgXmlFile != null) {
			ssrb.configure(cfgXmlFile);
			addCfgXmlMappings(metadataSources, cfgXmlFile);
		}
		ssrb.applySettings(getProperties());
		return metadataSources.buildMetadata(ssrb.build());
	}

	public File getCfgXmlFile() {
		return cfgXmlFile;
	}

	public File[] getMappingFiles() {
		return mappingFiles;
	}

	private void addMappingFiles(MetadataSources sources) {
		if (mappingFiles != null) {
			for (File file : mappingFiles) {
				if (file.getName().endsWith(".jar")) {
					sources.addJar(file);
				}
		else {
					sources.addFile(file);
				}
			}
		}
	}

	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY =
			createDocumentBuilderFactory();

	private static DocumentBuilderFactory createDocumentBuilderFactory() {
		try {
			DocumentBuilderFactory factory =
					DocumentBuilderFactory.newInstance(
							"com.sun.org.apache.xerces.internal"
							+ ".jaxp.DocumentBuilderFactoryImpl",
							null);
			factory.setFeature(
					"http://apache.org/xml/features/"
					+ "nonvalidating/load-external-dtd", false);
			return factory;
		}
		catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Parses the hibernate.cfg.xml and adds any {@code <mapping>}
	 * elements to the given MetadataSources.
	 * {@code StandardServiceRegistryBuilder.configure()} only
	 * processes properties from the cfg.xml — mapping references
	 * are ignored. This method fills that gap.
	 */
	private static void addCfgXmlMappings(MetadataSources sources,
										File cfgXml) {
		try {
			Document doc;
			try (FileInputStream fis = new FileInputStream(cfgXml)) {
				doc = DOCUMENT_BUILDER_FACTORY
						.newDocumentBuilder().parse(fis);
			}
			NodeList mappings =
					doc.getElementsByTagName("mapping");
			for (int i = 0; i < mappings.getLength(); i++) {
				addMappingElement(sources,
						(Element) mappings.item(i));
			}
		}
		catch (Exception e) {
			log.warnf("Failed to parse cfg.xml for mapping"
					+ " resources: %s — %s",
					cfgXml, e.getMessage());
		}
	}

	private static void addMappingElement(MetadataSources sources,
											Element mapping) {
		String resource = mapping.getAttribute("resource");
		if (resource != null && !resource.isEmpty()) {
			sources.addResource(resource);
		}
		String file = mapping.getAttribute("file");
		if (file != null && !file.isEmpty()) {
			sources.addFile(file);
		}
		String jar = mapping.getAttribute("jar");
		if (jar != null && !jar.isEmpty()) {
			sources.addJar(new File(jar));
		}
		String pkg = mapping.getAttribute("package");
		if (pkg != null && !pkg.isEmpty()) {
			sources.addPackage(pkg);
		}
		String className = mapping.getAttribute("class");
		if (className != null && !className.isEmpty()) {
			try {
				sources.addAnnotatedClass(
						Class.forName(className));
			}
			catch (ClassNotFoundException ignored) {
				// Class not on classpath — skip
			}
		}
	}

}
