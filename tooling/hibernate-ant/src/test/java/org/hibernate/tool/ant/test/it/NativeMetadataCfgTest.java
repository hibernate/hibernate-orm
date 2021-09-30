/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 * 
 * Copyright 2004-2020 Red Hat, Inc.
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
package org.hibernate.tool.ant.test.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.tools.ant.Project;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.ant.test.util.ProjectUtil;
import org.hibernate.tools.test.util.HibernateUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class NativeMetadataCfgTest {

	@TempDir
	Path tempDir;
	
	@Test
	public void testNativeMetadataExportCfg() throws Exception {
		File propertyFile = createPropertyFile();
		File buildXmlFile = createBuildXmlFile(
				propertyFile.getAbsolutePath(),
				getDestinationFolder());
		Project project = ProjectUtil.createProject(buildXmlFile);
		File generatedFile = new File(tempDir.toFile(), "hibernate.cfg.xml");
		assertFalse(generatedFile.exists());
		project.executeTarget("testNativeMetadataExportCfg");
		assertTrue(generatedFile.exists());
		String cfgXmlString = new String(Files.readAllBytes(generatedFile.toPath()));
		assertTrue(cfgXmlString.contains("hibernate.dialect"));
	}
	
	private String getDestinationFolder() {
		return tempDir.toFile().getAbsolutePath();
	}
	
	private File createPropertyFile() throws Exception {
		String propertyString = AvailableSettings.DIALECT + " ";
		propertyString += HibernateUtil.Dialect.class.getName() + "\n";
		propertyString += AvailableSettings.CONNECTION_PROVIDER + " ";
		propertyString += HibernateUtil.ConnectionProvider.class.getName() + "\n";
		File result = new File(tempDir.toFile(), "hibernate.properties");
		Files.write(result.toPath(), propertyString.getBytes());
		return result;
	}
	
	private File createBuildXmlFile(
			String propertyFile, 
			String destinationFolder) throws Exception {
		String buildXmlString = 
				"<project name='HibernateToolTaskTest'>                       " +
				"  <taskdef                                                   " +
                "      name='hibernatetool'                                   " +
				"      classname='org.hibernate.tool.ant.fresh.HibernateToolTask'/> " +
		        "  <target name='testNativeMetadataExportCfg'>                " +
				"    <hibernatetool>                                          " +
				"      <metadata                                              " +
				"          type='native'                                      " +
				"          propertyFile='" + propertyFile + "'/>              " +
				"      <exportCfg                                             " + 
				"          destinationFolder='" + destinationFolder + "'/>    " + 
				"    </hibernatetool>                                         " +
		        "  </target>                                                  " +
		        "</project>                                                   " ;
		File result = new File(tempDir.toFile(), "build.xml");
		Files.write(result.toPath(), buildXmlString.getBytes());
		return result;
	}

}
