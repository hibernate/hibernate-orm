/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.ArrayList;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.TruthValue;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.testing.TestForIssue;
import org.hibernate.tool.schema.extract.internal.ColumnInformationImpl;
import org.hibernate.tool.schema.extract.internal.ForeignKeyInformationImpl;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Emmanuel Duchastenier
 */
public class AbstractSchemaMigratorTest {

    @Test
    @TestForIssue(jiraKey = "HHH-13779")
    public void testForeignKeyPreExistenceDetectionIgnoresCaseForTableAndColumnName() {
        final AbstractSchemaMigrator schemaMigrator = new AbstractSchemaMigrator(null, null) {
            @Override
            protected NameSpaceTablesInformation performTablesMigration(Metadata metadata,
					DatabaseInformation existingDatabase, ExecutionOptions options, Dialect dialect,
					Formatter formatter, Set<String> exportIdentifiers, boolean tryToCreateCatalogs,
					boolean tryToCreateSchemas, Set<Identifier> exportedCatalogs, Namespace namespace,
					SqlStringGenerationContext sqlStringGenerationContext,
					GenerationTarget[] targets) { return null; }
        };

        final TableInformation existingTableInformation = mock(TableInformation.class);
        final ArrayList<ForeignKeyInformation.ColumnReferenceMapping> columnReferenceMappings = new ArrayList<>();

        final TableInformation destinationTableInformation = mock(TableInformation.class);
        doReturn(new QualifiedTableName(toIdentifier("catalog"), toIdentifier("schema"),
                toIdentifier("referenced_table"))) // Table name is lower case
                .when(destinationTableInformation).getName();
        columnReferenceMappings.add(new ForeignKeyInformationImpl.ColumnReferenceMappingImpl(
                new ColumnInformationImpl(null, toIdentifier("referencing_column"), // column name is lower case
                        0, "typeName", 255, 0, TruthValue.TRUE),
                new ColumnInformationImpl(destinationTableInformation, null, 1, "typeName", 0, 0, TruthValue.TRUE)));
        doReturn(singletonList(new ForeignKeyInformationImpl(toIdentifier("FKp8mpamfw2inhj88hwhty1eipm"), columnReferenceMappings)))
                .when(existingTableInformation).getForeignKeys();

        final boolean existInDatabase = schemaMigrator.equivalentForeignKeyExistsInDatabase(
                existingTableInformation,
                "REFERENCING_COLUMN", "REFERENCED_TABLE"); // Table and column names are UPPER-case here, to prove the test

        assertThat("Expected ForeignKey pre-existence check to be case-insensitive",
                existInDatabase,
                is(true));
    }

}