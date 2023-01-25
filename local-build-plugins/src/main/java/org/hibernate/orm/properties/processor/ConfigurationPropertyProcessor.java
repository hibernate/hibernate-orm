/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties.processor;


import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;


@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({
		Configuration.JAVADOC_LINK,
		Configuration.IGNORE_PATTERN,
		Configuration.IGNORE_KEY_VALUE_PATTERN,
		Configuration.MODULE_TITLE,
		Configuration.MODULE_LINK_ANCHOR
})
public class ConfigurationPropertyProcessor extends AbstractProcessor {

	private ConfigurationPropertyCollector propertyCollector;
	private Optional<Pattern> ignore;
	private final Path javadocFolder;
	private final ConfigPropertyHolder properties;

	public ConfigurationPropertyProcessor(Path javadocFolder, ConfigPropertyHolder properties) {
		this.javadocFolder = javadocFolder;
		this.properties = properties;
	}


	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init( processingEnv );

		String pattern = processingEnv.getOptions().get( Configuration.IGNORE_PATTERN );
		this.ignore = Optional.ofNullable( pattern ).map( Pattern::compile );
		String title = processingEnv.getOptions().getOrDefault( Configuration.MODULE_TITLE, "Unknown" );
		String anchor = processingEnv.getOptions().getOrDefault( Configuration.MODULE_LINK_ANCHOR, "hibernate-orm-" );

		String javadocsBaseLink = processingEnv.getOptions().getOrDefault( Configuration.JAVADOC_LINK, "" );

		String keyPattern = processingEnv.getOptions().getOrDefault( Configuration.IGNORE_KEY_VALUE_PATTERN, ".*\\.$" );
		Pattern ignoreKeys = Pattern.compile( keyPattern );

		this.propertyCollector = new ConfigurationPropertyCollector(
				properties,
				processingEnv,
				title,
				anchor,
				javadocFolder,
				ignoreKeys,
				javadocsBaseLink
		);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		Set<? extends Element> rootElements = roundEnv.getRootElements();

		// first let's go through all root elements and see if we can find *Settings classes:
		for ( Element element : rootElements ) {
			if ( isSettingsClass( element ) ) {
				process( propertyCollector, element );
			}
		}

		if ( roundEnv.processingOver() ) {
			beforeExit();
		}

		return true;
	}

	private void beforeExit() {
		// processor won't generate anything another gradle task would create an asciidoc file.
	}

	private void process(ConfigurationPropertyCollector propertyCollector, Element element) {
		if ( !ignore.map( p -> p.matcher( element.toString() ).matches() ).orElse( Boolean.FALSE ) ) {
			propertyCollector.visitType( (TypeElement) element );
		}
	}

	private boolean isSettingsClass(Element element) {
		return ( element.getKind().equals( ElementKind.CLASS ) || element.getKind().equals( ElementKind.INTERFACE ) ) &&
				element.getSimpleName().toString().endsWith( "Settings" );
	}
}
