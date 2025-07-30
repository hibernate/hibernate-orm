package org.hibernate.tool.it.ant;

import org.junit.jupiter.api.io.TempDir;

import java.io.File;

public class TestTemplate {

    @TempDir
    private File projectDir;

    protected File getProjectDir() {
        return projectDir;
    }

}
