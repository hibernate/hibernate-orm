/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.sebersole.testkit.Project;
import com.github.sebersole.testkit.ProjectScope;
import com.github.sebersole.testkit.TestKit;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test what we can.  TestKit is better than nothing, but still somewhat limited in what
 * you can test in my experience
 *
 * @author Steve Ebersole
 */
@TestKit
class HibernateOrmPluginTest {
	@Test
	public void testEnhancementTaskAsFinalizer(@Project( "simple" ) ProjectScope projectScope) {
		final GradleRunner gradleRunner = projectScope.createGradleRunner( "clean", "compileJava" );
		final BuildResult result = gradleRunner.build();
		final BuildTask task = result.task( ":hibernateEnhance" );
		assert task != null;

		assertThat(
				task.getOutcome(),
				anyOf( is( TaskOutcome.SUCCESS ), is( TaskOutcome.UP_TO_DATE ) )
		);
	}

	@Test
	public void testEnhancementTask(@Project( "simple" ) ProjectScope projectScope) {
		final GradleRunner gradleRunner = projectScope.createGradleRunner(
				"clean",
				"hibernateEnhance"
		);
		final BuildResult result = gradleRunner.build();
		final BuildTask task = result.task( ":hibernateEnhance" );
		assert task != null;

		assertThat( task.getOutcome(), is( TaskOutcome.SUCCESS ) );
	}

	@Test
	public void testEnhancementTaskUpToDate(@Project( "simple" ) ProjectScope projectScope) {
		final GradleRunner gradleRunner = projectScope.createGradleRunner(
				"clean",
				"hibernateEnhance"
		);
		final BuildResult result = gradleRunner.build();
		final BuildTask task = result.task( ":hibernateEnhance" );
		assert task != null;

		assertThat(
				task.getOutcome(),
				anyOf( is( TaskOutcome.SUCCESS ), is( TaskOutcome.UP_TO_DATE ) )
		);
	}

	@Test
//	@Disabled( "Problem with ClassPathAndModulePathAggregatedServiceLoader and loading Java services" )
	public void testJpaMetamodelGen(@Project( "simple" ) ProjectScope projectScope) {
		final GradleRunner gradleRunner = projectScope.createGradleRunner(
				"clean",
				"generateJpaMetamodel"
		);
		final BuildResult result = gradleRunner.build();
		final BuildTask task = result.task( ":generateJpaMetamodel" );
		assert task != null;

		assertThat(
				task.getOutcome(),
				anyOf( is( TaskOutcome.SUCCESS ), is( TaskOutcome.UP_TO_DATE ) )
		);
	}
}
