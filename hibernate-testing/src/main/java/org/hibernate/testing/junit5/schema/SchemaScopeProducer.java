/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5.schema;


import org.hibernate.testing.junit5.template.TestParameter;
import org.hibernate.testing.junit5.template.TestScopeProducer;

/**
 * @author Andrea Boriero
 */
public interface SchemaScopeProducer extends TestScopeProducer<SchemaScope, String> {
	@Override
	SchemaScope produceTestScope(TestParameter<String> metadataExtractionStrategy);
}
