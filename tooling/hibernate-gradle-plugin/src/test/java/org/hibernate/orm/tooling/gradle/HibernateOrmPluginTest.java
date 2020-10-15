/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.tooling.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test what we can.  ProjectBuilder is better than nothing, but still quited limited in what
 * you can test (e.g. you cannot test task execution).
 *
 * @author Steve Ebersole
 */
class HibernateOrmPluginTest {
	@Test
	public void testEnhancementTaskAsFinalizer() {
		final GradleRunner gradleRunner = TestHelper.gradleRunner( "simple", "compileJava" );
		final BuildResult result = gradleRunner.build();
		final BuildTask task = result.task( ":hibernateEnhance" );

		assertThat( task.getOutcome(), is( TaskOutcome.NO_SOURCE ) );
	}

	@Test
	public void testMetamodeGen() {
		final GradleRunner gradleRunner = TestHelper.gradleRunner( "simple", "generateJpaMetamodel" );
		final BuildResult result = gradleRunner.build();
		final BuildTask task = result.task( ":generateJpaMetamodel" );

		assertThat( task.getOutcome(), is( TaskOutcome.NO_SOURCE ) );
	}

//	@Test
//	public void testHibernateExtensionConfig() {
//		Project project = ProjectBuilder.builder().build()
//		project.plugins.apply 'org.hibernate.orm'
//
//		project.extensions.findByType( HibernateExtension.class ).enhance {
//			enableLazyInitialization = true
//			enableDirtyTracking = true
//			enableAssociationManagement = false
//			enableExtendedEnhancement = false
//		}
//	}
//
//	@Test
//	public void testEnhanceTask() {
//		Project project = ProjectBuilder.builder().build()
//		project.plugins.apply 'org.hibernate.orm'
//
//		def task = project.tasks.create( "finishHim", EnhanceTask )
//
//		task.options {
//			enableLazyInitialization = true
//			enableDirtyTracking = true
//			enableAssociationManagement = false
//			enableExtendedEnhancement = false
//		}
//
//		task.sourceSets = project.getConvention().getPlugin( JavaPluginConvention ).sourceSets.main
//
//		task.enhance()
//	}
//
//	@Test
//	public void testTaskAction() {
//		Project project = ProjectBuilder.builder().build()
//		project.plugins.apply 'org.hibernate.orm'
//
//		// the test sourceSet
//		def sourceSet = project.getConvention().getPlugin( JavaPluginConvention ).sourceSets.test;
//
//		// The compile task for the test sourceSet
//		final JavaCompile compileTestTask = project.getTasks().findByName( sourceSet.getCompileJavaTaskName() );
//
//		// Lets add our enhancer to enhance the test classes after the test are compiled
//		compileTestTask.doLast {
//			EnhancementHelper.enhance(
//					sourceSet,
//					project.extensions.findByType( HibernateExtension.class ).enhance,
//					project
//			)
//		}
//
//		// TODO find how to do this in Gradle 5
//		//  the class-level javadoc says it's not possible, and it was there in Gradle 4.x...
//		//compileTestTask.execute()
//	}
}
