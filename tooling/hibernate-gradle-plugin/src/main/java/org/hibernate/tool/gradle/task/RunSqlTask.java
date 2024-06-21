package org.hibernate.tool.gradle.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.tasks.TaskAction;

public class RunSqlTask extends DefaultTask {
	
	private String sqlToRun = "";
	
	public void setSqlToRun(String sqlToRun) {
		this.sqlToRun = sqlToRun;
	}

	@TaskAction
	public void performTask() {
		getLogger().lifecycle("Starting Task 'RunSqlTask'");
		ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(
					new URLClassLoader(
							resolveProjectClassPath(), 
							oldLoader));
			runSql();
		} finally {
			Thread.currentThread().setContextClassLoader(oldLoader);
			getLogger().lifecycle("Ending Task 'RunSqlTask");
		}
	}
	
	private URL[] resolveProjectClassPath() {
		try {
			ConfigurationContainer cc = getProject().getConfigurations();
			Configuration defaultConf = cc.getByName("compileClasspath");
			ResolvedConfiguration resolvedConf = defaultConf.getResolvedConfiguration();
			Set<ResolvedArtifact> ras = resolvedConf.getResolvedArtifacts();
			ResolvedArtifact[] resolvedArtifacts = ras.toArray(new ResolvedArtifact[ras.size()]);
			URL[] urls = new URL[ras.size()];
			for (int i = 0; i < ras.size(); i++) {
				urls[i] = resolvedArtifacts[i].getFile().toURI().toURL();
			}
			return urls;
		} catch (MalformedURLException e) {
			getLogger().error("MalformedURLException while compiling project classpath");
			throw new RuntimeException(e);
		}
	}
	
	private File getPropertyFile() {
		return new File(getProject().getProjectDir(), "src/main/resources/hibernate.properties");
	}

	private Properties loadPropertiesFile(File propertyFile) {
		getLogger().lifecycle("Loading the properties file : " + propertyFile.getPath());
		try (FileInputStream is = new FileInputStream(propertyFile)) {
			Properties result = new Properties();
			result.load(is);
			return result;
		} catch (FileNotFoundException e) {
			throw new BuildException(propertyFile + " not found.", e);
		} catch (IOException e) {
			throw new BuildException("Problem while loading " + propertyFile, e);
		}
	}
	
	private void runSql() {
		Properties properties = loadPropertiesFile(getPropertyFile());
		registerDriver(properties.getProperty("hibernate.connection.driver_class"));
		try {
			Connection connection = DriverManager
					.getConnection(properties.getProperty("hibernate.connection.url"), "sa", "");
			Statement statement = connection.createStatement();
			getLogger().lifecycle("Running SQL: " + sqlToRun);
			statement.execute(sqlToRun);
			statement.close();
			connection.close();
		} catch (SQLException e) {
			getLogger().error("SQLException");
			throw new RuntimeException(e);
		}
	}
	
	private Driver createDelegatingDriver(Driver driver) {
		return (Driver)Proxy.newProxyInstance(
				ClassLoader.getSystemClassLoader(), 
				new Class[] { Driver.class}, 
				new InvocationHandler() {					
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						return method.invoke(driver, args);
					}
				});
	}
	
	private void registerDriver(String driverClassName) {
		getLogger().lifecycle("Registering the database driver");
		try {
			Class<?> driverClass = Thread.currentThread().getContextClassLoader().loadClass(driverClassName);
			Constructor<?> constructor = driverClass.getDeclaredConstructor();
			DriverManager.registerDriver(createDelegatingDriver((Driver)constructor.newInstance()));
		} catch (Exception e) {
			getLogger().error("Exception while registering the database driver: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
}
