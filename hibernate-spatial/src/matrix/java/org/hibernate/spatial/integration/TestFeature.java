package org.hibernate.spatial.integration;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Interface for persistent entities in
 * integration tests.
 *
 * @author Karel Maesen, Geovise BVBA
 *         creation-date: Oct 2, 2010
 */
public interface TestFeature {

	public Integer getId();

	public void setId(Integer id);

	public Geometry getGeom();

	public void setGeom(Geometry geom);

}
