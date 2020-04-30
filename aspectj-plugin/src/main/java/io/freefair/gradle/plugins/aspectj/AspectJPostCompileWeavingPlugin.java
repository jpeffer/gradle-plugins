package io.freefair.gradle.plugins.aspectj;

import io.freefair.gradle.plugins.aspectj.internal.DefaultWeavingSourceSet;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.plugins.DslObject;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.scala.ScalaBasePlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.api.tasks.compile.GroovyCompile;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.scala.ScalaCompile;

@NonNullApi
public class AspectJPostCompileWeavingPlugin implements Plugin<Project> {

    private Project project;
    private AspectJBasePlugin aspectjBasePlugin;

    @Override
    public void apply(Project project) {
        this.project = project;
        aspectjBasePlugin = project.getPlugins().apply(AspectJBasePlugin.class);

        project.getPlugins().apply(JavaBasePlugin.class);

        project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets().all(this::configureSourceSet);
    }

    private void configureSourceSet(SourceSet sourceSet) {
        project.afterEvaluate(p ->
                p.getDependencies().add(sourceSet.getCompileConfigurationName(), "org.aspectj:aspectjrt:" + aspectjBasePlugin.getAspectjExtension().getVersion().get())
        );

        DefaultWeavingSourceSet weavingSourceSet = new DefaultWeavingSourceSet(sourceSet);
        new DslObject(sourceSet).getConvention().add("aspectj", weavingSourceSet);

        Configuration aspectpath = project.getConfigurations().create(weavingSourceSet.getAspectConfigurationName());
        weavingSourceSet.setAspectPath(aspectpath);

        Configuration inpath = project.getConfigurations().create(weavingSourceSet.getInpathConfigurationName());
        weavingSourceSet.setInPath(inpath);

        project.getConfigurations().getByName(sourceSet.getCompileConfigurationName())
                .extendsFrom(aspectpath);

        project.getConfigurations().getByName(sourceSet.getCompileOnlyConfigurationName()).extendsFrom(inpath);

        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
            JavaCompile compileJava = (JavaCompile) project.getTasks().getByName(sourceSet.getCompileJavaTaskName());

            enhanceWithWeavingAction(compileJava, aspectpath, inpath, aspectjBasePlugin.getAspectjConfiguration());
        });

        project.getPlugins().withType(GroovyPlugin.class, groovyPlugin -> {
            GroovyCompile compileGroovy = (GroovyCompile) project.getTasks().getByName(sourceSet.getCompileTaskName("groovy"));

            enhanceWithWeavingAction(compileGroovy, aspectpath, inpath, aspectjBasePlugin.getAspectjConfiguration());
        });

        project.getPlugins().withType(ScalaBasePlugin.class, scalaBasePlugin -> {
            ScalaCompile compileScala = (ScalaCompile) project.getTasks().getByName(sourceSet.getCompileTaskName("scala"));

            enhanceWithWeavingAction(compileScala, aspectpath, inpath, aspectjBasePlugin.getAspectjConfiguration());
        });
    }

    private void enhanceWithWeavingAction(AbstractCompile abstractCompile, Configuration aspectpath, Configuration inpath, Configuration aspectjConfiguration) {
        AjcAction action = project.getObjects().newInstance(AjcAction.class);

        action.getOptions().getAspectpath().from(aspectpath);
        action.getOptions().getInpath().from(inpath);
        action.getClasspath().from(aspectjConfiguration);

        action.addToTask(abstractCompile);
    }

}
