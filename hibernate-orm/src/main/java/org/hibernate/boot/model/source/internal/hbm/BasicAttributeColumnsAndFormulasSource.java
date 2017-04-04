/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;
import java.util.Set;

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
	public Set<String> getIndexConstraintNames() {
		return CommaSeparatedStringHelper.split( basicAttributeMapping.getIndex() );
	}

	@Override
	public boolean isUnique() {
		return basicAttributeMapping.isUnique();
	}

	@Override
	public Set<String> getUniqueKeyConstraintNames() {
		return CommaSeparatedStringHelper.split( basicAttributeMapping.getUniqueKey() );
	}
}
