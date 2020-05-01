package io.freefair.gradle.plugins.aspectj;

import lombok.Data;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.compile.AbstractOptions;
import org.gradle.process.CommandLineArgumentProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Compilation options to be passed to the AspectJ compiler.
 *
 * @author Lars Grefer
 * @see org.gradle.api.tasks.compile.GroovyCompileOptions
 * @see org.gradle.api.tasks.scala.ScalaCompileOptions
 */
@Data
public class AspectJCompileOptions extends AbstractOptions {

    /**
     * Accept as source bytecode any .class files in the .jar files or directories on Path.
     * The output will include these classes, possibly as woven with any applicable aspects.
     * Path is a single argument containing a list of paths to zip files or directories.
     */
    @InputFiles
    @SkipWhenEmpty
    private final ConfigurableFileCollection inpath;

    /**
     * Weave binary aspects from jar files and directories on path into all sources.
     * The aspects should have been output by the same version of the compiler.
     * When running the output classes, the run classpath should contain all aspectpath entries.
     * Path, like classpath, is a single argument containing a list of paths to jar files.
     */
    @Classpath
    private final ConfigurableFileCollection aspectpath;

    /**
     * Put output classes in zip file output.jar.
     */
    @OutputFile
    @Optional
    private final RegularFileProperty outjar;

    /**
     * Generate aop xml file for load-time weaving with default name (META-INF/aop-ajc.xml).
     */
    @Input
    private final Property<Boolean> outxml;

    /**
     * Generate aop.xml file for load-time weaving with custom name.
     */
    @OutputFile
    @Optional
    private final RegularFileProperty outxmlfile;

    /**
     * Find and build all .java or .aj source files under any directory listed in DirPaths.
     * DirPaths, like classpath, is a single argument containing a list of paths to directories.
     */
    @SkipWhenEmpty
    @InputFiles
    private final ConfigurableFileCollection sourceroots;

    /**
     * Generate a build .ajsym file into the output directory.
     * Used for viewing crosscutting references by tools like the AspectJ Browser.
     */
    @Input
    private final Property<Boolean> crossrefs;

    /**
     * Override location of VM's bootclasspath for purposes of evaluating types when compiling.
     * Path is a single argument containing a list of paths to zip files or directories.
     */
    @Classpath
    private final ConfigurableFileCollection bootclasspath;

    /**
     * Override location of VM's extension directories for purposes of evaluating types when compiling.
     * Path is a single argument containing a list of paths to directories.
     */
    @Classpath
    private final ConfigurableFileCollection extdirs;

    /**
     * Specify default source encoding format.
     */
    @Input
    @Optional
    private final Property<String> encoding;

    /**
     * Emit messages about accessed/processed compilation units.
     */
    @Console
    private final Property<Boolean> verbose;

    /**
     * Any additional arguments to be passed to the compiler.
     */
    @Input
    private List<String> compilerArgs = new ArrayList<>();
    private List<CommandLineArgumentProvider> compilerArgumentProviders = new ArrayList<>();

    /**
     * Options for running the compiler in a child process.
     */
    @Internal
    private AjcForkOptions forkOptions = new AjcForkOptions();

    public AspectJCompileOptions(ObjectFactory objectFactory, ConfigurableFileCollection configurableFileCollection, RegularFileProperty regularFileProperty) {
        inpath = configurableFileCollection;
        aspectpath = configurableFileCollection;
        outjar = regularFileProperty;
        outxml = getProperty(objectFactory.property(Boolean.class), false);
        outxmlfile = regularFileProperty;
        sourceroots = configurableFileCollection;
        crossrefs = getProperty(objectFactory.property(Boolean.class), false);
        bootclasspath = configurableFileCollection;
        extdirs = configurableFileCollection;
        encoding = objectFactory.property(String.class);
        verbose = getProperty(objectFactory.property(Boolean.class), false);
    }

    private Property getProperty(Property property, Object defaultValue) {
        if(property != null && property.getOrNull() == null) {
            property.set(defaultValue);
        }

        return property;
    }
}
