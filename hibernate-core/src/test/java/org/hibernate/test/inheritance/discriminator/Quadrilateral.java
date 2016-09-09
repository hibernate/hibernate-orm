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
package org.hibernate.test.inheritance.discriminator;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

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
