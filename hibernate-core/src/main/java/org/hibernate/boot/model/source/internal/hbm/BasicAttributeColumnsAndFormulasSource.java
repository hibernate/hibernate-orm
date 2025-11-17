/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;
import java.util.Set;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.model.source.spi.SizeSource;

import static org.hibernate.internal.util.StringHelper.splitAtCommas;

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
	public Set<String> getIndexConstraintNames() {
		return Set.of( splitAtCommas( basicAttributeMapping.getIndex() ) );
	}

	@Override
	public boolean isUnique() {
		return basicAttributeMapping.isUnique();
	}

	@Override
	public Set<String> getUniqueKeyConstraintNames() {
		return Set.of( splitAtCommas( basicAttributeMapping.getUniqueKey() ) );
	}
}
