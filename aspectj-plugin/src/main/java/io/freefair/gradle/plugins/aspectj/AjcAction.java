package io.freefair.gradle.plugins.aspectj;

import io.freefair.gradle.plugins.aspectj.internal.AspectJCompileSpec;
import io.freefair.gradle.plugins.aspectj.internal.AspectJCompiler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import org.gradle.api.Action;
import org.gradle.api.NonNullApi;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.ClasspathNormalizer;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.process.internal.JavaExecHandleFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Lars Grefer
 * @see AspectJPostCompileWeavingPlugin
 */
@Getter
@NonNullApi
public class AjcAction implements Action<Task> {

    private final ConfigurableFileCollection classpath;

    private final Property<Boolean> enabled;

    private final AspectJCompileOptions options;

    @Getter(AccessLevel.NONE)
    private final JavaExecHandleFactory javaExecHandleFactory;

    @Inject
    public AjcAction(ObjectFactory objectFactory, JavaExecHandleFactory javaExecHandleFactory) {
        options = new AspectJCompileOptions(objectFactory);
        classpath = objectFactory.fileCollection();

        enabled = objectFactory.property(Boolean.class).convention(true);
        this.javaExecHandleFactory = javaExecHandleFactory;
    }


    void addToTask(Task task) {
        task.doLast("ajc", this);
        task.getExtensions().add("ajc", this);

        task.getInputs().files(this.getClasspath())
                .withPropertyName("aspectjClasspath")
                .withNormalizer(ClasspathNormalizer.class)
                .optional(false);

        task.getInputs().files(this.getOptions().getAspectpath())
                .withPropertyName("aspectpath")
                .withNormalizer(ClasspathNormalizer.class)
                .optional(true);

        task.getInputs().files(this.getOptions().getInpath())
                .withPropertyName("ajcInpath")
                .withNormalizer(ClasspathNormalizer.class)
                .optional(true);

        task.getInputs().property("ajcArgs", this.getOptions().getCompilerArgs())
                .optional(true);

        task.getInputs().property("ajcEnabled", this.getEnabled())
                .optional(true);
    }

    @Override
    public void execute(Task task) {
        if (!enabled.getOrElse(true)) {
            return;
        }

        AspectJCompileSpec spec = createSpec((AbstractCompile) task);

        new AspectJCompiler(javaExecHandleFactory).execute(spec);
    }

    private AspectJCompileSpec createSpec(AbstractCompile compile) {
        AspectJCompileSpec spec = new AspectJCompileSpec();

        spec.setDestinationDir(compile.getDestinationDir());
        spec.setWorkingDir(compile.getProject().getProjectDir());
        spec.setTempDir(compile.getTemporaryDir());
        spec.setCompileClasspath(new ArrayList<>(compile.getClasspath().filter(File::exists).getFiles()));
        spec.setTargetCompatibility(compile.getTargetCompatibility());
        spec.setSourceCompatibility(compile.getSourceCompatibility());

        spec.setAspectJClasspath(getClasspath());
        spec.setAspectJCompileOptions(getOptions());

        spec.getAspectJCompileOptions().getInpath().from(compile.getDestinationDir());

        return spec;
    }
}
