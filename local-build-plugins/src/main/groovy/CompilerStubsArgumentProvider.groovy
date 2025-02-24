/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

/**
 * @author Steve Ebersole
 */
class CompilerStubsArgumentProvider implements CommandLineArgumentProvider {
    @InputDirectory
    @PathSensitive(PathSensitivity.NONE)
    File stubsDir

    @Override
    Iterable<String> asArguments() {
        { return ["-Astubs=${stubsDir}"]}
    }
}