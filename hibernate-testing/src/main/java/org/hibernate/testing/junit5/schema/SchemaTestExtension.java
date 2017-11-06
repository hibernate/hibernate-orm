/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5.schema;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;


import org.hibernate.tool.schema.JdbcMetadaAccessStrategy;

import org.hibernate.testing.junit5.template.TestParameter;
import org.hibernate.testing.junit5.template.TestTemplateExtension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

/**
 * @author Andrea Boriero
 */

public class SchemaTestExtension
		extends TestTemplateExtension {

	public static final List<String> METADATA_ACCESS_STRATEGIES = Arrays.asList( JdbcMetadaAccessStrategy.INDIVIDUALLY.toString(), JdbcMetadaAccessStrategy.GROUPED.toString());

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		return true;
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
			ExtensionContext context) {
		return METADATA_ACCESS_STRATEGIES.stream().map( metadataAccessStrategy ->  invocationContext( new SchemaTestParameter( (metadataAccessStrategy))) );
	}

	private TestTemplateInvocationContext invocationContext(SchemaTestParameter parameter) {
		return new CustomTestTemplateInvocationContext( parameter, SchemaScope.class );
	}

	public class SchemaTestParameter
			implements TestParameter<String> {
		private final String metadataExtractionStartegy;

		public SchemaTestParameter(String metadataExtractionStartegy) {
			this.metadataExtractionStartegy = metadataExtractionStartegy;
		}

		@Override
		public String getValue() {
			return metadataExtractionStartegy;
		}
	}
}
