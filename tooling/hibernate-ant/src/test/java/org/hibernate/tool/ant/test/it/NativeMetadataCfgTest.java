package org.hibernate.tool.ant.test.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.tools.ant.Project;
import org.hibernate.tool.ant.test.util.ProjectUtil;
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
	}
	
	private String getDestinationFolder() {
		return tempDir.toFile().getAbsolutePath();
	}
	
	private File createPropertyFile() throws Exception {
		String propertyString = "hibernate.dialect=H2";
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
				"      classname='org.hibernate.tool.ant.HibernateToolTask'/> " +
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
