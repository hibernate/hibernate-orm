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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.tools.ant.Project;
import org.hibernate.tool.ant.test.util.ProjectUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class NativeMetadataDdlTest {

	@TempDir
	Path tempDir;
	
	@Test
	public void testNativeMetadataExportCfg() throws Exception {
		File buildXmlFile = createBuildXmlFile();
		Project project = ProjectUtil.createProject(buildXmlFile);
		project.executeTarget("testNativeMetadataExportDdl");
	}
	
	private File createHbmXmlFile() {
		File result = new File(tempDir.toFile(), "ddl.hbm.xml");
		return result;
	}
	
	private File createBuildXmlFile() throws Exception {
		String hbmXmlFile = createHbmXmlFile().getAbsolutePath();
		String buildXmlString = 
				"<project name='NativeMetadataDdlTest'>                       " +
				"  <taskdef                                                   " +
                "      name='hibernatetool'                                   " +
				"      classname='org.hibernate.tool.ant.fresh.HibernateToolTask'/> " +
		        "  <target name='testNativeMetadataExportDdl'>                " +
				"    <hibernatetool>                                          " +
				"      <metadata                                              " +
				"          type='native'>                                     " +
				"        <fileset file='" + hbmXmlFile + "'/>                 " +
				"      </metadata>                                            " +
				"      <exportDdl/>                                           " + 
				"    </hibernatetool>                                         " +
		        "  </target>                                                  " +
		        "</project>                                                   " ;
		File result = new File(tempDir.toFile(), "build.xml");
		Files.write(result.toPath(), buildXmlString.getBytes());
		return result;
	}

}
