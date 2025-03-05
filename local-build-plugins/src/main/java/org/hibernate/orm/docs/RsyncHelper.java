/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.docs;

import org.gradle.api.Project;
import org.gradle.api.file.FileSystemLocation;

/**
 * Helper for performing rsync system commands, mainly used to centralize
 * the command options
 *
 * @author Steve Ebersole
 */
public class RsyncHelper {
	public static void rsync(
			FileSystemLocation source,
			String targetUrl,
			Project project) {
		project.exec( (exec) -> {
			exec.executable( "rsync" );
			exec.args( "--port=2222", "-avz", source.getAsFile().getAbsolutePath(), targetUrl );
		} );
	}
}
