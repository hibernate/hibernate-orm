/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.jdbc2cfg.ManyToMany;

import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.api.metadata.MetadataDescriptorFactory;
import org.hibernate.tool.api.reveng.RevengSettings;
import org.hibernate.tool.internal.metadata.RevengMetadataDescriptor;
import org.hibernate.tool.internal.reveng.strategy.AbstractStrategy;
import org.hibernate.tool.internal.reveng.strategy.DefaultStrategy;
import org.hibernate.tool.test.utils.JdbcUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author max
 * @author koen
 */
public class TestCase {

	@TempDir
	public File outputDir = new File("output");

	@BeforeEach
	public void setUp() {
		JdbcUtil.createDatabase(this);
	}

	@AfterEach
	public void tearDown() {
		JdbcUtil.dropDatabase(this);
	}

	@Test
	public void testNoManyToManyBiDirectional() {

        AbstractStrategy c = new DefaultStrategy();
        c.setSettings(new RevengSettings(c).setDetectManyToMany(false));
        List<ClassDetails> entities = ((RevengMetadataDescriptor) MetadataDescriptorFactory
        		.createReverseEngineeringDescriptor(c, null))
        		.getEntityClassDetails();

        ClassDetails project = findEntity(entities, "Project");
		assertNotNull(project);
		assertNotNull(findField(project, "worksOns"));
		// Non-ID fields count: worksOns + 2 others = 3
		long projectNonIdFields = project.getFields().stream()
				.filter(f -> !f.hasDirectAnnotationUsage(Id.class))
				.count();
		assertEquals(3, projectNonIdFields);
		assertNotNull(findIdField(project, "projectId"));

		ClassDetails employee = findEntity(entities, "Employee");
		assertNotNull(employee);
		assertNotNull(findField(employee, "worksOns"));
		assertNotNull(findField(employee, "employees"));
		assertNotNull(findField(employee, "employee"));
		long employeeNonIdFields = employee.getFields().stream()
				.filter(f -> !f.hasDirectAnnotationUsage(Id.class))
				.count();
		assertEquals(6, employeeNonIdFields);
		assertNotNull(findIdField(employee, "id"));

		ClassDetails worksOn = findEntity(entities, "WorksOn");
		assertNotNull(worksOn);
		FieldDetails worksOnProject = findField(worksOn, "project");
		assertNotNull(worksOnProject);
		assertTrue(worksOnProject.hasDirectAnnotationUsage(ManyToOne.class));
		FieldDetails worksOnEmployee = findField(worksOn, "employee");
		assertNotNull(worksOnEmployee);
		assertTrue(worksOnEmployee.hasDirectAnnotationUsage(ManyToOne.class));
		long worksOnNonIdFields = worksOn.getFields().stream()
				.filter(f -> !f.hasDirectAnnotationUsage(Id.class)
						&& !f.hasDirectAnnotationUsage(jakarta.persistence.EmbeddedId.class))
				.count();
		assertEquals(2, worksOnNonIdFields);
	}

	@Test
	public void testAutoCreation() {
		List<ClassDetails> entities = ((RevengMetadataDescriptor) MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null))
				.getEntityClassDetails();

        assertNull(findEntity(entities, "WorksOn"), "No middle class should be generated.");

        assertNotNull(findEntity(entities, "WorksOnContext"),
        		"Should create worksontext since one of the foreign keys is not part of pk");

        ClassDetails projectClass = findEntity(entities, "Project");
		assertNotNull(projectClass);

		ClassDetails employeeClass = findEntity(entities, "Employee");
		assertNotNull(employeeClass);

		assertNull(findField(projectClass, "worksOns"),
				"property worksOns should not exist on Project");
		assertNull(findField(employeeClass, "worksOns"),
				"property worksOns should not exist on Employee");

        FieldDetails projects = findField(employeeClass, "projects");
		assertNotNull(projects);
		assertTrue(projects.hasDirectAnnotationUsage(ManyToMany.class));
		FieldDetails employees = findField(projectClass, "employees");
		assertNotNull(employees);
		assertTrue(employees.hasDirectAnnotationUsage(ManyToMany.class));
	}

	@Test
	public void testFalsePositive() {
		List<ClassDetails> entities = ((RevengMetadataDescriptor) MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null))
				.getEntityClassDetails();
        assertNotNull(findEntity(entities, "NonMiddle"), "Middle class should be generated.");
	}

	@Test
	public void testBuildMappings() {
		List<ClassDetails> entities = ((RevengMetadataDescriptor) MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null))
				.getEntityClassDetails();
		assertNotNull(entities);
		assertFalse(entities.isEmpty());
	}

	@Test
	public void testGenerateAndReadable() {

		MetadataDescriptor metadataDescriptor = MetadataDescriptorFactory
				.createReverseEngineeringDescriptor(null, null);

		Exporter hme = ExporterFactory.createExporter(ExporterType.HBM);
		hme.getProperties().put(ExporterConstants.METADATA_DESCRIPTOR, metadataDescriptor);
		hme.getProperties().put(ExporterConstants.DESTINATION_FOLDER, outputDir);
		hme.start();

		assertFileAndExists( new File(outputDir, "Employee.hbm.xml") );
		assertFileAndExists( new File(outputDir, "Project.hbm.xml") );
		assertFileAndExists( new File(outputDir, "WorksOnContext.hbm.xml") );

		assertFileAndExists( new File(outputDir, "RightTable.hbm.xml") );
		assertFileAndExists( new File(outputDir, "LeftTable.hbm.xml") );
		assertFileAndExists( new File(outputDir, "NonMiddle.hbm.xml") ); //Must be there since it has a fkey that is not part of the pk

		assertFalse(new File(outputDir, "WorksOn.hbm.xml").exists() );

		assertEquals(6, Objects.requireNonNull(outputDir.listFiles()).length);

		File[] files = new File[3];
		files[0] = new File(outputDir, "Employee.hbm.xml");
		files[1] = new File(outputDir, "Project.hbm.xml");
		files[2] = new File(outputDir, "WorksOnContext.hbm.xml");

		assertNotNull(MetadataDescriptorFactory
				.createNativeDescriptor(null, files, null)
				.createMetadata());

	}


	private void assertFileAndExists(File file) {
		assertTrue(file.exists(), file + " does not exist");
		assertTrue(file.isFile(), file + " not a file");
		assertTrue(file.length()>0, file + " does not have any contents");
	}

	private ClassDetails findEntity(List<ClassDetails> entities, String name) {
		return entities.stream()
				.filter(cd -> {
					String className = cd.getName();
					// Strip backticks if present
					if (className.startsWith("`") && className.endsWith("`")) {
						className = className.substring(1, className.length() - 1);
					}
					return className.equals(name) || className.equalsIgnoreCase(name);
				})
				.findFirst()
				.orElse(null);
	}

	private FieldDetails findField(ClassDetails classDetails, String fieldName) {
		return classDetails.getFields().stream()
				.filter(f -> f.getName().equals(fieldName))
				.findFirst()
				.orElse(null);
	}

	private FieldDetails findIdField(ClassDetails classDetails, String fieldName) {
		return classDetails.getFields().stream()
				.filter(f -> f.getName().equals(fieldName) && f.hasDirectAnnotationUsage(Id.class))
				.findFirst()
				.orElse(null);
	}

}
