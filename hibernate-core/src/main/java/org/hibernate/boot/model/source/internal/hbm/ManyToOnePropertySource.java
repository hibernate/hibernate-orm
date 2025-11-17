/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmToolingHintType;
import org.hibernate.tuple.GenerationTiming;

/**
 * PropertySource implementation handling many-to-one attribute mappings.
 *
 * @author Steve Ebersole
 */
public class ManyToOnePropertySource implements PropertySource {
	private final JaxbHbmManyToOneType manyToOneMapping;

	public ManyToOnePropertySource(JaxbHbmManyToOneType manyToOneMapping) {
		this.manyToOneMapping = manyToOneMapping;
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.MANY_TO_ONE;
	}

	@Override
	public String getName() {
		return manyToOneMapping.getName();
	}

	@Override
	public String getXmlNodeName() {
		return manyToOneMapping.getNode();
	}

	@Override
	public String getPropertyAccessorName() {
		return manyToOneMapping.getAccess();
	}

	@Override
	public String getCascadeStyleName() {
		return manyToOneMapping.getCascade();
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return null;
	}

	@Override
	public Boolean isInsertable() {
		return manyToOneMapping.isInsert();
	}

	@Override
	public Boolean isUpdatable() {
		return manyToOneMapping.isUpdate();
	}

	@Override
	public boolean isUsedInOptimisticLocking() {
		return manyToOneMapping.isOptimisticLock();
	}

	@Override
	public boolean isLazy() {
		return false;
	}

	@Override
	public List<JaxbHbmToolingHintType> getToolingHints() {
		return manyToOneMapping.getToolingHints();
	}
}
