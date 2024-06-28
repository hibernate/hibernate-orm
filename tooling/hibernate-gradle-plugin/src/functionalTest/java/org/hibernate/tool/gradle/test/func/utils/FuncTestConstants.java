package org.hibernate.tool.gradle.test.func.utils;

public interface FuncTestConstants {

    public static final String DATABASE_NAME = "bardb";
    public static final String DATABASE_FILE_NAME = DATABASE_NAME + ".mv.db";
    public static final String DATABASE_FOLDER_NAME = "database";
    public static final String DATABASE_PATH = DATABASE_FOLDER_NAME + "/" + DATABASE_NAME;
    public static final String HIBERNATE_PROPERTIES_FILE_NAME = "hibernate.properties";
    public static final String RESOURCES_FOLDER_PATH = "src/main/resources";
    public static final String GRADLE_BUILD_FILE_NAME = "build.gradle";
    public static final String GRADLE_SETTINGS_FILE_NAME = "settions.gradle";
    public static final String PROJECT_DIR_PLACEHOLDER = "${projectDir}";

    public static final String HIBERNATE_PROPERTIES_CONTENTS = 
    		"hibernate.connection.driver_class=org.h2.Driver\n" +
    	    "hibernate.connection.url=jdbc:h2:" + PROJECT_DIR_PLACEHOLDER + "/" + DATABASE_PATH + "\n" +
    	    "hibernate.connection.username=sa\n" +
    	    "hibernate.connection.password=\n" 
    ;
    
    public static final String BUILD_FILE_PLUGINS_SECTION = 
            "plugins {\n" +
            "  id('application')\n" +
            "  id('org.hibernate.tool.hibernate-tools-gradle')\n" +
            "}\n";

    public static final String BUILD_FILE_REPOSITORIES_SECTION = 
            "repositories {\n" +
            "  mavenCentral()\n" +
            "}\n";

    public static final String BUILD_FILE_DEPENDENCIES_SECTION = 
            "dependencies {\n" +
            "  implementation('com.h2database:h2:2.1.214')\n" +
            "}\n";
    
    default String getBuildFilePluginsSection() {
    	return BUILD_FILE_PLUGINS_SECTION;
    }

    default String getBuildFileRepositoriesSection() {
    	return BUILD_FILE_REPOSITORIES_SECTION;
    }

    default String getBuildFileDependenciesSection() {
    	return BUILD_FILE_DEPENDENCIES_SECTION;
    }
    
    default String getBuildFileHibernateToolsSection() {
    	return "";
    }

    default String getBuildFileContents() {
    	return getBuildFilePluginsSection() +
                getBuildFileRepositoriesSection() +
                getBuildFileDependenciesSection() +
                getBuildFileHibernateToolsSection();
    }
    
    default String getHibernatePropertiesFileName() {
    	return HIBERNATE_PROPERTIES_FILE_NAME;
    }
    
}
