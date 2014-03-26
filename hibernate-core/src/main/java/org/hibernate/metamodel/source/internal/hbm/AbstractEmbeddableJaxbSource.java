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
package org.hibernate.metamodel.source.internal.hbm;

import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbAnyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbArrayElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbBagElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbComponentElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbDynamicComponentElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbKeyManyToOneElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbKeyPropertyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbListElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbManyToOneElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbMapElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbNestedCompositeElementElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbOneToOneElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbPrimitiveArrayElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbPropertiesElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbPropertyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbSetElement;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEmbeddableJaxbSource implements EmbeddableJaxbSource {
	@Override
	public List<JaxbKeyPropertyElement> getKeyPropertyElementList() {
		return Collections.emptyList();
	}

	@Override
	public List<JaxbKeyManyToOneElement> getKeyManyToOneElementList() {
		return Collections.emptyList();
	}

	@Override
	public List<JaxbPropertyElement> getPropertyElementList() {
		return Collections.emptyList();
	}

	@Override
	public List<JaxbManyToOneElement> getManyToOneElementList() {
		return Collections.emptyList();
	}

	@Override
	public List<JaxbOneToOneElement> getOneToOneElementList() {
		return Collections.emptyList();
	}

	@Override
	public List<JaxbComponentElement> getComponentElementList() {
		return Collections.emptyList();
	}

	@Override
	public List<JaxbNestedCompositeElementElement> getNestedCompositeElementList() {
		return Collections.emptyList();
	}

	@Override
	public List<JaxbDynamicComponentElement> getDynamicComponentElementList() {
		return Collections.emptyList();
	}

	@Override
	public List<JaxbPropertiesElement> getPropertiesElementList() {
		return Collections.emptyList();
	}

	@Override
	public List<JaxbAnyElement> getAnyElementList() {
		return Collections.emptyList();
	}

	@Override
	public List<JaxbMapElement> getMapElementList() {
		return Collections.emptyList();
	}

	@Override
	public List<JaxbSetElement> getSetElementList() {
		return Collections.emptyList();
	}

	@Override
	public List<JaxbListElement> getListElementList() {
		return Collections.emptyList();
	}

	@Override
	public List<JaxbBagElement> getBagElementList() {
		return Collections.emptyList();
	}

	@Override
	public List<JaxbArrayElement> getArrayElementList() {
		return Collections.emptyList();
	}

	@Override
	public List<JaxbPrimitiveArrayElement> getPrimitiveArrayElementList() {
		return Collections.emptyList();
	}
}
