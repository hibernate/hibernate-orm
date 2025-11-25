package org.hibernate.tool.maven;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import org.apache.maven.cli.MavenCli;
import org.hibernate.tool.api.version.Version;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TransformHbmTestIT {

    public static final String MVN_HOME = "maven.multiModuleProjectDirectory";
    private static File baseFolder;
    private static File localRepo;
 
    @TempDir
    private Path projectPath;

    @BeforeAll
    public static void beforeAll() throws Exception {
        // The needed resource for this test are put in place
        // in the 'baseFolder' (normally 'target/test-classes')
        // by the 'build-helper-maven-plugin' execution.
        // See the 'pom.xml'
        baseFolder = determineBaseFolder();
        localRepo = new File(baseFolder.getParentFile(), "local-repo");
    }
    @Test
    public void testSimpleHbmTransformation() throws Exception {
    	System.setProperty(MVN_HOME, projectPath.toAbsolutePath().toString());
        writePomFile();
        copyHbmFile();  
        runTransformHbmToOrm();
     }

    private void writePomFile() throws Exception {
    	File pomFile = new File(projectPath.toFile(), "pom.xml");
    	assertFalse(pomFile.exists());
        Path pomPath = projectPath.resolve("pom.xml");
        Files.writeString(pomPath, simplePomContents);
        assertTrue(pomFile.exists());
    }
    
    private void copyHbmFile() throws Exception {
    	URL originUrl = TransformHbmTestIT.class.getResource("simple.hbm.xml");
    	assertNotNull(originUrl);
    	Path originPath = Paths.get(Objects.requireNonNull(originUrl).toURI());
    	File destinationDir = new File(projectPath.toFile(), "src/main/resources/");
    	assertTrue(destinationDir.mkdirs());
    	File destinationFile = new File(destinationDir, "simple.hbm.xml");
    	assertFalse(destinationFile.exists());
    	Files.copy(originPath, destinationFile.toPath());
    	assertTrue(destinationFile.exists());
    }
    
    private void runTransformHbmToOrm() throws Exception {
    	File destinationDir = new File(projectPath.toFile(), "src/main/resources/");
    	File ormXmlFile = new File(destinationDir, "simple.mapping.xml");
    	assertFalse(ormXmlFile.exists());
        new MavenCli().doMain(
                new String[]{
                		"-Dmaven.repo.local=" + localRepo.getAbsolutePath(),
                		"org.hibernate.tool:hibernate-tools-maven:" + Version.versionString() + ":hbm2orm", 
                		"generate-sources"},
                projectPath.toAbsolutePath().toString(),
                null,
                null);
        // Check the existaence of the transformed file
        assertTrue(ormXmlFile.exists());
        // Check if it's pretty printed
        assertTrue(Files.readString(ormXmlFile.toPath()).contains("\n        <table name=\"Foo\"/>\n"));
    }

    private static File determineBaseFolder() throws Exception {
    	URL classUrl = TransformHbmTestIT.class.getResource(
    			"/" + TransformHbmTestIT.class.getName().replace(".", "/") + ".class");
    	return new File(classUrl.toURI())
    			.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
    }

    private static final String simplePomContents =
            """
                   <project>
                       <modelVersion>4.0.0</modelVersion>
                       <groupId>org.hibernate.tool.maven.test</groupId>
                       <artifactId>simplest</artifactId>
                       <version>0.1-SNAPSHOT</version>
                   </project>
                   """;

}
