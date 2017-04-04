/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.build;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.FixedValue;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * @author Andrea Boriero
 */
public class JavaGradlePluginDisablePublishing {

	public void disable(){
		ByteBuddyAgent.install();
		new ByteBuddy()
				.redefine( JavaGradlePluginPlugin.class )
				.method( named( "configurePublishing" ).and( takesArguments( 1 ) ) )
				.intercept( FixedValue.value( "" ) )
				.make()
				.load( JavaGradlePluginPlugin.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent() );

	}
}
