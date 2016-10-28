package org.jboss.windup.web.addons.websupport.tsmodelgen;

/**
 * @author <a href="mailto:jesse.sightler@gmail.com">Jesse Sightler</a>
 */
public interface TypeScriptModelsGeneratingService
{
    /**
     *  Generates typescript models based upon the provided configuration.
     */
    void generate(TypeScriptModelsGeneratorConfig config);
}
