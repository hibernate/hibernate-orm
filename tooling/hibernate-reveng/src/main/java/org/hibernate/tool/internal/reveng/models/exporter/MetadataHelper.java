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
import java.util.List;
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

	private MetadataHelper(MetadataDescriptor md) {
		this.metadata = md.createMetadata();
		MetadataImpl metadataImpl = (MetadataImpl) metadata;
		ModelsContext registryContext = metadataImpl.getBootstrapContext()
				.getModelsContext();
		List<ClassDetails> registryEntities = new ArrayList<>();
		registryContext.getClassDetailsRegistry().forEachClassDetails(cd -> {
			if (cd.hasAnnotationUsage(Entity.class, registryContext)) {
				registryEntities.add(cd);
			}
		});
		if (!registryEntities.isEmpty()) {
			this.entityClassDetails = registryEntities;
			this.modelsContext = registryContext;
		} else if (md instanceof RevengMetadataDescriptor rmd) {
			this.entityClassDetails = rmd.getEntityClassDetails();
			this.modelsContext = rmd.getModelsContext();
		} else if (md instanceof NativeMetadataDescriptor nmd
				&& nmd.getMappingFiles() != null) {
			File[] hbmFiles = Stream.of(nmd.getMappingFiles())
					.filter(f -> f.getName().endsWith(".xml"))
					.toArray(File[]::new);
			if (hbmFiles.length > 0) {
				HbmClassDetailsBuilder builder = new HbmClassDetailsBuilder();
				this.entityClassDetails = builder.buildFromFiles(hbmFiles);
				this.modelsContext = builder.getModelsContext();
			} else {
				this.entityClassDetails = registryEntities;
				this.modelsContext = registryContext;
			}
		} else {
			this.entityClassDetails = registryEntities;
			this.modelsContext = registryContext;
		}
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
}
