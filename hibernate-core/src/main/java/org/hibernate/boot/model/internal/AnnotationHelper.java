/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.HashMap;
import java.util.List;

import org.hibernate.annotations.Parameter;
import org.hibernate.models.spi.AnnotationUsage;

import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;

/**
 * @author Steve Ebersole
 */
public class AnnotationHelper {
	public static HashMap<String, String> extractParameterMap(List<AnnotationUsage<Parameter>> parameters) {
		final HashMap<String,String> paramMap = mapOfSize( parameters.size() );
		parameters.forEach( (usage) -> {
			paramMap.put( usage.getString( "name" ), usage.getString( "value" ) );
		} );
		return paramMap;
	}
}
