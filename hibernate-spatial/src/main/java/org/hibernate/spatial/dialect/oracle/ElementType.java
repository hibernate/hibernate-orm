/*
 * This file is part of Hibernate Spatial, an extension to the
 *  hibernate ORM solution for spatial (geographic) data.
 *
 *  Copyright © 2007-2012 Geovise BVBA
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.hibernate.spatial.dialect.oracle;


/**
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Jul 1, 2010
 */
enum ElementType {
	UNSUPPORTED(0, true), POINT(1, 1), ORIENTATION(1, 0), POINT_CLUSTER(1,
			true), LINE_STRAITH_SEGMENTS(2, 1), LINE_ARC_SEGMENTS(2, 2), INTERIOR_RING_STRAIGHT_SEGMENTS(
			2003, 1), EXTERIOR_RING_STRAIGHT_SEGMENTS(1003, 1), INTERIOR_RING_ARC_SEGMENTS(
			2003, 2), EXTERIOR_RING_ARC_SEGMENTS(1003, 2), INTERIOR_RING_RECT(
			2003, 3), EXTERIOR_RING_RECT(1003, 3), INTERIOR_RING_CIRCLE(
			2003, 4), EXTERIOR_RING_CIRCLE(1003, 4), COMPOUND_LINE(4, true), COMPOUND_EXTERIOR_RING(
			1005, true), COMPOUND_INTERIOR_RING(2005, true);

	private int etype;

	private int interpretation = 2;

	private boolean compound = false;

	private ElementType(int etype, int interp) {
		this.etype = etype;
		this.interpretation = interp;

	}

	private ElementType(int etype, boolean compound) {
		this.etype = etype;
		this.compound = compound;
	}

	public int getEType() {
		return this.etype;
	}

	public int getInterpretation() {
		return this.interpretation;
	}

	/**
	 * @return true, if the SDO_INTERPRETATION value is the number of points
	 *         or compounds in the element.
	 */
	public boolean isCompound() {
		return this.compound;
	}

	public boolean isLine() {
		return (etype == 2 || etype == 4);
	}

	public boolean isInteriorRing() {
		return (etype == 2003 || etype == 2005);
	}

	public boolean isExteriorRing() {
		return (etype == 1003 || etype == 1005);
	}

	public boolean isStraightSegment() {
		return (interpretation == 1);
	}

	public boolean isArcSegment() {
		return (interpretation == 2);
	}

	public boolean isCircle() {
		return (interpretation == 4);
	}

	public boolean isRect() {
		return (interpretation == 3);
	}

	public static ElementType parseType(int etype, int interpretation) {
		for (ElementType t : values()) {
			if (t.etype == etype) {
				if (t.isCompound()
						|| t.getInterpretation() == interpretation) {
					return t;
				}
			}
		}
		throw new RuntimeException(
				"Can't determine ElementType from etype:" + etype
						+ " and interp.:" + interpretation);
	}

}