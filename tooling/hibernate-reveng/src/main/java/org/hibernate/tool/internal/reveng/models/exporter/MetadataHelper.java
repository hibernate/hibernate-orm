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
package org.hibernate.tool.internal.reveng.models.exporter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.persistence.Entity;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.metadata.NativeMetadataDescriptor;
import org.hibernate.tool.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.internal.reveng.models.builder.hbm.HbmClassDetailsBuilder;

/**
 * Extracts {@link ClassDetails} and {@link ModelsContext} from a
 * {@link MetadataDescriptor}.
 * <p>
 * For annotation-based and mapping.xml metadata, entities are extracted
 * from the {@link org.hibernate.models.spi.ClassDetailsRegistry}.
 * For hbm.xml-based metadata (via {@link NativeMetadataDescriptor}),
 * entities are built from the hbm.xml files using
 * {@link HbmClassDetailsBuilder}.
 *
 * @author Koen Aers
 */
public class MetadataHelper {

	private final List<ClassDetails> entityClassDetails;
	private final ModelsContext modelsContext;
	private final Metadata metadata;
	private final Map<String, Map<String, List<String>>> allClassMetaAttributes;
	private final Map<String, Map<String, Map<String, List<String>>>> allFieldMetaAttributes;

	private MetadataHelper(MetadataDescriptor md) {
		// Try descriptor-specific paths first to avoid calling
		// createMetadata() when not needed (hbm.xml files may use
		// types that are no longer resolvable in newer Hibernate versions).
		if (md instanceof RevengMetadataDescriptor rmd) {
			this.metadata = md.createMetadata();
			this.entityClassDetails = rmd.getEntityClassDetails();
			this.modelsContext = rmd.getModelsContext();
			this.allClassMetaAttributes = Collections.emptyMap();
			this.allFieldMetaAttributes = Collections.emptyMap();
		} else if (md instanceof NativeMetadataDescriptor nmd
				&& nmd.getMappingFiles() != null) {
			File[] hbmFiles = Stream.of(nmd.getMappingFiles())
					.filter(f -> f.getName().endsWith(".xml"))
					.toArray(File[]::new);
			if (hbmFiles.length > 0) {
				this.metadata = null;
				HbmClassDetailsBuilder builder = new HbmClassDetailsBuilder();
				this.entityClassDetails = builder.buildFromFiles(hbmFiles);
				this.modelsContext = builder.getModelsContext();
				this.allClassMetaAttributes = builder.getAllClassMetaAttributes();
				this.allFieldMetaAttributes = builder.getAllFieldMetaAttributes();
			} else {
				this.metadata = md.createMetadata();
				MetadataImpl metadataImpl = (MetadataImpl) metadata;
				ModelsContext registryContext = metadataImpl.getBootstrapContext()
						.getModelsContext();
				this.entityClassDetails = extractEntities(registryContext);
				this.modelsContext = registryContext;
				this.allClassMetaAttributes = Collections.emptyMap();
				this.allFieldMetaAttributes = Collections.emptyMap();
			}
		} else {
			this.metadata = md.createMetadata();
			MetadataImpl metadataImpl = (MetadataImpl) metadata;
			ModelsContext registryContext = metadataImpl.getBootstrapContext()
					.getModelsContext();
			this.entityClassDetails = extractEntities(registryContext);
			this.modelsContext = registryContext;
			this.allClassMetaAttributes = Collections.emptyMap();
			this.allFieldMetaAttributes = Collections.emptyMap();
		}
	}

	private static List<ClassDetails> extractEntities(ModelsContext registryContext) {
		List<ClassDetails> entities = new ArrayList<>();
		registryContext.getClassDetailsRegistry().forEachClassDetails(cd -> {
			if (cd.hasAnnotationUsage(Entity.class, registryContext)) {
				entities.add(cd);
			}
		});
		return entities;
	}

	public static MetadataHelper from(MetadataDescriptor md) {
		return new MetadataHelper(md);
	}

	public List<ClassDetails> getEntityClassDetails() {
		return entityClassDetails;
	}

	public ModelsContext getModelsContext() {
		return modelsContext;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public Map<String, Map<String, List<String>>> getAllClassMetaAttributes() {
		return allClassMetaAttributes;
	}

	public Map<String, List<String>> getClassMetaAttributes(String className) {
		return allClassMetaAttributes.getOrDefault(className, Collections.emptyMap());
	}

	public Map<String, Map<String, List<String>>> getFieldMetaAttributes(String className) {
		return allFieldMetaAttributes.getOrDefault(className, Collections.emptyMap());
	}
}
