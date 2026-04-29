/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.util;

import org.hibernate.engine.jdbc.internal.TokenBasedFormatterImpl;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for the ANTLR-based token formatter
 *
 * @author Hibernate Team
 */
@BaseUnitTest
public class TokenBasedFormatterTest {

	private final TokenBasedFormatterImpl formatter = new TokenBasedFormatterImpl();

	@Test
	public void testComplexQuery() {
		final String sql = """
			-- Non-indented single line comment\n \
			with recent_customer_activity as (select \"o\".`customer_id`,COUNT(*) AS order_count,MAX(o.created_at) \
			AS last_order_date,SUM(o.total_amount) AS total_spent FROM orders o WHERE o.created_at >= \
			CURRENT_DATE - INTERVAL '12 months' GROUP BY o.customer_id HAVING SUM(o.total_amount) > 1000),/* Non-indented multi-line\n \
			comment */\nproduct_performance AS (SELECT oi.product_id,/* Indented multi-line\n \
			comment */\ntrim(both '' from [oi].description),AVG(oi.unit_price) \
			AS avg_price,SUM(oi.quantity) AS total_units_sold FROM order_items oi GROUP BY oi.product_id \
			HAVING SUM(oi.quantity) > 10) select c.customer_id,UPPER(c.full_name) AS customer_name, c.region,\
			rca.order_count,rca.total_spent,latest_order.order_id,latest_order.created_at AS latest_order_date,\
			pp.avg_price,pp.total_units_sold,ROUND(rca.total_spent / NULLIF(rca.order_count, 0),2) AS avg_order_value, \
			COUNT(DISTINCT s.shipment_id) AS shipment_count FROM customers c JOIN recent_customer_activity rca ON \
			rca.customer_id = c.customer_id LEFT JOIN orders o ON o.customer_id = c.customer_id AND o.status = 'COMPLETED' \
			LEFT JOIN shipments s ON s.order_id = o.order_id AND s.shipped_at IS NOT NULL LEFT JOIN LATERAL (SELECT  \
			o2.order_id,o2.created_at FROM orders o2 WHERE o2.customer_id = c.customer_id AND o2.status IN ('COMPLETED', \
			'SHIPPED') ORDER BY o2.created_at DESC FETCH FIRST 1 ROW ONLY) latest_order ON TRUE JOIN order_items oi ON oi.order_id = \
			o.order_id JOIN product_performance pp ON pp.product_id = oi.product_id AND pp.avg_price > 20 \
			WHERE c.is_active = TRUE \n-- Indented single line comment\n \
			AND EXISTS (SELECT 1 FROM customer_tags ct WHERE ct.customer_id = c.customer_id \
			AND ct.tag_name = 'VIP') GROUP BY c.customer_id,c.full_name,c.region,rca.order_count,rca.total_spent, \
			latest_order.order_id,latest_order.created_at,pp.avg_price,pp.total_units_sold HAVING COUNT(DISTINCT  \
			s.shipment_id) >= 2 ORDER BY rca.total_spent DESC,latest_order.created_at DESC OFFSET 10 ROWS FETCH \
			FIRST 5 ROWS WITH TIES;
		""";
		// Using concatenated String here, since the text block format includes *all* of the leading tabs, not just the
		// indentations resulting from the formatting.
		final String formattedSql =
				"-- Non-indented single line comment\n" +
				"WITH recent_customer_activity AS (\n" +
				"\tSELECT\n" +
				"\t\t\"o\".`customer_id`,\n" +
				"\t\tCOUNT(*) AS order_count,\n" +
				"\t\tMAX(o.created_at) AS last_order_date,\n" +
				"\t\tSUM(o.total_amount) AS total_spent\n" +
				"\tFROM orders o\n" +
				"\tWHERE o.created_at >= CURRENT_DATE - INTERVAL '12 months'\n" +
				"\tGROUP BY o.customer_id\n" +
				"\tHAVING SUM(o.total_amount) > 1000\n" +
				"),\n" +
				"/* Non-indented multi-line\n" +
				"comment */\n" +
				"product_performance AS (\n" +
				"\tSELECT\n" +
				"\t\toi.product_id,\n" +
				"\t\t/* Indented multi-line\n" +
				"\t\tcomment */\n" +
				"\t\tTRIM(BOTH '' FROM [oi].description),\n" +
				"\t\tAVG(oi.unit_price) AS avg_price,\n" +
				"\t\tSUM(oi.quantity) AS total_units_sold\n" +
				"\tFROM order_items oi\n" +
				"\tGROUP BY oi.product_id\n" +
				"\tHAVING SUM(oi.quantity) > 10\n" +
				")\n" +
				"SELECT\n" +
				"\tc.customer_id,\n" +
				"\tUPPER(c.full_name) AS customer_name,\n" +
				"\tc.region,\n" +
				"\trca.order_count,\n" +
				"\trca.total_spent,\n" +
				"\tlatest_order.order_id,\n" +
				"\tlatest_order.created_at AS latest_order_date,\n" +
				"\tpp.avg_price,\n" +
				"\tpp.total_units_sold,\n" +
				"\tROUND(rca.total_spent / NULLIF(rca.order_count, 0), 2) AS avg_order_value,\n" +
				"\tCOUNT(DISTINCT s.shipment_id) AS shipment_count\n" +
				"FROM customers c\n" +
				"JOIN recent_customer_activity rca\n" +
				"\tON rca.customer_id = c.customer_id\n" +
				"LEFT JOIN orders o\n" +
				"\tON o.customer_id = c.customer_id\n" +
				"\tAND o.status = 'COMPLETED'\n" +
				"LEFT JOIN shipments s\n" +
				"\tON s.order_id = o.order_id\n" +
				"\tAND s.shipped_at IS NOT NULL\n" +
				"LEFT JOIN LATERAL (\n" +
				"\tSELECT o2.order_id, o2.created_at\n" +
				"\tFROM orders o2\n" +
				"\tWHERE o2.customer_id = c.customer_id\n" +
				"\t\tAND o2.status IN ('COMPLETED', 'SHIPPED')\n" +
				"\tORDER BY o2.created_at DESC\n" +
				"\tFETCH FIRST 1 ROW ONLY\n" +
				") latest_order\n" +
				"\tON TRUE\n" +
				"JOIN order_items oi\n" +
				"\tON oi.order_id = o.order_id\n" +
				"JOIN product_performance pp\n" +
				"\tON pp.product_id = oi.product_id\n" +
				"\tAND pp.avg_price > 20\n" +
				"WHERE c.is_active = TRUE\n" +
				"\t-- Indented single line comment\n" +
				"\tAND EXISTS (\n" +
				"\t\tSELECT 1\n" +
				"\t\tFROM customer_tags ct\n" +
				"\t\tWHERE ct.customer_id = c.customer_id\n" +
				"\t\t\tAND ct.tag_name = 'VIP'\n" +
				"\t)\n" +
				"GROUP BY\n" +
				"\tc.customer_id,\n" +
				"\tc.full_name,\n" +
				"\tc.region,\n" +
				"\trca.order_count,\n" +
				"\trca.total_spent,\n" +
				"\tlatest_order.order_id,\n" +
				"\tlatest_order.created_at,\n" +
				"\tpp.avg_price,\n" +
				"\tpp.total_units_sold\n" +
				"HAVING COUNT(DISTINCT s.shipment_id) >= 2\n" +
				"ORDER BY rca.total_spent DESC, latest_order.created_at DESC\n" +
				"OFFSET 10 ROWS\n" +
				"FETCH FIRST 5 ROWS\n" +
				"WITH TIES;";
		String actual = formatter.format( sql );

		assertEquals( formattedSql, actual );
	}

	@Test
	public void testEmptyString() {
		String formatted = formatter.format("");
		assertTrue(formatted.isEmpty() || formatted.isBlank(), "Empty string should remain empty");
	}

	@Test
	public void testNullString() {
		String formatted = formatter.format(null);
		assertTrue(formatted == null || formatted.isEmpty(), "Null should be handled gracefully");
	}

}
