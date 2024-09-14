/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Sub class for testing joined inheritance with a discriminator column.
 *
 * @author Etienne Miret
 */
@Entity
@DiscriminatorValue( "quadrilateral" )
public class Quadrilateral extends Polygon {

	private Double angleA;
	private Double angleB;
	private Double angleC;

	public Double getAngleA() {
		return angleA;
	}

	public void setAngleA(final Double angleA) {
		this.angleA = angleA;
	}

	public Double getAngleB() {
		return angleB;
	}

	public void setAngleB(final Double angleB) {
		this.angleB = angleB;
	}

	public Double getAngleC() {
		return angleC;
	}

	public void setAngleC(final Double angleC) {
		this.angleC = angleC;
	}

	/**
	 * Compute angle D.
	 *
	 * @return angle D.
	 * @throws NullPointerException if one of the other angles is not set.
	 */
	public Double getAngleD() {
		return new Double( Math.PI * 2 - angleA.doubleValue() - angleB.doubleValue() - angleC.doubleValue() );
	}

}
