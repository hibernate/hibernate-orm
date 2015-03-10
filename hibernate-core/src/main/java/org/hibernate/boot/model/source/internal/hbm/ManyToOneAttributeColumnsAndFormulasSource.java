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
