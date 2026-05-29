/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.nonjpaentity;

@NoSqlEntity
public class Product {
	Long id;
	String name;
	Double price;
}
