/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;
import java.util.Set;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;

import static org.hibernate.internal.util.StringHelper.splitAtCommas;

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
	public Set<String> getIndexConstraintNames() {
		return Set.of( splitAtCommas( manyToOneMapping.getIndex() ) );
	}

	@Override
	public boolean isUnique() {
		return manyToOneMapping.isUnique();
	}

	@Override
	public Set<String> getUniqueKeyConstraintNames() {
		return Set.of( splitAtCommas( manyToOneMapping.getUniqueKey() ) );
	}
}
