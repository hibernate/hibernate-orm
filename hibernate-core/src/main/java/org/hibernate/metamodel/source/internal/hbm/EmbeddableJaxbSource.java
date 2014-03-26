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
 * Describes the JAXB source (HBM) for embeddable (composite) information.  The
 * HBM XML schema unfortunately did not define these consistently between
 * {@code <component/>} and {@code <composite-id/>}, so this contract unifies
 * access to this information.
 *
 * @author Steve Ebersole
 */
public interface EmbeddableJaxbSource {
	public String getClazz();

	public String findParent();
	public String findTuplizer();

	// todo : ultimately would be nice to remove the distinction between:
	//		* getKeyPropertyElementList() / getPropertyElementList()
	//		* getKeyManyToOneElementList() / getManyToOneElementList()
	// the difference is really a matter of the "container", not the attribute

	public List<JaxbKeyPropertyElement> getKeyPropertyElementList();

	public List<JaxbKeyManyToOneElement> getKeyManyToOneElementList();

	public List<JaxbPropertyElement> getPropertyElementList();

	public List<JaxbManyToOneElement> getManyToOneElementList();

	public List<JaxbOneToOneElement> getOneToOneElementList();

	public List<JaxbComponentElement> getComponentElementList();

	public List<JaxbNestedCompositeElementElement> getNestedCompositeElementList();

	public List<JaxbDynamicComponentElement> getDynamicComponentElementList();

	public List<JaxbPropertiesElement> getPropertiesElementList();

	public List<JaxbAnyElement> getAnyElementList();

	public List<JaxbMapElement> getMapElementList();

	public List<JaxbSetElement> getSetElementList();

	public List<JaxbListElement> getListElementList();

	public List<JaxbBagElement> getBagElementList();

	public List<JaxbArrayElement> getArrayElementList();

	public List<JaxbPrimitiveArrayElement> getPrimitiveArrayElementList();
}
