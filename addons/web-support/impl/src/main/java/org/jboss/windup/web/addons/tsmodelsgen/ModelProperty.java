package org.jboss.windup.web.addons.tsmodelsgen;

/**
 * @author <a href="mailto:jesse.sightler@gmail.com">Jesse Sightler</a>
 */

import org.jboss.windup.web.addons.websupport.tsmodelgen.TypeScriptModelsGeneratorConfig;

import static org.jboss.windup.web.addons.tsmodelsgen.TsGenUtils.quoteIfNotNull;

/**
 * A property - annotated with @Property in the original model. Can be a {@link java.io.Serializable} too, but in Windup models, it's only primitive
 * types IIRC.
 *
 * @author <a href="http://ondra.zizka.cz/">Ondrej Zizka, zizka@seznam.cz</a>
 */
class ModelProperty extends ModelMember
{
    String graphPropertyName;

    public ModelProperty(String beanPropertyName, String graphPropertyName, PrimitiveType type)
    {
        this.beanPropertyName = beanPropertyName;
        this.graphPropertyName = graphPropertyName;
        this.type = type;
    }

    String toTypeScript(TypeScriptModelsGeneratorConfig.AdjacencyMode mode)
    {
        StringBuilder sb = new StringBuilder();
        if (TypeScriptModelsGeneratorConfig.AdjacencyMode.DECORATED.equals(mode))
            sb.append(String.format("    @GraphProperty(%s)\n", quoteIfNotNull(this.graphPropertyName)));
        sb.append(String.format("    %s: %s;\n", this.beanPropertyName, this.type.getTypeScriptTypeName()));
        return sb.toString();
    }
}
