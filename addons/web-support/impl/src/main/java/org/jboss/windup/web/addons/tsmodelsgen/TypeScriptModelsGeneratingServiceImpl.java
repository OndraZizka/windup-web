package org.jboss.windup.web.addons.tsmodelsgen;

import java.util.Set;
import javax.inject.Inject;
import org.jboss.windup.graph.GraphTypeManager;
import org.jboss.windup.graph.model.WindupFrame;
import org.jboss.windup.web.addons.websupport.tsmodelgen.TypeScriptModelsGeneratingService;
import org.jboss.windup.web.addons.websupport.tsmodelgen.TypeScriptModelsGeneratorConfig;

/**
 * The service to generate the typescript models.
 * 
 * @author <a href="http://ondra.zizka.cz/">Ondrej Zizka, zizka@seznam.cz</a>
 */
public class TypeScriptModelsGeneratingServiceImpl implements TypeScriptModelsGeneratingService
{
    @Inject
    private GraphTypeManager graphTypeManager;

    /**
     *  Generates typescript models based upon the provided configuration.
     */
    @Override
    public void generate(TypeScriptModelsGeneratorConfig config)
    {
        Set<Class<? extends WindupFrame<?>>> modelTypes = graphTypeManager.getRegisteredTypes();
        try
        {
            new TypeScriptModelsGenerator(config).generate(modelTypes);
        }
        catch (Exception ex)
        {
            throw new RuntimeException("Failed generating TypeScript models:\n\t" + ex.getMessage(), ex);
        }
    }
}
