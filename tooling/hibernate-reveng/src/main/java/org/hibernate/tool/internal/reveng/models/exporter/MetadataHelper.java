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

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ModelsContext;

/**
 * Extracts {@link ClassDetails} and {@link ModelsContext} from a
 * Hibernate ORM {@link Metadata} instance.
 *
 * @author Koen Aers
 */
public class MetadataHelper {

	private final List<ClassDetails> entityClassDetails;
	private final ModelsContext modelsContext;

	private MetadataHelper(Metadata metadata) {
		MetadataImpl metadataImpl = (MetadataImpl) metadata;
		this.modelsContext = metadataImpl.getBootstrapContext()
				.getModelsContext();
		this.entityClassDetails = new ArrayList<>();
		modelsContext.getClassDetailsRegistry().forEachClassDetails(cd -> {
			if (cd.hasAnnotationUsage(Entity.class, modelsContext)) {
				entityClassDetails.add(cd);
			}
		});
	}

	public static MetadataHelper from(Metadata metadata) {
		return new MetadataHelper(metadata);
	}

	public List<ClassDetails> getEntityClassDetails() {
		return entityClassDetails;
	}

	public ModelsContext getModelsContext() {
		return modelsContext;
	}
}
