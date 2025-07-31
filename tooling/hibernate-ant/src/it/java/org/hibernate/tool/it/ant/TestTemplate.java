package org.hibernate.tool.it.ant;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class TestTemplate {

    @TempDir
    private File projectDir;

    protected File getProjectDir() {
        return projectDir;
    }

    protected abstract String hibernateToolTaskXml();

    protected abstract String[] createDatabaseScript();

    protected String constructBuildXmlFileContents() {
        return buildXmlFileContents.replace("@hibernateToolTaskXml@", hibernateToolTaskXml());
    }

    protected void runAntBuild() {
        File buildXmlFile = new File(getProjectDir(), "build.xml");
        Project project = new Project();
        project.setBaseDir(getProjectDir());
        project.addBuildListener(getConsoleLogger());
        ProjectHelper.getProjectHelper().parse(project, buildXmlFile);
        project.executeTarget(project.getDefaultTarget());
    }

    protected void createBuildXmlFile() throws Exception {
        File buildXmlFile = new File(getProjectDir(), "build.xml");
        assertFalse(buildXmlFile.exists());
        Files.writeString(buildXmlFile.toPath(), constructBuildXmlFileContents());
        assertTrue(buildXmlFile.exists());
    }

    protected String constructJdbcConnectionString() {
        return "jdbc:h2:" + getProjectDir().getAbsolutePath() + "/database/test;AUTO_SERVER=TRUE";
    }

    protected void createDatabase() throws Exception {
        File databaseFile = new File(getProjectDir(), "database/test.mv.db");
        assertFalse(databaseFile.exists());
        assertFalse(databaseFile.isFile());
        Connection connection = DriverManager.getConnection(constructJdbcConnectionString());
        Statement statement = connection.createStatement();
        for (String sql : createDatabaseScript()) {
            statement.execute(sql);
        }
        statement.close();
        connection.close();
        assertTrue(databaseFile.exists());
        assertTrue(databaseFile.isFile());
    }

    private DefaultLogger getConsoleLogger() {
        DefaultLogger consoleLogger = new DefaultLogger();
        consoleLogger.setErrorPrintStream(System.err);
        consoleLogger.setOutputPrintStream(System.out);
        consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
        return consoleLogger;
    }

    private static final String buildXmlFileContents =
            "<project name='tutorial' default='reveng'>                           \n" +
            "    <taskdef                                                         \n" +
            "            name='hibernatetool'                                     \n" +
            "            classname='org.hibernate.tool.ant.HibernateToolTask'/>   \n" +
            "    <target name='reveng'>                                           \n" +
            "@hibernateToolTaskXml@" +
            "    </target>                                                        \n" +
            "</project>                                                           \n" ;

}
