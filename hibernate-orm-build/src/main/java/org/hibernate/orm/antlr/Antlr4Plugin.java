/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.antlr;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;

/**
 * Custom Antlr v4 Plugin
 *
 * The Gradle-supplied Antlr plugin attempts to simultaneously support multiple
 * versions of Antlr which leads to many difficulties. This custom plugin provides
 * dedicated and simplified support for Antlr v4
 *
 * @author Steve Ebersole
 */
public class Antlr4Plugin implements Plugin<Project> {
	public static final String HQL_PKG = "org.hibernate.grammars.hql";
	public static final String IMPORT_SQL_PKG = "org.hibernate.grammars.importsql";
	public static final String GRAPH_PKG = "org.hibernate.grammars.graph";
	public static final String ORDER_PKG = "org.hibernate.grammars.ordering";
	public static final String ANTLR = "antlr";

	public final GrammarDescriptor[] grammarDescriptors = new GrammarDescriptor[] {
			new GrammarDescriptor( "HqlLexer", HQL_PKG ),
			new GrammarDescriptor( "HqlParser", HQL_PKG ),
			new GrammarDescriptor( "SqlScriptLexer", IMPORT_SQL_PKG ),
			new GrammarDescriptor( "SqlScriptParser", IMPORT_SQL_PKG ),
			new GrammarDescriptor( "GraphLanguageLexer", GRAPH_PKG ),
			new GrammarDescriptor( "GraphLanguageParser", GRAPH_PKG ),
			new GrammarDescriptor( "OrderingLexer", ORDER_PKG ),
			new GrammarDescriptor( "OrderingParser", ORDER_PKG )
	};

	@Override
	public void apply(Project project) {
		final Antlr4Spec antlr4Spec = project.getExtensions().create(
				Antlr4Spec.REGISTRATION_NAME,
				Antlr4Spec.class
		);

		final Configuration antlrDependencies = project.getConfigurations().maybeCreate( ANTLR );

		final Task groupingTask = project.getTasks().create( ANTLR );
		groupingTask.setDescription( "Performs all defined Antlr grammar generations" );
		groupingTask.setGroup( ANTLR );

		for ( GrammarDescriptor grammarDescriptor : grammarDescriptors ) {
			final GeneratorTask generatorTask = project.getTasks().create(
					"generate" + grammarDescriptor.getGrammarName() + "Grammar",
					GeneratorTask.class,
					grammarDescriptor,
					antlr4Spec
			);
			generatorTask.setDescription( "Performs Antlr grammar generation for `" + grammarDescriptor.getGrammarName() + "`" );
			generatorTask.setGroup( ANTLR );
			groupingTask.dependsOn( generatorTask );
		}

		final SourceSet mainSourceSet = project.getConvention()
				.getPlugin( JavaPluginConvention.class )
				.getSourceSets()
				.getByName( SourceSet.MAIN_SOURCE_SET_NAME );
		mainSourceSet.setCompileClasspath( mainSourceSet.getCompileClasspath().plus( antlrDependencies ) );
		mainSourceSet.getJava().srcDir( antlr4Spec.getOutputBaseDirectory() );

		final Task compileTask = project.getTasks().getByName( mainSourceSet.getCompileJavaTaskName() );
		compileTask.dependsOn( groupingTask );

//		SourceSet testSourceSet = project.convention.getPlugin( JavaPluginConvention ).sourceSets.getByName( SourceSet.TEST_SOURCE_SET_NAME );
//		testSourceSet.compileClasspath += configurations.antlr
	}
}
