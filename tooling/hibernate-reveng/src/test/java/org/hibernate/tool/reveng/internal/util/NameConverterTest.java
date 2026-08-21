/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NameConverterTest {

	// --- toUpperCamelCase ---

	@Test
	public void testToUpperCamelCaseSimple() {
		assertEquals("EmployeeName", NameConverter.toUpperCamelCase("employee_name"));
	}

	@Test
	public void testToUpperCamelCaseEmpty() {
		assertEquals("", NameConverter.toUpperCamelCase(""));
	}

	@Test
	public void testToUpperCamelCaseSingleWord() {
		assertEquals("Employee", NameConverter.toUpperCamelCase("employee"));
	}

	@Test
	public void testToUpperCamelCaseAllCaps() {
		assertEquals("EmployeeName", NameConverter.toUpperCamelCase("EMPLOYEE_NAME"));
	}

	@Test
	public void testToUpperCamelCaseWithSpaces() {
		assertEquals("EmployeeName", NameConverter.toUpperCamelCase("employee name"));
	}

	@Test
	public void testToUpperCamelCaseWithHyphens() {
		assertEquals("EmployeeName", NameConverter.toUpperCamelCase("employee-name"));
	}

	@Test
	public void testToUpperCamelCaseMultipleUnderscores() {
		assertEquals("AbcDef", NameConverter.toUpperCamelCase("abc__def"));
	}

	@Test
	public void testToUpperCamelCaseMixedCase() {
		assertEquals("EmployeeName", NameConverter.toUpperCamelCase("employeeName"));
	}

	@Test
	public void testToUpperCamelCaseSingleChar() {
		assertEquals("A", NameConverter.toUpperCamelCase("a"));
	}

	@Test
	public void testToUpperCamelCaseAlreadyCamelCase() {
		assertEquals("EmployeeName", NameConverter.toUpperCamelCase("EmployeeName"));
	}

	// --- simplePluralize ---

	@Test
	public void testSimplePluralizeRegular() {
		assertEquals("employees", NameConverter.simplePluralize("employee"));
	}

	@Test
	public void testSimplePluralizeEndingInS() {
		assertEquals("addresses", NameConverter.simplePluralize("address"));
	}

	@Test
	public void testSimplePluralizeEndingInX() {
		assertEquals("boxes", NameConverter.simplePluralize("box"));
	}

	@Test
	public void testSimplePluralizeEndingInConsonantY() {
		assertEquals("companies", NameConverter.simplePluralize("company"));
	}

	@Test
	public void testSimplePluralizeEndingInVowelY() {
		assertEquals("days", NameConverter.simplePluralize("day"));
	}

	@Test
	public void testSimplePluralizeEndingInCh() {
		assertEquals("watches", NameConverter.simplePluralize("watch"));
	}

	@Test
	public void testSimplePluralizeEndingInSh() {
		assertEquals("dishes", NameConverter.simplePluralize("dish"));
	}

	@Test
	public void testSimplePluralizeEndingInTh() {
		assertEquals("moths", NameConverter.simplePluralize("moth"));
	}

	// --- isReservedJavaKeyword ---

	@Test
	public void testIsReservedJavaKeywordTrue() {
		assertTrue(NameConverter.isReservedJavaKeyword("class"));
		assertTrue(NameConverter.isReservedJavaKeyword("int"));
		assertTrue(NameConverter.isReservedJavaKeyword("abstract"));
		assertTrue(NameConverter.isReservedJavaKeyword("synchronized"));
		assertTrue(NameConverter.isReservedJavaKeyword("volatile"));
	}

	@Test
	public void testIsReservedJavaKeywordFalse() {
		assertFalse(NameConverter.isReservedJavaKeyword("employee"));
		assertFalse(NameConverter.isReservedJavaKeyword("Class"));
		assertFalse(NameConverter.isReservedJavaKeyword("INT"));
		assertFalse(NameConverter.isReservedJavaKeyword(""));
	}
}
