/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure.internal;

import java.util.Map;

import org.hibernate.query.QueryParameter;

/**
 * @author Steve Ebersole
 */
public class ParameterMetadataImpl extends org.hibernate.query.internal.ParameterMetadataImpl {
	public ParameterMetadataImpl(
			Map<String, QueryParameter> namedQueryParameters,
			Map<Integer, QueryParameter> positionalQueryParameters) {
		super( namedQueryParameters, positionalQueryParameters );
	}
}
