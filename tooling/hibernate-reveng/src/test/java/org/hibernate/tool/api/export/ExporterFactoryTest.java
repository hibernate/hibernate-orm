/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 * 
 * Copyright 2018-2020 Red Hat, Inc.
 *
 * Licensed under the GNU Lesser General Public License (LGPL), 
 * version 2.1 or later (the "License").
 * You may not use this file except in compliance with the License.
 * You may read the licence in the 'lgpl.txt' file in the root folder of 
 * project or obtain a copy at
 *
 *     http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.api.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.hibernate.tool.internal.export.common.AbstractExporter;
import org.junit.jupiter.api.Test;

public class ExporterFactoryTest {
	
	@Test
	public void testCreateExporter() {
		try {
			ExporterFactory.createExporter("foobar");
			fail();
		} catch(Throwable t) {
			assertTrue(t.getMessage().contains("foobar"));
		}
		Exporter exporter = ExporterFactory.createExporter(
				"org.hibernate.tool.api.export.ExporterFactoryTest$TestExporter");
		assertNotNull(exporter);
		assertTrue(exporter instanceof TestExporter);
		exporter = ExporterFactory.createExporter(ExporterType.JAVA);
		assertNotNull(exporter);
		assertEquals(
				ExporterType.JAVA.className(), 
				exporter.getClass().getName());
	}
	
	public static class TestExporter extends AbstractExporter {
		@Override protected void doStart() {}
	}

}
