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
	public void testToUpperCamelCaseWithDashes() {
		assertEquals("EmployeeName", NameConverter.toUpperCamelCase("employee-name"));
	}

	@Test
	public void testToUpperCamelCaseMixedCase() {
		assertEquals("EmployeeName", NameConverter.toUpperCamelCase("employeeName"));
	}

	@Test
	public void testSimplePluralizeSuffix() {
		assertEquals("dogs", NameConverter.simplePluralize("dog"));
	}

	@Test
	public void testSimplePluralizeEndingInS() {
		assertEquals("busses", NameConverter.simplePluralize("buss"));
	}

	@Test
	public void testSimplePluralizeEndingInX() {
		assertEquals("boxes", NameConverter.simplePluralize("box"));
	}

	@Test
	public void testSimplePluralizeEndingInConsonantY() {
		assertEquals("cities", NameConverter.simplePluralize("city"));
	}

	@Test
	public void testSimplePluralizeEndingInVowelY() {
		assertEquals("days", NameConverter.simplePluralize("day"));
	}

	@Test
	public void testSimplePluralizeEndingInCh() {
		assertEquals("churches", NameConverter.simplePluralize("church"));
	}

	@Test
	public void testSimplePluralizeEndingInSh() {
		assertEquals("bushes", NameConverter.simplePluralize("bush"));
	}

	@Test
	public void testSimplePluralizeEndingInH() {
		assertEquals("moths", NameConverter.simplePluralize("moth"));
	}

	@Test
	public void testIsReservedJavaKeyword() {
		assertTrue(NameConverter.isReservedJavaKeyword("class"));
		assertTrue(NameConverter.isReservedJavaKeyword("int"));
		assertTrue(NameConverter.isReservedJavaKeyword("for"));
		assertTrue(NameConverter.isReservedJavaKeyword("while"));
		assertFalse(NameConverter.isReservedJavaKeyword("foo"));
		assertFalse(NameConverter.isReservedJavaKeyword("String"));
	}
}
