package org.hibernate.tool.it.ant;

import org.junit.jupiter.api.io.TempDir;

import java.io.File;

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
