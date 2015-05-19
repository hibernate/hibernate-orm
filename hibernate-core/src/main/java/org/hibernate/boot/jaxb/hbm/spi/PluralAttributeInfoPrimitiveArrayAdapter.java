/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.hbm.spi;

import java.util.Collections;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public abstract class PluralAttributeInfoPrimitiveArrayAdapter
		extends JaxbHbmToolingHintContainer
		implements PluralAttributeInfo {
	@Override
	public boolean isInverse() {
		return false;
	}

	@Override
	public JaxbHbmLazyWithExtraEnum getLazy() {
		return JaxbHbmLazyWithExtraEnum.FALSE;
	}

	public JaxbHbmOneToManyCollectionElementType getOneToMany() {
		return null;
	}

	@Override
	public JaxbHbmCompositeCollectionElementType getCompositeElement() {
		return null;
	}

	@Override
	public JaxbHbmManyToManyCollectionElementType getManyToMany() {
		return null;
	}

	@Override
	public JaxbHbmManyToAnyCollectionElementType getManyToAny() {
		return null;
	}

	@Override
	public List<JaxbHbmFilterType> getFilter() {
		return Collections.emptyList();
	}

	@Override
	public String getCascade() {
		return null;
	}
}
