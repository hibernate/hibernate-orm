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
