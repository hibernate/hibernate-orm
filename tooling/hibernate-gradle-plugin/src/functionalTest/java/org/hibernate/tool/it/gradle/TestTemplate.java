package org.hibernate.tool.it.gradle;

import java.io.File;

import org.junit.jupiter.api.io.TempDir;

public class TestTemplate {

    @TempDir
    private File projectDir;

    protected File getProjectDir() {
        return projectDir;
    }

}
