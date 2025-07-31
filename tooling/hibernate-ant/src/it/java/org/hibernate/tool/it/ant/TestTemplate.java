package org.hibernate.tool.it.ant;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.PrintStream;

public abstract class TestTemplate {

    @TempDir
    private File projectDir;

    protected File getProjectDir() {
        return projectDir;
    }

    protected abstract String hibernateToolTaskXml();

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
