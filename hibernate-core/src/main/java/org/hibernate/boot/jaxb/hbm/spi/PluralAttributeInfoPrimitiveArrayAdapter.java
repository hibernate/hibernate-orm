/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
