/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.testing.datareader;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.spatial.GeomCodec;
import org.hibernate.spatial.testing.AbstractExpectationsFactory;
import org.hibernate.spatial.testing.dialects.NativeSQLTemplates;
import org.hibernate.spatial.testing.dialects.PredicateRegexes;


/**
 * @author Karel Maesen, Geovise BVBA
 * creation-date: Sep 30, 2010
 */
@Deprecated
public abstract class TestSupport {

	//TODO -- make this abstract
	public NativeSQLTemplates templates() {
		return null;
	}

	//TODO -- make this abstract
	public PredicateRegexes predicateRegexes() {
		return null;
	}

	public Map<CommonSpatialFunction, String> hqlOverrides() {
		return new HashMap<>();
	}

	public enum TestDataPurpose {
		SpatialFunctionsData,
		StoreRetrieveData
	}

	public abstract TestData createTestData(TestDataPurpose purpose);

	public GeomCodec codec() {
		throw new NotYetImplementedFor6Exception();
	}

}
