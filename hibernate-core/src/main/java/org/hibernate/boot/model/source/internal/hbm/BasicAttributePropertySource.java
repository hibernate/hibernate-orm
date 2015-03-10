/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
