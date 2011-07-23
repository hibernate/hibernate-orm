/*
 * $Id: GeometryFactoryHelper.java 200 2010-03-31 19:52:12Z maesenka $
 *
 * This file is part of Hibernate Spatial, an extension to the
 * hibernate ORM solution for geographic data.
 *
 * Copyright © 2007-2010 Geovise BVBA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, visit: http://www.hibernatespatial.org/
 */
package org.hibernate.spatial.helper;

import com.vividsolutions.jts.geom.PrecisionModel;
import org.hibernate.spatial.cfg.HSProperty;
import org.hibernate.spatial.mgeom.MGeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Factory for creating a <code>GeometryFactory</code> given a map of
 * configuration parameters.
 *
 * @author Karel Maesen, Geovise BVBA
 */
public class GeometryFactoryHelper {

    private static Logger logger = LoggerFactory.getLogger(GeometryFactoryHelper.class);

    public static MGeometryFactory createGeometryFactory(Map map) {

        if (map == null) {
            return new MGeometryFactory();
        }
        String precisionModelName = null;
        Double scale = null;
        if (map.containsKey(HSProperty.PRECISION_MODEL.toString())) {
            precisionModelName = (String) map.get(HSProperty.PRECISION_MODEL
                    .toString());
        }
        if (map.containsKey(HSProperty.PRECISION_MODEL_SCALE.toString())) {
            scale = Double.parseDouble(((String) map
                    .get(HSProperty.PRECISION_MODEL_SCALE.toString())));
        }
        if (scale != null && !scale.isNaN() && precisionModelName != null
                && precisionModelName.equalsIgnoreCase("FIXED")) {
            return new MGeometryFactory(new PrecisionModel(scale));
        }
        if (precisionModelName == null) {
            return new MGeometryFactory();
        }
        if (precisionModelName.equalsIgnoreCase("FIXED")) {
            return new MGeometryFactory(
                    new PrecisionModel(PrecisionModel.FIXED));
        }
        if (precisionModelName.equalsIgnoreCase("FLOATING")) {
            return new MGeometryFactory(new PrecisionModel(
                    PrecisionModel.FLOATING));
        }
        if (precisionModelName.equalsIgnoreCase("FLOATING_SINGLE")) {
            return new MGeometryFactory(new PrecisionModel(
                    PrecisionModel.FLOATING_SINGLE));
        }
        logger.warn("Configured for PrecisionModel: " + precisionModelName
                + " but don't know how to instantiate.");
        logger.warn("Reverting to default GeometryModel");
        return new MGeometryFactory();
    }

}
