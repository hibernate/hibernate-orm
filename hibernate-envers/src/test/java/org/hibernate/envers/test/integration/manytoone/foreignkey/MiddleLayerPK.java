/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytoone.foreignkey;

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
