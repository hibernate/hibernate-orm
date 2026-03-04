/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;

/**
 * Tests for {@link ManyToManyMetadata}.
 *
 * @author Koen Aers
 */
public class ManyToManyMetadataTest {

	@Test
	public void testConstructorAndDefaults() {
		ManyToManyMetadata mtm = new ManyToManyMetadata(
			"courses", "Course", "com.example");

		assertEquals("courses", mtm.getFieldName());
		assertEquals("Course", mtm.getTargetEntityClassName());
		assertEquals("com.example", mtm.getTargetEntityPackage());
		assertNull(mtm.getMappedBy());
		assertNull(mtm.getJoinTableName());
		assertNull(mtm.getJoinColumnName());
		assertNull(mtm.getInverseJoinColumnName());
		assertNull(mtm.getFetchType());
		assertNull(mtm.getCascadeTypes());
	}

	@Test
	public void testOwningSideWithJoinTable() {
		ManyToManyMetadata mtm = new ManyToManyMetadata(
			"courses", "Course", "com.example")
			.joinTable("STUDENT_COURSE", "STUDENT_ID", "COURSE_ID")
			.cascade(CascadeType.PERSIST);

		assertEquals("STUDENT_COURSE", mtm.getJoinTableName());
		assertEquals("STUDENT_ID", mtm.getJoinColumnName());
		assertEquals("COURSE_ID", mtm.getInverseJoinColumnName());
		assertArrayEquals(new CascadeType[]{ CascadeType.PERSIST }, mtm.getCascadeTypes());
	}

	@Test
	public void testInverseSideWithMappedBy() {
		ManyToManyMetadata mtm = new ManyToManyMetadata(
			"students", "Student", "com.example")
			.mappedBy("courses");

		assertEquals("courses", mtm.getMappedBy());
	}

	@Test
	public void testFetchType() {
		ManyToManyMetadata mtm = new ManyToManyMetadata(
			"courses", "Course", "com.example")
			.fetchType(FetchType.EAGER);

		assertEquals(FetchType.EAGER, mtm.getFetchType());
	}

	@Test
	public void testMultipleCascadeTypes() {
		ManyToManyMetadata mtm = new ManyToManyMetadata(
			"courses", "Course", "com.example")
			.cascade(CascadeType.PERSIST, CascadeType.MERGE);

		assertArrayEquals(
			new CascadeType[]{ CascadeType.PERSIST, CascadeType.MERGE },
			mtm.getCascadeTypes());
	}

	@Test
	public void testFluentChaining() {
		ManyToManyMetadata mtm = new ManyToManyMetadata(
			"courses", "Course", "com.example")
			.joinTable("STUDENT_COURSE", "STUDENT_ID", "COURSE_ID")
			.fetchType(FetchType.LAZY)
			.cascade(CascadeType.ALL);

		assertEquals("STUDENT_COURSE", mtm.getJoinTableName());
		assertEquals("STUDENT_ID", mtm.getJoinColumnName());
		assertEquals("COURSE_ID", mtm.getInverseJoinColumnName());
		assertEquals(FetchType.LAZY, mtm.getFetchType());
		assertArrayEquals(new CascadeType[]{ CascadeType.ALL }, mtm.getCascadeTypes());
	}
}
