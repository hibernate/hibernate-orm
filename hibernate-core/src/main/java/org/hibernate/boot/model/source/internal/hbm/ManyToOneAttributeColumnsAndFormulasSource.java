/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;

/**
 * ColumnAndFormulaSource implementation handling many-to-one attribute mappings.
 *
 * @author Steve Ebersole
 */
public class ManyToOneAttributeColumnsAndFormulasSource extends RelationalValueSourceHelper.AbstractColumnsAndFormulasSource {
	private final JaxbHbmManyToOneType manyToOneMapping;

	public ManyToOneAttributeColumnsAndFormulasSource(JaxbHbmManyToOneType manyToOneMapping) {
		this.manyToOneMapping = manyToOneMapping;
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.MANY_TO_ONE;
	}

	@Override
	public String getSourceName() {
		return manyToOneMapping.getName();
	}

	@Override
	public String getFormulaAttribute() {
		return manyToOneMapping.getFormulaAttribute();
	}

	@Override
	public String getColumnAttribute() {
		return manyToOneMapping.getColumnAttribute();
	}

	@Override
	public List getColumnOrFormulaElements() {
		return manyToOneMapping.getColumnOrFormula();
	}

	@Override
	public Boolean isNullable() {
		return manyToOneMapping.isNotNull() == null
				? null
				: !manyToOneMapping.isNotNull();
	}

	@Override
	public String getIndex() {
		return manyToOneMapping.getIndex();
	}

	@Override
	public boolean isUnique() {
		return manyToOneMapping.isUnique();
	}

	@Override
	public String getUniqueKey() {
		return manyToOneMapping.getUniqueKey();
	}
}
