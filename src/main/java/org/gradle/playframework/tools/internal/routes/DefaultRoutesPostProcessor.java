package org.gradle.playframework.tools.internal.routes;

import org.gradle.util.RelativePathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

class DefaultRoutesPostProcessor implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRoutesPostProcessor.class);

    void execute(RoutesCompileSpec spec) {
        String sourceReplacementString = getSourceReplacementString(spec.getSources(), spec.getProjectDir());

        try (Stream<Path> stream = Files.find(spec.getDestinationDir().toPath(), Integer.MAX_VALUE, (filePath, fileAttr) -> fileAttr.isRegularFile())) {
            stream.forEach(routeFile -> process(routeFile, sourceReplacementString));
        } catch (IOException e) {
            LOGGER.warn("Unable to post-process routes", e);
        }
    }

    private String getSourceReplacementString(Iterable<File> sources, File projectDir) {
        String sourceReplacementString = "";

        if(sources.iterator().hasNext()) {
            File sourceFile = sources.iterator().next();
            sourceReplacementString = "// @(SOURCE):" + RelativePathUtil.relativePath(projectDir, sourceFile);
        }

        return sourceReplacementString;
    }

    private void process(Path routeFile, String sourceReplacementString) {
        try {
            String content = new String(Files.readAllBytes(routeFile), StandardCharsets.UTF_8);
            content = content.replaceAll("(?m)^// @(SOURCE):.*", sourceReplacementString);
            content = content.replaceAll("(?m)^// @(DATE):.*", "");
            Files.write(routeFile, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.warn(String.format("Unable to post-process route file %s", routeFile.getFileName()), e);
        }
    }
}
