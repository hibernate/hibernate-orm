package org.hibernate.spatial.helper;

import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;

import com.vividsolutions.jts.geom.Geometry;

/**
 * This <code>FinderStrategy</code> implementation returns the first
 * geometry-valued property.
 * 
 */
public class GeometryPropertyFinder implements FinderStrategy<String, ClassMetadata> {

	public String find(ClassMetadata metadata) throws FinderException {
		for (String prop : metadata.getPropertyNames()) {
			Type type = metadata.getPropertyType(prop);

			if (Geometry.class.isAssignableFrom(type.getReturnedClass())) {
				return prop;
			}
		}
		throw new FinderException(
				"Could not find a Geometry-valued property in "
						+ metadata.getEntityName());
	}
}