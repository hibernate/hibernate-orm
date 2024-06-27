package org.hibernate.tool.gradle.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.gradle.api.tasks.TaskAction;

public class RunSqlTask extends AbstractTask {
	
	private Properties hibernateProperties = null;
	
	private String getHibernateProperty(String name) {
		if (hibernateProperties == null) {
			loadPropertiesFile(getPropertyFile());
		}
		return hibernateProperties.getProperty(name);
	}
	
	private File getPropertyFile() {
		return new File(getProject().getProjectDir(), "src/main/resources/hibernate.properties");
	}

	private void loadPropertiesFile(File propertyFile) {
		getLogger().lifecycle("Loading the properties file : " + propertyFile.getPath());
		try (FileInputStream is = new FileInputStream(propertyFile)) {
			hibernateProperties = new Properties();
			hibernateProperties.load(is);
			getLogger().lifecycle("Properties file is loaded");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new BuildException(propertyFile + " not found.", e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new BuildException("Problem while loading " + propertyFile, e);
		}
	}
	
	@TaskAction
	public void performTask() {
		super.perform();
	}
	
	void doWork() {
		registerDriver();
		runSql();
	}
	
	private void registerDriver() {
		String driverClassName = getHibernateProperty("hibernate.connection.driver_class");
		getLogger().lifecycle("Registering the database driver: " + driverClassName);
		try {
			Class<?> driverClass = Thread.currentThread().getContextClassLoader().loadClass(driverClassName);
			Constructor<?> constructor = driverClass.getDeclaredConstructor();
			DriverManager.registerDriver(createDelegatingDriver((Driver)constructor.newInstance()));
			getLogger().lifecycle("Database driver is registered");
		} catch (Exception e) {
			getLogger().error("Exception while registering the database driver: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	private void runSql() {
		try {
			String databaseUrl = getHibernateProperty("hibernate.connection.url");
			getLogger().lifecycle("Connecting to database: " + databaseUrl);
			Connection connection = DriverManager
					.getConnection(databaseUrl, "sa", "");
			Statement statement = connection.createStatement();
			getLogger().lifecycle("Running SQL: " + getExtension().sqlToRun);
			statement.execute(getExtension().sqlToRun);
			statement.close();
			connection.close();
		} catch (SQLException e) {
			getLogger().error("SQLException");
			throw new RuntimeException(e);
		}
	}
		
	private Driver createDelegatingDriver(Driver driver) {
		return (Driver)Proxy.newProxyInstance(
				DriverManager.class.getClassLoader(), 
				new Class[] { Driver.class}, 
				new InvocationHandler() {					
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						return method.invoke(driver, args);
					}
				});
	}
	
}
