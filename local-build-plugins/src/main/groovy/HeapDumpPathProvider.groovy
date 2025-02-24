/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.OutputDirectory
import org.gradle.process.CommandLineArgumentProvider

/**
 * @author Steve Ebersole
 */
class HeapDumpPathProvider implements CommandLineArgumentProvider {
    @OutputDirectory
    Provider<Directory> path

    @Override
    Iterable<String> asArguments() {
        ["-XX:HeapDumpPath=${path.get().asFile.absolutePath}"]
    }
}
