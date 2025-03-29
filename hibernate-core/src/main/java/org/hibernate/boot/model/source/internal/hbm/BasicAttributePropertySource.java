/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmToolingHintType;
import org.hibernate.tuple.GenerationTiming;

/**
 * PropertySource implementation handling basic attribute mappings.
 *
 * @author Steve Ebersole
 */
public class BasicAttributePropertySource implements PropertySource {
	private final JaxbHbmBasicAttributeType basicAttributeMapping;

	public BasicAttributePropertySource(JaxbHbmBasicAttributeType basicAttributeMapping) {
		this.basicAttributeMapping = basicAttributeMapping;
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.PROPERTY;
	}

	@Override
	public String getName() {
		return basicAttributeMapping.getName();
	}

	@Override
	public String getXmlNodeName() {
		return basicAttributeMapping.getNode();
	}

	@Override
	public String getPropertyAccessorName() {
		return basicAttributeMapping.getAccess();
	}

	@Override
	public String getCascadeStyleName() {
		return null;
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return basicAttributeMapping.getGenerated();
	}

	@Override
	public Boolean isInsertable() {
		return basicAttributeMapping.isInsert();
	}

	@Override
	public Boolean isUpdatable() {
		return basicAttributeMapping.isUpdate();
	}

	@Override
	public boolean isUsedInOptimisticLocking() {
		return basicAttributeMapping.isOptimisticLock();
	}

	@Override
	public boolean isLazy() {
		return basicAttributeMapping.isLazy();
	}

	@Override
	public List<JaxbHbmToolingHintType> getToolingHints() {
		return basicAttributeMapping.getToolingHints();
	}
}
