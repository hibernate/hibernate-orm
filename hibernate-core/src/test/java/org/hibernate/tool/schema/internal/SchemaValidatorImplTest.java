/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import static org.junit.Assert.assertTrue;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace.Name;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Dominique Toupin
 */
@TestForIssue(jiraKey = "HHH-10332")
public class SchemaValidatorImplTest {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, SchemaValidatorImplTest.class.getName() ) );

	@Test
	public void testMissingEntityContainsQualifiedEntityName() throws Exception {

		StandardServiceRegistryBuilder srb = new StandardServiceRegistryBuilder();

		final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( srb.build() )
				.addAnnotatedClass( MissingEntity.class )
				.buildMetadata();

		try {
			new SchemaValidatorImpl( new H2Dialect() ).doValidation( metadata, new DatabaseInformation() {

				@Override
				public boolean schemaExists(Name schema) {
					return false;
				}

				@Override
				public void registerTable(TableInformation tableInformation) {
				}

				@Override
				public TableInformation getTableInformation(QualifiedTableName tableName) {
					return null;
				}

				@Override
				public TableInformation getTableInformation(Name schemaName, Identifier tableName) {
					return null;
				}

				@Override
				public TableInformation getTableInformation(Identifier catalogName, Identifier schemaName, Identifier tableName) {
					return null;
				}

				@Override
				public SequenceInformation getSequenceInformation(QualifiedSequenceName sequenceName) {
					return null;
				}

				@Override
				public SequenceInformation getSequenceInformation(Name schemaName, Identifier sequenceName) {
					return null;
				}

				@Override
				public SequenceInformation getSequenceInformation(Identifier catalogName, Identifier schemaName, Identifier sequenceName) {
					return null;
				}

				@Override
				public void cleanup() {
				}

				@Override
				public boolean catalogExists(Identifier catalog) {
					return false;
				}
			} );

			Assert.fail( "SchemaManagementException expected" );
		}
		catch (SchemaManagementException e) {
			assertTrue( "Expected qualified table name in SchemaManagementException exception message but got: " + e.getMessage(),
					e.getMessage().matches(
							".*\\b\\Q" + metadata.getEntityBinding( MissingEntity.class.getName() ).getTable().getQualifiedTableName() + "\\E\\b.*" ) );
		}
	}

	@Entity
	@PrimaryKeyJoinColumn
	@Table(name = "MissingEntity", catalog = "SomeCatalog", schema = "SomeSchema")
	public static class MissingEntity {

		private String id;

		@Id
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}

}
