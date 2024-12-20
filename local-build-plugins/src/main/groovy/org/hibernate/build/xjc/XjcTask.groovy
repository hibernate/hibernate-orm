package org.hibernate.build.xjc

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*

/**
 * @author Steve Ebersole
 */
@CacheableTask
class XjcTask extends DefaultTask {
	private final DirectoryProperty outputDirectory

	private final RegularFileProperty xsdFile
	private final RegularFileProperty xjcBindingFile
	private final SetProperty<String> xjcExtensions

	XjcTask() {
		xsdFile = project.getObjects().fileProperty()
		xjcBindingFile = project.getObjects().fileProperty()
		xjcExtensions = project.objects.setProperty( String.class )

		outputDirectory = project.objects.directoryProperty()
	}

	@InputFile
	@PathSensitive( PathSensitivity.RELATIVE )
	RegularFileProperty getXsdFile() {
		return xsdFile
	}

	@InputFile
	@PathSensitive( PathSensitivity.RELATIVE )
	RegularFileProperty getXjcBindingFile() {
		return xjcBindingFile
	}

	@Input
	SetProperty<String> getXjcExtensions() {
		return xjcExtensions
	}

	@OutputDirectory
	DirectoryProperty getOutputDirectory() {
		return outputDirectory
	}

	@TaskAction
	void generateJaxbBindings() {
		project.delete( outputDirectory.get().asFileTree )

		project.ant.xjc(
				destdir: outputDirectory.get().asFile.absolutePath,
				binding: xjcBindingFile.get().asFile.absolutePath,
				schema: xsdFile.get().asFile.absolutePath,
				extension: 'true') {
			project.ant.arg line: '-no-header'
			project.ant.arg line: '-npa'

			if ( xjcExtensions.isPresent() ) {
				def extensionsToEnable = xjcExtensions.get()
				if ( ! extensionsToEnable.isEmpty() ) {
					def extensionsSwitches = extensionsToEnable.collect { "-X${it}" }.join( " " )
					project.ant.arg line: extensionsSwitches
				}
			}
		}
	}
}
