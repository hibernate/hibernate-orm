/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.gradle.task;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.gradle.api.tasks.TaskAction;

public class RunSqlTask extends AbstractTask {
	
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
