/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect(H2Dialect.class)
@JiraKey(value = "HHH-8805")
@ServiceRegistry(settings = @Setting(name = HBM2DDL_AUTO, value = "none"))
@DomainModel(annotatedClasses = SchemaUpdateJoinColumnNoConstraintSecondaryTableTest.Parent.class)
public class SchemaUpdateJoinColumnNoConstraintSecondaryTableTest {

	// Expected script:
	//
	// create table Child (
	//		id bigint not null,
	//		some_fk bigint,
	//		primary key (id)
	// );
	//
	// create table Parent (
	//		id bigint not null,
	//		primary key (id)
	//	);

	private static final String DELIMITER = ";";

	@Test
	public void test(
			DomainModelScope modelScope,
			@TempDir File tmpDir) throws Exception {
		var output = new File( tmpDir, "update_script.sql" );

		var model = modelScope.getDomainModel();
		model.orderColumns( false );
		model.validate();

		new SchemaUpdate()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setDelimiter( DELIMITER )
				.setFormat( true )
				.execute( EnumSet.of( TargetType.SCRIPT ), model );

		String outputContent = new String(Files.readAllBytes(output.toPath()));
		Assertions.assertFalse( outputContent.toLowerCase().contains( "foreign key" ) );
	}

	@Entity(name = "Parent")
	@SecondaryTable(
			name = "ParentDetails",
			foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT)
	)
	public static class Parent {

		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
