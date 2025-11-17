/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytoone.foreignkey;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * @author Chris Cranford
 */
public class MiddleLayerPK implements Serializable {
	private Long rootLayer;
	private LocalDate validFrom;

	public Long getRootLayer() {
		return rootLayer;
	}

	public void setRootLayer(Long rootLayer) {
		this.rootLayer = rootLayer;
	}

	public LocalDate getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(LocalDate validFrom) {
		this.validFrom = validFrom;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		MiddleLayerPK that = (MiddleLayerPK) o;
		return Objects.equals( rootLayer, that.rootLayer ) &&
				Objects.equals( validFrom, that.validFrom );
	}

	@Override
	public int hashCode() {
		return Objects.hash( rootLayer, validFrom );
	}
}
