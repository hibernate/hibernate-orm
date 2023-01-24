/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties.processor;

import static org.hibernate.orm.properties.processor.AnnotationUtils.findAnnotation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ConfigurationPropertyCollector {

	// assume that spi/impl/internal packages are not for public use and consider all of them as SPI:
	private static final Pattern SPI_PATTERN = Pattern.compile(
			"(.*\\.spi$)|(.*\\.spi\\..*)|(.*\\.impl$)|(.*\\.impl\\..*)|(.*\\.internal$)|(.*\\.internal\\..*)" );

	private final Set<Name> processedTypes = new HashSet<>();
	private final ConfigPropertyHolder properties;
	private final Elements elementUtils;
	private final String title;
	private final String anchor;
	private final Path javadocsLocation;
	private final String javadocsBaseLink;
	private final Pattern ignoreKeys;
	private final Messager messager;

	public ConfigurationPropertyCollector(ConfigPropertyHolder properties, ProcessingEnvironment processingEnvironment,
			String title,
			String anchor, Path javadocsLocation, Pattern ignoreKeys,
			String javadocsBaseLink) {
		this.properties = properties;
		this.elementUtils = processingEnvironment.getElementUtils();
		this.title = title;
		this.anchor = anchor;
		this.javadocsLocation = javadocsLocation;
		this.javadocsBaseLink = javadocsBaseLink;
		this.ignoreKeys = ignoreKeys;
		this.messager = processingEnvironment.getMessager();
	}

	public void visitType(TypeElement element) {
		Name qualifiedName = element.getQualifiedName();
		if ( !processedTypes.contains( qualifiedName ) ) {
			processedTypes.add( qualifiedName );

			Optional<AnnotationUtils.AnnotationAttributeHolder> annotation = findAnnotation(
					element, HibernateOrmConfiguration.class );
			Optional<List<String>> classPrefix = annotation
					.flatMap( a -> a.multiAttribute( "prefix", String.class ) );
			Optional<String> title = annotation.flatMap( a -> a.attribute( "title", String.class ) );
			Optional<String> anchorPrefix = annotation.flatMap( a -> a.attribute( "anchorPrefix", String.class ) );

			for ( Element inner : elementUtils.getAllMembers( element ) ) {
				if ( inner.getKind().equals( ElementKind.FIELD ) && inner instanceof VariableElement ) {
					processConstant( ( (VariableElement) inner ), classPrefix, title, anchorPrefix );
				}
			}
		}
	}

	private void processConstant(VariableElement constant, Optional<List<String>> classPrefix,
			Optional<String> classTitle,
			Optional<String> classAnchorPrefix) {
		Optional<AnnotationUtils.AnnotationAttributeHolder> annotation = findAnnotation(
				constant, HibernateOrmConfiguration.class );
		if ( annotation.flatMap( a -> a.attribute( "ignore", Boolean.class ) ).orElse( false ) ) {
			return;
		}

		Optional<String> title = annotation.flatMap( a -> a.attribute( "title", String.class ) );
		Optional<String> anchorPrefix = annotation.flatMap( a -> a.attribute( "anchorPrefix", String.class ) );

		ConfigurationProperty.Key key = extractKey(
				constant,
				classPrefix,
				annotation.flatMap( a -> a.multiAttribute( "prefix", String.class ) )
		);
		if ( !key.matches( ignoreKeys ) ) {
			properties.put(
					constant.getEnclosingElement().toString() + "#" + constant.getSimpleName().toString(),
					new ConfigurationProperty()
							.javadoc( extractJavadoc( constant ) )
							.key( key )
							.sourceClass( constant.getEnclosingElement().toString() )
							.type( extractType( constant ) )
							.withModuleName( title.orElse( classTitle.orElse( this.title ) ) )
							.withAnchorPrefix( anchorPrefix.orElse( classAnchorPrefix.orElse( this.anchor ) ) )
			);
		}
	}


	private ConfigurationProperty.Key extractKey(VariableElement constant, Optional<List<String>> classPrefix,
			Optional<List<String>> constantPrefix) {
		List<String> prefix;
		if ( constantPrefix.isPresent() ) {
			prefix = constantPrefix.get();
		}
		else if ( classPrefix.isPresent() ) {
			prefix = classPrefix.get();
		}
		else {
			prefix = Collections.emptyList();
		}

		return new ConfigurationProperty.Key(
				prefix,
				Objects.toString( constant.getConstantValue(), "NOT_FOUND#" + constant.getSimpleName() )
		);
	}

	private HibernateOrmConfiguration.Type extractType(VariableElement constant) {
		String packageName = packageElement( constant ).getQualifiedName().toString();
		return SPI_PATTERN.matcher( packageName ).matches() ?
				HibernateOrmConfiguration.Type.SPI :
				HibernateOrmConfiguration.Type.API;
	}

	private String extractJavadoc(VariableElement constant) {
		try {
			Element enclosingClass = constant.getEnclosingElement();
			Path docs = javadocsLocation.resolve(
					enclosingClass.toString().replace( ".", File.separator ) + ".html"
			);

			String packagePath = packageElement( enclosingClass ).getQualifiedName().toString().replace( ".", File.separator );

			Document javadoc = Jsoup.parse( docs.toFile() );

			org.jsoup.nodes.Element block = javadoc.selectFirst( "#" + constant.getSimpleName() + " + ul li.blockList");
			if ( block != null ) {
				for ( org.jsoup.nodes.Element link : block.getElementsByTag( "a" ) ) {
					String href = link.attr( "href" );
					// only update links if they are not external:
					if ( !link.hasClass( "external-link" ) ) {
						if ( href.startsWith( "#" ) ) {
							href = enclosingClass.getSimpleName().toString() + ".html" + href;
						}
						href = javadocsBaseLink + packagePath + "/" + href;
					}
					else if ( href.contains( "/build/parents/" ) && href.contains( "/apidocs" ) ) {
						// means a link was to a class from other module and javadoc plugin generated some external link
						// that won't work. So we replace it:
						href = javadocsBaseLink + href.substring( href.indexOf( "/apidocs" ) + "/apidocs".length() );
					}
					link.attr( "href", href );
				}

				org.jsoup.nodes.Element result = new org.jsoup.nodes.Element( "div" );
				for ( org.jsoup.nodes.Element child : block.children() ) {
					if ( "h4".equalsIgnoreCase( child.tagName() ) || "pre".equalsIgnoreCase( child.tagName() ) ) {
						continue;
					}
					result.appendChild( child );
				}

				return result.toString();
			}
			else {
				return elementUtils.getDocComment( constant );
			}
		}
		catch (IOException e) {
			messager.printMessage( Diagnostic.Kind.NOTE, "Wasn't able to find rendered javadocs for " + constant + ". Trying to read plain javadoc comment." );
			return elementUtils.getDocComment( constant );
		}
	}

	private PackageElement packageElement(Element element) {
		Element packageElement = element;
		while ( !( packageElement instanceof PackageElement ) && packageElement.getEnclosingElement() != null ) {
			packageElement = packageElement.getEnclosingElement();
		}

		return packageElement instanceof PackageElement ? (PackageElement) packageElement : null;
	}

}
