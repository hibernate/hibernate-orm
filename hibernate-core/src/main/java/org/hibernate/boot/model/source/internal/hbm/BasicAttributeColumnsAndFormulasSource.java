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
import org.hibernate.boot.model.source.spi.SizeSource;

/**
 * ColumnAndFormulaSource implementation handling basic attribute mappings.
 *
 * @author Steve Ebersole
 */
public class BasicAttributeColumnsAndFormulasSource
		extends RelationalValueSourceHelper.AbstractColumnsAndFormulasSource
		implements RelationalValueSourceHelper.ColumnsAndFormulasSource {
	private final JaxbHbmBasicAttributeType basicAttributeMapping;

	public BasicAttributeColumnsAndFormulasSource(JaxbHbmBasicAttributeType basicAttributeMapping) {
		this.basicAttributeMapping = basicAttributeMapping;
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.PROPERTY;
	}

	@Override
	public String getSourceName() {
		return basicAttributeMapping.getName();
	}

	@Override
	public String getFormulaAttribute() {
		return basicAttributeMapping.getFormulaAttribute();
	}

	@Override
	public String getColumnAttribute() {
		return basicAttributeMapping.getColumnAttribute();
	}

	@Override
	public List getColumnOrFormulaElements() {
		return basicAttributeMapping.getColumnOrFormula();
	}

	@Override
	public SizeSource getSizeSource() {
		return Helper.interpretSizeSource(
				basicAttributeMapping.getLength(),
				basicAttributeMapping.getScale(),
				basicAttributeMapping.getPrecision()
		);
	}

	@Override
	public Boolean isNullable() {
		return basicAttributeMapping.isNotNull() == null
				? null
				: !basicAttributeMapping.isNotNull();
	}

	@Override
	public String getIndex() {
		return basicAttributeMapping.getIndex();
	}

	@Override
	public boolean isUnique() {
		return basicAttributeMapping.isUnique();
	}

	@Override
	public String getUniqueKey() {
		return basicAttributeMapping.getUniqueKey();
	}
}
